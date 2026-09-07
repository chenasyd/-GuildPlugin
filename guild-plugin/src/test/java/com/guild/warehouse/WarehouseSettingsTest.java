package com.guild.warehouse;

import com.guild.GuildPlugin;
import com.guild.core.config.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarehouseSettingsTest {

    @Test
    void pageMath_computesPagesAndOffsets() {
        assertEquals(1, WarehouseSettings.getPageCount(0));
        assertEquals(1, WarehouseSettings.getPageCount(9));
        assertEquals(2, WarehouseSettings.getPageCount(55));
        assertEquals(0, WarehouseSettings.getPageOffset(1));
        assertEquals(54, WarehouseSettings.getPageOffset(2));
        assertEquals(46, WarehouseSettings.getPageSlotCount(100, 2));
        assertEquals(0, WarehouseSettings.getPageSlotCount(100, 99));
    }

    @Test
    void reload_readsCustomSlotsAndFlags() {
        GuildPlugin plugin = mock(GuildPlugin.class);
        ConfigManager configManager = mock(ConfigManager.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("guild-warehouse.enabled", false);
        cfg.set("guild-warehouse.access-log", true);
        cfg.set("guild-warehouse.slots-by-level.1", 18);
        cfg.set("guild-warehouse.slots-by-level.5", 45);
        cfg.set("guild-warehouse.slots-by-level.bad", 10);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);

        WarehouseSettings settings = new WarehouseSettings(plugin);

        assertFalse(settings.isEnabled());
        assertTrue(settings.isAccessLogEnabled());
        assertEquals(18, settings.getSlotsForPeakLevel(1));
        assertEquals(45, settings.getSlotsForPeakLevel(5));
        assertEquals(45, settings.getSlotsForPeakLevel(99));
    }

    @Test
    void getSlotsForPeakLevel_usesFloorWhenExactMissing() {
        GuildPlugin plugin = mock(GuildPlugin.class);
        ConfigManager configManager = mock(ConfigManager.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("guild-warehouse.slots-by-level.1", 9);
        cfg.set("guild-warehouse.slots-by-level.3", 27);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);

        WarehouseSettings settings = new WarehouseSettings(plugin);

        assertEquals(9, settings.getSlotsForPeakLevel(2));
        assertEquals(27, settings.getSlotsForPeakLevel(3));
        assertEquals(9, settings.getSlotsForPeakLevel(0));
    }

    @Test
    void reload_normalizesSlotsToMultiplesOfNine() {
        GuildPlugin plugin = mock(GuildPlugin.class);
        ConfigManager configManager = mock(ConfigManager.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("guild-warehouse.slots-by-level.1", 10);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);

        WarehouseSettings settings = new WarehouseSettings(plugin);

        assertEquals(9, settings.getSlotsForPeakLevel(1));
    }
}
