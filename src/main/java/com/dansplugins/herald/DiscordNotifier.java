package com.dansplugins.herald;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordNotifier {
    
    private final String webhookUrl;
    
    public DiscordNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl;
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
        connection.setDoOutput(true);
        
        // Create JSON payload with the message content
        String jsonPayload = String.format("{\"content\": \"%s\"}", escapeJson(content));
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("Discord webhook returned error code: " + responseCode);
        }
    }
    
    /**
     * Escape special characters in JSON strings
     * @param text The text to escape
     * @return The escaped text
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
