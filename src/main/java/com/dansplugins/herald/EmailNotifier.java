package com.dansplugins.herald;

import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailNotifier implements Notifier {

    /** Subject used when {@code email.subject} is absent or empty. */
    static final String DEFAULT_SUBJECT = "{player} joined {server} server";

    /** Body used when {@code email.body} is absent or empty. */
    static final String DEFAULT_BODY = "{player} has joined {server} at {time}";

    /**
     * Milliseconds allowed for the connection to the SMTP server to be established.
     * Jakarta Mail waits forever when this is left unset, which would park the
     * sending task on a host that never answers.
     */
    static final int CONNECT_TIMEOUT_MILLIS = 10000;

    /**
     * Milliseconds allowed for the SMTP server to answer a command. Longer than the
     * connect timeout because a relay that has accepted the message is entitled to
     * take its time scanning or queueing it before acknowledging.
     */
    static final int READ_TIMEOUT_MILLIS = 30000;

    /** Milliseconds allowed for a write to the SMTP server to complete. */
    static final int WRITE_TIMEOUT_MILLIS = 30000;

    private final String smtpServer;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String emailSender;
    private final boolean useTLS;
    private final List<String> recipients;
    private final String subjectTemplate;
    private final String bodyTemplate;

    /**
     * Create a notifier that formats its emails with the built-in default
     * subject and body templates.
     */
    public EmailNotifier(String smtpServer, int smtpPort, String smtpUsername,
                         String smtpPassword, String emailSender, boolean useTLS,
                         List<String> recipients) {
        this(smtpServer, smtpPort, smtpUsername, smtpPassword, emailSender, useTLS, recipients, null, null);
    }

    /**
     * Create a notifier with operator-supplied message templates.
     * Both templates support the {@code {player}}, {@code {server}} and
     * {@code {time}} placeholders. A template that is {@code null} or empty
     * falls back to the built-in default, mirroring how {@link DiscordNotifier}
     * treats an absent {@code discord.join-messages} list.
     *
     * @param subjectTemplate the configured {@code email.subject}, or {@code null} for the default
     * @param bodyTemplate    the configured {@code email.body}, or {@code null} for the default
     */
    public EmailNotifier(String smtpServer, int smtpPort, String smtpUsername,
                         String smtpPassword, String emailSender, boolean useTLS,
                         List<String> recipients, String subjectTemplate, String bodyTemplate) {
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.emailSender = emailSender;
        this.useTLS = useTLS;
        this.recipients = recipients != null ? new ArrayList<>(recipients) : new ArrayList<>();
        this.subjectTemplate = templateOrDefault(subjectTemplate, DEFAULT_SUBJECT);
        this.bodyTemplate = templateOrDefault(bodyTemplate, DEFAULT_BODY);
    }

    /**
     * @param template     the configured template
     * @param defaultValue the built-in template to use when none is configured
     * @return {@code template} when it holds something, otherwise {@code defaultValue}
     */
    private static String templateOrDefault(String template, String defaultValue) {
        return (template != null && !template.isEmpty()) ? template : defaultValue;
    }

    /**
     * Check the configuration keys email notifications require.
     * Callers use this to report every missing or unusable key at startup
     * instead of failing once per player join. Addresses that cannot be parsed
     * are reported here because {@link #sendNotification(String, String)} would
     * otherwise only discover them when the first player joins.
     *
     * @param recipients  the configured {@code email-recipients}
     * @param smtpServer  the configured {@code smtp.server}
     * @param smtpPort    the configured {@code smtp.port}
     * @param emailSender the configured {@code email.sender}
     * @return a list of human-readable problems, empty when the configuration is complete
     */
    public static List<String> validateConfiguration(List<String> recipients, String smtpServer, int smtpPort, String emailSender) {
        List<String> problems = new ArrayList<>();
        if (recipients == null || recipients.isEmpty()) {
            problems.add("'email-recipients' is empty");
        } else {
            for (String recipient : recipients) {
                String problem = describeAddressProblem(recipient);
                if (problem != null) {
                    problems.add("'email-recipients' contains an invalid email address '"
                            + recipient + "': " + problem);
                }
            }
        }
        if (smtpServer == null || smtpServer.isEmpty()) {
            problems.add("'smtp.server' is missing or empty");
        }
        if (smtpPort < 1 || smtpPort > 65535) {
            problems.add("'smtp.port' must be between 1 and 65535");
        }
        if (emailSender == null || emailSender.isEmpty()) {
            problems.add("'email.sender' is missing or empty");
        } else {
            String problem = describeAddressProblem(emailSender);
            if (problem != null) {
                problems.add("'email.sender' is not a valid email address '" + emailSender + "': " + problem);
            }
        }
        return problems;
    }

    /**
     * Describe the cost of sending SMTP credentials over an unencrypted connection.
     * Reported as a warning rather than as a problem from
     * {@link #validateConfiguration(List, String, int, String)} because the
     * combination is deliberate on a relay with no STARTTLS support, and email is
     * still delivered; what it is not is private.
     *
     * @param smtpUsername the configured {@code smtp.username}
     * @param useTLS       the configured {@code smtp.use-tls}
     * @return the warning to log, or {@code null} when no credentials are exposed
     */
    public static String describeCredentialExposure(String smtpUsername, boolean useTLS) {
        if (useTLS || smtpUsername == null || smtpUsername.isEmpty()) {
            return null;
        }
        return "'smtp.username' is set but 'smtp.use-tls' is false, so the SMTP username and password "
                + "are sent over an unencrypted connection, along with every notification. "
                + "Set 'smtp.use-tls' to true unless the server genuinely has no STARTTLS support.";
    }

    /**
     * Describe why an address cannot be used, using the same parser that
     * {@link #sendNotification(String, String)} relies on at send time.
     * {@code validate()} is called as well as the constructor because the
     * constructor alone accepts a bare local part with no domain.
     *
     * @param address the configured address
     * @return the reason the address is unusable, or {@code null} when it is fine
     */
    private static String describeAddressProblem(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "the address is empty";
        }
        try {
            InternetAddress parsed = new InternetAddress(address);
            parsed.validate();
            return null;
        } catch (AddressException e) {
            return e.getMessage();
        }
    }

    /**
     * Send a player-join notification via email.
     * Fills the configured subject and body templates and sends via SMTP.
     * Both templates support {player}, {server} and {time} placeholders.
     *
     * @param playerName the name of the player who joined
     * @param serverName the name of the server they joined
     * @throws IllegalStateException if recipients or SMTP server are not configured
     * @throws MessagingException    if there is an error sending the email
     */
    @Override
    public void notifyPlayerJoin(String playerName, String serverName) throws MessagingException {
        // One timestamp for both templates, so a subject and body that each use
        // {time} cannot disagree about when the player joined.
        String time = new java.util.Date().toString();
        String subject = fillTemplate(subjectTemplate, playerName, serverName, time);
        String body = fillTemplate(bodyTemplate, playerName, serverName, time);
        sendNotification(subject, body);
    }

    /**
     * Substitute the supported placeholders into a message template.
     *
     * @param template   the template to fill
     * @param playerName the value for {player}
     * @param serverName the value for {server}
     * @param time       the value for {time}
     * @return the template with every placeholder replaced
     */
    private static String fillTemplate(String template, String playerName, String serverName, String time) {
        return template
                .replace("{player}", playerName)
                .replace("{server}", serverName)
                .replace("{time}", time);
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "email";
    }

    /**
     * @return whether the configured {@code smtp.username} calls for authentication
     */
    private boolean usesAuthentication() {
        return smtpUsername != null && !smtpUsername.isEmpty();
    }

    /**
     * Assemble the properties the SMTP session is built from.
     * Package-private so that the settings can be asserted without a mail server
     * being involved, in the way {@link DiscordNotifier} exposes its own send-time
     * decisions for direct testing.
     *
     * <p>STARTTLS is requested as {@code required} as well as {@code enable}, because
     * {@code enable} on its own is advisory: Jakarta Mail falls back to an unencrypted
     * connection against a server that does not advertise STARTTLS, which would leave
     * {@code smtp.use-tls: true} sitting in the config file while nothing is encrypted.
     * A server with no STARTTLS support therefore fails the send and is reported,
     * rather than being talked to in the clear.
     *
     * @return the SMTP properties for this notifier's configuration
     */
    Properties buildSessionProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", usesAuthentication() ? "true" : "false");
        props.put("mail.smtp.connectiontimeout", String.valueOf(CONNECT_TIMEOUT_MILLIS));
        props.put("mail.smtp.timeout", String.valueOf(READ_TIMEOUT_MILLIS));
        props.put("mail.smtp.writetimeout", String.valueOf(WRITE_TIMEOUT_MILLIS));

        if (useTLS) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        return props;
    }

    /**
     * Send an email notification with the given subject and body.
     *
     * @param subject The email subject line
     * @param body    The email body text
     * @throws IllegalStateException if recipients or SMTP server are not configured
     * @throws MessagingException    if there is an error sending the email
     */
    public void sendNotification(String subject, String body) throws MessagingException {
        if (recipients.isEmpty()) {
            throw new IllegalStateException("No email recipients configured");
        }

        if (smtpServer == null || smtpServer.isEmpty()) {
            throw new IllegalStateException("SMTP server not configured");
        }

        if (emailSender == null || emailSender.isEmpty()) {
            throw new IllegalStateException("Email sender address not configured");
        }

        boolean useAuth = usesAuthentication();

        Properties props = buildSessionProperties();

        Session session;
        if (useAuth) {
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            };
            session = Session.getInstance(props, authenticator);
        } else {
            session = Session.getInstance(props);
        }

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailSender));

        for (String recipient : recipients) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }

        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }
}
