package com.guild.world.registry;

import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldStatus;
import com.guild.world.model.WorldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldRegistryTest {

    @TempDir
    File worldsDir;

    private WorldRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WorldRegistry(worldsDir, Logger.getLogger("test"));
    }

    @Test
    void load_missingFile_startsEmpty() {
        registry.load();
        assertEquals(0, registry.size());
        assertFalse(registry.isCleanShutdown());
    }

    @Test
    void putSaveReload_preservesWorld() {
        GuildWorld gw = new GuildWorld("gw_alpha");
        gw.setType(WorldType.BATTLE);
        gw.setStatus(WorldStatus.READY);
        registry.put(gw);
        registry.setCleanShutdown(true);
        registry.save();

        WorldRegistry reloaded = new WorldRegistry(worldsDir, Logger.getLogger("test"));
        reloaded.load();

        assertTrue(reloaded.isCleanShutdown());
        GuildWorld loaded = reloaded.get("gw_alpha");
        assertNotNull(loaded);
        assertEquals(WorldType.BATTLE, loaded.getType());
        assertEquals(WorldStatus.READY, loaded.getStatus());
    }

    @Test
    void remove_deletesEntryOnSave() {
        registry.put(new GuildWorld("gw_remove"));
        registry.save();
        registry.remove("gw_remove");
        registry.save();

        WorldRegistry reloaded = new WorldRegistry(worldsDir, Logger.getLogger("test"));
        reloaded.load();
        assertNull(reloaded.get("gw_remove"));
    }

    @Test
    void save_doesNotLeaveTmpFile() {
        registry.put(new GuildWorld("gw_tmp"));
        registry.save();
        assertFalse(new File(worldsDir, "worlds.yml.tmp").exists());
        assertTrue(registry.getFile().isFile());
    }
}
