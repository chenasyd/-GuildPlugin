package com.guild.module.example.territory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorldGuardTerritoryBridgeTest {

    @Test
    void parseGuildId_valid() {
        assertEquals(42, WorldGuardTerritoryBridge.parseGuildId("guild_42"));
    }

    @Test
    void parseGuildId_invalid() {
        assertNull(WorldGuardTerritoryBridge.parseGuildId("__global__"));
        assertNull(WorldGuardTerritoryBridge.parseGuildId("guild_abc"));
        assertNull(WorldGuardTerritoryBridge.parseGuildId(null));
    }

    @Test
    void normalizeBounds() {
        TerritoryBounds.Normalized n = TerritoryBounds.normalize(10, 5, 3, 1, 8, 7);
        assertEquals(1, n.minX());
        assertEquals(5, n.minY());
        assertEquals(3, n.minZ());
        assertEquals(10, n.maxX());
        assertEquals(8, n.maxY());
        assertEquals(7, n.maxZ());
    }
}
