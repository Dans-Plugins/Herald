package com.dansplugins.herald;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordNotifier implements Notifier {
    
    static final List<String> DEFAULT_JOIN_MESSAGES = List.of(
            "\u2694\uFE0F Hear ye, hear ye! **{player}** hath entered the realm of **{server}**! \u2694\uFE0F",
            "\uD83C\uDFF0 The gates of **{server}** open wide for **{player}**! Welcome, brave soul!",
            "\uD83D\uDCDC By royal decree, **{player}** hath been granted passage into **{server}**!",
            "\uD83D\uDDE1\uFE0F A new champion approaches! **{player}** rides into **{server}**!",
            "\uD83C\uDF1F The bards shall sing of this day! **{player}** hath arrived in **{server}**!",
            "\uD83D\uDC51 All hail **{player}**, who now graces the lands of **{server}**!",
            "\uD83D\uDD25 The torches flicker as **{player}** strides into **{server}**!",
            "\uD83C\uDFBA Sound the trumpets! **{player}** hath joined the kingdom of **{server}**!",
            "\uD83D\uDEE1\uFE0F The defenders of **{server}** welcome **{player}** to their ranks!",
            "\u2728 By the stars above, **{player}** hath made their presence known in **{server}**!"
    );
    
    /** Maximum number of characters of an error response body included in failure messages. */
    static final int MAX_ERROR_BODY_LENGTH = 500;

    /** Maximum number of characters Discord accepts in the content of a webhook message. */
    static final int MAX_MESSAGE_LENGTH = 2000;

    /** Longest a Minecraft Java Edition player name can be, which sizes the worst case of a template. */
    static final int MAX_PLAYER_NAME_LENGTH = 16;

    /** The status Discord answers with when the webhook has been asked for too much, too quickly. */
    static final int RATE_LIMITED_STATUS = 429;

    /**
     * Longest Herald waits out a rate limit before giving up on a message.
     * A rate-limited send is retried from an asynchronous task, so waiting costs
     * nothing the server thread notices, but an unbounded wait would let one
     * pathological {@code Retry-After} park that task indefinitely.
     */
    static final long MAX_RETRY_AFTER_MILLIS = 10_000L;

    /** How long to wait when a rate-limited response states no usable delay of its own. */
    static final long DEFAULT_RETRY_AFTER_MILLIS = 1_000L;

    /** The {@code retry_after} field Discord repeats in the JSON body of a rate-limited response. */
    private static final Pattern RETRY_AFTER_FIELD =
            Pattern.compile("\"retry_after\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    /** The placeholder replaced with the name of the player who joined. */
    private static final String PLAYER_PLACEHOLDER = "{player}";

    /** The placeholder replaced with the name of the server they joined. */
    private static final String SERVER_PLACEHOLDER = "{server}";

    private final String webhookUrl;
    private final List<String> joinMessages;
    private final Random random;
    private final Logger logger;

    public DiscordNotifier(String webhookUrl, List<String> joinMessages) {
        this(webhookUrl, joinMessages, new Random());
    }

    /**
     * Build a notifier that reports transient trouble through the given logger.
     * The plugin logger is passed here so that a rate-limit warning reaches the
     * server log under the same {@code Herald} prefix as every other line
     * Herald writes, without this class having to know about Bukkit.
     *
     * @param webhookUrl   the configured {@code discord.webhook-url}
     * @param joinMessages the configured {@code discord.join-messages}, or {@code null} for the defaults
     * @param logger       the logger transient trouble is reported through
     */
    public DiscordNotifier(String webhookUrl, List<String> joinMessages, Logger logger) {
        this(webhookUrl, joinMessages, new Random(), logger);
    }

    DiscordNotifier(String webhookUrl, List<String> joinMessages, Random random) {
        this(webhookUrl, joinMessages, random, Logger.getLogger(DiscordNotifier.class.getName()));
    }

    DiscordNotifier(String webhookUrl, List<String> joinMessages, Random random, Logger logger) {
        this.webhookUrl = webhookUrl;
        this.joinMessages = (joinMessages != null && !joinMessages.isEmpty())
                ? Collections.unmodifiableList(new ArrayList<>(joinMessages))
                : DEFAULT_JOIN_MESSAGES;
        this.random = random;
        // Defaulted here rather than trusted, because the only code path that reads it
        // is the rate-limited one, so a null would surface as a failure to survive the
        // very condition the retry exists for.
        this.logger = logger != null ? logger : Logger.getLogger(DiscordNotifier.class.getName());
    }

    /**
     * Check the configuration keys Discord notifications require, against the
     * built-in join messages and a server name sized as if it were empty.
     *
     * @param webhookUrl the configured {@code discord.webhook-url}
     * @return a list of human-readable problems, empty when the configuration is complete
     * @see #validateConfiguration(String, List, String)
     */
    public static List<String> validateConfiguration(String webhookUrl) {
        return validateConfiguration(webhookUrl, null);
    }

    /**
     * Check the configuration keys Discord notifications require, sizing the
     * templates as if the server name were empty.
     *
     * @param webhookUrl   the configured {@code discord.webhook-url}
     * @param joinMessages the configured {@code discord.join-messages}, or {@code null} for the defaults
     * @return a list of human-readable problems, empty when the configuration is complete
     * @see #validateConfiguration(String, List, String)
     */
    public static List<String> validateConfiguration(String webhookUrl, List<String> joinMessages) {
        return validateConfiguration(webhookUrl, joinMessages, null);
    }

    /**
     * Check the configuration keys Discord notifications require.
     * Callers use this to report every missing or unusable key at startup
     * instead of failing once per player join. A syntactically invalid URL is
     * reported here because {@link #sendMessage(String)} would otherwise only
     * discover it when the first player joins, and a blank message template is
     * reported for the same reason: Discord rejects an empty message, so a
     * blank entry fails on the joins that happen to draw it and no others. A
     * template long enough to breach {@link #MAX_MESSAGE_LENGTH} once filled is
     * reported for the same reason again, which is why the server name that
     * will fill its {@code {server}} placeholder is needed here.
     *
     * @param webhookUrl   the configured {@code discord.webhook-url}
     * @param joinMessages the configured {@code discord.join-messages}, or {@code null} for the defaults
     * @param serverName   the resolved server name the {@code {server}} placeholder will be
     *                     filled with, or {@code null} to size the templates as if it were empty
     * @return a list of human-readable problems, empty when the configuration is complete
     */
    public static List<String> validateConfiguration(String webhookUrl, List<String> joinMessages, String serverName) {
        List<String> problems = new ArrayList<>(validateWebhookUrl(webhookUrl));
        problems.addAll(validateJoinMessages(joinMessages, serverName));
        return problems;
    }

    /**
     * @param webhookUrl the configured {@code discord.webhook-url}
     * @return the problems with the webhook URL, empty when it is usable
     */
    private static List<String> validateWebhookUrl(String webhookUrl) {
        List<String> problems = new ArrayList<>();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            problems.add("'discord.webhook-url' is missing or empty");
            return problems;
        }

        URI uri;
        try {
            uri = URI.create(webhookUrl);
            uri.toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            problems.add("'discord.webhook-url' is not a valid URL: " + describeUrlProblem(e));
            return problems;
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            problems.add("'discord.webhook-url' must use http or https, but uses '" + scheme + "'");
        }
        return problems;
    }

    /**
     * Check that every configured message template can actually be sent.
     * An absent or empty list is fine, because the constructor falls back to
     * {@link #DEFAULT_JOIN_MESSAGES} in that case, and those defaults are short
     * enough to send under any server name an operator would plausibly type; a
     * list holding a blank entry is not fine, because that entry is kept and
     * would be sent as an empty message. Positions are reported 1-based so the
     * offending line can be found in {@code config.yml} without counting from zero.
     *
     * @param joinMessages the configured {@code discord.join-messages}
     * @param serverName   the server name the {@code {server}} placeholder will be filled with
     * @return the problems with the message templates, empty when they are all usable
     */
    private static List<String> validateJoinMessages(List<String> joinMessages, String serverName) {
        List<String> problems = new ArrayList<>();
        if (joinMessages == null) {
            return problems;
        }
        for (int i = 0; i < joinMessages.size(); i++) {
            String message = joinMessages.get(i);
            if (message == null || message.trim().isEmpty()) {
                problems.add("'discord.join-messages' entry " + (i + 1) + " is blank");
                continue;
            }
            int longestFilled = longestFilledLength(message, serverName);
            if (longestFilled > MAX_MESSAGE_LENGTH) {
                problems.add("'discord.join-messages' entry " + (i + 1) + " can produce a message of up to "
                        + longestFilled + " characters, but Discord accepts at most " + MAX_MESSAGE_LENGTH);
            }
        }
        return problems;
    }

    /**
     * Work out how long the longest message a template can produce would be.
     * The server name is known at startup, and the only other variable is the
     * player name, which a Minecraft Java Edition account caps at
     * {@link #MAX_PLAYER_NAME_LENGTH} characters, so the worst case is exact
     * rather than an estimate. Length is counted the same way Discord counts it
     * when enforcing its own limit, in UTF-16 code units, so a template of
     * emoji is measured as Discord will measure it.
     *
     * @param template   the message template
     * @param serverName the server name the {@code {server}} placeholder will be filled with,
     *                   or {@code null} to size the template as if it were empty
     * @return the length of the longest message the template can produce
     */
    private static int longestFilledLength(String template, String serverName) {
        int serverNameLength = serverName != null ? serverName.length() : 0;
        return template.length()
                + countOccurrences(template, PLAYER_PLACEHOLDER)
                        * (MAX_PLAYER_NAME_LENGTH - PLAYER_PLACEHOLDER.length())
                + countOccurrences(template, SERVER_PLACEHOLDER)
                        * (serverNameLength - SERVER_PLACEHOLDER.length());
    }

    /**
     * @param text   the text to search
     * @param needle the substring to count
     * @return the number of non-overlapping occurrences of {@code needle} in {@code text}
     */
    private static int countOccurrences(String text, String needle) {
        int count = 0;
        for (int index = text.indexOf(needle); index >= 0; index = text.indexOf(needle, index + needle.length())) {
            count++;
        }
        return count;
    }

    /**
     * Describe why a webhook URL could not be parsed, without repeating the URL.
     * A Discord webhook URL carries a bearer token, so the value must not reach
     * the server log even when it is malformed. {@link URI#create(String)} echoes
     * its whole input in the exception message, but wraps a
     * {@link URISyntaxException} whose reason is the diagnostic on its own.
     *
     * @param e the failure raised while parsing the URL
     * @return the reason the URL is unusable, safe to log
     */
    private static String describeUrlProblem(Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof URISyntaxException) {
            String reason = ((URISyntaxException) cause).getReason();
            if (reason != null && !reason.isEmpty()) {
                return reason;
            }
        }
        String message = e.getMessage();
        return (message != null && !message.isEmpty()) ? message : "the URL could not be parsed";
    }

    /**
     * Send a player-join notification to Discord.
     * Picks a random message from the configured templates and sends it via webhook.
     * Each template supports {player} and {server} placeholders.
     *
     * @param playerName the name of the player who joined
     * @param serverName the name of the server they joined
     * @throws IOException if there's an error sending the message
     */
    @Override
    public void notifyPlayerJoin(String playerName, String serverName) throws IOException {
        String template = joinMessages.get(random.nextInt(joinMessages.size()));
        String content = template.replace(PLAYER_PLACEHOLDER, playerName).replace(SERVER_PLACEHOLDER, serverName);
        sendMessage(content);
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "Discord";
    }

    /**
     * Send a message to Discord via webhook.
     * A rate-limited send is waited out and retried once, because a rate limit
     * is the one failure Herald reports that the operator cannot fix and that
     * clears on its own: several players joining within the same couple of
     * seconds is ordinary operation, and the response states exactly how long
     * to wait. Every other failure, and a second rate limit, is reported as it
     * happens.
     *
     * @param content The message content to send
     * @throws IOException if there's an error sending the message
     */
    public void sendMessage(String content) throws IOException {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            throw new IllegalArgumentException("Discord webhook URL is not configured");
        }

        URL url = URI.create(webhookUrl).toURL();

        // Create JSON payload with the message content
        String jsonPayload = String.format("{\"content\": \"%s\"}", escapeJson(content));

        try {
            sendOnce(url, jsonPayload);
        } catch (RateLimitedException rateLimited) {
            retryAfterRateLimit(url, jsonPayload, rateLimited);
        }
    }

    /**
     * Wait out a rate limit and send the message one more time.
     * A wait longer than {@link #MAX_RETRY_AFTER_MILLIS} is refused rather than
     * shortened, because sending again before Discord is ready would only earn
     * a second rate limit.
     *
     * @param url          the parsed webhook URL
     * @param jsonPayload  the request body that was rate limited
     * @param rateLimited  the rate-limited response that prompted the retry
     * @throws IOException if the wait was too long to be worth taking, was interrupted,
     *                     or the retry itself failed
     */
    private void retryAfterRateLimit(URL url, String jsonPayload, RateLimitedException rateLimited)
            throws IOException {
        long waitMillis = rateLimited.getRetryAfterMillis();
        if (waitMillis > MAX_RETRY_AFTER_MILLIS) {
            throw new IOException(rateLimited.getMessage() + "; the wait of " + waitMillis
                    + "ms it asked for is longer than the " + MAX_RETRY_AFTER_MILLIS
                    + "ms Herald waits at most, so the message was not retried", rateLimited);
        }

        logger.warning("Discord rate-limited a join notification; retrying in " + waitMillis
                + "ms. This clears on its own and needs no action.");
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting to retry a rate-limited Discord webhook message", e);
        }

        try {
            sendOnce(url, jsonPayload);
        } catch (RateLimitedException rateLimitedAgain) {
            throw new IOException(rateLimitedAgain.getMessage()
                    + "; the message had already been retried once after being rate-limited, "
                    + "so it was not retried again", rateLimitedAgain);
        }
    }

    /**
     * Send one webhook request, with no retry of its own.
     *
     * @param url         the parsed webhook URL
     * @param jsonPayload the request body to send
     * @throws RateLimitedException if Discord answered with {@link #RATE_LIMITED_STATUS}
     * @throws IOException          if the request failed for any other reason
     */
    private void sendOnce(URL url, String jsonPayload) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);  // 5 seconds connect timeout
        connection.setReadTimeout(10000);    // 10 seconds read timeout
        connection.setDoOutput(true);

        try {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return;
            }

            String errorBody = readErrorBody(connection);
            String failure = "Discord webhook returned error code: " + responseCode
                    + (errorBody.isEmpty() ? "" : " (" + errorBody + ")");
            if (responseCode == RATE_LIMITED_STATUS) {
                throw new RateLimitedException(failure,
                        retryAfterMillis(connection.getHeaderField("Retry-After"), errorBody));
            }
            throw new IOException(failure);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Work out how long a rate-limited response asks Herald to wait.
     * Discord states the delay twice, as a {@code Retry-After} header and as a
     * {@code retry_after} field in the JSON body, both counted in seconds; the
     * header is preferred and the body is read only when the header is absent
     * or unusable. A response stating neither is waited out for
     * {@link #DEFAULT_RETRY_AFTER_MILLIS}, which is short enough to be worth
     * taking on the chance the limit has already cleared.
     *
     * @param retryAfterHeader the {@code Retry-After} header, or {@code null} when absent
     * @param responseBody     the response body, which may hold a {@code retry_after} field
     * @return how long to wait before retrying, in milliseconds
     */
    static long retryAfterMillis(String retryAfterHeader, String responseBody) {
        long fromHeader = parseSecondsAsMillis(retryAfterHeader);
        if (fromHeader >= 0) {
            return fromHeader;
        }
        long fromBody = parseSecondsAsMillis(retryAfterField(responseBody));
        if (fromBody >= 0) {
            return fromBody;
        }
        return DEFAULT_RETRY_AFTER_MILLIS;
    }

    /**
     * @param responseBody the response body of a rate-limited request, which may be empty
     * @return the value of its {@code retry_after} field, or {@code null} when it holds none
     */
    private static String retryAfterField(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        Matcher matcher = RETRY_AFTER_FIELD.matcher(responseBody);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Read a delay stated in seconds, rounding up so that a sub-millisecond
     * delay still waits rather than retrying immediately.
     * {@code Retry-After} may legally be an HTTP date rather than a count of
     * seconds; Discord does not send one, and such a value is reported as
     * unusable here so the caller falls back rather than guessing.
     *
     * @param seconds the delay as stated, or {@code null} when it was not stated
     * @return the delay in milliseconds, or {@code -1} when it is absent or unusable
     */
    private static long parseSecondsAsMillis(String seconds) {
        if (seconds == null || seconds.trim().isEmpty()) {
            return -1;
        }
        double parsed;
        try {
            parsed = Double.parseDouble(seconds.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
        if (Double.isNaN(parsed) || parsed < 0) {
            return -1;
        }
        return (long) Math.ceil(parsed * 1000.0);
    }

    /**
     * Raised when Discord answers a webhook request with {@link #RATE_LIMITED_STATUS}.
     * This is an {@link IOException} like every other webhook failure, so that a
     * rate limit reaching a caller past the one retry reads the same way in the
     * log as the failures around it.
     */
    private static final class RateLimitedException extends IOException {

        private static final long serialVersionUID = 1L;

        private final long retryAfterMillis;

        RateLimitedException(String message, long retryAfterMillis) {
            super(message);
            this.retryAfterMillis = retryAfterMillis;
        }

        /** @return how long the response asked Herald to wait, in milliseconds */
        long getRetryAfterMillis() {
            return retryAfterMillis;
        }
    }


    /**
     * Read the error response body of a failed webhook call so the reason for the
     * failure (invalid token, unknown webhook, rate limit) survives into the log.
     * Whitespace is collapsed and the result is capped at {@link #MAX_ERROR_BODY_LENGTH}
     * characters to keep the message to a single readable log line.
     *
     * @param connection the connection that returned a non-2xx status
     * @return the error body, or an empty string if there is none or it cannot be read
     */
    private String readErrorBody(HttpURLConnection connection) {
        InputStream errorStream = connection.getErrorStream();
        if (errorStream == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[256];
            int read;
            while (body.length() <= MAX_ERROR_BODY_LENGTH && (read = reader.read(buffer)) != -1) {
                body.append(buffer, 0, read);
            }
        } catch (IOException e) {
            return "";
        }
        String collapsed = body.toString().replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= MAX_ERROR_BODY_LENGTH) {
            return collapsed;
        }
        int keepLength = Math.max(0, MAX_ERROR_BODY_LENGTH - 3);
        return collapsed.substring(0, keepLength) + "...";
    }

    /**
     * Escape special characters in JSON strings
     * @param text The text to escape
     * @return The escaped text
     */
    String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u00");
                        if (hex.length() == 1) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
