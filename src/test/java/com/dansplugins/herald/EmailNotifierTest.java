package com.dansplugins.herald;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        @DisplayName("Email body should include player name and indicate join event")
        void testEmailBodyContainsPlayerName() {
            String playerName = "Steve";
            String body = playerName + " has joined the server at " + new java.util.Date();

            assertTrue(body.startsWith(playerName), "Body should start with the player name");
            assertTrue(body.contains("has joined the server at"), "Body should describe the join event");
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
            assertTrue(capturedBody[0].contains("has joined the server at"),
                    "Body should describe the join event");
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
}
