package com.dansplugins.herald;

import jakarta.mail.*;
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
        String body = playerName + " has joined the server at " + new java.util.Date();
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
