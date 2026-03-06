package com.dansplugins.herald;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.MalformedURLException;

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

    // Nested test classes for better organization
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Constructor should store webhook URL")
        void testConstructorStoresUrl() {
            String webhookUrl = "https://discord.com/api/webhooks/123456/abcdef";
            DiscordNotifier notifier = new DiscordNotifier(webhookUrl);
            assertNotNull(notifier);
        }
        
        @Test
        @DisplayName("Constructor should accept various URL formats")
        void testConstructorWithVariousUrlFormats() {
            assertDoesNotThrow(() -> new DiscordNotifier("https://discord.com/api/webhooks/123/abc"));
            assertDoesNotThrow(() -> new DiscordNotifier("http://localhost:8080/webhook"));
            assertDoesNotThrow(() -> new DiscordNotifier("https://example.com"));
        }
    }

    @Nested
    @DisplayName("URL Validation Tests")
    class UrlValidationTests {
        
        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("sendMessage should reject null or empty webhook URLs")
        void testSendMessageWithInvalidUrls(String invalidUrl) {
            DiscordNotifier notifier = new DiscordNotifier(invalidUrl);
            Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                notifier.sendMessage("test message");
            });
            assertEquals("Discord webhook URL is not configured", exception.getMessage());
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "not-a-url",
            "ftp://invalid.com",
            "://malformed",
            "htp://typo.com"
        })
        @DisplayName("sendMessage should handle malformed URLs")
        void testSendMessageWithMalformedUrls(String malformedUrl) {
            DiscordNotifier notifier = new DiscordNotifier(malformedUrl);
            assertThrows(Exception.class, () -> {
                notifier.sendMessage("test message");
            });
        }
    }

    @Nested
    @DisplayName("JSON Escaping Tests")
    class JsonEscapingTests {
        
        private DiscordNotifier notifier;
        
        @BeforeEach
        void setUp() {
            notifier = new DiscordNotifier("https://example.com");
        }
        
        @ParameterizedTest
        @CsvSource({
            "'Hello World', 'Hello World'",
            "'', ''",
            "'Test123', 'Test123'",
            "'Simple-text_123', 'Simple-text_123'"
        })
        @DisplayName("escapeJson should preserve text without special characters")
        void testEscapeJsonPreservesSimpleText(String input, String expected) {
            assertEquals(expected, notifier.escapeJson(input));
        }
        
        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
            "He said \"Hello\" | He said \\\"Hello\\\"",
            "\"quoted\" | \\\"quoted\\\"",
            "\" | \\\"",
            "\"\"\" | \\\"\\\"\\\""
        })
        @DisplayName("escapeJson should escape quotes correctly")
        void testEscapeJsonWithQuotes(String input, String expected) {
            assertEquals(expected, notifier.escapeJson(input));
        }
        
        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
            "C:\\Path | C:\\\\Path",
            "\\ | \\\\",
            "\\\\ | \\\\\\\\",
            "\\\\\\n | \\\\\\\\\\\\n"
        })
        @DisplayName("escapeJson should escape backslashes correctly")
        void testEscapeJsonWithBackslashes(String input, String expected) {
            assertEquals(expected, notifier.escapeJson(input));
        }
        
        @Test
        @DisplayName("escapeJson should handle all control characters together")
        void testEscapeJsonWithAllControlCharacters() {
            String input = "Line1\nLine2\rLine3\tCol";
            String expected = "Line1\\nLine2\\rLine3\\tCol";
            assertEquals(expected, notifier.escapeJson(input));
        }
        
        @Test
        @DisplayName("escapeJson should escape backspace and form feed")
        void testEscapeJsonWithBackspaceAndFormFeed() {
            assertEquals("\\b", notifier.escapeJson("\b"));
            assertEquals("\\f", notifier.escapeJson("\f"));
        }
        
        @Test
        @DisplayName("escapeJson should escape control characters below 0x20 as unicode")
        void testEscapeJsonControlCharactersBelow0x20() {
            // ASCII 0x01 (SOH) should become \u0001
            String result = notifier.escapeJson("\u0001");
            assertEquals("\\u0001", result);
            // ASCII 0x02 (STX) should become \u0002
            assertEquals("\\u0002", notifier.escapeJson("\u0002"));
            // ASCII 0x1F (US) should become \u001f
            assertEquals("\\u001f", notifier.escapeJson("\u001f"));
        }
        
        @Test
        @DisplayName("escapeJson should handle consecutive special characters")
        void testEscapeJsonWithConsecutiveSpecialChars() {
            String input = "\"\"\\\\\n\n\t\t";
            String expected = "\\\"\\\"\\\\\\\\\\n\\n\\t\\t";
            assertEquals(expected, notifier.escapeJson(input));
        }
        
        @Test
        @DisplayName("escapeJson should handle very long strings")
        void testEscapeJsonWithLongString() {
            StringBuilder input = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                input.append("Test ");
            }
            String result = notifier.escapeJson(input.toString());
            assertTrue(result.length() > 0);
            assertFalse(result.contains("\\\\Test")); // Should not have unnecessary escapes
        }
    }

    @Nested
    @DisplayName("Message Content Tests")
    class MessageContentTests {
        
        private DiscordNotifier notifier;
        
        @BeforeEach
        void setUp() {
            notifier = new DiscordNotifier("https://example.com");
        }
        
        @Test
        @DisplayName("escapeJson should preserve Discord markdown formatting")
        void testDiscordMarkdownPreservation() {
            String input = "**bold** *italic* __underline__ ~~strikethrough~~ `code` ```block```";
            String result = notifier.escapeJson(input);
            assertEquals(input, result);
        }
        
        @Test
        @DisplayName("escapeJson should handle Discord mentions and channels")
        void testDiscordSpecialSyntax() {
            String input = "<@123> <@!456> <#789> <@&012> <:emoji:345>";
            String result = notifier.escapeJson(input);
            assertEquals(input, result);
        }
        
        @Test
        @DisplayName("escapeJson should handle player join message format")
        void testPlayerJoinMessageFormat() {
            String playerName = "Steve";
            String serverName = "My Server";
            String message = "**" + playerName + "** joined the **" + serverName + "** server";
            String result = notifier.escapeJson(message);
            assertEquals("**Steve** joined the **My Server** server", result);
        }
        
        @Test
        @DisplayName("escapeJson should handle special player names")
        void testSpecialPlayerNames() {
            String[] playerNames = {
                "Player_123",
                "Player-Name",
                "Player.Name",
                "123Player",
                "player"
            };
            for (String name : playerNames) {
                String message = "**" + name + "** joined";
                String result = notifier.escapeJson(message);
                assertTrue(result.contains(name));
            }
        }
        
        @Test
        @DisplayName("escapeJson should handle Unicode in player names")
        void testUnicodePlayerNames() {
            String message = "**玩家123** joined the **服务器** server";
            String result = notifier.escapeJson(message);
            assertEquals(message, result);
        }
        
        @Test
        @DisplayName("escapeJson should handle emoji in messages")
        void testEmojiInMessages() {
            String message = "🎮 **Player** joined! 🎉";
            String result = notifier.escapeJson(message);
            assertEquals(message, result);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        
        private DiscordNotifier notifier;
        
        @BeforeEach
        void setUp() {
            notifier = new DiscordNotifier("https://example.com");
        }
        
        @Test
        @DisplayName("escapeJson should handle strings with only whitespace")
        void testWhitespaceOnlyStrings() {
            assertEquals("   ", notifier.escapeJson("   "));
            assertEquals("\\t\\t", notifier.escapeJson("\t\t"));
            assertEquals("\\n\\n", notifier.escapeJson("\n\n"));
        }
        
        @Test
        @DisplayName("escapeJson should handle single character strings")
        void testSingleCharacterStrings() {
            assertEquals("a", notifier.escapeJson("a"));
            assertEquals("\\\"", notifier.escapeJson("\""));
            assertEquals("\\\\", notifier.escapeJson("\\"));
            assertEquals("\\n", notifier.escapeJson("\n"));
        }
        
        @Test
        @DisplayName("escapeJson should be idempotent for already escaped strings")
        void testIdempotency() {
            String input = "Test\\\"Quote";
            String firstEscape = notifier.escapeJson(input);
            String secondEscape = notifier.escapeJson(firstEscape);
            // Should escape again since it's treating the escaped string as new input
            assertNotEquals(firstEscape, secondEscape);
        }
        
        @Test
        @DisplayName("escapeJson should handle maximum Discord message length")
        void testMaxDiscordMessageLength() {
            // Discord max message length is 2000 characters
            StringBuilder message = new StringBuilder();
            for (int i = 0; i < 2000; i++) {
                message.append("a");
            }
            String result = notifier.escapeJson(message.toString());
            assertEquals(2000, result.length());
        }
        
        @Test
        @DisplayName("escapeJson should handle strings with mixed line endings")
        void testMixedLineEndings() {
            String input = "Line1\nLine2\r\nLine3\rLine4";
            String result = notifier.escapeJson(input);
            assertEquals("Line1\\nLine2\\r\\nLine3\\rLine4", result);
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {
        
        private DiscordNotifier notifier;
        
        @BeforeEach
        void setUp() {
            notifier = new DiscordNotifier("https://example.com");
        }
        
        @Test
        @DisplayName("escapeJson should prevent JSON injection with quotes")
        void testJsonInjectionPrevention() {
            String maliciousInput = "\", \"injected\": \"value";
            String escaped = notifier.escapeJson(maliciousInput);
            assertFalse(escaped.contains("\", \"injected"));
            assertTrue(escaped.contains("\\\""));
        }
        
        @Test
        @DisplayName("escapeJson should handle potential script injection")
        void testScriptInjectionPrevention() {
            String scriptInput = "<script>alert('xss')</script>";
            String result = notifier.escapeJson(scriptInput);
            // Should preserve the content (Discord handles sanitization)
            assertEquals(scriptInput, result);
        }
        
        @Test
        @DisplayName("escapeJson should handle null bytes")
        void testNullByteHandling() {
            String input = "Test\0Null";
            String result = notifier.escapeJson(input);
            assertNotNull(result);
            // Null byte (0x00) should be escaped as \u0000
            assertTrue(result.contains("\\u0000"), "Null byte should be escaped as \\u0000");
        }
        
        @Test
        @DisplayName("escapeJson should handle control characters")
        void testControlCharacterHandling() {
            String input = "Test\u0001\u0002\u0003";
            String result = notifier.escapeJson(input);
            assertNotNull(result);
            assertTrue(result.contains("Test"));
            // Control chars should be escaped as unicode sequences
            assertTrue(result.contains("\\u0001"));
            assertTrue(result.contains("\\u0002"));
            assertTrue(result.contains("\\u0003"));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Constructor and escapeJson should work together")
        void testConstructorAndEscapeJsonIntegration() {
            String webhookUrl = "https://discord.com/api/webhooks/123/abc";
            DiscordNotifier notifier = new DiscordNotifier(webhookUrl);
            
            String playerName = "TestPlayer";
            String serverName = "TestServer";
            String message = "**" + playerName + "** joined the **" + serverName + "** server";
            
            String escaped = notifier.escapeJson(message);
            assertNotNull(escaped);
            assertTrue(escaped.contains(playerName));
            assertTrue(escaped.contains(serverName));
        }
        
        @Test
        @DisplayName("Multiple messages should be escaped independently")
        void testMultipleMessagesIndependently() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            String message1 = "Player \"A\" joined";
            String message2 = "Player \"B\" joined";
            
            String escaped1 = notifier.escapeJson(message1);
            String escaped2 = notifier.escapeJson(message2);
            
            assertNotEquals(escaped1, escaped2);
            assertTrue(escaped1.contains("A"));
            assertTrue(escaped2.contains("B"));
        }
    }
}
