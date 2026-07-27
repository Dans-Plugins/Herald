package com.dansplugins.herald;

import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailNotifier implements Notifier {

    private final String smtpServer;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String emailSender;
    private final boolean useTLS;
    private final List<String> recipients;

    public EmailNotifier(String smtpServer, int smtpPort, String smtpUsername,
                         String smtpPassword, String emailSender, boolean useTLS,
                         List<String> recipients) {
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.emailSender = emailSender;
        this.useTLS = useTLS;
        this.recipients = recipients != null ? new ArrayList<>(recipients) : new ArrayList<>();
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
     * Formats a plain-text subject and body and sends via SMTP.
     *
     * @param playerName the name of the player who joined
     * @param serverName the name of the server they joined
     * @throws IllegalStateException if recipients or SMTP server are not configured
     * @throws MessagingException    if there is an error sending the email
     */
    @Override
    public void notifyPlayerJoin(String playerName, String serverName) throws MessagingException {
        String subject = playerName + " joined " + serverName + " server";
        String body = playerName + " has joined " + serverName + " at " + new java.util.Date();
        sendNotification(subject, body);
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

        boolean useAuth = smtpUsername != null && !smtpUsername.isEmpty();

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", useAuth ? "true" : "false");

        if (useTLS) {
            props.put("mail.smtp.starttls.enable", "true");
        }

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
