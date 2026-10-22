package com.dansplugins.herald;

import com.dansplugins.herald.trace.TraceClient;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Herald extends JavaPlugin implements Listener {

    private static final String USAGE_REPORTING_ENABLED_KEY = "usage-reporting.enabled";
    private static final String USAGE_REPORTING_ENDPOINT_KEY = "usage-reporting.endpoint";
    private static final String USAGE_REPORTING_KEY_KEY = "usage-reporting.key";
    private static final String DEFAULT_USAGE_REPORTING_ENDPOINT = "https://trace.danielstephenson.dev";

    private final List<Notifier> notifiers = new ArrayList<>();
    private String serverName = ServerName.DEFAULT;

    // A no-op until the config has been read, so anything that reports before
    // onEnable() finishes has something safe to report to.
    private TraceClient trace = TraceClient.disabled();

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Load configuration
        loadConfiguration();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // usage reporting: one event now; see config.yml. The block is written to a
        // config.yml that predates it so the opt-out is visible, and the server-wide
        // switch in plugins/trace/config.yml is created if absent and honoured.
        if (ensureUsageReportingBlockOnDisk(getConfig())) {
            saveConfig();
        }
        String endpoint = getUsageReportingEndpoint();
        trace = TraceClient.builder(endpoint, getName(), getDescription().getVersion())
                .key(getUsageReportingKey())
                .enabled(isUsageReportingEnabled())
                .serverWideConfig(getDataFolder().getParentFile())
                .logger(getLogger())
                .build();
        if (trace.isEnabled()) {
            getLogger().info(usageReportingNotice(getName(), endpoint));
        } else {
            getLogger().info("Usage reporting is off (" + trace.disabledReason() + ").");
        }
        trace.report("startup");

        getLogger().info("Herald has been enabled!");
    }

    @Override
    public void onDisable() {
        trace.close();

        getLogger().info("Herald has been disabled!");
    }

    /**
     * Copies the {@code usage-reporting} block from the bundled defaults into a configuration
     * that lacks it, so that a server upgraded from a version before the block existed sees
     * the opt-out in its config.yml. {@code saveDefaultConfig()} never touches an existing
     * file, which is why this is needed at all. The values are the bundled defaults, not new
     * literals. {@code isSet} is deliberate: unlike {@code contains} it ignores the registered
     * defaults and answers only for what the file holds.
     *
     * @return true when the block was added and the configuration needs saving
     */
    static boolean ensureUsageReportingBlockOnDisk(FileConfiguration config) {
        if (config.isSet("usage-reporting")) {
            return false;
        }
        Configuration defaults = config.getDefaults();
        if (defaults == null) {
            return false;
        }
        for (String key : List.of(USAGE_REPORTING_ENABLED_KEY, USAGE_REPORTING_ENDPOINT_KEY, USAGE_REPORTING_KEY_KEY)) {
            config.set(key, defaults.get(key));
        }
        return true;
    }

    /**
     * The startup notice logged while usage reporting is on. It names {@code endpoint}, the
     * server the client was built with, so that a changed {@code usage-reporting.endpoint}
     * is what the console reports rather than the bundled default.
     */
    static String usageReportingNotice(String pluginName, String endpoint) {
        return "Usage reporting is on: " + pluginName + " sends its name and version to"
                + " " + endpoint + ", plus a random server ID (server-id in plugins/trace/config.yml) - nothing about players. Turn it off with"
                + " usage-reporting.enabled: false in this plugin's config.yml, or for every plugin with"
                + " enabled: false in plugins/trace/config.yml."
                + " Details: https://danielstephenson.dev/usage-reporting";
    }

    // The one-argument getters, deliberately. Bukkit registers the jar's config.yml
    // as the defaults for the file on disk, and the one-argument getters fall
    // through to them for any key the file lacks -- the two-argument getters
    // return their explicit fallback instead, which for the key would be "" and
    // would read as "off". ensureUsageReportingBlockOnDisk() writes the block for
    // an upgraded server, so on a normal enable the file has the keys; the
    // fall-through only matters if that write failed. Verified against
    // YamlConfiguration, not assumed.

    private boolean isUsageReportingEnabled() {
        return getConfig().getBoolean(USAGE_REPORTING_ENABLED_KEY);
    }

    private String getUsageReportingEndpoint() {
        String endpoint = getConfig().getString(USAGE_REPORTING_ENDPOINT_KEY);
        return endpoint != null ? endpoint : DEFAULT_USAGE_REPORTING_ENDPOINT;
    }

    /** Empty when no key is configured or bundled, which the client treats as "off". */
    private String getUsageReportingKey() {
        String key = getConfig().getString(USAGE_REPORTING_KEY_KEY);
        return key != null ? key : "";
    }

    private void loadConfiguration() {
        notifiers.clear();

        // Cache server name with fallback
        serverName = ServerName.resolve(getConfig().getString("server-name", ""));

        notifiers.addAll(NotifierLoader.load(getConfig(), serverName, getLogger()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();

        getServer().getScheduler().runTaskAsynchronously(this,
                () -> notifyPlayerJoin(notifiers, playerName, serverName, getLogger()));
    }

    /**
     * Sends one join notification through every notifier in turn. Each send has its own
     * try/catch, so a channel that fails is logged and the remaining channels are still
     * tried -- this is what keeps Discord and email independent at send time.
     */
    static void notifyPlayerJoin(List<Notifier> notifiers, String playerName, String serverName, Logger logger) {
        for (Notifier notifier : notifiers) {
            try {
                notifier.notifyPlayerJoin(playerName, serverName);
                logger.info("Notification sent successfully for player: " + playerName
                        + " via " + notifier.getDisplayName());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to send notification via "
                        + notifier.getDisplayName() + ": " + e.getMessage(), e);
            }
        }
    }
}