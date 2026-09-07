package com.guild.world.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildWorldTest {

    @Test
    void loadFrom_nullSection_returnsNull() {
        assertNull(GuildWorld.loadFrom(null, "gw_test"));
    }

    @Test
    void saveLoad_roundTrip_preservesFields() {
        GuildWorld original = new GuildWorld("gw_arena");
        original.setType(WorldType.EDIT);
        original.setStatus(WorldStatus.STALE);
        original.setPresetName("castle");
        original.setOwnerGuildId("42");
        original.setCreatedAt(1_000L);
        original.setLastActiveAt(2_000L);
        original.setSpawn("1.5,64.0,2.5,90.0,0.0");

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("gw_arena");
        original.saveTo(section);

        GuildWorld loaded = GuildWorld.loadFrom(section, "gw_arena");

        assertEquals("gw_arena", loaded.getWorldName());
        assertEquals(WorldType.EDIT, loaded.getType());
        assertEquals(WorldStatus.STALE, loaded.getStatus());
        assertEquals("castle", loaded.getPresetName());
        assertEquals("42", loaded.getOwnerGuildId());
        assertEquals(1_000L, loaded.getCreatedAt());
        assertEquals(2_000L, loaded.getLastActiveAt());
        assertEquals("1.5,64.0,2.5,90.0,0.0", loaded.getSpawn());
    }

    @Test
    void loadFrom_invalidEnum_usesFallback() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("gw_bad");
        section.set("type", "not-a-type");
        section.set("status", "unknown");

        GuildWorld loaded = GuildWorld.loadFrom(section, "gw_bad");

        assertEquals(WorldType.BATTLE, loaded.getType());
        assertEquals(WorldStatus.REGISTERED, loaded.getStatus());
    }

    @Test
    void setSpawnLocation_serializesCoordinates() {
        GuildWorld gw = new GuildWorld("gw_spawn");
        org.bukkit.Location loc = new org.bukkit.Location(null, 10.25, 64.0, -3.5, 45f, -10f);
        gw.setSpawnLocation(loc);
        assertEquals("10.25,64.0,-3.5,45.0,-10.0", gw.getSpawn());
    }

    @Test
    void setSpawnLocation_clearsWhenNull() {
        GuildWorld gw = new GuildWorld("gw_spawn");
        gw.setSpawnLocation(null);
        assertEquals("", gw.getSpawn());
    }

    @Test
    void touch_updatesLastActiveAt() {
        GuildWorld gw = new GuildWorld("gw_touch");
        gw.setLastActiveAt(100L);
        long before = System.currentTimeMillis();
        gw.touch();
        assertTrue(gw.getLastActiveAt() >= before);
    }
}
