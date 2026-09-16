package com.dansplugins.herald;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the {@code usage-reporting} block reaches a config.yml that predates it. A real
 * {@link YamlConfiguration} is used because the point is that {@code isSet} answers for the
 * file alone while the bundled defaults are registered on it.
 */
class HeraldUsageReportingConfigTest {

    private static YamlConfiguration bundledDefaults() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                HeraldUsageReportingConfigTest.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
    }

    @Test
    void theBlockIsCopiedFromTheBundledDefaultsWhenTheFileLacksIt() {
        YamlConfiguration bundled = bundledDefaults();
        YamlConfiguration onDisk = new YamlConfiguration(); // an upgraded server's file
        onDisk.set("server-name", "Test");
        onDisk.setDefaults(bundled);
        assertFalse(onDisk.isSet("usage-reporting"), "the file itself must start without the block");

        assertTrue(Herald.ensureUsageReportingBlockOnDisk(onDisk));

        assertEquals(bundled.get("usage-reporting.enabled"), onDisk.get("usage-reporting.enabled"));
        assertEquals(bundled.get("usage-reporting.endpoint"), onDisk.get("usage-reporting.endpoint"));
        assertEquals(bundled.get("usage-reporting.key"), onDisk.get("usage-reporting.key"));
        assertTrue(onDisk.isSet("usage-reporting.key"));
        assertEquals("Test", onDisk.getString("server-name"), "the rest of the file is untouched");
    }

    @Test
    void aFileThatAlreadyHasTheBlockIsLeftAlone() {
        YamlConfiguration onDisk = new YamlConfiguration();
        onDisk.set("usage-reporting.enabled", false);
        onDisk.setDefaults(bundledDefaults());

        assertFalse(Herald.ensureUsageReportingBlockOnDisk(onDisk));

        assertEquals(false, onDisk.get("usage-reporting.enabled"), "an operator's opt-out must survive");
        assertFalse(onDisk.isSet("usage-reporting.key"), "nothing is added to a file that has the block");
    }

    @Test
    void aConfigurationWithoutDefaultsIsLeftAlone() {
        assertFalse(Herald.ensureUsageReportingBlockOnDisk(new YamlConfiguration()));
    }
}
