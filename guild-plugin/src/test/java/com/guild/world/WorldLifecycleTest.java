package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldType;
import com.guild.world.registry.WorldJournal;
import com.guild.world.registry.WorldRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldLifecycleTest {

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
    void createVoidWorld_whenDisabled_failsFast() {
        WorldLifecycle lifecycle = lifecycle(false);

        var future = lifecycle.createVoidWorld("arena", WorldType.BATTLE, null, null, null);

        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("disabled", ex.getCause().getMessage());
    }

    @Test
    void createVoidWorld_invalidName_failsBeforeBukkit() {
        WorldLifecycle lifecycle = lifecycle(true);

        var future = lifecycle.createVoidWorld("bad name!", WorldType.BATTLE, null, null, null);

        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void createVoidWorld_duplicateRegistryEntry_failsBeforeBukkit() {
        WorldLifecycle lifecycle = lifecycle(true);
        registry.put(new GuildWorld("gw_dup"));

        var future = lifecycle.createVoidWorld("dup", WorldType.BATTLE, null, null, null);

        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    void loadWorld_unknownWorld_failsFast() {
        WorldLifecycle lifecycle = lifecycle(true);

        var future = lifecycle.loadWorld("gw_missing");

        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void deleteWorld_whenDisabled_failsFast() {
        WorldLifecycle lifecycle = lifecycle(false);

        var future = lifecycle.deleteWorld("gw_x", true);

        assertTrue(future.isCompletedExceptionally());
        assertInstanceOf(IllegalStateException.class,
                assertThrows(CompletionException.class, () -> future.join()).getCause());
    }

    private WorldLifecycle lifecycle(boolean enabled) {
        return new WorldLifecycle(
                plugin,
                registry,
                journal,
                () -> enabled,
                () -> "disabled",
                name -> name.startsWith("gw_") ? name : "gw_" + name,
                player -> {
                }
        );
    }
}
