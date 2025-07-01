package com.dansplugins.herald;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class Herald extends JavaPlugin implements Listener {

    private List<String> emailRecipients = new ArrayList<>();
    private String smtpServer;
    private int smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String emailSender;
    private boolean useTLS;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Load configuration
        loadConfiguration();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Herald has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Herald has been disabled!");
    }

    private void loadConfiguration() {
        // Load email recipients
        emailRecipients = getConfig().getStringList("email-recipients");

        // Load SMTP settings
        smtpServer = getConfig().getString("smtp.server");
        smtpPort = getConfig().getInt("smtp.port");
        smtpUsername = getConfig().getString("smtp.username");
        smtpPassword = getConfig().getString("smtp.password");
        emailSender = getConfig().getString("email.sender");
        useTLS = getConfig().getBoolean("smtp.use-tls", true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String serverName = getServer().getName().isEmpty() ? "Minecraft" : getServer().getName();

        // Send email notification
        String subject = playerName + " joined " + serverName + " server";
        String body = playerName + " has joined the server at " + new java.util.Date();

        // Send email asynchronously to not block the main server thread
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                sendEmail(subject, body);
                getLogger().info("Email notification sent successfully for player: " + playerName);
            } catch (Exception e) {
                getLogger().severe("Failed to send email notification: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void sendEmail(String subject, String body) throws MessagingException {
        if (emailRecipients.isEmpty()) {
            getLogger().warning("No email recipients configured. Skipping email notification.");
            return;
        }

        if (smtpServer == null || smtpServer.isEmpty()) {
            getLogger().warning("SMTP server not configured. Skipping email notification.");
            return;
        }

        // Set up mail server properties
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", "true");

        if (useTLS) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        // Create a mail session with authenticator
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        };

        Session session = Session.getInstance(props, authenticator);

        // Create a message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailSender));

        // Add all recipients
        for (String recipient : emailRecipients) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }

        // Set subject and body
        message.setSubject(subject);
        message.setText(body);

        // Send the message
        Transport.send(message);
    }
}