package com.dansplugins.herald;

/**
 * Resolution of the configured {@code server-name} into the value that fills the
 * {@code {server}} placeholder.
 * <p>
 * This lives outside {@link Herald} because {@code Herald} extends
 * {@code JavaPlugin}, whose API is a {@code compileOnly} dependency and so cannot
 * be loaded from a test. Keeping the rule here gives it a single home that both
 * notifiers are wired from and that the tests can exercise directly.
 */
final class ServerName {

    /** The name used when {@code server-name} is left blank. */
    static final String DEFAULT = "Minecraft";

    private ServerName() {
    }

    /**
     * Resolve the configured value into the name notifications should use.
     * <p>
     * A value that holds nothing but whitespace is treated as blank rather than
     * kept, because a whitespace-only name would otherwise be filled into the
     * {@code {server}} placeholder verbatim and sent as bold markdown wrapped
     * around nothing. Surrounding whitespace is stripped from a populated name
     * for the same reason. This matches how a whitespace-only entry in
     * {@code discord.join-messages} and a whitespace-only email address are
     * already treated.
     *
     * @param configuredValue the configured {@code server-name}, which may be {@code null}
     * @return the trimmed configured name, or {@link #DEFAULT} when it is blank
     */
    static String resolve(String configuredValue) {
        if (configuredValue == null) {
            return DEFAULT;
        }
        String trimmed = configuredValue.trim();
        return trimmed.isEmpty() ? DEFAULT : trimmed;
    }
}
