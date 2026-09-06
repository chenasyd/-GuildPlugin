package com.guild.module.example.territory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TerritorySnapshotsTest {

    @Test
    void toSnapshot_mapsRecordFields() {
        long now = System.currentTimeMillis();
        TerritoryRecord record = new TerritoryRecord(
                7, "Guild7", TerritoryRecord.defaultRegionId(7), "srv-a", "world",
                1, 2, 3, 4, 5, 6, now, TerritorySyncState.PENDING, now);

        var snapshot = TerritorySnapshots.toSnapshot(record);

        assertNotNull(snapshot);
        assertEquals(7, snapshot.getGuildId());
        assertEquals("Guild7", snapshot.getGuildName());
        assertEquals("guild_7", snapshot.getRegionId());
        assertEquals("srv-a", snapshot.getServerId());
        assertEquals("world", snapshot.getWorldName());
        assertEquals(com.guild.sdk.territory.TerritorySyncState.PENDING, snapshot.getSyncState());
        assertEquals(64L, snapshot.getVolume());
    }
}
