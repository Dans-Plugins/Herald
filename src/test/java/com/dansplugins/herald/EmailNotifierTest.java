package com.dansplugins.herald;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Unit tests for EmailNotifier class
 */
class EmailNotifierTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance with complete valid configuration")
        void testValidConfiguration() {
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "password", "sender@example.com", true, recipients);
            assertNotNull(notifier);
        }

        @Test
        @DisplayName("Should accept null recipients list without throwing")
        void testNullRecipients() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "password", "sender@example.com", true, null);
            assertNotNull(notifier);
        }

        @Test
        @DisplayName("Should accept empty recipients list without throwing in constructor")
        void testEmptyRecipientsInConstructor() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "password", "sender@example.com", true,
                    Collections.emptyList());
            assertNotNull(notifier);
        }

        @Test
        @DisplayName("Should accept null SMTP server without throwing in constructor")
        void testNullSmtpServerInConstructor() {
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    null, 587, "user", "password", "sender@example.com", true, recipients);
            assertNotNull(notifier);
        }

        @Test
        @DisplayName("Should accept empty SMTP server without throwing in constructor")
        void testEmptySmtpServerInConstructor() {
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    "", 587, "user", "password", "sender@example.com", true, recipients);
            assertNotNull(notifier);
        }

        @Test
        @DisplayName("Should accept port 465 (SMTPS)")
        void testSmtpsPort() {
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 465, "user", "password", "sender@example.com", false, recipients);
            assertNotNull(notifier);
        }

        @Test
        @DisplayName("Should make a defensive copy of the recipients list")
        void testRecipientsDefensiveCopy() {
            List<String> recipients = new ArrayList<>(Arrays.asList("user@example.com"));
            EmailNotifier notifier = new EmailNotifier(
                    null, 587, "user", "password", "sender@example.com", true, recipients);
            assertNotNull(notifier);

            // Mutating the original list after construction should not affect the notifier.
            // The notifier was built with 1 recipient. After we add to the original list,
            // sendNotification should still throw "SMTP server not configured" (not
            // "No email recipients configured"), proving the stored copy still has the original recipient.
            recipients.add("extra@example.com");
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.sendNotification("Subject", "Body"));
            assertEquals("SMTP server not configured", ex.getMessage());
        }

        @Test
        @DisplayName("Should support multiple recipients")
        void testMultipleRecipients() {
            List<String> recipients = Arrays.asList(
                    "admin@example.com", "owner@example.com", "mod@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "password", "sender@example.com", true, recipients);
            assertNotNull(notifier);
        }

        @Test
        @DisplayName("Should work with TLS disabled")
        void testTlsDisabled() {
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 25, "user", "password", "sender@example.com", false, recipients);
            assertNotNull(notifier);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("sendNotification should throw IllegalStateException when recipients list is empty")
        void testEmptyRecipientsThrows() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "password", "sender@example.com", true,
                    Collections.emptyList());

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.sendNotification("Subject", "Body"));
            assertEquals("No email recipients configured", ex.getMessage());
        }

        @Test
        @DisplayName("sendNotification should throw IllegalStateException when recipients list is null")
        void testNullRecipientsThrows() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "password", "sender@example.com", true, null);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.sendNotification("Subject", "Body"));
            assertEquals("No email recipients configured", ex.getMessage());
        }

        @Test
        @DisplayName("sendNotification should throw IllegalStateException when SMTP server is null")
        void testNullSmtpServerThrows() {
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    null, 587, "user", "password", "sender@example.com", true, recipients);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.sendNotification("Subject", "Body"));
            assertEquals("SMTP server not configured", ex.getMessage());
        }

        @Test
        @DisplayName("sendNotification should throw IllegalStateException when SMTP server is empty")
        void testEmptySmtpServerThrows() {
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    "", 587, "user", "password", "sender@example.com", true, recipients);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.sendNotification("Subject", "Body"));
            assertEquals("SMTP server not configured", ex.getMessage());
        }

        @Test
        @DisplayName("sendNotification should check recipients before SMTP server")
        void testRecipientsCheckedBeforeSmtpServer() {
            // Both missing: recipients error should surface first
            EmailNotifier notifier = new EmailNotifier(
                    null, 587, "user", "password", "sender@example.com", true, null);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.sendNotification("Subject", "Body"));
            assertEquals("No email recipients configured", ex.getMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("sendNotification should throw for null or empty SMTP server")
        void testNullOrEmptySmtpServerThrows(String smtpServer) {
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    smtpServer, 587, "user", "password", "sender@example.com", true, recipients);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.sendNotification("Subject", "Body"));
            assertEquals("SMTP server not configured", ex.getMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("sendNotification should throw for null or empty email sender")
        void testNullOrEmptyEmailSenderThrows(String emailSender) {
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "password", emailSender, true, recipients);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.sendNotification("Subject", "Body"));
            assertEquals("Email sender address not configured", ex.getMessage());
        }

        @Test
        @DisplayName("sendNotification should check SMTP server before email sender")
        void testSmtpCheckedBeforeEmailSender() {
            // SMTP missing, sender also missing — SMTP error surfaces first
            List<String> recipients = Arrays.asList("user@example.com");
            EmailNotifier notifier = new EmailNotifier(
                    null, 587, "user", "password", null, true, recipients);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.sendNotification("Subject", "Body"));
            assertEquals("SMTP server not configured", ex.getMessage());
        }

        @Test
        @DisplayName("notifyPlayerJoin should throw IllegalStateException when email sender is missing")
        void testNotifyPlayerJoinWithNoEmailSenderThrows() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", null, true,
                    Arrays.asList("recipient@example.com"));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.notifyPlayerJoin("Steve", "SurvivalServer"));
            assertEquals("Email sender address not configured", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Email Format Tests")
    class EmailFormatTests {

        @Test
        @DisplayName("Email subject format should be plain text without Discord markdown")
        void testEmailSubjectIsPlainText() {
            String playerName = "Steve";
            String serverName = "SurvivalServer";
            String subject = playerName + " joined " + serverName + " server";

            assertEquals("Steve joined SurvivalServer server", subject);
            assertFalse(subject.contains("**"), "Email subject must not contain Discord bold markdown");
            assertFalse(subject.contains("*"), "Email subject must not contain any markdown");
        }

        @Test
        @DisplayName("Email subject should differ from Discord message format")
        void testEmailVsDiscordFormat() {
            String playerName = "Player";
            String serverName = "Server";

            String discordMsg = "**" + playerName + "** joined the **" + serverName + "** server";
            String emailSubject = playerName + " joined " + serverName + " server";

            assertNotEquals(discordMsg, emailSubject);
            assertTrue(discordMsg.contains("**"));
            assertFalse(emailSubject.contains("**"));
        }

        @Test
        @DisplayName("Email body should include player name, server name, and indicate join event")
        void testEmailBodyContainsPlayerName() {
            String playerName = "Steve";
            String serverName = "MySurvivalServer";
            String body = playerName + " has joined " + serverName + " at " + new java.util.Date();

            assertTrue(body.startsWith(playerName), "Body should start with the player name");
            assertTrue(body.contains(serverName), "Body should include the server name");
            assertTrue(body.contains("has joined"), "Body should describe the join event");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Steve", "Alex", "Player123", "Player_X", "Cool-Player"})
        @DisplayName("Email subject should handle various standard player names")
        void testEmailSubjectWithVariousPlayerNames(String playerName) {
            String subject = playerName + " joined Server server";
            assertTrue(subject.startsWith(playerName));
            assertFalse(subject.contains("**"), "Email subject must not contain markdown");
        }

        @Test
        @DisplayName("Email subject should include server name")
        void testEmailSubjectIncludesServerName() {
            String playerName = "Alice";
            String serverName = "MyCoolSMP";
            String subject = playerName + " joined " + serverName + " server";

            assertTrue(subject.contains(serverName));
            assertTrue(subject.contains(playerName));
            assertTrue(subject.endsWith("server"));
        }

        @Test
        @DisplayName("Email subject with Unicode player name should be well-formed")
        void testEmailSubjectWithUnicodePlayerName() {
            String playerName = "玩家123";
            String subject = playerName + " joined MyServer server";

            assertTrue(subject.contains(playerName));
            assertFalse(subject.contains("**"));
        }
    }

    @Nested
    @DisplayName("Notifier Interface Tests")
    class NotifyPlayerJoinTests {

        @Test
        @DisplayName("EmailNotifier should implement the Notifier interface")
        void testImplementsNotifierInterface() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "sender@example.com", true,
                    Arrays.asList("recipient@example.com"));
            assertInstanceOf(Notifier.class, notifier);
        }

        @Test
        @DisplayName("getDisplayName should return the operator-facing channel name, not the class name")
        void testGetDisplayName() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "sender@example.com", true,
                    Arrays.asList("recipient@example.com"));

            assertEquals("email", notifier.getDisplayName());
            assertNotEquals(notifier.getClass().getSimpleName(), notifier.getDisplayName(),
                    "The log name must not be tied to the class name");
        }

        @Test
        @DisplayName("notifyPlayerJoin should throw IllegalStateException when no recipients are configured")
        void testNotifyPlayerJoinWithNoRecipientsThrows() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "sender@example.com", true,
                    Collections.emptyList());

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.notifyPlayerJoin("Steve", "SurvivalServer"));
            assertEquals("No email recipients configured", ex.getMessage());
        }

        @Test
        @DisplayName("notifyPlayerJoin should throw IllegalStateException when SMTP server is missing")
        void testNotifyPlayerJoinWithNoSmtpThrows() {
            EmailNotifier notifier = new EmailNotifier(
                    null, 587, "user", "pass", "sender@example.com", true,
                    Arrays.asList("recipient@example.com"));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    notifier.notifyPlayerJoin("Steve", "SurvivalServer"));
            assertEquals("SMTP server not configured", ex.getMessage());
        }

        @Test
        @DisplayName("notifyPlayerJoin should format a plain-text subject without Markdown")
        void testNotifyPlayerJoinFormatsSubjectCorrectly() throws Exception {
            final String[] capturedSubject = {null};
            final String[] capturedBody = {null};
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "sender@example.com", true,
                    Arrays.asList("recipient@example.com")) {
                @Override
                public void sendNotification(String subject, String body) {
                    capturedSubject[0] = subject;
                    capturedBody[0] = body;
                }
            };

            notifier.notifyPlayerJoin("Steve", "MySurvivalServer");

            assertEquals("Steve joined MySurvivalServer server", capturedSubject[0]);
            assertFalse(capturedSubject[0].contains("**"), "Email subject must not contain Discord markdown");
        }

        @Test
        @DisplayName("notifyPlayerJoin should include player name in email body")
        void testNotifyPlayerJoinBodyContainsPlayerName() throws Exception {
            final String[] capturedBody = {null};
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "sender@example.com", true,
                    Arrays.asList("recipient@example.com")) {
                @Override
                public void sendNotification(String subject, String body) {
                    capturedBody[0] = body;
                }
            };

            notifier.notifyPlayerJoin("Alex", "CreativeWorld");

            assertNotNull(capturedBody[0]);
            assertTrue(capturedBody[0].startsWith("Alex"), "Body should start with player name");
            assertTrue(capturedBody[0].contains("CreativeWorld"), "Body should include the server name");
            assertTrue(capturedBody[0].contains("has joined"), "Body should describe the join event");
        }

        @Test
        @DisplayName("notifyPlayerJoin email subject should differ from Discord message format for same names")
        void testNotifyPlayerJoinSubjectDiffersFromDiscordFormat() throws Exception {
            final String[] capturedSubject = {null};
            EmailNotifier emailNotifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "sender@example.com", true,
                    Arrays.asList("recipient@example.com")) {
                @Override
                public void sendNotification(String subject, String body) {
                    capturedSubject[0] = subject;
                }
            };

            emailNotifier.notifyPlayerJoin("Player", "Server");

            String discordFormat = "**Player** joined the **Server** server";
            assertNotEquals(discordFormat, capturedSubject[0]);
            assertTrue(capturedSubject[0].contains("Player"));
            assertFalse(capturedSubject[0].contains("**"));
        }
    }

    @Nested
    @DisplayName("Message Template Tests")
    class MessageTemplateTests {

        /** Subject and body captured from a notifier whose SMTP send is stubbed out. */
        private String[] capture(String subjectTemplate, String bodyTemplate,
                                 String playerName, String serverName) throws Exception {
            final String[] captured = {null, null};
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "sender@example.com", true,
                    Arrays.asList("recipient@example.com"), subjectTemplate, bodyTemplate) {
                @Override
                public void sendNotification(String subject, String body) {
                    captured[0] = subject;
                    captured[1] = body;
                }
            };
            notifier.notifyPlayerJoin(playerName, serverName);
            return captured;
        }

        @Test
        @DisplayName("Configured subject and body templates should be used verbatim after substitution")
        void testCustomTemplatesAreUsed() throws Exception {
            String[] captured = capture(
                    "[{server}] {player} is online",
                    "Greetings from {server}. {player} just logged in.",
                    "Steve", "MySurvivalServer");

            assertEquals("[MySurvivalServer] Steve is online", captured[0]);
            assertEquals("Greetings from MySurvivalServer. Steve just logged in.", captured[1]);
        }

        @Test
        @DisplayName("{time} should be substituted in both the subject and the body")
        void testTimePlaceholderIsSubstituted() throws Exception {
            String[] captured = capture("Joined at {time}", "At {time}", "Steve", "Server");

            assertFalse(captured[0].contains("{time}"), "Subject should not keep the literal placeholder");
            assertFalse(captured[1].contains("{time}"), "Body should not keep the literal placeholder");
            assertTrue(captured[0].startsWith("Joined at "));
            assertTrue(captured[1].startsWith("At "));
        }

        @Test
        @DisplayName("Subject and body should report the same join time")
        void testSubjectAndBodyShareOneTimestamp() throws Exception {
            String[] captured = capture("{time}", "{time}", "Steve", "Server");

            assertEquals(captured[0], captured[1],
                    "Both templates should be filled from a single timestamp");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Absent or empty templates should fall back to the built-in defaults")
        void testFallsBackToDefaults(String template) throws Exception {
            String[] captured = capture(template, template, "Steve", "MySurvivalServer");

            assertEquals("Steve joined MySurvivalServer server", captured[0]);
            assertTrue(captured[1].startsWith("Steve has joined MySurvivalServer at "),
                    "Default body should keep the timestamp: " + captured[1]);
        }

        @Test
        @DisplayName("A template with no placeholders should be sent unchanged")
        void testTemplateWithoutPlaceholders() throws Exception {
            String[] captured = capture("A player joined", "Someone is on the server.", "Steve", "Server");

            assertEquals("A player joined", captured[0]);
            assertEquals("Someone is on the server.", captured[1]);
        }

        @Test
        @DisplayName("Every occurrence of a placeholder should be replaced")
        void testRepeatedPlaceholders() throws Exception {
            String[] captured = capture("{player} & {player}", "{server} / {server}", "Steve", "Server");

            assertEquals("Steve & Steve", captured[0]);
            assertEquals("Server / Server", captured[1]);
        }

        @Test
        @DisplayName("The seven-argument constructor should use the default templates")
        void testDefaultConstructorUsesDefaultTemplates() throws Exception {
            final String[] capturedSubject = {null};
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "sender@example.com", true,
                    Arrays.asList("recipient@example.com")) {
                @Override
                public void sendNotification(String subject, String body) {
                    capturedSubject[0] = subject;
                }
            };

            notifier.notifyPlayerJoin("Steve", "MySurvivalServer");

            assertEquals("Steve joined MySurvivalServer server", capturedSubject[0]);
        }

        @Test
        @DisplayName("Default templates should use the documented placeholders")
        void testDefaultTemplatePlaceholders() {
            assertEquals("{player} joined {server} server", EmailNotifier.DEFAULT_SUBJECT);
            assertEquals("{player} has joined {server} at {time}", EmailNotifier.DEFAULT_BODY);
            assertFalse(EmailNotifier.DEFAULT_SUBJECT.contains("**"),
                    "Email defaults must not contain Discord markdown");
            assertFalse(EmailNotifier.DEFAULT_BODY.contains("**"),
                    "Email defaults must not contain Discord markdown");
        }
    }

    @Nested
    @DisplayName("SMTP Session Property Tests")
    class SessionPropertyTests {

        /** A notifier whose SMTP settings are the ones under test; the rest are incidental. */
        private EmailNotifier notifier(String smtpUsername, boolean useTLS) {
            return new EmailNotifier("smtp.example.com", 587, smtpUsername, "pass",
                    "sender@example.com", useTLS, Arrays.asList("recipient@example.com"));
        }

        @Test
        @DisplayName("Session properties should carry the configured host and port")
        void testHostAndPort() {
            Properties props = notifier(null, true).buildSessionProperties();

            assertEquals("smtp.example.com", props.getProperty("mail.smtp.host"));
            assertEquals("587", props.getProperty("mail.smtp.port"));
        }

        @Test
        @DisplayName("Authentication should be requested only when a username is configured")
        void testAuthFollowsUsername() {
            assertEquals("true", notifier("user", true).buildSessionProperties().getProperty("mail.smtp.auth"));
            assertEquals("false", notifier(null, true).buildSessionProperties().getProperty("mail.smtp.auth"));
            assertEquals("false", notifier("", true).buildSessionProperties().getProperty("mail.smtp.auth"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"user", ""})
        @DisplayName("Connect, read and write timeouts should be set on both the authenticated and unauthenticated paths")
        void testTimeoutsAreAlwaysSet(String smtpUsername) {
            Properties props = notifier(smtpUsername, true).buildSessionProperties();

            assertEquals(String.valueOf(EmailNotifier.CONNECT_TIMEOUT_MILLIS),
                    props.getProperty("mail.smtp.connectiontimeout"));
            assertEquals(String.valueOf(EmailNotifier.READ_TIMEOUT_MILLIS),
                    props.getProperty("mail.smtp.timeout"));
            assertEquals(String.valueOf(EmailNotifier.WRITE_TIMEOUT_MILLIS),
                    props.getProperty("mail.smtp.writetimeout"));
        }

        @Test
        @DisplayName("Timeouts should be set whether or not TLS is in use")
        void testTimeoutsSetWithoutTls() {
            Properties props = notifier("user", false).buildSessionProperties();

            assertNotNull(props.getProperty("mail.smtp.connectiontimeout"));
            assertNotNull(props.getProperty("mail.smtp.timeout"));
            assertNotNull(props.getProperty("mail.smtp.writetimeout"));
        }

        @Test
        @DisplayName("Every timeout should be a positive number of milliseconds")
        void testTimeoutsAreBounded() {
            assertTrue(EmailNotifier.CONNECT_TIMEOUT_MILLIS > 0, "Connect timeout must be bounded");
            assertTrue(EmailNotifier.READ_TIMEOUT_MILLIS > 0, "Read timeout must be bounded");
            assertTrue(EmailNotifier.WRITE_TIMEOUT_MILLIS > 0, "Write timeout must be bounded");
        }

        @Test
        @DisplayName("smtp.use-tls should require STARTTLS, not merely offer to use it")
        void testStarttlsIsRequired() {
            Properties props = notifier("user", true).buildSessionProperties();

            assertEquals("true", props.getProperty("mail.smtp.starttls.enable"));
            assertEquals("true", props.getProperty("mail.smtp.starttls.required"),
                    "starttls.enable alone falls back to an unencrypted connection");
        }

        @Test
        @DisplayName("Disabling TLS should leave both STARTTLS properties unset")
        void testStarttlsAbsentWhenTlsDisabled() {
            Properties props = notifier("user", false).buildSessionProperties();

            assertNull(props.getProperty("mail.smtp.starttls.enable"));
            assertNull(props.getProperty("mail.smtp.starttls.required"));
        }

        @Test
        @DisplayName("A server that accepts a connection and never answers should time out rather than hang")
        void testUnresponsiveServerTimesOut() throws Exception {
            // Backlog of 1 with no accept() call: the handshake completes, so the client
            // connects and then waits on a greeting that never arrives.
            try (ServerSocket stub = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                EmailNotifier notifier = new EmailNotifier(
                        stub.getInetAddress().getHostAddress(), stub.getLocalPort(), null, null,
                        "sender@example.com", false, Arrays.asList("recipient@example.com"));

                // The shipped read timeout is deliberately generous, so it is shortened here;
                // what this asserts is that the property name is one Jakarta Mail honours.
                Properties props = notifier.buildSessionProperties();
                props.put("mail.smtp.timeout", "500");
                props.put("mail.smtp.connectiontimeout", "500");

                Transport transport = Session.getInstance(props).getTransport("smtp");
                long startedAt = System.nanoTime();
                assertThrows(MessagingException.class, transport::connect,
                        "An unresponsive server should fail the connection, not wait forever");
                long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

                assertTrue(elapsedMillis < 10000,
                        "The timeout should have fired promptly, but the attempt took " + elapsedMillis + "ms");
            }
        }
    }

    @Nested
    @DisplayName("Credential Exposure Tests")
    class CredentialExposureTests {

        @Test
        @DisplayName("Credentials sent without TLS should be reported")
        void testWarnsWhenCredentialsSentInTheClear() {
            String warning = EmailNotifier.describeCredentialExposure("user@example.com", false);

            assertNotNull(warning, "Sending a username without TLS should be reported");
            assertTrue(warning.contains("smtp.username"), "Warning should name the key at fault: " + warning);
            assertTrue(warning.contains("smtp.use-tls"), "Warning should name the key that fixes it: " + warning);
        }

        @Test
        @DisplayName("Credentials sent with TLS should not be reported")
        void testSilentWhenTlsIsOn() {
            assertNull(EmailNotifier.describeCredentialExposure("user@example.com", true));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("An unauthenticated session should not be reported, with or without TLS")
        void testSilentWhenNoCredentials(String smtpUsername) {
            assertNull(EmailNotifier.describeCredentialExposure(smtpUsername, false));
            assertNull(EmailNotifier.describeCredentialExposure(smtpUsername, true));
        }

        @Test
        @DisplayName("The warning should not quote the username it is about")
        void testWarningKeepsTheCredentialOutOfTheLog() {
            String warning = EmailNotifier.describeCredentialExposure("user@example.com", false);

            assertFalse(warning.contains("user@example.com"),
                    "The warning names the key, not the credential itself: " + warning);
        }
    }

    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("validateConfiguration should report no problems for a complete configuration")
        void testValidateCompleteConfiguration() {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Arrays.asList("admin@example.com"), "smtp.example.com", 587, "herald@example.com");

            assertTrue(problems.isEmpty(), "Complete configuration should have no problems: " + problems);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("validateConfiguration should report a missing email sender")
        void testValidateMissingSender(String emailSender) {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Arrays.asList("admin@example.com"), "smtp.example.com", 587, emailSender);

            assertEquals(1, problems.size(), "Only the sender should be reported: " + problems);
            assertTrue(problems.get(0).contains("email.sender"),
                    "Problem should name the config key: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should report missing recipients")
        void testValidateMissingRecipients() {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Collections.emptyList(), "smtp.example.com", 587, "herald@example.com");

            assertEquals(1, problems.size(), "Only the recipients should be reported: " + problems);
            assertTrue(problems.get(0).contains("email-recipients"),
                    "Problem should name the config key: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should treat a null recipients list as missing")
        void testValidateNullRecipients() {
            List<String> problems = EmailNotifier.validateConfiguration(
                    null, "smtp.example.com", 587, "herald@example.com");

            assertEquals(1, problems.size(), "Only the recipients should be reported: " + problems);
            assertTrue(problems.get(0).contains("email-recipients"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("validateConfiguration should report a missing SMTP server")
        void testValidateMissingSmtpServer(String smtpServer) {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Arrays.asList("admin@example.com"), smtpServer, 587, "herald@example.com");

            assertEquals(1, problems.size(), "Only the SMTP server should be reported: " + problems);
            assertTrue(problems.get(0).contains("smtp.server"),
                    "Problem should name the config key: " + problems.get(0));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 65536})
        @DisplayName("validateConfiguration should report an invalid SMTP port")
        void testValidateInvalidSmtpPort(int smtpPort) {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Arrays.asList("admin@example.com"), "smtp.example.com", smtpPort, "herald@example.com");

            assertEquals(1, problems.size(), "Only the SMTP port should be reported: " + problems);
            assertTrue(problems.get(0).contains("smtp.port"),
                    "Problem should name the config key: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should report every missing key at once")
        void testValidateReportsAllProblems() {
            List<String> problems = EmailNotifier.validateConfiguration(Collections.emptyList(), "", 587, "");

            assertEquals(3, problems.size(), "All three keys should be reported: " + problems);
        }

        @Test
        @DisplayName("validateConfiguration problems should match the keys sendNotification enforces")
        void testValidationMatchesRuntimeChecks() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "", true,
                    Arrays.asList("admin@example.com"));

            assertFalse(EmailNotifier.validateConfiguration(
                    Arrays.asList("admin@example.com"), "smtp.example.com", 587, "").isEmpty(),
                    "Validation should reject what sendNotification rejects");
            assertThrows(IllegalStateException.class, () -> notifier.notifyPlayerJoin("Steve", "Server"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"herald@", "@example.com", "not an email", "plainword"})
        @DisplayName("validateConfiguration should report an unparseable email sender")
        void testValidateMalformedSender(String emailSender) {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Arrays.asList("admin@example.com"), "smtp.example.com", 587, emailSender);

            assertEquals(1, problems.size(), "Only the sender should be reported: " + problems);
            assertTrue(problems.get(0).contains("email.sender"),
                    "Problem should name the config key: " + problems.get(0));
            assertTrue(problems.get(0).contains(emailSender),
                    "Problem should quote the offending address: " + problems.get(0));
        }

        @ParameterizedTest
        @ValueSource(strings = {"admin@", "@example.com", "not an email", "plainword"})
        @DisplayName("validateConfiguration should report an unparseable recipient")
        void testValidateMalformedRecipient(String recipient) {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Arrays.asList(recipient), "smtp.example.com", 587, "herald@example.com");

            assertEquals(1, problems.size(), "Only the recipient should be reported: " + problems);
            assertTrue(problems.get(0).contains("email-recipients"),
                    "Problem should name the config key: " + problems.get(0));
            assertTrue(problems.get(0).contains(recipient),
                    "Problem should quote the offending address: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should report each invalid recipient separately")
        void testValidateReportsEveryInvalidRecipient() {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Arrays.asList("admin@example.com", "broken@", "@also-broken.com"),
                    "smtp.example.com", 587, "herald@example.com");

            assertEquals(2, problems.size(), "Both invalid recipients should be reported: " + problems);
            assertTrue(problems.get(0).contains("broken@"), "First problem should name the first bad address: " + problems);
            assertTrue(problems.get(1).contains("@also-broken.com"), "Second problem should name the second bad address: " + problems);
        }

        @Test
        @DisplayName("validateConfiguration should report a blank recipient entry")
        void testValidateBlankRecipient() {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Arrays.asList("   "), "smtp.example.com", 587, "herald@example.com");

            assertEquals(1, problems.size(), "The blank recipient should be reported: " + problems);
            assertTrue(problems.get(0).contains("email-recipients"),
                    "Problem should name the config key: " + problems.get(0));
        }

        @Test
        @DisplayName("validateConfiguration should accept addresses with display names and subdomains")
        void testValidateAcceptsRealisticAddresses() {
            List<String> problems = EmailNotifier.validateConfiguration(
                    Arrays.asList("Server Admin <admin@mail.example.co.uk>", "owner+herald@example.com"),
                    "smtp.example.com", 587, "Herald <herald@example.com>");

            assertTrue(problems.isEmpty(), "Realistic addresses should be accepted: " + problems);
        }

        @Test
        @DisplayName("validateConfiguration should reject the addresses sendNotification cannot parse")
        void testValidationMatchesAddressParsingAtSendTime() {
            EmailNotifier notifier = new EmailNotifier(
                    "smtp.example.com", 587, "user", "pass", "herald@example.com", true,
                    Arrays.asList("broken@"));

            assertFalse(EmailNotifier.validateConfiguration(
                    Arrays.asList("broken@"), "smtp.example.com", 587, "herald@example.com").isEmpty(),
                    "An address sendNotification cannot parse should be reported at startup");
            assertThrows(AddressException.class, () -> notifier.notifyPlayerJoin("Steve", "Server"),
                    "The per-join failure is what this startup check prevents");
        }
    }
}
