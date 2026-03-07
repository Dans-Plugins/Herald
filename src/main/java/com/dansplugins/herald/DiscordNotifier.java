package com.dansplugins.herald;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordNotifier implements Notifier {
    
    private final String webhookUrl;
    
    public DiscordNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    /**
     * Send a player-join notification to Discord.
     * Formats the message using Discord Markdown bold syntax and sends it via webhook.
     *
     * @param playerName the name of the player who joined
     * @param serverName the name of the server they joined
     * @throws IOException if there's an error sending the message
     */
    @Override
    public void notifyPlayerJoin(String playerName, String serverName) throws IOException {
        String content = "**" + playerName + "** joined the **" + serverName + "** server";
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
