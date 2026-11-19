package com.dansplugins.herald.trace;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An operator who sets {@code usage-reporting.enabled: false} must find reporting off, every
 * time, whatever the plugin does with config.yml afterwards.
 */
class UsageReportingSettingTest {

    @TempDir
    File folder;

    private File configFile(String contents) throws IOException {
        File file = new File(folder, "config.yml");
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    /** What JavaPlugin.getConfig() loads: the file, with the jar's config.yml as defaults. */
    static YamlConfiguration loadLikeBukkit(File file) throws IOException {
        // Bukkit's loadConfiguration() logs a file it cannot parse and returns it empty.
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            // left empty, as Bukkit does
        }
        try (InputStream stream = UsageReportingSettingTest.class.getResourceAsStream("/config.yml")) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        }
        return config;
    }

    /** The guarded save every write of config.yml goes through. */
    static void guardedSave(YamlConfiguration config, File file) throws IOException {
        if (UsageReportingSetting.isUnparseable(file)) {
            return;
        }
        UsageReportingSetting.keepOptOutFromDisk(config, file);
        config.save(file);
    }

    /** A start that copies every default into the file and saves it. */
    private static void startupSave(File file) throws IOException {
        YamlConfiguration config = loadLikeBukkit(file);
        config.options().copyDefaults(true);
        guardedSave(config, file);
    }

    @Test
    void aBlankEnabledValueIsAnOptOut() throws Exception {
        File file = configFile("usage-reporting:\n  enabled:   # off for now\n  key: k\n");
        // Bukkit drops the key, so the bundled default would win.
        assertTrue(loadLikeBukkit(file).getBoolean(UsageReportingSetting.ENABLED_KEY));

        assertTrue(UsageReportingSetting.turnedOffOnDisk(file));
        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
    }

    @Test
    void anEnabledKeyOutsideTheBlockIsNotTheSwitch() throws Exception {
        File file = configFile("other:\n  enabled:\nusage-reporting:\n  enabled: true\n");
        assertFalse(UsageReportingSetting.turnedOffOnDisk(file));
    }

    @Test
    void anUpgradeKeepsReportingOff() throws IOException {
        File file = configFile("usage-reporting:\n  enabled: false\n");

        startupSave(file);

        assertEquals(false, UsageReportingSetting.readFromDisk(file));
        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
    }

    @Test
    void aFileWithoutTheBlockReportsByDefault() throws IOException {
        File file = configFile("other: 1\n");

        assertTrue(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
    }

    @Test
    void aQuotedFalseTurnsReportingOff() throws IOException {
        File file = configFile("usage-reporting:\n  enabled: \"false\"\n");
        // Bukkit's getBoolean() returns the bundled default, true, for the string "false".
        assertTrue(loadLikeBukkit(file).getBoolean(UsageReportingSetting.ENABLED_KEY));

        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
    }

    @Test
    void anUnrecognisedValueTurnsReportingOff() throws IOException {
        File file = configFile("usage-reporting:\n  enabled: disabled\n");

        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
    }

    @Test
    void aStaleLoadedConfigCannotOverwriteAnOptOutMadeWhileRunning() throws IOException {
        File file = configFile("usage-reporting:\n  enabled: true\n");
        YamlConfiguration loaded = loadLikeBukkit(file);
        // The operator turns reporting off by hand while the server runs.
        Files.write(file.toPath(), "usage-reporting:\n  enabled: false\n".getBytes(StandardCharsets.UTF_8));

        // Something later saves the loaded, stale config.
        loaded.set("some-other-setting", true);
        guardedSave(loaded, file);

        assertEquals(false, UsageReportingSetting.readFromDisk(file));
    }

    @Test
    void aFileBukkitCannotParseIsNeverOverwrittenAndReportingStaysOff() throws IOException {
        String broken = "usage-reporting:\n  enabled: false\n\tother: [\n";
        File file = configFile(broken);
        // Bukkit loads it as empty, so every getter falls through to the bundled defaults.
        assertTrue(loadLikeBukkit(file).getBoolean(UsageReportingSetting.ENABLED_KEY));

        assertTrue(UsageReportingSetting.isUnparseable(file));
        assertFalse(UsageReportingSetting.isEnabled(loadLikeBukkit(file), file));
        startupSave(file);
        assertEquals(broken, read(file), "the operator's file must be left as it is");
    }

    @Test
    void aMissingFileIsNotAnOptOut() {
        File file = new File(folder, "absent.yml");

        assertNull(UsageReportingSetting.readFromDisk(file));
        assertFalse(UsageReportingSetting.turnedOffOnDisk(file));
        assertFalse(UsageReportingSetting.isUnparseable(file));
    }
}
