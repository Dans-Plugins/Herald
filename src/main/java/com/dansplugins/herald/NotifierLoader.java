package com.dansplugins.herald;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Turns the plugin configuration into the notifiers a player join is sent through,
 * logging at startup what was loaded and what was skipped.
 * <p>
 * This lives outside {@link Herald}, as {@link ServerName} does, so that which
 * notifiers a given {@code config.yml} produces, and what it logs about them, can be
 * exercised from a test against a plain {@code YamlConfiguration} without a running
 * server. {@code Herald} only hands it the configuration and keeps the result.
 */
final class NotifierLoader {

    private NotifierLoader() {
    }

    /**
     * Build the notifiers the configuration asks for.
     *
     * @param config     the plugin configuration
     * @param serverName the resolved server name, used to measure Discord join messages
     * @param logger     where startup messages about each channel are written
     * @return the configured notifiers, Discord first; empty when none is configured
     */
    static List<Notifier> load(ConfigurationSection config, String serverName, Logger logger) {
        List<Notifier> notifiers = new ArrayList<>();

        // Load Discord settings (primary notification method)
        boolean discordEnabled = config.getBoolean("discord.enabled", false);
        String discordWebhookUrl = config.getString("discord.webhook-url");
        List<String> discordJoinMessages = config.getStringList("discord.join-messages");

        if (discordEnabled) {
            List<String> discordProblems = DiscordNotifier.validateConfiguration(discordWebhookUrl, discordJoinMessages, serverName);
            if (discordProblems.isEmpty()) {
                notifiers.add(new DiscordNotifier(discordWebhookUrl, discordJoinMessages, logger));
                logger.info("Discord notifications enabled");
            } else {
                logger.warning("Discord notifications are enabled in config, but the configuration is incomplete: "
                        + String.join("; ", discordProblems) + ". Discord notifications will be skipped.");
            }
        }

        // Load email settings (secondary notification method).
        // The toggle defaults to true so that a config.yml written before the key
        // existed keeps sending the emails it sends today.
        boolean emailEnabled = config.getBoolean("email.enabled", true);

        if (emailEnabled) {
            List<String> emailRecipients = config.getStringList("email-recipients");
            String smtpServer = config.getString("smtp.server");
            int smtpPort = config.getInt("smtp.port");
            String smtpUsername = config.getString("smtp.username");
            String smtpPassword = config.getString("smtp.password");
            String emailSender = config.getString("email.sender");
            boolean useTLS = config.getBoolean("smtp.use-tls", true);
            // Defaults to false so that a config.yml written before the key existed keeps
            // using STARTTLS on the submission port it was set up against.
            boolean implicitTLS = config.getBoolean("smtp.implicit-tls", false);
            // Defaults to true, unlike the two keys above, because it tightens rather than
            // switches: a config.yml written before the key existed was already asking for
            // an encrypted connection to a named host, and checking the certificate against
            // that name is what it was asking for.
            boolean verifyServerIdentity = config.getBoolean("smtp.verify-server-identity", true);
            String emailSubject = config.getString("email.subject");
            String emailBody = config.getString("email.body");

            List<String> emailProblems = EmailNotifier.validateConfiguration(emailRecipients, smtpServer, smtpPort, emailSender);
            boolean emailPartiallyConfigured = !emailRecipients.isEmpty()
                    || (smtpServer != null && !smtpServer.isEmpty())
                    || (emailSender != null && !emailSender.isEmpty());

            // Reported outside the partial-configuration check because the two encryption
            // keys can only conflict after a deliberate edit, so the conflict is worth
            // naming even on a config that has nothing else filled in yet.
            String tlsModeConflict = EmailNotifier.describeTlsModeConflict(useTLS, implicitTLS);

            if (tlsModeConflict != null) {
                logger.warning(tlsModeConflict + " Email notifications will be skipped.");
            }

            if (tlsModeConflict == null && emailProblems.isEmpty()) {
                notifiers.add(new EmailNotifier(smtpServer, smtpPort, smtpUsername, smtpPassword, emailSender, useTLS,
                        implicitTLS, verifyServerIdentity, emailRecipients, emailSubject, emailBody));
                logger.info("Email notifications enabled");

                // Warned rather than refused: the combination still delivers mail, and an
                // operator who turned TLS off for a relay that cannot do STARTTLS should
                // learn from the log what it costs them.
                String credentialExposure = EmailNotifier.describeCredentialExposure(smtpUsername, useTLS, implicitTLS);
                if (credentialExposure != null) {
                    logger.warning(credentialExposure);
                }

                // Also a warning rather than a refusal: the conventional ports are the
                // common case, not the rule, and a relay is free to offer either mode
                // anywhere.
                String portMismatch = EmailNotifier.describePortTlsMismatch(smtpPort, implicitTLS);
                if (portMismatch != null) {
                    logger.warning(portMismatch);
                }

                // Warned for the same reason as the two above: the opt-out exists for relays
                // whose certificate cannot name the address they are reached at, so it is a
                // choice to be recorded in the log rather than a configuration to refuse.
                String unverifiedIdentity = EmailNotifier.describeUnverifiedServerIdentity(
                        verifyServerIdentity, useTLS, implicitTLS);
                if (unverifiedIdentity != null) {
                    logger.warning(unverifiedIdentity);
                }
            } else if (emailPartiallyConfigured && !emailProblems.isEmpty()) {
                // Logged even when a mode conflict was reported above, so that a config file
                // with both problems names both at once instead of surfacing the second one
                // on the restart after the first is fixed.
                logger.warning("Email configuration is incomplete: " + String.join("; ", emailProblems)
                        + ". Email notifications will be skipped.");
            }
        } else {
            // Reported because 'email.enabled' defaults to true, so false is always a
            // deliberate choice, and an operator asking why no mail arrives should find
            // the answer in the log rather than in the absence of a line.
            logger.info("Email notifications are disabled in config");
        }

        if (notifiers.isEmpty()) {
            logger.warning("No notification methods are configured, so Herald will not send any notifications. "
                    + "Enable Discord or email notifications in plugins/Herald/config.yml - see CONFIG.md for details.");
        }

        return notifiers;
    }
}
