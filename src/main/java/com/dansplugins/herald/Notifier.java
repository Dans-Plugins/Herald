package com.dansplugins.herald;

/**
 * Common interface for all player-join notification channels.
 * Implementations handle their own message formatting and transport.
 */
public interface Notifier {

    /**
     * Send a notification for a player joining the server.
     *
     * @param playerName the name of the player who joined
     * @param serverName the name of the server they joined
     * @throws Exception if the notification could not be delivered
     */
    void notifyPlayerJoin(String playerName, String serverName) throws Exception;

    /**
     * The name this channel is called by in operator-facing log messages.
     * Implementations declare this explicitly rather than letting their class
     * name leak into the log, so the vocabulary of the log stays that of the
     * config sections the operator edits.
     *
     * @return the operator-facing name of this channel, e.g. {@code "Discord"}
     */
    String getDisplayName();
}
