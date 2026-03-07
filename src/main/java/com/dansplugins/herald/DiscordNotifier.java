package com.dansplugins.herald;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
        String content = template.replace("{player}", playerName).replace("{server}", serverName);
        sendMessage(content);
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
        
        URL url = new URL(webhookUrl);
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
                throw new IOException("Discord webhook returned error code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
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
