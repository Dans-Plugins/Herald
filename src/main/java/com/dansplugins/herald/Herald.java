package com.dansplugins.herald;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Herald extends JavaPlugin implements Listener {

    private boolean discordEnabled;
    private String discordWebhookUrl;
    private DiscordNotifier discordNotifier;
    private EmailNotifier emailNotifier;

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
        // Load Discord settings (primary notification method)
        discordEnabled = getConfig().getBoolean("discord.enabled", false);
        discordWebhookUrl = getConfig().getString("discord.webhook-url");

        // Initialize Discord notifier if enabled
        if (discordEnabled && discordWebhookUrl != null && !discordWebhookUrl.isEmpty()) {
            discordNotifier = new DiscordNotifier(discordWebhookUrl);
            getLogger().info("Discord notifications enabled");
        } else if (discordEnabled) {
            getLogger().warning("Discord notifications are enabled in config, but 'discord.webhook-url' is missing or empty. Discord notifications will be skipped.");
        }

        // Load email settings (secondary notification method)
        List<String> emailRecipients = getConfig().getStringList("email-recipients");
        String smtpServer = getConfig().getString("smtp.server");
        int smtpPort = getConfig().getInt("smtp.port");
        String smtpUsername = getConfig().getString("smtp.username");
        String smtpPassword = getConfig().getString("smtp.password");
        String emailSender = getConfig().getString("email.sender");
        boolean useTLS = getConfig().getBoolean("smtp.use-tls", true);

        boolean hasRecipients = !emailRecipients.isEmpty();
        boolean hasSmtp = smtpServer != null && !smtpServer.isEmpty();

        if (hasRecipients && hasSmtp) {
            emailNotifier = new EmailNotifier(smtpServer, smtpPort, smtpUsername, smtpPassword, emailSender, useTLS, emailRecipients);
            getLogger().info("Email notifications enabled");
        } else if (hasRecipients || hasSmtp) {
            getLogger().warning("Email configuration is incomplete. Email notifications will be skipped.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String serverName = getServer().getName().isEmpty() ? "Minecraft" : getServer().getName();

        // Send Discord notification first (primary notification method)
        if (discordEnabled && discordNotifier != null) {
            String discordMessage = "**" + playerName + "** joined the **" + serverName + "** server";

            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    discordNotifier.sendMessage(discordMessage);
                    getLogger().info("Discord notification sent successfully for player: " + playerName);
                } catch (Exception e) {
                    getLogger().severe("Failed to send Discord notification: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }

        // Send email notification (secondary notification method)
        if (emailNotifier != null) {
            String subject = playerName + " joined " + serverName + " server";
            String body = playerName + " has joined the server at " + new java.util.Date();

            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    emailNotifier.sendNotification(subject, body);
                    getLogger().info("Email notification sent successfully for player: " + playerName);
                } catch (Exception e) {
                    getLogger().severe("Failed to send email notification: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}