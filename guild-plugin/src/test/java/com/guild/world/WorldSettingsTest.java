package com.guild.world;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSettingsTest {

    @Test
    void constructor_appliesDefaultsWhenConfigEmpty() {
        WorldSettings settings = new WorldSettings(new YamlConfiguration());

        assertEquals("gw_", settings.namePrefix);
        assertEquals("world", settings.fallbackWorldName);
        assertTrue(settings.recoveryCheckEnabled);
        assertFalse(settings.autoLoadStale);
        assertTrue(settings.autoCleanOrphans);
        assertEquals(Material.WOODEN_AXE, settings.wandMaterial);
        assertEquals(2_000_000, settings.maxSchematicVolume);
        assertTrue(settings.ignoreAirOnPaste);
        assertTrue(settings.includeBlockEntities);
        assertEquals("destroy", settings.postMatchPolicy);
    }

    @Test
    void constructor_clampsMaxSchematicVolume() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world.schematic.max-volume", 500);

        WorldSettings settings = new WorldSettings(config);

        assertEquals(1000, settings.maxSchematicVolume);
    }

    @Test
    void constructor_invalidWandMaterial_fallsBackToDefault() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world.edit.wand-material", "NOT_A_BLOCK");

        WorldSettings settings = new WorldSettings(config);

        assertEquals(Material.WOODEN_AXE, settings.wandMaterial);
    }

    @Test
    void constructor_readsCustomValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world.name-prefix", "custom_");
        config.set("world.safety.fallback-world", "lobby");
        config.set("world.recovery.check-on-startup", false);
        config.set("world.recovery.auto-load-stale", true);
        config.set("world.edit.wand-material", "BLAZE_ROD");
        config.set("world.arena.post-match", "keep");

        WorldSettings settings = new WorldSettings(config);

        assertEquals("custom_", settings.namePrefix);
        assertEquals("lobby", settings.fallbackWorldName);
        assertFalse(settings.recoveryCheckEnabled);
        assertTrue(settings.autoLoadStale);
        assertEquals(Material.BLAZE_ROD, settings.wandMaterial);
        assertEquals("keep", settings.postMatchPolicy);
    }
}
