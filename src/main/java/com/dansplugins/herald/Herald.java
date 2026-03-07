package com.dansplugins.herald;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Herald extends JavaPlugin implements Listener {

    private final List<Notifier> notifiers = new ArrayList<>();

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

        // Load Discord settings (primary notification method)
        boolean discordEnabled = getConfig().getBoolean("discord.enabled", false);
        String discordWebhookUrl = getConfig().getString("discord.webhook-url");
        String discordJoinMessage = getConfig().getString("discord.join-message");

        if (discordEnabled && discordWebhookUrl != null && !discordWebhookUrl.isEmpty()) {
            notifiers.add(new DiscordNotifier(discordWebhookUrl, discordJoinMessage));
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
            notifiers.add(new EmailNotifier(smtpServer, smtpPort, smtpUsername, smtpPassword, emailSender, useTLS, emailRecipients));
            getLogger().info("Email notifications enabled");
        } else if (hasRecipients || hasSmtp) {
            getLogger().warning("Email configuration is incomplete. Email notifications will be skipped.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        String serverName = getServer().getName().isEmpty() ? "Minecraft" : getServer().getName();

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            for (Notifier notifier : notifiers) {
                try {
                    notifier.notifyPlayerJoin(playerName, serverName);
                    getLogger().info("Notification sent successfully for player: " + playerName
                            + " via " + notifier.getClass().getSimpleName());
                } catch (Exception e) {
                    getLogger().severe("Failed to send notification via "
                            + notifier.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
}