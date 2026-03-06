#!/usr/bin/env bash

# Discord Webhook Test Script
# This script allows you to test Discord webhook notifications without running a Minecraft server

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Discord Webhook Test Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if webhook URL is provided
if [ -z "$1" ]; then
    echo -e "${RED}Error: Discord webhook URL is required${NC}"
    echo ""
    echo "Usage: $0 <webhook-url> [player-name] [server-name]"
    echo ""
    echo "Example:"
    echo "  $0 https://discord.com/api/webhooks/123456/abcdef"
    echo "  $0 https://discord.com/api/webhooks/123456/abcdef Steve"
    echo "  $0 https://discord.com/api/webhooks/123456/abcdef Steve \"My Awesome Server\""
    echo ""
    echo -e "${YELLOW}To get a Discord webhook URL:${NC}"
    echo "  1. Go to your Discord server settings"
    echo "  2. Navigate to Integrations → Webhooks"
    echo "  3. Click 'New Webhook'"
    echo "  4. Configure the webhook and copy the URL"
    echo ""
    exit 1
fi

WEBHOOK_URL="$1"
PLAYER_NAME="${2:-TestPlayer}"
SERVER_NAME="${3:-Minecraft}"

# Create temporary Java file
TEMP_DIR=$(mktemp -d)
JAVA_FILE="$TEMP_DIR/DiscordWebhookTest.java"

cat > "$JAVA_FILE" << 'EOF'
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookTest {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java DiscordWebhookTest <webhook-url> [player-name] [server-name]");
            System.exit(1);
        }
        
        String webhookUrl = args[0];
        String playerName = args.length > 1 ? args[1] : "TestPlayer";
        String serverName = args.length > 2 ? args[2] : "Minecraft";
        
        System.out.println("Testing Discord webhook...");
        System.out.println("Webhook URL: " + maskWebhookUrl(webhookUrl));
        System.out.println("Player Name: " + playerName);
        System.out.println("Server Name: " + serverName);
        System.out.println();
        
        String message = "**" + playerName + "** joined the **" + serverName + "** server";
        
        try {
            sendDiscordMessage(webhookUrl, message);
            System.out.println("✓ SUCCESS: Message sent to Discord!");
            System.out.println("Check your Discord channel for the notification.");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("✗ ERROR: Failed to send message to Discord");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void sendDiscordMessage(String webhookUrl, String content) throws IOException {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            throw new IllegalArgumentException("Discord webhook URL is not configured");
        }
        
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(15000);    // 15 seconds
        connection.setDoOutput(true);
        
        // Create JSON payload with the message content
        String jsonPayload = String.format("{\"content\": \"%s\"}", escapeJson(content));
        
        System.out.println("Sending payload: " + jsonPayload);
        
        try {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            System.out.println("Response code: " + responseCode);
            
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Discord webhook returned error code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
    
    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u00");
                        if (hex.length() == 1) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
    
    private static String maskWebhookUrl(String url) {
        // Mask the webhook token for security
        if (url.contains("/webhooks/")) {
            String[] parts = url.split("/webhooks/");
            if (parts.length > 1) {
                String[] tokenParts = parts[1].split("/");
                if (tokenParts.length > 1) {
                    return parts[0] + "/webhooks/" + tokenParts[0] + "/****";
                }
            }
        }
        return url;
    }
}
EOF

echo -e "${YELLOW}Compiling test script...${NC}"
if ! javac "$JAVA_FILE"; then
    echo -e "${RED}Compilation failed${NC}"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo -e "${GREEN}Compilation successful${NC}"
echo ""
echo -e "${YELLOW}Sending test message...${NC}"
echo ""

# Run the test, capturing exit code without 'set -e' interference
cd "$TEMP_DIR"
set +e
java DiscordWebhookTest "$WEBHOOK_URL" "$PLAYER_NAME" "$SERVER_NAME"
TEST_RESULT=$?
set -e

# Cleanup
cd - > /dev/null
rm -rf "$TEMP_DIR"

echo ""
if [ $TEST_RESULT -eq 0 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Test completed successfully!${NC}"
    echo -e "${GREEN}========================================${NC}"
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}Test failed!${NC}"
    echo -e "${RED}========================================${NC}"
fi

exit $TEST_RESULT
