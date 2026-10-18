package com.dansplugins.herald;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which notifiers a {@code config.yml} produces at startup, and what is logged about each
 * channel. A real {@link YamlConfiguration} stands in for the plugin configuration, so the
 * keys are read exactly as {@link Herald} reads them on a server.
 */
class NotifierLoaderTest {

    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/123456/abcdef";

    private final List<LogRecord> logRecords = Collections.synchronizedList(new ArrayList<>());
    private Logger logger;

    /**
     * Give each test a logger of its own, so that what the loader logs can be read
     * back without any of it reaching the console.
     */
    @BeforeEach
    void createCapturingLogger() {
        logger = Logger.getLogger("NotifierLoaderTest-" + UUID.randomUUID());
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

    private List<Notifier> load(YamlConfiguration config) {
        return NotifierLoader.load(config, ServerName.DEFAULT, logger);
    }

    private List<String> messagesAt(Level level) {
        return logRecords.stream()
                .filter(record -> record.getLevel() == level)
                .map(LogRecord::getMessage)
                .collect(Collectors.toList());
    }

    private boolean logged(Level level, String fragment) {
        return messagesAt(level).stream().anyMatch(message -> message.contains(fragment));
    }

    /**
     * @return the config.yml that ships inside the plugin JAR, as a fresh install has it
     */
    private static YamlConfiguration bundledConfig() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                NotifierLoaderTest.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
    }

    /**
     * @return a configuration with Discord enabled and pointed at a valid webhook
     */
    private static YamlConfiguration discordConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("discord.enabled", true);
        config.set("discord.webhook-url", WEBHOOK_URL);
        return config;
    }

    /**
     * @return a configuration with every required email key filled in, on port 587 with
     *         STARTTLS left at its default, and no Discord block
     */
    private static YamlConfiguration emailConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("email-recipients", List.of("admin@example.com"));
        config.set("smtp.server", "smtp.example.com");
        config.set("smtp.port", 587);
        config.set("email.sender", "herald@example.com");
        return config;
    }

    @Nested
    @DisplayName("Fresh install")
    class FreshInstallTests {

        @Test
        @DisplayName("The shipped config.yml should load no notifiers and say so")
        void testBundledConfigLoadsNothing() {
            assertTrue(load(bundledConfig()).isEmpty());

            assertTrue(logged(Level.WARNING, "No notification methods are configured"));
        }

        @Test
        @DisplayName("The shipped config.yml should log no incomplete-configuration warning for either channel")
        void testBundledConfigIsNotReportedAsIncomplete() {
            load(bundledConfig());

            assertEquals(List.of(), messagesAt(Level.INFO));
            assertEquals(1, messagesAt(Level.WARNING).size(),
                    "only the no-notifiers warning is expected, got " + messagesAt(Level.WARNING));
        }
    }

    @Nested
    @DisplayName("Discord")
    class DiscordTests {

        @Test
        @DisplayName("An enabled Discord block with a valid webhook should load a Discord notifier")
        void testValidDiscordConfigLoadsNotifier() {
            List<Notifier> notifiers = load(discordConfig());

            assertEquals(1, notifiers.size());
            assertInstanceOf(DiscordNotifier.class, notifiers.get(0));
            assertTrue(logged(Level.INFO, "Discord notifications enabled"));
            assertFalse(logged(Level.WARNING, "No notification methods are configured"));
        }

        @Test
        @DisplayName("A valid webhook with discord.enabled false should load nothing and log nothing about Discord")
        void testDisabledDiscordIsSilent() {
            YamlConfiguration config = discordConfig();
            config.set("discord.enabled", false);
            config.set("email.enabled", false);

            assertTrue(load(config).isEmpty());

            assertTrue(logRecords.stream().noneMatch(record -> record.getMessage().startsWith("Discord")),
                    "discord.enabled false is the shipped default, so nothing about Discord should be logged");
        }

        @Test
        @DisplayName("discord.enabled should default to false when the key is absent")
        void testDiscordEnabledDefaultsToFalse() {
            YamlConfiguration config = discordConfig();
            config.set("discord.enabled", null);

            assertTrue(load(config).stream().noneMatch(notifier -> notifier instanceof DiscordNotifier));
        }

        @Test
        @DisplayName("An enabled Discord block with an unusable webhook should be skipped with a warning naming the key")
        void testInvalidWebhookIsReported() {
            YamlConfiguration config = discordConfig();
            config.set("discord.webhook-url", "discord.com/api/webhooks/123456/abcdef");

            assertTrue(load(config).isEmpty());

            assertTrue(logged(Level.WARNING,
                    "Discord notifications are enabled in config, but the configuration is incomplete: "));
            assertTrue(logged(Level.WARNING, "discord.webhook-url"));
            assertTrue(logged(Level.WARNING, "Discord notifications will be skipped."));
        }

        @Test
        @DisplayName("A blank join message should skip Discord with a warning")
        void testBlankJoinMessageIsReported() {
            YamlConfiguration config = discordConfig();
            config.set("discord.join-messages", List.of("Welcome {player}!", "  "));

            assertTrue(load(config).isEmpty());

            assertTrue(logged(Level.WARNING, "discord.join-messages"));
        }
    }

    @Nested
    @DisplayName("Email")
    class EmailTests {

        @Test
        @DisplayName("A complete email configuration should load an email notifier with no warnings")
        void testCompleteEmailConfigLoadsNotifier() {
            List<Notifier> notifiers = load(emailConfig());

            assertEquals(1, notifiers.size());
            assertInstanceOf(EmailNotifier.class, notifiers.get(0));
            assertTrue(logged(Level.INFO, "Email notifications enabled"));
            assertEquals(List.of(), messagesAt(Level.WARNING));
        }

        @Test
        @DisplayName("email.enabled should default to true, so a config.yml written before the key keeps sending")
        void testEmailEnabledDefaultsToTrue() {
            YamlConfiguration config = emailConfig();
            assertFalse(config.isSet("email.enabled"));

            assertEquals(1, load(config).size());
        }

        @Test
        @DisplayName("email.enabled false should load nothing from a complete configuration and say so")
        void testDisabledEmailIsReported() {
            YamlConfiguration config = emailConfig();
            config.set("email.enabled", false);

            assertTrue(load(config).isEmpty());

            assertTrue(logged(Level.INFO, "Email notifications are disabled in config"));
            assertFalse(logged(Level.WARNING, "Email configuration is incomplete"));
        }

        @Test
        @DisplayName("A partly filled-in email configuration should be reported as incomplete, naming the missing key")
        void testPartialEmailConfigIsReported() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("email.sender", "herald@example.com");

            assertTrue(load(config).isEmpty());

            assertTrue(logged(Level.WARNING, "Email configuration is incomplete: "));
            assertTrue(logged(Level.WARNING, "email-recipients"));
            assertTrue(logged(Level.WARNING, "smtp.server"));
        }

        @Test
        @DisplayName("Both encryption modes on should skip email with the conflict warning")
        void testTlsModeConflictSkipsEmail() {
            YamlConfiguration config = emailConfig();
            config.set("smtp.use-tls", true);
            config.set("smtp.implicit-tls", true);

            assertTrue(load(config).isEmpty());

            assertTrue(logged(Level.WARNING, "'smtp.use-tls' and 'smtp.implicit-tls' are both true"));
            assertTrue(logged(Level.WARNING, "Email notifications will be skipped."));
            assertFalse(logged(Level.WARNING, "Email configuration is incomplete"));
        }

        @Test
        @DisplayName("The conflict warning should be logged on an otherwise empty email configuration")
        void testTlsModeConflictIsReportedWithoutOtherKeys() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("smtp.implicit-tls", true);

            load(config);

            assertTrue(logged(Level.WARNING, "'smtp.use-tls' and 'smtp.implicit-tls' are both true"));
            assertFalse(logged(Level.WARNING, "Email configuration is incomplete"),
                    "nothing else has been filled in, so email is not reported as incomplete");
        }

        @Test
        @DisplayName("A conflict and missing keys should both be reported in the same startup")
        void testTlsModeConflictAndIncompleteConfigAreBothReported() {
            YamlConfiguration config = emailConfig();
            config.set("email.sender", null);
            config.set("smtp.implicit-tls", true);

            assertTrue(load(config).isEmpty());

            assertTrue(logged(Level.WARNING, "'smtp.use-tls' and 'smtp.implicit-tls' are both true"));
            assertTrue(logged(Level.WARNING, "Email configuration is incomplete: "));
            assertTrue(logged(Level.WARNING, "email.sender"));
        }

        @Test
        @DisplayName("Credentials with no encryption should still load email, with a warning")
        void testCredentialExposureIsWarned() {
            YamlConfiguration config = emailConfig();
            config.set("smtp.port", 25);
            config.set("smtp.username", "user");
            config.set("smtp.use-tls", false);

            assertEquals(1, load(config).size());

            assertTrue(logged(Level.WARNING, "'smtp.username' is set but neither"));
        }

        @Test
        @DisplayName("Port 465 without implicit TLS should still load email, with a warning")
        void testPortTlsMismatchIsWarned() {
            YamlConfiguration config = emailConfig();
            config.set("smtp.port", 465);

            assertEquals(1, load(config).size());

            assertTrue(logged(Level.WARNING, "'smtp.port' is 465"));
        }

        @Test
        @DisplayName("Turning off server identity checks should still load email, with a warning")
        void testUnverifiedServerIdentityIsWarned() {
            YamlConfiguration config = emailConfig();
            config.set("smtp.verify-server-identity", false);

            assertEquals(1, load(config).size());

            assertTrue(logged(Level.WARNING, "'smtp.verify-server-identity' is false"));
        }
    }

    /**
     * That each {@code smtp.*} key reaches the parameter it configures. The notifier's
     * constructor takes three booleans side by side, so a key wired to its neighbour would
     * still compile; reading the loaded notifier's session properties back catches it.
     */
    @Nested
    @DisplayName("Email wiring")
    class EmailWiringTests {

        private Properties sessionPropertiesOf(YamlConfiguration config) {
            List<Notifier> notifiers = load(config);
            assertEquals(1, notifiers.size());
            return assertInstanceOf(EmailNotifier.class, notifiers.get(0)).buildSessionProperties();
        }

        @Test
        @DisplayName("smtp.server and smtp.port should reach the session as host and port")
        void testServerAndPortAreWired() {
            YamlConfiguration config = emailConfig();
            config.set("smtp.server", "mail.example.org");
            config.set("smtp.port", 2525);

            Properties props = sessionPropertiesOf(config);

            assertEquals("mail.example.org", props.getProperty("mail.smtp.host"));
            assertEquals("2525", props.getProperty("mail.smtp.port"));
        }

        @Test
        @DisplayName("Left at their defaults, the encryption keys should require STARTTLS and verify the server")
        void testEncryptionDefaultsAreWired() {
            Properties props = sessionPropertiesOf(emailConfig());

            assertEquals("true", props.getProperty("mail.smtp.starttls.enable"));
            assertEquals("true", props.getProperty("mail.smtp.starttls.required"));
            assertNull(props.getProperty("mail.smtp.ssl.enable"));
            assertEquals("true", props.getProperty("mail.smtp.ssl.checkserveridentity"));
        }

        @Test
        @DisplayName("smtp.implicit-tls should switch the session to SMTPS instead of STARTTLS")
        void testImplicitTlsIsWired() {
            YamlConfiguration config = emailConfig();
            config.set("smtp.port", 465);
            config.set("smtp.use-tls", false);
            config.set("smtp.implicit-tls", true);

            Properties props = sessionPropertiesOf(config);

            assertEquals("true", props.getProperty("mail.smtp.ssl.enable"));
            assertNull(props.getProperty("mail.smtp.starttls.enable"));
        }

        @Test
        @DisplayName("smtp.use-tls false with no implicit TLS should leave the session unencrypted")
        void testUseTlsOffIsWired() {
            YamlConfiguration config = emailConfig();
            config.set("smtp.port", 25);
            config.set("smtp.use-tls", false);

            Properties props = sessionPropertiesOf(config);

            assertNull(props.getProperty("mail.smtp.starttls.enable"));
            assertNull(props.getProperty("mail.smtp.ssl.enable"));
            assertNull(props.getProperty("mail.smtp.ssl.checkserveridentity"));
        }

        @Test
        @DisplayName("smtp.verify-server-identity false should turn the certificate name check off")
        void testVerifyServerIdentityIsWired() {
            YamlConfiguration config = emailConfig();
            config.set("smtp.verify-server-identity", false);

            assertEquals("false", sessionPropertiesOf(config).getProperty("mail.smtp.ssl.checkserveridentity"));
        }

        @Test
        @DisplayName("smtp.username should turn SMTP authentication on, and its absence leave it off")
        void testUsernameIsWiredToAuthentication() {
            assertEquals("false", sessionPropertiesOf(emailConfig()).getProperty("mail.smtp.auth"));

            YamlConfiguration config = emailConfig();
            config.set("smtp.username", "user@example.com");
            config.set("smtp.password", "secret");

            assertEquals("true", sessionPropertiesOf(config).getProperty("mail.smtp.auth"));
        }

        @Test
        @DisplayName("An smtp.port outside 1-65535 should skip email and name the key")
        void testOutOfRangePortIsReported() {
            YamlConfiguration config = emailConfig();
            config.set("smtp.port", 70000);

            assertTrue(load(config).isEmpty());

            assertTrue(logged(Level.WARNING, "Email configuration is incomplete: "));
            assertTrue(logged(Level.WARNING, "'smtp.port' must be between 1 and 65535"));
        }
    }

    @Nested
    @DisplayName("Both channels")
    class BothChannelsTests {

        @Test
        @DisplayName("Discord and email together should load both, Discord first")
        void testBothChannelsLoadInOrder() {
            YamlConfiguration config = emailConfig();
            config.set("discord.enabled", true);
            config.set("discord.webhook-url", WEBHOOK_URL);

            List<Notifier> notifiers = load(config);

            assertEquals(2, notifiers.size());
            assertInstanceOf(DiscordNotifier.class, notifiers.get(0));
            assertInstanceOf(EmailNotifier.class, notifiers.get(1));
            assertFalse(logged(Level.WARNING, "No notification methods are configured"));
        }

        @Test
        @DisplayName("A broken Discord block should not stop a working email configuration from loading")
        void testChannelsAreIndependent() {
            YamlConfiguration config = emailConfig();
            config.set("discord.enabled", true);

            List<Notifier> notifiers = load(config);

            assertEquals(1, notifiers.size());
            assertInstanceOf(EmailNotifier.class, notifiers.get(0));
            assertTrue(logged(Level.WARNING, "Discord notifications will be skipped."));
        }
    }
}
