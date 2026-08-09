package com.dansplugins.herald;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

/**
 * Unit tests for DiscordNotifier class
 */
class DiscordNotifierTest {

    @Test
    @DisplayName("Constructor should accept valid webhook URL")
    void testConstructorWithValidUrl() {
        String webhookUrl = "https://discord.com/api/webhooks/123456/abcdef";
        DiscordNotifier notifier = new DiscordNotifier(webhookUrl, null);
        assertNotNull(notifier);
    }

    @Test
    @DisplayName("Constructor should accept null webhook URL")
    void testConstructorWithNullUrl() {
        DiscordNotifier notifier = new DiscordNotifier(null, null);
        assertNotNull(notifier);
    }

    @Test
    @DisplayName("sendMessage should throw IllegalArgumentException for null webhook URL")
    void testSendMessageWithNullUrl() {
        DiscordNotifier notifier = new DiscordNotifier(null, null);
        assertThrows(IllegalArgumentException.class, () -> {
            notifier.sendMessage("test message");
        });
    }

    @Test
    @DisplayName("sendMessage should throw IllegalArgumentException for empty webhook URL")
    void testSendMessageWithEmptyUrl() {
        DiscordNotifier notifier = new DiscordNotifier("", null);
        assertThrows(IllegalArgumentException.class, () -> {
            notifier.sendMessage("test message");
        });
    }

    @Test
    @DisplayName("escapeJson should handle null input")
    void testEscapeJsonWithNull() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String result = notifier.escapeJson(null);
        assertEquals("", result);
    }

    @Test
    @DisplayName("escapeJson should handle empty string")
    void testEscapeJsonWithEmptyString() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String result = notifier.escapeJson("");
        assertEquals("", result);
    }

    @Test
    @DisplayName("escapeJson should not modify simple text")
    void testEscapeJsonWithSimpleText() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "Hello World";
        String result = notifier.escapeJson(input);
        assertEquals("Hello World", result);
    }

    @Test
    @DisplayName("escapeJson should escape double quotes")
    void testEscapeJsonWithDoubleQuotes() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "He said \"Hello\"";
        String result = notifier.escapeJson(input);
        assertEquals("He said \\\"Hello\\\"", result);
    }

    @Test
    @DisplayName("escapeJson should escape backslashes")
    void testEscapeJsonWithBackslashes() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "Path: C:\\Users\\Test";
        String result = notifier.escapeJson(input);
        assertEquals("Path: C:\\\\Users\\\\Test", result);
    }

    @Test
    @DisplayName("escapeJson should escape newlines")
    void testEscapeJsonWithNewlines() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "Line1\nLine2";
        String result = notifier.escapeJson(input);
        assertEquals("Line1\\nLine2", result);
    }

    @Test
    @DisplayName("escapeJson should escape carriage returns")
    void testEscapeJsonWithCarriageReturns() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "Line1\rLine2";
        String result = notifier.escapeJson(input);
        assertEquals("Line1\\rLine2", result);
    }

    @Test
    @DisplayName("escapeJson should escape tabs")
    void testEscapeJsonWithTabs() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "Col1\tCol2";
        String result = notifier.escapeJson(input);
        assertEquals("Col1\\tCol2", result);
    }

    @Test
    @DisplayName("escapeJson should handle multiple special characters")
    void testEscapeJsonWithMultipleSpecialChars() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "Text with \"quotes\", \nnewlines, \ttabs, and \\backslashes";
        String result = notifier.escapeJson(input);
        assertEquals("Text with \\\"quotes\\\", \\nnewlines, \\ttabs, and \\\\backslashes", result);
    }

    @Test
    @DisplayName("escapeJson should properly escape Discord message format")
    void testEscapeJsonWithDiscordMessageFormat() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String playerName = "Steve";
        String serverName = "My Server";
        String message = "**" + playerName + "** joined the **" + serverName + "** server";
        String result = notifier.escapeJson(message);
        assertEquals("**Steve** joined the **My Server** server", result);
    }

    @Test
    @DisplayName("escapeJson should handle backslash before quote correctly")
    void testEscapeJsonWithBackslashBeforeQuote() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "Test\\\"Quote";
        String result = notifier.escapeJson(input);
        // Backslash is escaped first, then quote is escaped
        assertEquals("Test\\\\\\\"Quote", result);
    }

    @Test
    @DisplayName("escapeJson should handle empty markdown formatting")
    void testEscapeJsonWithMarkdown() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "**bold** *italic* __underline__ ~~strikethrough~~";
        String result = notifier.escapeJson(input);
        assertEquals("**bold** *italic* __underline__ ~~strikethrough~~", result);
    }

    @Test
    @DisplayName("escapeJson should handle Unicode characters")
    void testEscapeJsonWithUnicodeCharacters() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
        String input = "Hello 世界 🌍";
        String result = notifier.escapeJson(input);
        assertEquals("Hello 世界 🌍", result);
    }

    @Test
    @DisplayName("escapeJson should handle special Discord mentions")
    void testEscapeJsonWithDiscordMentions() {
        DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
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
            DiscordNotifier notifier = new DiscordNotifier(webhookUrl, null);
            assertNotNull(notifier);
        }
        
        @Test
        @DisplayName("Constructor should accept various URL formats")
        void testConstructorWithVariousUrlFormats() {
            assertDoesNotThrow(() -> new DiscordNotifier("https://discord.com/api/webhooks/123/abc", null));
            assertDoesNotThrow(() -> new DiscordNotifier("http://localhost:8080/webhook", null));
            assertDoesNotThrow(() -> new DiscordNotifier("https://example.com", null));
        }
    }

    @Nested
    @DisplayName("URL Validation Tests")
    class UrlValidationTests {
        
        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("sendMessage should reject null or empty webhook URLs")
        void testSendMessageWithInvalidUrls(String invalidUrl) {
            DiscordNotifier notifier = new DiscordNotifier(invalidUrl, null);
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
            DiscordNotifier notifier = new DiscordNotifier(malformedUrl, null);
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
            notifier = new DiscordNotifier("https://example.com", null);
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
            "\\\\ | \\\\\\\\"
        })
        @DisplayName("escapeJson should escape backslashes correctly")
        void testEscapeJsonWithBackslashes(String input, String expected) {
            assertEquals(expected, notifier.escapeJson(input));
        }

        @Test
        @DisplayName("escapeJson should escape newline after backslash correctly")
        void testEscapeJsonBackslashThenNewline() {
            String input = "\\\n";
            String expected = "\\\\\\n";
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
            notifier = new DiscordNotifier("https://example.com", null);
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
            notifier = new DiscordNotifier("https://example.com", null);
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
            notifier = new DiscordNotifier("https://example.com", null);
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
            DiscordNotifier notifier = new DiscordNotifier(webhookUrl, null);
            
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
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
            String message1 = "Player \"A\" joined";
            String message2 = "Player \"B\" joined";
            
            String escaped1 = notifier.escapeJson(message1);
            String escaped2 = notifier.escapeJson(message2);
            
            assertNotEquals(escaped1, escaped2);
            assertTrue(escaped1.contains("A"));
            assertTrue(escaped2.contains("B"));
        }
    }

    @Nested
    @DisplayName("JSON Payload Tests")
    class JsonPayloadTests {

        private DiscordNotifier notifier;

        @BeforeEach
        void setUp() {
            notifier = new DiscordNotifier("https://example.com", null);
        }

        @Test
        @DisplayName("Escaped content should produce valid JSON when embedded in payload")
        void testEscapedContentProducesValidJsonStructure() {
            String content = "Steve joined";
            String escaped = notifier.escapeJson(content);
            String payload = "{\"content\": \"" + escaped + "\"}";
            // Basic structural check: starts with { and ends with }
            assertTrue(payload.startsWith("{"));
            assertTrue(payload.endsWith("}"));
            assertTrue(payload.contains("\"content\""));
        }

        @Test
        @DisplayName("Escaped quotes in content should not break JSON structure")
        void testEscapedQuotesSafeInPayload() {
            String content = "Player \"Steve\" joined";
            String escaped = notifier.escapeJson(content);
            String payload = "{\"content\": \"" + escaped + "\"}";
            // The raw unescaped quote should not appear outside the value
            assertFalse(escaped.contains("\"Steve\""));
            assertTrue(escaped.contains("\\\"Steve\\\""));
            assertTrue(payload.contains("\\\"Steve\\\""));
        }

        @Test
        @DisplayName("Escaped backslash in content should not break JSON structure")
        void testEscapedBackslashSafeInPayload() {
            String content = "C:\\Users\\Steve joined";
            String escaped = notifier.escapeJson(content);
            // Raw single backslash should not appear (would break JSON)
            assertFalse(escaped.contains("C:\\U"));
            assertTrue(escaped.contains("C:\\\\U"));
        }

        @Test
        @DisplayName("Newlines in content should be escaped so payload stays single-line")
        void testNewlinesEscapedInPayload() {
            String content = "Line1\nLine2";
            String escaped = notifier.escapeJson(content);
            assertFalse(escaped.contains("\n"), "Literal newline must not appear in escaped JSON string");
            assertTrue(escaped.contains("\\n"));
        }

        @Test
        @DisplayName("All named control escapes should not appear as literal characters")
        void testAllNamedControlEscapesAreSafe() {
            String content = "a\bb\fc\nd\re\tf";
            String escaped = notifier.escapeJson(content);
            assertFalse(escaped.contains("\b"), "Literal backspace must not appear");
            assertFalse(escaped.contains("\f"), "Literal form-feed must not appear");
            assertFalse(escaped.contains("\n"), "Literal newline must not appear");
            assertFalse(escaped.contains("\r"), "Literal carriage-return must not appear");
            assertFalse(escaped.contains("\t"), "Literal tab must not appear");
            assertTrue(escaped.contains("\\b"));
            assertTrue(escaped.contains("\\f"));
            assertTrue(escaped.contains("\\n"));
            assertTrue(escaped.contains("\\r"));
            assertTrue(escaped.contains("\\t"));
        }

        @ParameterizedTest
        @ValueSource(chars = {'\u0001', '\u0002', '\u0003', '\u0004', '\u0010', '\u001A', '\u001F'})
        @DisplayName("Control characters below 0x20 should be escaped as \\uXXXX")
        void testLowControlCharsEscapedAsUnicode(char controlChar) {
            String input = "prefix" + controlChar + "suffix";
            String escaped = notifier.escapeJson(input);
            assertFalse(escaped.contains(String.valueOf(controlChar)),
                    "Control char 0x" + Integer.toHexString(controlChar) + " must not appear literally");
            assertTrue(escaped.contains("\\u00"),
                    "Control char should be escaped as \\uXXXX");
        }

        @Test
        @DisplayName("Normal printable ASCII should pass through unchanged")
        void testPrintableAsciiPassesThrough() {
            // All printable ASCII 0x20–0x7E except " and \
            String printable = " !#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~";
            String escaped = notifier.escapeJson(printable);
            assertEquals(printable, escaped);
        }
    }

    @Nested
    @DisplayName("Notifier Interface Tests")
    class NotifyPlayerJoinTests {

        @Test
        @DisplayName("DiscordNotifier should implement the Notifier interface")
        void testImplementsNotifierInterface() {
            DiscordNotifier notifier = new DiscordNotifier("https://discord.com/api/webhooks/123/abc", null);
            assertInstanceOf(Notifier.class, notifier);
        }

        @Test
        @DisplayName("getDisplayName should return the operator-facing channel name, not the class name")
        void testGetDisplayName() {
            DiscordNotifier notifier = new DiscordNotifier("https://discord.com/api/webhooks/123/abc", null);

            assertEquals("Discord", notifier.getDisplayName());
            assertNotEquals(notifier.getClass().getSimpleName(), notifier.getDisplayName(),
                    "The log name must not be tied to the class name");
        }

        @Test
        @DisplayName("notifyPlayerJoin should throw IllegalArgumentException when URL is null")
        void testNotifyPlayerJoinWithNullUrlThrows() {
            DiscordNotifier notifier = new DiscordNotifier(null, null);
            assertThrows(IllegalArgumentException.class, () ->
                    notifier.notifyPlayerJoin("Steve", "SurvivalServer"));
        }

        @Test
        @DisplayName("notifyPlayerJoin should throw IllegalArgumentException when URL is empty")
        void testNotifyPlayerJoinWithEmptyUrlThrows() {
            DiscordNotifier notifier = new DiscordNotifier("", null);
            assertThrows(IllegalArgumentException.class, () ->
                    notifier.notifyPlayerJoin("Steve", "SurvivalServer"));
        }

        @Test
        @DisplayName("notifyPlayerJoin should format message using one of the default medieval-themed templates")
        void testNotifyPlayerJoinFormatsMessageCorrectly() throws Exception {
            final String[] capturedMessage = {null};
            Random seededRandom = new Random(42);
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null, seededRandom) {
                @Override
                public void sendMessage(String content) {
                    capturedMessage[0] = content;
                }
            };

            int index = new Random(42).nextInt(DiscordNotifier.DEFAULT_JOIN_MESSAGES.size());
            String expected = DiscordNotifier.DEFAULT_JOIN_MESSAGES.get(index)
                    .replace("{player}", "Steve")
                    .replace("{server}", "MySurvivalServer");

            notifier.notifyPlayerJoin("Steve", "MySurvivalServer");

            assertEquals(expected, capturedMessage[0]);
        }

        @Test
        @DisplayName("notifyPlayerJoin should include both player name and server name")
        void testNotifyPlayerJoinIncludesBothNames() throws Exception {
            final String[] capturedMessage = {null};
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null) {
                @Override
                public void sendMessage(String content) {
                    capturedMessage[0] = content;
                }
            };

            notifier.notifyPlayerJoin("Notch", "ClassicSMP");

            assertNotNull(capturedMessage[0]);
            assertTrue(capturedMessage[0].contains("Notch"));
            assertTrue(capturedMessage[0].contains("ClassicSMP"));
        }

        @Test
        @DisplayName("notifyPlayerJoin should use Discord bold markdown for player and server names")
        void testNotifyPlayerJoinUsesBoldMarkdown() throws Exception {
            final String[] capturedMessage = {null};
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null) {
                @Override
                public void sendMessage(String content) {
                    capturedMessage[0] = content;
                }
            };

            notifier.notifyPlayerJoin("Alex", "Server");

            assertNotNull(capturedMessage[0]);
            assertTrue(capturedMessage[0].contains("**Alex**"), "Player name should be bolded");
            assertTrue(capturedMessage[0].contains("**Server**"), "Server name should be bolded");
        }

        @ParameterizedTest
        @CsvSource({"Steve,SurvivalServer", "Alex,CreativeWorld", "Player123,MyCoolSMP"})
        @DisplayName("notifyPlayerJoin should correctly format various player/server name combinations")
        void testNotifyPlayerJoinVariousNames(String playerName, String serverName) throws Exception {
            final String[] capturedMessage = {null};
            Random seededRandom = new Random(0);
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null, seededRandom) {
                @Override
                public void sendMessage(String content) {
                    capturedMessage[0] = content;
                }
            };

            notifier.notifyPlayerJoin(playerName, serverName);

            assertNotNull(capturedMessage[0]);
            assertTrue(capturedMessage[0].contains(playerName));
            assertTrue(capturedMessage[0].contains(serverName));
        }

        @Test
        @DisplayName("notifyPlayerJoin should use custom join message when provided")
        void testNotifyPlayerJoinWithCustomMessage() throws Exception {
            final String[] capturedMessage = {null};
            String customMessage = "Welcome, **{player}**, to the **{server}** kingdom!";
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", List.of(customMessage)) {
                @Override
                public void sendMessage(String content) {
                    capturedMessage[0] = content;
                }
            };

            notifier.notifyPlayerJoin("Steve", "MySurvivalServer");

            assertEquals("Welcome, **Steve**, to the **MySurvivalServer** kingdom!", capturedMessage[0]);
        }

        @Test
        @DisplayName("notifyPlayerJoin should fall back to defaults when join messages list is null")
        void testNotifyPlayerJoinFallsBackToDefaultWhenNull() throws Exception {
            final String[] capturedMessage = {null};
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null) {
                @Override
                public void sendMessage(String content) {
                    capturedMessage[0] = content;
                }
            };

            notifier.notifyPlayerJoin("Steve", "Server");

            assertNotNull(capturedMessage[0]);
            assertTrue(capturedMessage[0].contains("Steve"));
            assertTrue(capturedMessage[0].contains("Server"));
        }

        @Test
        @DisplayName("notifyPlayerJoin should fall back to defaults when join messages list is empty")
        void testNotifyPlayerJoinFallsBackToDefaultWhenEmpty() throws Exception {
            final String[] capturedMessage = {null};
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", Collections.emptyList()) {
                @Override
                public void sendMessage(String content) {
                    capturedMessage[0] = content;
                }
            };

            notifier.notifyPlayerJoin("Steve", "Server");

            assertNotNull(capturedMessage[0]);
            assertTrue(capturedMessage[0].contains("Steve"));
            assertTrue(capturedMessage[0].contains("Server"));
        }

        @Test
        @DisplayName("notifyPlayerJoin should support message without placeholders")
        void testNotifyPlayerJoinWithNoPlaceholders() throws Exception {
            final String[] capturedMessage = {null};
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", List.of("A new adventurer has arrived!")) {
                @Override
                public void sendMessage(String content) {
                    capturedMessage[0] = content;
                }
            };

            notifier.notifyPlayerJoin("Steve", "Server");

            assertEquals("A new adventurer has arrived!", capturedMessage[0]);
        }

        @Test
        @DisplayName("notifyPlayerJoin should pick random messages from the list")
        void testNotifyPlayerJoinPicksRandomMessages() throws Exception {
            List<String> messages = List.of("Message A: {player}", "Message B: {player}", "Message C: {player}");
            java.util.Set<String> seen = new java.util.HashSet<>();

            for (int seed = 0; seed < 100; seed++) {
                final String[] capturedMessage = {null};
                DiscordNotifier notifier = new DiscordNotifier("https://example.com", messages, new Random(seed)) {
                    @Override
                    public void sendMessage(String content) {
                        capturedMessage[0] = content;
                    }
                };
                notifier.notifyPlayerJoin("Steve", "Server");
                seen.add(capturedMessage[0]);
            }

            assertTrue(seen.size() > 1, "Multiple different messages should be selected across different seeds");
        }

        @Test
        @DisplayName("Default join messages list should contain exactly 10 messages")
        void testDefaultJoinMessagesCount() {
            assertEquals(10, DiscordNotifier.DEFAULT_JOIN_MESSAGES.size());
        }

        @Test
        @DisplayName("All default join messages should contain {player} and {server} placeholders")
        void testDefaultJoinMessagesContainPlaceholders() {
            for (String msg : DiscordNotifier.DEFAULT_JOIN_MESSAGES) {
                assertTrue(msg.contains("{player}"), "Message should contain {player}: " + msg);
                assertTrue(msg.contains("{server}"), "Message should contain {server}: " + msg);
            }
        }
    }

    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("validateConfiguration should report no problems for a configured webhook URL")
        void testValidateConfigurationWithUrl() {
            assertTrue(DiscordNotifier.validateConfiguration("https://discord.com/api/webhooks/123/abc").isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("validateConfiguration should report a missing webhook URL")
        void testValidateConfigurationWithoutUrl(String webhookUrl) {
            List<String> problems = DiscordNotifier.validateConfiguration(webhookUrl);

            assertEquals(1, problems.size());
            assertTrue(problems.get(0).contains("discord.webhook-url"),
                    "Problem should name the config key: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should accept a plain http webhook URL")
        void testValidateConfigurationWithHttpUrl() {
            assertTrue(DiscordNotifier.validateConfiguration("http://localhost:8080/webhook").isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "discord.com/api/webhooks/123/abc",
                "not a url",
                "webhooks/123/abc"
        })
        @DisplayName("validateConfiguration should report a webhook URL that is not a valid URL")
        void testValidateConfigurationWithMalformedUrl(String webhookUrl) {
            List<String> problems = DiscordNotifier.validateConfiguration(webhookUrl);

            assertEquals(1, problems.size(), "Expected exactly one problem, got: " + problems);
            assertTrue(problems.get(0).contains("discord.webhook-url"),
                    "Problem should name the config key: " + problems.get(0));
            assertTrue(problems.get(0).contains("not a valid URL"),
                    "Problem should explain the URL is invalid: " + problems.get(0));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "ftp://discord.com/api/webhooks/123/abc",
                "file:///tmp/webhook"
        })
        @DisplayName("validateConfiguration should report a webhook URL that does not use http or https")
        void testValidateConfigurationWithNonHttpScheme(String webhookUrl) {
            List<String> problems = DiscordNotifier.validateConfiguration(webhookUrl);

            assertEquals(1, problems.size(), "Expected exactly one problem, got: " + problems);
            assertTrue(problems.get(0).contains("discord.webhook-url"),
                    "Problem should name the config key: " + problems.get(0));
            assertTrue(problems.get(0).contains("http or https"),
                    "Problem should explain the scheme requirement: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should accept an uppercase scheme")
        void testValidateConfigurationWithUppercaseScheme() {
            assertTrue(DiscordNotifier.validateConfiguration("HTTPS://discord.com/api/webhooks/123/abc").isEmpty());
        }

        @Test
        @DisplayName("validateConfiguration should not echo the webhook URL, which carries a bearer token")
        void testValidateConfigurationDoesNotLeakWebhookToken() {
            String secretToken = "s3cret-webhook-token";
            // A stray space makes the URL unparseable while leaving the token intact
            String webhookUrl = "https://discord.com/api/webhooks/123/" + secretToken + " ";

            List<String> problems = DiscordNotifier.validateConfiguration(webhookUrl);

            assertEquals(1, problems.size(), "Expected exactly one problem, got: " + problems);
            assertFalse(problems.get(0).contains(secretToken),
                    "Problem must not repeat the webhook token into the log: " + problems.get(0));
            assertFalse(problems.get(0).contains("discord.com"),
                    "Problem must not repeat the webhook URL into the log: " + problems.get(0));
            assertTrue(problems.get(0).contains("not a valid URL"),
                    "Problem should still explain the URL is invalid: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should still explain why an unparseable URL is invalid")
        void testValidateConfigurationExplainsWhyUrlIsInvalid() {
            List<String> relative = DiscordNotifier.validateConfiguration("webhooks/123/abc");
            List<String> illegalCharacter = DiscordNotifier.validateConfiguration("https://discord.com/a b");

            assertTrue(relative.get(0).contains("not absolute"),
                    "A relative URL should say so: " + relative.get(0));
            assertTrue(illegalCharacter.get(0).contains("Illegal character"),
                    "An illegal character should be named: " + illegalCharacter.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should reject the URLs sendMessage cannot open")
        void testValidateConfigurationRejectsUrlsSendMessageCannotOpen() {
            DiscordNotifier notifier = new DiscordNotifier("discord.com/api/webhooks/123/abc", null);

            assertFalse(DiscordNotifier.validateConfiguration("discord.com/api/webhooks/123/abc").isEmpty(),
                    "A URL sendMessage cannot open should be reported at startup");
            assertThrows(IllegalArgumentException.class, () -> notifier.sendMessage("test"),
                    "sendMessage should be the failure this startup check prevents");
        }
    }

    @Nested
    @DisplayName("Join Message Validation Tests")
    class JoinMessageValidationTests {

        private static final String VALID_URL = "https://discord.com/api/webhooks/123/abc";

        @Test
        @DisplayName("validateConfiguration should accept a list of usable templates")
        void testValidateConfigurationWithUsableTemplates() {
            assertTrue(DiscordNotifier.validateConfiguration(VALID_URL,
                    List.of("{player} joined {server}", "Welcome {player}!")).isEmpty());
        }

        @Test
        @DisplayName("validateConfiguration should accept an absent join message list")
        void testValidateConfigurationWithNullTemplates() {
            assertTrue(DiscordNotifier.validateConfiguration(VALID_URL, null).isEmpty(),
                    "An absent list falls back to the built-in defaults, which are usable");
        }

        @Test
        @DisplayName("validateConfiguration should accept an empty join message list")
        void testValidateConfigurationWithEmptyTemplateList() {
            assertTrue(DiscordNotifier.validateConfiguration(VALID_URL, Collections.emptyList()).isEmpty(),
                    "An empty list falls back to the built-in defaults, which are usable");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t", "\n", "   \t  "})
        @DisplayName("validateConfiguration should report a blank join message")
        void testValidateConfigurationWithBlankTemplate(String blank) {
            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL, List.of(blank));

            assertEquals(1, problems.size(), "Expected exactly one problem, got: " + problems);
            assertTrue(problems.get(0).contains("discord.join-messages"),
                    "Problem should name the config key: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should report a null join message entry")
        void testValidateConfigurationWithNullTemplateEntry() {
            List<String> withNull = new java.util.ArrayList<>();
            withNull.add("{player} joined {server}");
            withNull.add(null);

            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL, withNull);

            assertEquals(1, problems.size(), "Expected exactly one problem, got: " + problems);
            assertTrue(problems.get(0).contains("entry 2"),
                    "Problem should name the position of the null entry: " + problems.get(0));
        }

        @Test
        @DisplayName("Blank join message should be named by its 1-based position in the list")
        void testBlankTemplateIsNamedByPosition() {
            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL,
                    List.of("{player} joined {server}", "Welcome {player}!", "  "));

            assertEquals(1, problems.size(), "Expected exactly one problem, got: " + problems);
            assertTrue(problems.get(0).contains("entry 3"),
                    "Third entry should be reported as entry 3, not entry 2: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should report every blank join message, not just the first")
        void testEveryBlankTemplateIsReported() {
            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL,
                    List.of("", "Welcome {player}!", ""));

            assertEquals(2, problems.size(), "Expected two problems, got: " + problems);
            assertTrue(problems.get(0).contains("entry 1"), "First problem should be entry 1: " + problems.get(0));
            assertTrue(problems.get(1).contains("entry 3"), "Second problem should be entry 3: " + problems.get(1));
        }

        @Test
        @DisplayName("validateConfiguration should report a blank join message alongside an unusable URL")
        void testBlankTemplateReportedAlongsideBadUrl() {
            List<String> problems = DiscordNotifier.validateConfiguration("", List.of(""));

            assertEquals(2, problems.size(),
                    "Both keys should be reported in one startup pass, got: " + problems);
            assertTrue(problems.get(0).contains("discord.webhook-url"),
                    "URL problem should be reported: " + problems.get(0));
            assertTrue(problems.get(1).contains("discord.join-messages"),
                    "Join message problem should be reported: " + problems.get(1));
        }

        @Test
        @DisplayName("The URL-only overload should validate against the built-in defaults")
        void testUrlOnlyOverloadUsesDefaults() {
            assertEquals(DiscordNotifier.validateConfiguration(VALID_URL, null),
                    DiscordNotifier.validateConfiguration(VALID_URL));
        }

        @Test
        @DisplayName("The built-in default join messages should pass validation")
        void testDefaultJoinMessagesAreValid() {
            assertTrue(DiscordNotifier.validateConfiguration(VALID_URL,
                    DiscordNotifier.DEFAULT_JOIN_MESSAGES).isEmpty(),
                    "The defaults Herald falls back to must themselves be sendable");
        }

        @Test
        @DisplayName("A blank template is kept and sent as the empty message Discord rejects")
        void testBlankTemplateWouldBeSentAsAnEmptyMessage() throws IOException {
            List<String> requestBodies = new java.util.ArrayList<>();
            HttpServer stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            stub.createContext("/webhook", exchange -> {
                requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            stub.start();
            String url = "http://127.0.0.1:" + stub.getAddress().getPort() + "/webhook";

            try {
                // A list holding only a blank entry is non-empty, so the constructor keeps it
                // instead of falling back to DEFAULT_JOIN_MESSAGES
                new DiscordNotifier(url, List.of("   ")).notifyPlayerJoin("Steve", "TestServer");
            } finally {
                stub.stop(0);
            }

            assertEquals(1, requestBodies.size(), "The webhook should have been called once");
            assertEquals("{\"content\": \"   \"}", requestBodies.get(0),
                    "A blank template is sent verbatim, which is the failure startup validation prevents");
            assertFalse(DiscordNotifier.validateConfiguration(url, List.of("   ")).isEmpty(),
                    "Startup validation should reject the template before it is ever sent");
        }
    }

    @Nested
    @DisplayName("Join Message Length Validation Tests")
    class JoinMessageLengthValidationTests {

        private static final String VALID_URL = "https://discord.com/api/webhooks/123/abc";

        /**
         * Build a template that fills out to exactly the given length.
         *
         * @param filledLength the length the template should reach once {@code {player}} is
         *                     filled with a name of the maximum length
         * @return a template holding one {@code {player}} placeholder
         */
        private String templateFillingTo(int filledLength) {
            return "{player}" + "x".repeat(filledLength - DiscordNotifier.MAX_PLAYER_NAME_LENGTH);
        }

        @Test
        @DisplayName("A template that fills to exactly the limit should be accepted")
        void testTemplateAtTheLimitIsAccepted() {
            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL,
                    List.of(templateFillingTo(DiscordNotifier.MAX_MESSAGE_LENGTH)), "Server");

            assertTrue(problems.isEmpty(), "A message Discord accepts should not be reported: " + problems);
        }

        @Test
        @DisplayName("A template that fills to one character over the limit should be reported")
        void testTemplateOverTheLimitIsReported() {
            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL,
                    List.of(templateFillingTo(DiscordNotifier.MAX_MESSAGE_LENGTH + 1)), "Server");

            assertEquals(1, problems.size(), "Expected one problem, got: " + problems);
            assertTrue(problems.get(0).contains("'discord.join-messages'"),
                    "The problem should name the key an operator edits: " + problems.get(0));
            assertTrue(problems.get(0).contains(String.valueOf(DiscordNotifier.MAX_MESSAGE_LENGTH + 1)),
                    "The problem should state how long the message would be: " + problems.get(0));
            assertTrue(problems.get(0).contains(String.valueOf(DiscordNotifier.MAX_MESSAGE_LENGTH)),
                    "The problem should state the limit: " + problems.get(0));
        }

        @Test
        @DisplayName("An over-long template should be named by its position in the list")
        void testOverLongTemplateIsNamedByPosition() {
            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL,
                    List.of("Welcome {player}!",
                            "{player} arrived at {server}",
                            templateFillingTo(DiscordNotifier.MAX_MESSAGE_LENGTH + 500)),
                    "Server");

            assertEquals(1, problems.size(), "Expected one problem, got: " + problems);
            assertTrue(problems.get(0).contains("entry 3"),
                    "The third entry should be named, counting from one: " + problems.get(0));
        }

        @Test
        @DisplayName("A long server name should push an otherwise acceptable template over the limit")
        void testLongServerNamePushesTemplateOverTheLimit() {
            List<String> templates = List.of("{player} has entered {server}!");

            assertTrue(DiscordNotifier.validateConfiguration(VALID_URL, templates, "Server").isEmpty(),
                    "The template is well within the limit under an ordinary server name");
            assertFalse(DiscordNotifier.validateConfiguration(VALID_URL, templates,
                            "S".repeat(DiscordNotifier.MAX_MESSAGE_LENGTH)).isEmpty(),
                    "The same template should be reported once the server name fills it past the limit");
        }

        @Test
        @DisplayName("Every occurrence of a placeholder should count towards the length")
        void testRepeatedPlaceholdersAreCounted() {
            // Three {server} placeholders, each growing by 500 characters once filled
            String template = "{server}{server}{server}" + "x".repeat(500);

            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL, List.of(template),
                    "S".repeat(508));

            assertEquals(1, problems.size(),
                    "Three placeholders of 508 characters each should breach the limit: " + problems);
        }

        @Test
        @DisplayName("A template shorter once filled than it is written should be accepted")
        void testShortServerNameShrinksTheTemplate() {
            // "{server}" is eight characters; a one-character server name makes the message shorter
            String template = "{server}".repeat(300);

            assertTrue(DiscordNotifier.validateConfiguration(VALID_URL, List.of(template), "S").isEmpty(),
                    "A template of 2400 characters filling out to 300 should be accepted");
        }

        @Test
        @DisplayName("An over-long template should be reported alongside a blank one")
        void testOverLongAndBlankTemplatesAreBothReported() {
            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL,
                    List.of("   ", templateFillingTo(DiscordNotifier.MAX_MESSAGE_LENGTH + 1)), "Server");

            assertEquals(2, problems.size(), "Both entries should be reported in one pass: " + problems);
            assertTrue(problems.get(0).contains("entry 1") && problems.get(0).contains("blank"),
                    "The blank entry should be reported first: " + problems.get(0));
            assertTrue(problems.get(1).contains("entry 2") && problems.get(1).contains("characters"),
                    "The over-long entry should be reported second: " + problems.get(1));
        }

        @Test
        @DisplayName("A blank entry should be reported as blank rather than measured")
        void testBlankTemplateIsNotAlsoMeasured() {
            List<String> problems = DiscordNotifier.validateConfiguration(VALID_URL, List.of(""), "Server");

            assertEquals(1, problems.size(), "A blank entry should raise one problem, not two: " + problems);
            assertTrue(problems.get(0).contains("blank"), "The problem should be about blankness: " + problems.get(0));
        }

        @Test
        @DisplayName("The built-in defaults should stay within the limit under a long server name")
        void testDefaultsSurviveALongServerName() {
            assertTrue(DiscordNotifier.validateConfiguration(VALID_URL,
                            DiscordNotifier.DEFAULT_JOIN_MESSAGES, "S".repeat(200)).isEmpty(),
                    "The defaults Herald falls back to must stay sendable under a long server name");
        }

        @Test
        @DisplayName("The overload without a server name should size templates as if it were empty")
        void testOverloadWithoutServerNameSizesAsEmpty() {
            String template = "{server}" + "x".repeat(DiscordNotifier.MAX_MESSAGE_LENGTH - 3);

            assertTrue(DiscordNotifier.validateConfiguration(VALID_URL, List.of(template)).isEmpty(),
                    "An empty server name should shrink the template below the limit");
            assertFalse(DiscordNotifier.validateConfiguration(VALID_URL, List.of(template), "Server").isEmpty(),
                    "A real server name should push the same template past the limit");
        }

        @Test
        @DisplayName("A validated template should be sendable as the message it produces")
        void testValidatedTemplateProducesASendableMessage() {
            String serverName = "MySurvivalServer";
            String template = templateFillingTo(DiscordNotifier.MAX_MESSAGE_LENGTH);
            assertTrue(DiscordNotifier.validateConfiguration(VALID_URL, List.of(template), serverName).isEmpty());

            String filled = template
                    .replace("{player}", "x".repeat(DiscordNotifier.MAX_PLAYER_NAME_LENGTH))
                    .replace("{server}", serverName);

            assertEquals(DiscordNotifier.MAX_MESSAGE_LENGTH, filled.length(),
                    "The measured worst case should match the message that is actually sent");
        }
    }

    @Nested
    @DisplayName("Webhook Error Response Tests")
    class WebhookErrorResponseTests {

        private HttpServer server;

        /**
         * Start a local webhook that always answers with the given status and body.
         *
         * @return the URL of the stub webhook endpoint
         */
        private String startStubWebhook(int statusCode, String responseBody) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/webhook", exchange -> {
                exchange.getRequestBody().readAllBytes();
                byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(statusCode, body.length == 0 ? -1 : body.length);
                if (body.length > 0) {
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                }
                exchange.close();
            });
            server.start();
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/webhook";
        }

        @AfterEach
        void stopStubWebhook() {
            if (server != null) {
                server.stop(0);
            }
        }

        @Test
        @DisplayName("sendMessage should include the Discord error body in the exception message")
        void testSendMessageIncludesErrorBody() throws IOException {
            String url = startStubWebhook(401, "{\"message\": \"Invalid Webhook Token\", \"code\": 50027}");
            DiscordNotifier notifier = new DiscordNotifier(url, null);

            IOException exception = assertThrows(IOException.class, () -> notifier.sendMessage("hello"));

            assertTrue(exception.getMessage().contains("401"),
                    "Message should include the status code: " + exception.getMessage());
            assertTrue(exception.getMessage().contains("Invalid Webhook Token"),
                    "Message should include the response body: " + exception.getMessage());
        }

        @Test
        @DisplayName("sendMessage should report the status code alone when the error body is empty")
        void testSendMessageWithEmptyErrorBody() throws IOException {
            String url = startStubWebhook(500, "");
            DiscordNotifier notifier = new DiscordNotifier(url, null);

            IOException exception = assertThrows(IOException.class, () -> notifier.sendMessage("hello"));

            assertEquals("Discord webhook returned error code: 500", exception.getMessage());
        }

        @Test
        @DisplayName("sendMessage should collapse newlines in the error body onto one line")
        void testSendMessageCollapsesErrorBodyWhitespace() throws IOException {
            String url = startStubWebhook(400, "{\n  \"message\": \"Cannot send an empty message\"\n}");
            DiscordNotifier notifier = new DiscordNotifier(url, null);

            IOException exception = assertThrows(IOException.class, () -> notifier.sendMessage("hello"));

            assertFalse(exception.getMessage().contains("\n"),
                    "Message should be a single line: " + exception.getMessage());
            assertTrue(exception.getMessage().contains("Cannot send an empty message"),
                    "Message should include the response body: " + exception.getMessage());
        }

        @Test
        @DisplayName("sendMessage should truncate an oversized error body")
        void testSendMessageTruncatesLongErrorBody() throws IOException {
            String url = startStubWebhook(400, "x".repeat(DiscordNotifier.MAX_ERROR_BODY_LENGTH * 3));
            DiscordNotifier notifier = new DiscordNotifier(url, null);

            IOException exception = assertThrows(IOException.class, () -> notifier.sendMessage("hello"));

            assertTrue(exception.getMessage().endsWith("...)"),
                    "Truncated body should be marked with an ellipsis: " + exception.getMessage());
            int bodyStart = exception.getMessage().indexOf('(') + 1;
            int bodyEnd = exception.getMessage().lastIndexOf(')');
            String truncatedBody = exception.getMessage().substring(bodyStart, bodyEnd);
            assertEquals(DiscordNotifier.MAX_ERROR_BODY_LENGTH, truncatedBody.length(),
                    "Truncated body should stay within the documented cap");
            assertTrue(exception.getMessage().length() < DiscordNotifier.MAX_ERROR_BODY_LENGTH * 2,
                    "Message should be bounded in length");
        }

        @Test
        @DisplayName("sendMessage should succeed without throwing on a 204 response")
        void testSendMessageSucceedsOnNoContent() throws IOException {
            String url = startStubWebhook(204, "");
            DiscordNotifier notifier = new DiscordNotifier(url, null);

            assertDoesNotThrow(() -> notifier.sendMessage("hello"));
        }
    }

    @Nested
    @DisplayName("Rate Limit Tests")
    class RateLimitTests {

        /** The body Discord answers a rate-limited webhook request with. */
        private static final String RATE_LIMIT_BODY =
                "{\"message\": \"You are being rate limited.\", \"retry_after\": %s, \"global\": false}";

        /** One scripted answer from the stub webhook. */
        private record StubResponse(int status, String retryAfterHeader, String body) {

            static StubResponse rateLimited(String retryAfterHeader, String body) {
                return new StubResponse(DiscordNotifier.RATE_LIMITED_STATUS, retryAfterHeader, body);
            }

            static StubResponse accepted() {
                return new StubResponse(204, null, "");
            }
        }

        private HttpServer server;
        private final List<String> requestBodies = Collections.synchronizedList(new ArrayList<>());
        private final List<LogRecord> logRecords = Collections.synchronizedList(new ArrayList<>());
        private Logger logger;

        /**
         * Give each test a logger of its own, so that the warning a retry writes
         * can be read back without any of it reaching the console.
         */
        @BeforeEach
        void createCapturingLogger() {
            logger = Logger.getLogger("HeraldRateLimitTest-" + UUID.randomUUID());
            logger.setUseParentHandlers(false);
            logger.addHandler(new Handler() {
                @Override
                public void publish(LogRecord record) {
                    logRecords.add(record);
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            });
        }

        /**
         * Start a local webhook that answers each request with the next scripted
         * response, repeating the last one once the script runs out.
         *
         * @param responses the answers to give, in order
         * @return the URL of the stub webhook endpoint
         */
        private String startStubWebhook(StubResponse... responses) throws IOException {
            AtomicInteger answered = new AtomicInteger();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/webhook", exchange -> {
                requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                StubResponse response = responses[Math.min(answered.getAndIncrement(), responses.length - 1)];
                if (response.retryAfterHeader() != null) {
                    exchange.getResponseHeaders().set("Retry-After", response.retryAfterHeader());
                }
                byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(response.status(), body.length == 0 ? -1 : body.length);
                if (body.length > 0) {
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                }
                exchange.close();
            });
            server.start();
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/webhook";
        }

        @AfterEach
        void stopStubWebhook() {
            if (server != null) {
                server.stop(0);
            }
        }

        /**
         * @return the warnings written while the test ran
         */
        private List<String> warnings() {
            return logRecords.stream()
                    .filter(record -> record.getLevel() == Level.WARNING)
                    .map(LogRecord::getMessage)
                    .toList();
        }

        @Test
        @DisplayName("A rate-limited message should be retried once and delivered")
        void testRateLimitedMessageIsRetriedAndDelivered() throws IOException {
            String url = startStubWebhook(
                    StubResponse.rateLimited("0.05", String.format(RATE_LIMIT_BODY, "0.05")),
                    StubResponse.accepted());
            DiscordNotifier notifier = new DiscordNotifier(url, null, logger);

            assertDoesNotThrow(() -> notifier.sendMessage("hello"));

            assertEquals(2, requestBodies.size(), "The message should have been sent twice");
            assertEquals(requestBodies.get(0), requestBodies.get(1),
                    "The retry should carry the same message as the attempt it repeats");
        }

        @Test
        @DisplayName("A retry should be reported as a warning naming the wait it takes")
        void testRetryIsReportedAsAWarning() throws IOException {
            String url = startStubWebhook(
                    StubResponse.rateLimited("0.05", String.format(RATE_LIMIT_BODY, "0.05")),
                    StubResponse.accepted());
            DiscordNotifier notifier = new DiscordNotifier(url, null, logger);

            notifier.sendMessage("hello");

            assertEquals(1, warnings().size(), "One warning should be written: " + warnings());
            assertTrue(warnings().get(0).contains("rate-limited"),
                    "The warning should say what happened: " + warnings().get(0));
            assertTrue(warnings().get(0).contains("50ms"),
                    "The warning should say how long the retry waits: " + warnings().get(0));
        }

        @Test
        @DisplayName("A second rate limit should fail rather than be retried again")
        void testSecondRateLimitFails() throws IOException {
            String url = startStubWebhook(
                    StubResponse.rateLimited("0.05", String.format(RATE_LIMIT_BODY, "0.05")));
            DiscordNotifier notifier = new DiscordNotifier(url, null, logger);

            IOException exception = assertThrows(IOException.class, () -> notifier.sendMessage("hello"));

            assertEquals(2, requestBodies.size(), "The message should have been sent exactly twice");
            assertTrue(exception.getMessage().contains(String.valueOf(DiscordNotifier.RATE_LIMITED_STATUS)),
                    "The failure should name the status: " + exception.getMessage());
            assertTrue(exception.getMessage().contains("not retried again"),
                    "The failure should say the retry was already spent: " + exception.getMessage());
        }

        @Test
        @DisplayName("A wait longer than the cap should fail without being taken")
        void testWaitBeyondTheCapIsNotTaken() throws IOException {
            String longWaitSeconds = String.valueOf(DiscordNotifier.MAX_RETRY_AFTER_MILLIS / 1000 + 60);
            String url = startStubWebhook(
                    StubResponse.rateLimited(longWaitSeconds, String.format(RATE_LIMIT_BODY, longWaitSeconds)),
                    StubResponse.accepted());
            DiscordNotifier notifier = new DiscordNotifier(url, null, logger);

            long startedAt = System.nanoTime();
            IOException exception = assertThrows(IOException.class, () -> notifier.sendMessage("hello"));
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

            assertEquals(1, requestBodies.size(), "The message should not have been sent a second time");
            assertTrue(elapsedMillis < DiscordNotifier.MAX_RETRY_AFTER_MILLIS,
                    "The wait should have been refused rather than taken: " + elapsedMillis + "ms");
            assertTrue(exception.getMessage().contains(String.valueOf(DiscordNotifier.MAX_RETRY_AFTER_MILLIS)),
                    "The failure should name the cap: " + exception.getMessage());
            assertTrue(warnings().isEmpty(), "No retry was made, so none should be reported: " + warnings());
        }

        @Test
        @DisplayName("A rate limit stating no delay at all should still be retried")
        void testRateLimitWithoutAnyStatedDelayIsRetried() throws IOException {
            String url = startStubWebhook(
                    StubResponse.rateLimited(null, ""),
                    StubResponse.accepted());
            DiscordNotifier notifier = new DiscordNotifier(url, null, logger);

            long startedAt = System.nanoTime();
            assertDoesNotThrow(() -> notifier.sendMessage("hello"));
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

            assertEquals(2, requestBodies.size(), "The message should have been sent twice");
            assertTrue(elapsedMillis >= DiscordNotifier.DEFAULT_RETRY_AFTER_MILLIS - 50,
                    "The default wait should have been taken: " + elapsedMillis + "ms");
        }

        @Test
        @DisplayName("A rate limit stating its delay only in the body should be retried after that delay")
        void testRateLimitDelayIsReadFromTheBody() throws IOException {
            String url = startStubWebhook(
                    StubResponse.rateLimited(null, String.format(RATE_LIMIT_BODY, "0.05")),
                    StubResponse.accepted());
            DiscordNotifier notifier = new DiscordNotifier(url, null, logger);

            long startedAt = System.nanoTime();
            assertDoesNotThrow(() -> notifier.sendMessage("hello"));
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

            assertEquals(2, requestBodies.size(), "The message should have been sent twice");
            assertTrue(elapsedMillis < DiscordNotifier.DEFAULT_RETRY_AFTER_MILLIS,
                    "The body should have been believed over the default: " + elapsedMillis + "ms");
        }

        @Test
        @DisplayName("A failure that is not a rate limit should not be retried")
        void testOtherFailuresAreNotRetried() throws IOException {
            String url = startStubWebhook(new StubResponse(401, null,
                    "{\"message\": \"Invalid Webhook Token\", \"code\": 50027}"));
            DiscordNotifier notifier = new DiscordNotifier(url, null, logger);

            IOException exception = assertThrows(IOException.class, () -> notifier.sendMessage("hello"));

            assertEquals(1, requestBodies.size(), "A misconfiguration should be reported, not retried");
            assertTrue(exception.getMessage().contains("Invalid Webhook Token"),
                    "The failure should still carry the error body: " + exception.getMessage());
            assertTrue(warnings().isEmpty(), "No retry was made, so none should be reported: " + warnings());
        }
    }

    @Nested
    @DisplayName("Retry-After Parsing Tests")
    class RetryAfterParsingTests {

        @Test
        @DisplayName("A whole number of seconds should be read from the header")
        void testWholeSecondsHeader() {
            assertEquals(2000L, DiscordNotifier.retryAfterMillis("2", ""));
        }

        @Test
        @DisplayName("A fractional number of seconds should be rounded up to the next millisecond")
        void testFractionalSecondsAreRoundedUp() {
            assertEquals(529L, DiscordNotifier.retryAfterMillis("0.529", ""));
            assertEquals(1L, DiscordNotifier.retryAfterMillis("0.0001", ""));
        }

        @Test
        @DisplayName("The header should be preferred over the body")
        void testHeaderIsPreferredOverBody() {
            assertEquals(2000L, DiscordNotifier.retryAfterMillis("2",
                    "{\"message\": \"You are being rate limited.\", \"retry_after\": 5.0}"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "Wed, 21 Oct 2026 07:28:00 GMT", "soon", "-1", "1,5"})
        @DisplayName("An absent or unusable header should fall through to the body")
        void testUnusableHeaderFallsThroughToTheBody(String header) {
            assertEquals(5000L, DiscordNotifier.retryAfterMillis(header,
                    "{\"message\": \"You are being rate limited.\", \"retry_after\": 5.0, \"global\": false}"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"{}", "{\"message\": \"You are being rate limited.\"}", "not json at all"})
        @DisplayName("A response stating no usable delay at all should fall back to the default")
        void testNoUsableDelayFallsBackToTheDefault(String body) {
            assertEquals(DiscordNotifier.DEFAULT_RETRY_AFTER_MILLIS,
                    DiscordNotifier.retryAfterMillis(null, body));
        }

        @Test
        @DisplayName("A delay stated as zero should be honoured rather than treated as absent")
        void testZeroDelayIsHonoured() {
            assertEquals(0L, DiscordNotifier.retryAfterMillis("0", ""));
        }

        @Test
        @DisplayName("A pathological delay should be reported as-is, for the caller to refuse")
        void testPathologicalDelayIsReportedAsIs() {
            assertTrue(DiscordNotifier.retryAfterMillis("86400", "") > DiscordNotifier.MAX_RETRY_AFTER_MILLIS,
                    "A day-long delay should be left for the caller to reject");
        }
    }
}
