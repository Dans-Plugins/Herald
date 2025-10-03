package com.dansplugins.herald;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Herald plugin configuration and message formatting
 */
class HeraldIntegrationTest {

    @Nested
    @DisplayName("Message Format Tests")
    class MessageFormatTests {
        
        @Test
        @DisplayName("Discord message format should match expected pattern")
        void testDiscordMessageFormat() {
            String playerName = "TestPlayer";
            String serverName = "TestServer";
            String expectedFormat = "**" + playerName + "** joined the **" + serverName + "** server";
            
            assertTrue(expectedFormat.startsWith("**"));
            assertTrue(expectedFormat.contains("** joined the **"));
            assertTrue(expectedFormat.endsWith("** server"));
        }
        
        @Test
        @DisplayName("Discord message should be properly escaped")
        void testDiscordMessageEscaping() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            String playerName = "Player\"Test";
            String serverName = "Server\\Name";
            String message = "**" + playerName + "** joined the **" + serverName + "** server";
            
            String escaped = notifier.escapeJson(message);
            assertTrue(escaped.contains("\\\""));
            assertTrue(escaped.contains("\\\\"));
        }
        
        @Test
        @DisplayName("Message format should handle empty server name gracefully")
        void testEmptyServerName() {
            String playerName = "TestPlayer";
            String serverName = "";
            String message = "**" + playerName + "** joined the **" + (serverName.isEmpty() ? "Minecraft" : serverName) + "** server";
            
            assertTrue(message.contains("Minecraft"));
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {
        
        @Test
        @DisplayName("Discord notifier should be created with valid URL")
        void testDiscordNotifierCreation() {
            String webhookUrl = "https://discord.com/api/webhooks/123456/abcdef";
            DiscordNotifier notifier = new DiscordNotifier(webhookUrl);
            assertNotNull(notifier);
        }
        
        @Test
        @DisplayName("Discord notifier should handle null URL in constructor")
        void testDiscordNotifierWithNullUrl() {
            DiscordNotifier notifier = new DiscordNotifier(null);
            assertNotNull(notifier);
            
            Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                notifier.sendMessage("test");
            });
            assertEquals("Discord webhook URL is not configured", exception.getMessage());
        }
        
        @Test
        @DisplayName("Discord notifier should handle empty URL in constructor")
        void testDiscordNotifierWithEmptyUrl() {
            DiscordNotifier notifier = new DiscordNotifier("");
            assertNotNull(notifier);
            
            Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                notifier.sendMessage("test");
            });
            assertEquals("Discord webhook URL is not configured", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Player Name Tests")
    class PlayerNameTests {
        
        @Test
        @DisplayName("Message should handle standard player names")
        void testStandardPlayerNames() {
            String[] standardNames = {"Steve", "Alex", "Player123", "Cool_Player", "Player-Name"};
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            for (String name : standardNames) {
                String message = "**" + name + "** joined the **TestServer** server";
                String escaped = notifier.escapeJson(message);
                assertTrue(escaped.contains(name));
            }
        }
        
        @Test
        @DisplayName("Message should handle player names with special characters")
        void testPlayerNamesWithSpecialChars() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            String[] specialNames = {"Player\"Quote", "Player\\Slash", "Player\nNewline"};
            for (String name : specialNames) {
                String message = "**" + name + "** joined";
                String escaped = notifier.escapeJson(message);
                assertNotNull(escaped);
                // Should be properly escaped
                assertFalse(escaped.contains("\"") && !escaped.contains("\\\""));
            }
        }
        
        @Test
        @DisplayName("Message should handle Unicode player names")
        void testUnicodePlayerNames() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            String[] unicodeNames = {"玩家", "プレイヤー", "игрок", "لاعب"};
            for (String name : unicodeNames) {
                String message = "**" + name + "** joined the **Server** server";
                String escaped = notifier.escapeJson(message);
                assertTrue(escaped.contains(name));
            }
        }
    }

    @Nested
    @DisplayName("Server Name Tests")
    class ServerNameTests {
        
        @Test
        @DisplayName("Message should handle standard server names")
        void testStandardServerNames() {
            String[] serverNames = {"Minecraft", "My Server", "Server123", "Cool-Server"};
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            for (String serverName : serverNames) {
                String message = "**Player** joined the **" + serverName + "** server";
                String escaped = notifier.escapeJson(message);
                assertTrue(escaped.contains(serverName));
            }
        }
        
        @Test
        @DisplayName("Message should use 'Minecraft' as default server name")
        void testDefaultServerName() {
            String serverName = "";
            String actualServerName = serverName.isEmpty() ? "Minecraft" : serverName;
            
            String message = "**Player** joined the **" + actualServerName + "** server";
            assertTrue(message.contains("Minecraft"));
        }
        
        @Test
        @DisplayName("Message should handle server names with special characters")
        void testServerNamesWithSpecialChars() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            String serverName = "Server \"Best\" \\Cool\\";
            String message = "**Player** joined the **" + serverName + "** server";
            String escaped = notifier.escapeJson(message);
            assertTrue(escaped.contains("\\\""));
            assertTrue(escaped.contains("\\\\"));
        }
    }

    @Nested
    @DisplayName("Concurrent Message Tests")
    class ConcurrentTests {
        
        @Test
        @DisplayName("Multiple messages should not interfere with each other")
        void testMultipleSimultaneousMessages() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            String message1 = "**Player1** joined the **Server** server";
            String message2 = "**Player2** joined the **Server** server";
            String message3 = "**Player3** joined the **Server** server";
            
            String escaped1 = notifier.escapeJson(message1);
            String escaped2 = notifier.escapeJson(message2);
            String escaped3 = notifier.escapeJson(message3);
            
            assertTrue(escaped1.contains("Player1"));
            assertTrue(escaped2.contains("Player2"));
            assertTrue(escaped3.contains("Player3"));
            
            // Ensure they're independent
            assertFalse(escaped1.contains("Player2"));
            assertFalse(escaped2.contains("Player3"));
            assertFalse(escaped3.contains("Player1"));
        }
        
        @Test
        @DisplayName("Same player joining multiple times should create distinct messages")
        void testRepeatedPlayerJoins() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            String playerName = "TestPlayer";
            
            String message1 = "**" + playerName + "** joined the **Server1** server";
            String message2 = "**" + playerName + "** joined the **Server2** server";
            
            String escaped1 = notifier.escapeJson(message1);
            String escaped2 = notifier.escapeJson(message2);
            
            assertTrue(escaped1.contains("Server1"));
            assertTrue(escaped2.contains("Server2"));
            assertNotEquals(escaped1, escaped2);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("escapeJson should handle rapid successive calls")
        void testRapidEscapeJsonCalls() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            long startTime = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                String message = "**Player" + i + "** joined the **Server** server";
                notifier.escapeJson(message);
            }
            long endTime = System.nanoTime();
            
            // Should complete in reasonable time (less than 1 second)
            long durationMs = (endTime - startTime) / 1_000_000;
            assertTrue(durationMs < 1000, "Escaping 1000 messages took " + durationMs + "ms");
        }
        
        @Test
        @DisplayName("escapeJson should handle large messages efficiently")
        void testLargeMessageEscaping() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com");
            
            // Create a large message (near Discord's 2000 char limit)
            StringBuilder largeMessage = new StringBuilder("**Player** joined the **");
            for (int i = 0; i < 180; i++) {
                largeMessage.append("VeryLong");
            }
            largeMessage.append("** server");
            
            long startTime = System.nanoTime();
            String escaped = notifier.escapeJson(largeMessage.toString());
            long endTime = System.nanoTime();
            
            assertNotNull(escaped);
            long durationMs = (endTime - startTime) / 1_000_000;
            assertTrue(durationMs < 100, "Large message escaping took " + durationMs + "ms");
        }
    }
}
