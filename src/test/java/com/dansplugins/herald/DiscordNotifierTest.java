package com.dansplugins.herald;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Unit tests for DiscordNotifier class
 */
class DiscordNotifierTest {

    @Test
    @DisplayName("Constructor should accept valid webhook URL")
    void testConstructorWithValidUrl() {
        String webhookUrl = "https://discord.com/api/webhooks/123456/abcdef";
        DiscordNotifier notifier = new DiscordNotifier(webhookUrl);
        assertNotNull(notifier);
    }

    @Test
    @DisplayName("Constructor should accept null webhook URL")
    void testConstructorWithNullUrl() {
        DiscordNotifier notifier = new DiscordNotifier(null);
        assertNotNull(notifier);
    }

    @Test
    @DisplayName("sendMessage should throw IllegalArgumentException for null webhook URL")
    void testSendMessageWithNullUrl() {
        DiscordNotifier notifier = new DiscordNotifier(null);
        assertThrows(IllegalArgumentException.class, () -> {
            notifier.sendMessage("test message");
        });
    }

    @Test
    @DisplayName("sendMessage should throw IllegalArgumentException for empty webhook URL")
    void testSendMessageWithEmptyUrl() {
        DiscordNotifier notifier = new DiscordNotifier("");
        assertThrows(IllegalArgumentException.class, () -> {
            notifier.sendMessage("test message");
        });
    }

    @Test
    @DisplayName("escapeJson should handle null input")
    void testEscapeJsonWithNull() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String result = notifier.escapeJson(null);
        assertEquals("", result);
    }

    @Test
    @DisplayName("escapeJson should handle empty string")
    void testEscapeJsonWithEmptyString() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String result = notifier.escapeJson("");
        assertEquals("", result);
    }

    @Test
    @DisplayName("escapeJson should not modify simple text")
    void testEscapeJsonWithSimpleText() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "Hello World";
        String result = notifier.escapeJson(input);
        assertEquals("Hello World", result);
    }

    @Test
    @DisplayName("escapeJson should escape double quotes")
    void testEscapeJsonWithDoubleQuotes() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "He said \"Hello\"";
        String result = notifier.escapeJson(input);
        assertEquals("He said \\\"Hello\\\"", result);
    }

    @Test
    @DisplayName("escapeJson should escape backslashes")
    void testEscapeJsonWithBackslashes() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "Path: C:\\Users\\Test";
        String result = notifier.escapeJson(input);
        assertEquals("Path: C:\\\\Users\\\\Test", result);
    }

    @Test
    @DisplayName("escapeJson should escape newlines")
    void testEscapeJsonWithNewlines() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "Line1\nLine2";
        String result = notifier.escapeJson(input);
        assertEquals("Line1\\nLine2", result);
    }

    @Test
    @DisplayName("escapeJson should escape carriage returns")
    void testEscapeJsonWithCarriageReturns() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "Line1\rLine2";
        String result = notifier.escapeJson(input);
        assertEquals("Line1\\rLine2", result);
    }

    @Test
    @DisplayName("escapeJson should escape tabs")
    void testEscapeJsonWithTabs() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "Col1\tCol2";
        String result = notifier.escapeJson(input);
        assertEquals("Col1\\tCol2", result);
    }

    @Test
    @DisplayName("escapeJson should handle multiple special characters")
    void testEscapeJsonWithMultipleSpecialChars() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "Text with \"quotes\", \nnewlines, \ttabs, and \\backslashes";
        String result = notifier.escapeJson(input);
        assertEquals("Text with \\\"quotes\\\", \\nnewlines, \\ttabs, and \\\\backslashes", result);
    }

    @Test
    @DisplayName("escapeJson should properly escape Discord message format")
    void testEscapeJsonWithDiscordMessageFormat() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String playerName = "Steve";
        String serverName = "My Server";
        String message = "**" + playerName + "** joined the **" + serverName + "** server";
        String result = notifier.escapeJson(message);
        assertEquals("**Steve** joined the **My Server** server", result);
    }

    @Test
    @DisplayName("escapeJson should handle backslash before quote correctly")
    void testEscapeJsonWithBackslashBeforeQuote() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "Test\\\"Quote";
        String result = notifier.escapeJson(input);
        // Backslash is escaped first, then quote is escaped
        assertEquals("Test\\\\\\\"Quote", result);
    }

    @Test
    @DisplayName("escapeJson should handle empty markdown formatting")
    void testEscapeJsonWithMarkdown() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "**bold** *italic* __underline__ ~~strikethrough~~";
        String result = notifier.escapeJson(input);
        assertEquals("**bold** *italic* __underline__ ~~strikethrough~~", result);
    }

    @Test
    @DisplayName("escapeJson should handle Unicode characters")
    void testEscapeJsonWithUnicodeCharacters() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "Hello 世界 🌍";
        String result = notifier.escapeJson(input);
        assertEquals("Hello 世界 🌍", result);
    }

    @Test
    @DisplayName("escapeJson should handle special Discord mentions")
    void testEscapeJsonWithDiscordMentions() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com");
        String input = "<@123456789> <#987654321> @everyone @here";
        String result = notifier.escapeJson(input);
        assertEquals("<@123456789> <#987654321> @everyone @here", result);
    }
}
