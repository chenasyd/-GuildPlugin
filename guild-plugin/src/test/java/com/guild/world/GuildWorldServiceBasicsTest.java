package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.core.config.ConfigManager;
import com.guild.world.preset.PresetService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
class GuildWorldServiceBasicsTest {

    @TempDir
    File dataFolder;

    private GuildPlugin plugin;
    private GuildWorldService service;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        YamlConfiguration config = new YamlConfiguration();
        config.set("world.name-prefix", "gw_");
        config.set("world.edit.wand-material", "BLAZE_ROD");
        config.set("world.schematic.max-volume", 500);
        config.set("world.arena.post-match", "keep");

        ConfigManager configManager = mock(ConfigManager.class);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(config);

        service = new GuildWorldService(plugin);
    }

    @Test
    void buildWorldName_addsPrefix() {
        assertEquals("gw_arena", service.buildWorldName("arena"));
    }

    @Test
    void buildWorldName_skipsDuplicatePrefix() {
        assertEquals("gw_arena", service.buildWorldName("gw_arena"));
    }

    @Test
    void buildWorldName_nullOrEmpty_returnsAsIs() {
        assertNull(service.buildWorldName(null));
        assertEquals("", service.buildWorldName(""));
    }

    @Test
    void reloadSettings_readsWandPrefixAndPolicy() {
        assertEquals("gw_", service.getWorldNamePrefix());
        assertEquals(Material.BLAZE_ROD, service.getWandMaterial());
        assertEquals("keep", service.getPostMatchPolicy());
    }

    @Test
    void reloadSettings_invalidWandMaterial_fallsBackToDefault() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world.edit.wand-material", "NOT_A_BLOCK");
        ConfigManager configManager = mock(ConfigManager.class);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(config);

        GuildWorldService reloaded = new GuildWorldService(plugin);
        assertEquals(Material.WOODEN_AXE, reloaded.getWandMaterial());
    }

    @Test
    void resolvePresetSpawns_computesAnchors() {
        World world = mock(World.class);
        Location pasteAt = new Location(world, 0.5, 64.0, 0.5);
        PresetService.Anchor spawnA = new PresetService.Anchor(1.0, 0.0, 2.0, 90f, 0f);
        PresetService.Anchor spawnB = new PresetService.Anchor(-1.0, 0.0, -2.0, 180f, 0f);
        PresetService.PresetMeta meta = new PresetService.PresetMeta(
                "castle", "", "", 0L, "", true, 16, 8, 16, 0, "",
                spawnA, spawnB, null
        );

        GuildWorldService.ArenaSpawns spawns = service.resolvePresetSpawns(world, pasteAt, meta);

        assertNotNull(spawns.spawnA());
        assertEquals(1.5, spawns.spawnA().getX(), 0.001);
        assertEquals(64.0, spawns.spawnA().getY(), 0.001);
        assertEquals(2.5, spawns.spawnA().getZ(), 0.001);
        assertEquals(180f, spawns.spawnB().getYaw(), 0.001);
        assertNull(spawns.spectator());
    }

    @Test
    void isManaged_falseForUnknownWorld() {
        assertFalse(service.isManaged("gw_unknown"));
    }
}
