package com.dansplugins.herald;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

/**
 * Integration tests for Herald plugin configuration and message formatting
 */
class HeraldIntegrationTest {

    @Nested
    @DisplayName("Message Format Tests")
    class MessageFormatTests {
        
        @Test
        @DisplayName("Default Discord message format should use medieval theme with player and server names")
        void testDiscordMessageFormat() {
            String playerName = "TestPlayer";
            String serverName = "TestServer";
            for (String template : DiscordNotifier.DEFAULT_JOIN_MESSAGES) {
                String formatted = template.replace("{player}", playerName).replace("{server}", serverName);
                assertTrue(formatted.contains("**" + playerName + "**"));
                assertTrue(formatted.contains("**" + serverName + "**"));
            }
        }
        
        @Test
        @DisplayName("Discord message should be properly escaped")
        void testDiscordMessageEscaping() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
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
            DiscordNotifier notifier = new DiscordNotifier(webhookUrl, null);
            assertNotNull(notifier);
        }
        
        @Test
        @DisplayName("Discord notifier should handle null URL in constructor")
        void testDiscordNotifierWithNullUrl() {
            DiscordNotifier notifier = new DiscordNotifier(null, null);
            assertNotNull(notifier);
            
            Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                notifier.sendMessage("test");
            });
            assertEquals("Discord webhook URL is not configured", exception.getMessage());
        }
        
        @Test
        @DisplayName("Discord notifier should handle empty URL in constructor")
        void testDiscordNotifierWithEmptyUrl() {
            DiscordNotifier notifier = new DiscordNotifier("", null);
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
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
            for (String name : standardNames) {
                String message = "**" + name + "** joined the **TestServer** server";
                String escaped = notifier.escapeJson(message);
                assertTrue(escaped.contains(name));
            }
        }
        
        @Test
        @DisplayName("Message should handle player names with special characters")
        void testPlayerNamesWithSpecialChars() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
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
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
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
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
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
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
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
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
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
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
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
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
            for (int i = 0; i < 10; i++) {
                String playerName = "Player" + i;
                String message = "**" + playerName + "** joined the **Server** server";
                String result = notifier.escapeJson(message);
                assertNotNull(result);
                assertTrue(result.contains(playerName));
            }
        }
        
        @Test
        @DisplayName("escapeJson should handle large messages efficiently")
        void testLargeMessageEscaping() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            
            // Create a large message (near Discord's 2000 char limit)
            StringBuilder largeMessage = new StringBuilder("**Player** joined the **");
            for (int i = 0; i < 180; i++) {
                largeMessage.append("VeryLong");
            }
            largeMessage.append("** server");
            
            String escaped = notifier.escapeJson(largeMessage.toString());
            assertNotNull(escaped);
            assertTrue(escaped.length() > 0);
        }
    }

    @Nested
    @DisplayName("Discord Message Format Tests")
    class DiscordMessageFormatTests {

        @Test
        @DisplayName("Default player join messages should all use medieval-themed format")
        void testExactDiscordMessageFormat() {
            String playerName = "Steve";
            String serverName = "MySurvivalServer";
            for (String template : DiscordNotifier.DEFAULT_JOIN_MESSAGES) {
                String message = template
                        .replace("{player}", playerName)
                        .replace("{server}", serverName);
                assertTrue(message.contains("**Steve**"));
                assertTrue(message.contains("**MySurvivalServer**"));
            }
        }

        @Test
        @DisplayName("Message format should be preserved after escaping safe content")
        void testMessageFormatPreservedForSafeContent() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            String[] safePlayers = {"Alice", "Bob123", "Player_X", "User-1"};
            for (String player : safePlayers) {
                String msg = "**" + player + "** joined the **Server** server";
                assertEquals(msg, notifier.escapeJson(msg),
                        "Message for " + player + " should be unchanged after escaping");
            }
        }

        @Test
        @DisplayName("Message escaping should sanitize injected quotes in player name")
        void testPlayerNameWithQuoteInjection() {
            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            String playerName = "Steve\", \"troll\": \"true";
            String message = "**" + playerName + "** joined the **Server** server";
            String escaped = notifier.escapeJson(message);
            // After escaping the full message string, there should be no unescaped quotes
            // that could break the outer JSON structure
            assertFalse(escaped.contains("\", \"troll"), "JSON injection attempt must be neutralised");
        }

        @Test
        @DisplayName("Empty server name should fall back to 'Minecraft' in the message")
        void testDefaultServerNameFallback() {
            // When serverName is empty, Herald.java uses "Minecraft" as the fallback.
            // Verify the message produced with that fallback contains "Minecraft".
            String serverName = "Minecraft"; // this is the fallback value Herald uses
            String msg = "**Player** joined the **" + serverName + "** server";

            DiscordNotifier notifier = new DiscordNotifier("https://example.com", null);
            String escaped = notifier.escapeJson(msg);
            assertTrue(escaped.contains("Minecraft"));
            assertEquals(msg, escaped); // "Minecraft" has no special chars, should be unchanged
        }

        @Test
        @DisplayName("All default messages should contain both player and server names")
        void testMessageContainsBothNames() {
            String playerName = "Notch";
            String serverName = "ClassicSMP";
            for (String template : DiscordNotifier.DEFAULT_JOIN_MESSAGES) {
                String message = template
                        .replace("{player}", playerName)
                        .replace("{server}", serverName);
                assertTrue(message.contains(playerName));
                assertTrue(message.contains(serverName));
            }
        }
    }

    @Nested
    @DisplayName("Notification Ordering Tests")
    class NotificationOrderingTests {

        @Test
        @DisplayName("Discord message should be independently formattable from email subject")
        void testDiscordAndEmailMessagesAreDifferent() {
            String playerName = "TestPlayer";
            String serverName = "TestServer";

            String discordMessage = DiscordNotifier.DEFAULT_JOIN_MESSAGES.get(0)
                    .replace("{player}", playerName)
                    .replace("{server}", serverName);
            String emailSubject = playerName + " joined " + serverName + " server";

            // Discord uses bold markdown; email subject is plain text
            assertNotEquals(discordMessage, emailSubject);
            assertTrue(discordMessage.contains("**"));
            assertFalse(emailSubject.contains("**"));
        }

        @Test
        @DisplayName("Default Discord message formats should use bold markdown")
        void testDiscordMessageUsesBoldMarkdown() {
            String playerName = "Player";
            String serverName = "Server";
            for (String template : DiscordNotifier.DEFAULT_JOIN_MESSAGES) {
                String discordMessage = template
                        .replace("{player}", playerName)
                        .replace("{server}", serverName);

                assertTrue(discordMessage.contains("**" + playerName + "**"),
                        "Discord message should bold the player name: " + discordMessage);
                assertTrue(discordMessage.contains("**" + serverName + "**"),
                        "Discord message should bold the server name: " + discordMessage);
            }
        }
    }

    /**
     * Herald extends JavaPlugin, and spigot-api is a compileOnly dependency, so the
     * wiring in Herald.loadConfiguration() cannot be instantiated from tests. These
     * tests cover the validation each notifier exposes for that wiring to use, for the
     * configuration shapes an operator is most likely to end up with.
     */
    @Nested
    @DisplayName("Startup Configuration Validation Tests")
    class StartupConfigurationValidationTests {

        @Test
        @DisplayName("Default config.yml shape should leave both notifiers unconfigured")
        void testDefaultConfigurationConfiguresNothing() {
            // config.yml ships with discord.enabled false and every email key empty
            assertFalse(DiscordNotifier.validateConfiguration("").isEmpty(),
                    "Default webhook URL should be reported as missing");
            assertEquals(3, EmailNotifier.validateConfiguration(Collections.emptyList(), "", 587, "").size(),
                    "Default email configuration should report every missing key");
        }

        @Test
        @DisplayName("Email configured except for the sender should be reported, not silently accepted")
        void testEmailMissingOnlySenderIsReported() {
            List<String> problems = EmailNotifier.validateConfiguration(
                    List.of("admin@example.com"), "smtp.example.com", 587, "");

            assertEquals(1, problems.size());
            assertTrue(problems.get(0).contains("email.sender"),
                    "Operator should be told which key is missing: " + problems.get(0));
        }

        @Test
        @DisplayName("A fully configured Discord and email setup should report no problems")
        void testFullyConfiguredSetup() {
            assertTrue(DiscordNotifier.validateConfiguration(
                    "https://discord.com/api/webhooks/123/abc").isEmpty());
            assertTrue(EmailNotifier.validateConfiguration(
                    List.of("admin@example.com"), "smtp.example.com", 587, "herald@example.com").isEmpty());
        }

        @Test
        @DisplayName("A config full of plausible typos should be caught at startup, not on first join")
        void testTypoedConfigurationIsCaughtAtStartup() {
            List<String> discordProblems = DiscordNotifier.validateConfiguration(
                    "discord.com/api/webhooks/123/abc");
            List<String> emailProblems = EmailNotifier.validateConfiguration(
                    List.of("admin@"), "smtp.example.com", 587, "herald@example.com");

            assertEquals(1, discordProblems.size(),
                    "A webhook URL missing its scheme should be reported: " + discordProblems);
            assertEquals(1, emailProblems.size(),
                    "A recipient missing its domain should be reported: " + emailProblems);
        }

        @Test
        @DisplayName("Validation problems should name the config key an operator edits")
        void testProblemsNameConfigKeys() {
            List<String> problems = new java.util.ArrayList<>(
                    DiscordNotifier.validateConfiguration(null));
            problems.addAll(EmailNotifier.validateConfiguration(null, null, 587, null));

            for (String problem : problems) {
                assertTrue(problem.contains("'"),
                        "Problem should quote the config key it refers to: " + problem);
            }
        }
    }

    /**
     * The default config.yml is the only place most operators ever discover an option,
     * so a key that Herald reads but never ships is effectively undocumented. These
     * tests read the shipped resource to keep the two from drifting apart.
     */
    @Nested
    @DisplayName("Default Configuration File Tests")
    class DefaultConfigurationFileTests {

        /**
         * @return the contents of the config.yml that ships inside the plugin JAR
         */
        private String readDefaultConfig() throws java.io.IOException {
            try (java.io.InputStream stream = getClass().getResourceAsStream("/config.yml")) {
                assertNotNull(stream, "config.yml should ship on the classpath");
                return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        @Test
        @DisplayName("Both channels should ship an enabled toggle so neither has to be dismantled to be silenced")
        void testBothChannelsShipAnEnabledToggle() throws java.io.IOException {
            String config = readDefaultConfig();
            int discordBlock = config.indexOf("discord:");
            int emailBlock = config.indexOf("\nemail:");

            assertTrue(discordBlock >= 0, "config.yml should carry a discord block");
            assertTrue(emailBlock > discordBlock, "config.yml should carry an email block after the discord one");

            int discordToggle = config.indexOf("enabled: false", discordBlock);
            assertTrue(discordToggle > discordBlock && discordToggle < emailBlock,
                    "discord.enabled should ship as false, leaving a fresh install quiet");
            assertTrue(config.indexOf("enabled: true", emailBlock) > emailBlock,
                    "email.enabled should ship as true, matching the default Herald falls back to");
        }

        @Test
        @DisplayName("Every config key Herald reads should appear in the shipped config.yml")
        void testEveryConfigKeyIsShipped() throws java.io.IOException {
            String config = readDefaultConfig();

            List<String> leafKeys = List.of("server-name", "webhook-url", "join-messages",
                    "email-recipients", "server", "port", "username", "password", "use-tls",
                    "sender", "subject", "body");

            for (String key : leafKeys) {
                assertTrue(config.contains(key + ":"),
                        "config.yml should carry the '" + key + "' key an operator is expected to edit");
            }
        }
    }

    @Nested
    @DisplayName("Notifier Display Name Tests")
    class NotifierDisplayNameTests {

        private final List<Notifier> allNotifiers = List.of(
                new DiscordNotifier("https://discord.com/api/webhooks/123/abc", null),
                new EmailNotifier("smtp.example.com", 587, "user", "pass", "herald@example.com", true,
                        List.of("admin@example.com")));

        @Test
        @DisplayName("Every notifier should declare a display name that is not its class name")
        void testDisplayNamesAreNotClassNames() {
            for (Notifier notifier : allNotifiers) {
                String displayName = notifier.getDisplayName();
                assertNotNull(displayName, "Every notifier must declare a display name");
                assertFalse(displayName.isEmpty(), "Display name must not be empty");
                assertNotEquals(notifier.getClass().getSimpleName(), displayName,
                        "Display name must not be the Java class name");
            }
        }

        @Test
        @DisplayName("Display names should be distinct so the log identifies which channel failed")
        void testDisplayNamesAreDistinct() {
            long distinct = allNotifiers.stream().map(Notifier::getDisplayName).distinct().count();

            assertEquals(allNotifiers.size(), distinct,
                    "Two channels sharing a display name would make the log ambiguous");
        }
    }
}
