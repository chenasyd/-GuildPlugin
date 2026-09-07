package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.world.preset.PresetService;
import com.guild.world.registry.WorldJournal;
import com.guild.world.registry.WorldRegistry;
import com.guild.world.selection.SelectionManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldPresetOpsTest {

    @TempDir
    File worldsDir;

    private WorldRegistry registry;
    private WorldJournal journal;
    private PresetService presets;
    private SelectionManager selections;
    private GuildPlugin plugin;

    @BeforeEach
    void setUp() {
        registry = new WorldRegistry(worldsDir, Logger.getLogger("test"));
        journal = new WorldJournal(worldsDir, Logger.getLogger("test"));
        presets = new PresetService(worldsDir, Logger.getLogger("test"));
        selections = new SelectionManager();
        plugin = mock(GuildPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
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

        WorldPresetOps.ArenaSpawns spawns = presetOps(true).resolvePresetSpawns(world, pasteAt, meta);

        assertNotNull(spawns.spawnA());
        assertEquals(1.5, spawns.spawnA().getX(), 0.001);
        assertEquals(64.0, spawns.spawnA().getY(), 0.001);
        assertEquals(2.5, spawns.spawnA().getZ(), 0.001);
        assertEquals(180f, spawns.spawnB().getYaw(), 0.001);
        assertNull(spawns.spectator());
    }

    @Test
    void pastePreset_whenDisabled_failsFast() {
        World world = mock(World.class);
        var future = presetOps(false).pastePreset(world, new Location(world, 0, 64, 0), "castle");
        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    void pastePreset_missingPreset_failsFast() {
        World world = mock(World.class);
        var future = presetOps(true).pastePreset(world, new Location(world, 0, 64, 0), "missing");
        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void savePresetFromSelection_incompleteSelection_failsFast() {
        Player editor = mock(Player.class);
        when(editor.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        var future = presetOps(true).savePresetFromSelection(editor, "castle");
        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    void createArenaFromPreset_missingPreset_failsFast() {
        var future = presetOps(true).createArenaFromPreset("arena1", "missing");
        assertTrue(future.isCompletedExceptionally());
        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(CompletionException.class, () -> future.join()).getCause());
    }

    private WorldPresetOps presetOps(boolean enabled) {
        WorldLifecycle lifecycle = new WorldLifecycle(
                plugin, registry, journal,
                () -> enabled,
                () -> "disabled",
                name -> "gw_" + name,
                player -> {
                }
        );
        return new WorldPresetOps(
                plugin, registry, presets, selections, lifecycle,
                () -> enabled,
                () -> "disabled",
                () -> new WorldPresetOps.SchematicSettings(2_000_000, true, true)
        );
    }
}
