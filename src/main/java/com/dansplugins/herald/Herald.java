package com.dansplugins.herald;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class Herald extends JavaPlugin implements Listener {

    private final List<Notifier> notifiers = new ArrayList<>();
    private String serverName = ServerName.DEFAULT;

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
        notifiers.clear();

        // Cache server name with fallback
        serverName = ServerName.resolve(getConfig().getString("server-name", ""));

        // Load Discord settings (primary notification method)
        boolean discordEnabled = getConfig().getBoolean("discord.enabled", false);
        String discordWebhookUrl = getConfig().getString("discord.webhook-url");
        List<String> discordJoinMessages = getConfig().getStringList("discord.join-messages");

        if (discordEnabled) {
            List<String> discordProblems = DiscordNotifier.validateConfiguration(discordWebhookUrl, discordJoinMessages, serverName);
            if (discordProblems.isEmpty()) {
                notifiers.add(new DiscordNotifier(discordWebhookUrl, discordJoinMessages));
                getLogger().info("Discord notifications enabled");
            } else {
                getLogger().warning("Discord notifications are enabled in config, but the configuration is incomplete: "
                        + String.join("; ", discordProblems) + ". Discord notifications will be skipped.");
            }
        }

        // Load email settings (secondary notification method).
        // The toggle defaults to true so that a config.yml written before the key
        // existed keeps sending the emails it sends today.
        boolean emailEnabled = getConfig().getBoolean("email.enabled", true);

        if (emailEnabled) {
            List<String> emailRecipients = getConfig().getStringList("email-recipients");
            String smtpServer = getConfig().getString("smtp.server");
            int smtpPort = getConfig().getInt("smtp.port");
            String smtpUsername = getConfig().getString("smtp.username");
            String smtpPassword = getConfig().getString("smtp.password");
            String emailSender = getConfig().getString("email.sender");
            boolean useTLS = getConfig().getBoolean("smtp.use-tls", true);
            String emailSubject = getConfig().getString("email.subject");
            String emailBody = getConfig().getString("email.body");

            List<String> emailProblems = EmailNotifier.validateConfiguration(emailRecipients, smtpServer, smtpPort, emailSender);
            boolean emailPartiallyConfigured = !emailRecipients.isEmpty()
                    || (smtpServer != null && !smtpServer.isEmpty())
                    || (emailSender != null && !emailSender.isEmpty());

            if (emailProblems.isEmpty()) {
                notifiers.add(new EmailNotifier(smtpServer, smtpPort, smtpUsername, smtpPassword, emailSender, useTLS,
                        emailRecipients, emailSubject, emailBody));
                getLogger().info("Email notifications enabled");
            } else if (emailPartiallyConfigured) {
                getLogger().warning("Email configuration is incomplete: " + String.join("; ", emailProblems)
                        + ". Email notifications will be skipped.");
            }
        } else {
            // Reported because 'email.enabled' defaults to true, so false is always a
            // deliberate choice, and an operator asking why no mail arrives should find
            // the answer in the log rather than in the absence of a line.
            getLogger().info("Email notifications are disabled in config");
        }

        if (notifiers.isEmpty()) {
            getLogger().warning("No notification methods are configured, so Herald will not send any notifications. "
                    + "Enable Discord or email notifications in plugins/Herald/config.yml - see CONFIG.md for details.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            for (Notifier notifier : notifiers) {
                try {
                    notifier.notifyPlayerJoin(playerName, serverName);
                    getLogger().info("Notification sent successfully for player: " + playerName
                            + " via " + notifier.getDisplayName());
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to send notification via "
                            + notifier.getDisplayName() + ": " + e.getMessage(), e);
                }
            }
        });
    }
}