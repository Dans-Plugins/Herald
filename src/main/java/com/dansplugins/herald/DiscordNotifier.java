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

    /** The placeholder replaced with the name of the player who joined. */
    private static final String PLAYER_PLACEHOLDER = "{player}";

    /** The placeholder replaced with the name of the server they joined. */
    private static final String SERVER_PLACEHOLDER = "{server}";

    private final String webhookUrl;
    private final List<String> joinMessages;
    private final Random random;

    public DiscordNotifier(String webhookUrl, List<String> joinMessages) {
        this(webhookUrl, joinMessages, new Random());
    }

    DiscordNotifier(String webhookUrl, List<String> joinMessages, Random random) {
        this.webhookUrl = webhookUrl;
        this.joinMessages = (joinMessages != null && !joinMessages.isEmpty())
                ? Collections.unmodifiableList(new ArrayList<>(joinMessages))
                : DEFAULT_JOIN_MESSAGES;
        this.random = random;
    }

    /**
     * Check the configuration keys Discord notifications require, against the
     * built-in join messages.
     *
     * @param webhookUrl the configured {@code discord.webhook-url}
     * @return a list of human-readable problems, empty when the configuration is complete
     * @see #validateConfiguration(String, List, String)
     */
    public static List<String> validateConfiguration(String webhookUrl) {
        return validateConfiguration(webhookUrl, null);
    }

    /**
     * Check the configuration keys Discord notifications require, against a
     * server name that contributes nothing to the length of a message.
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
     * Send a message to Discord via webhook
     * @param content The message content to send
     * @throws IOException if there's an error sending the message
     */
    public void sendMessage(String content) throws IOException {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            throw new IllegalArgumentException("Discord webhook URL is not configured");
        }
        
        URL url = URI.create(webhookUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);  // 5 seconds connect timeout
        connection.setReadTimeout(10000);    // 10 seconds read timeout
        connection.setDoOutput(true);
        
        // Create JSON payload with the message content
        String jsonPayload = String.format("{\"content\": \"%s\"}", escapeJson(content));
        
        try {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                String errorBody = readErrorBody(connection);
                throw new IOException("Discord webhook returned error code: " + responseCode
                        + (errorBody.isEmpty() ? "" : " (" + errorBody + ")"));
            }
        } finally {
            connection.disconnect();
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
