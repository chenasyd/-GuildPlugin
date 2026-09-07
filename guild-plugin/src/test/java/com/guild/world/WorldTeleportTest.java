package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.world.model.GuildWorld;
import com.guild.world.registry.WorldJournal;
import com.guild.world.registry.WorldRegistry;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldTeleportTest {

    @TempDir
    File worldsDir;

    private WorldRegistry registry;
    private WorldJournal journal;
    private GuildPlugin plugin;

    @BeforeEach
    void setUp() {
        registry = new WorldRegistry(worldsDir, Logger.getLogger("test"));
        journal = new WorldJournal(worldsDir, Logger.getLogger("test"));
        plugin = mock(GuildPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
    }

    @Test
    void teleportToWorld_whenDisabled_failsFast() {
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);

        var future = teleport(false).teleportToWorld(player, "arena");

        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    void teleportToWorld_nullPlayer_returnsFalse() throws Exception {
        assertFalse(teleport(true).teleportToWorld(null, "arena").get());
    }

    @Test
    void teleportToWorld_unmanagedWorld_failsFast() {
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);

        var future = teleport(true).teleportToWorld(player, "arena");

        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void resolveTeleportLocation_usesWorldSpawnWhenUnset() {
        World world = mock(World.class);
        when(world.getSpawnLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(world.getMinHeight()).thenReturn(-64);

        Location loc = teleport(true).resolveTeleportLocation(new GuildWorld("gw_test"), world);

        assertEquals(64.0, loc.getY(), 0.001);
    }

    @Test
    void resolveTeleportLocation_clampsLowSpawnY() {
        World world = mock(World.class);
        when(world.getSpawnLocation()).thenReturn(new Location(world, 0, -63, 0));
        when(world.getMinHeight()).thenReturn(-64);

        Location loc = teleport(true).resolveTeleportLocation(new GuildWorld("gw_test"), world);

        assertEquals(64.0, loc.getY(), 0.001);
    }

    private WorldTeleport teleport(boolean enabled) {
        WorldLifecycle lifecycle = new WorldLifecycle(
                plugin, registry, journal,
                () -> enabled,
                () -> "disabled",
                name -> "gw_" + name,
                player -> {
                }
        );
        return new WorldTeleport(
                plugin, registry, lifecycle,
                () -> enabled,
                () -> "disabled",
                name -> "gw_" + name,
                () -> "world"
        );
    }
}
