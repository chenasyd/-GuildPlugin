package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.world.model.GuildWorld;
import com.guild.world.recovery.WorldRecoveryService;
import com.guild.world.registry.WorldRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldRecoveryBootstrapTest {

    @TempDir
    File worldsDir;

    private WorldRegistry registry;
    private WorldRecoveryService recovery;
    private GuildPlugin plugin;

    @BeforeEach
    void setUp() {
        registry = new WorldRegistry(worldsDir, Logger.getLogger("test"));
        recovery = new WorldRecoveryService(Logger.getLogger("test"));
        plugin = mock(GuildPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
    }

    @Test
    void load_marksRunStartedAndClearsCleanShutdown() {
        registry.put(new GuildWorld("gw_a"));
        registry.setCleanShutdown(true);
        registry.save();

        bootstrap(true, true).load();

        assertFalse(registry.isCleanShutdown());
        assertEquals(1, registry.size());
    }

    @Test
    void runRecovery_whenDisabled_marksRanWithoutExecutingRecovery() {
        GuildWorldService service = mock(GuildWorldService.class);
        when(service.isEnabled()).thenReturn(false);
        when(service.unsupportedMessage()).thenReturn("disabled");

        bootstrap(false, true).runRecovery(service);

        assertTrue(recovery.hasRan());
    }

    @Test
    void runRecovery_whenCheckDisabled_marksRan() {
        GuildWorldService service = mock(GuildWorldService.class);

        bootstrap(true, false).runRecovery(service);

        assertTrue(recovery.hasRan());
    }

    private WorldRecoveryBootstrap bootstrap(boolean enabled, boolean recoveryCheckEnabled) {
        return new WorldRecoveryBootstrap(
                plugin, registry, recovery,
                () -> enabled,
                () -> "disabled",
                () -> recoveryCheckEnabled
        );
    }
}
