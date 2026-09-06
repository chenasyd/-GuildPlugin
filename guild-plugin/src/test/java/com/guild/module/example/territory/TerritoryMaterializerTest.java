package com.guild.module.example.territory;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.guild.core.module.ModuleContext;
import com.guild.sdk.config.ModuleConfigSection;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TerritoryMaterializerTest {

    private TerritoryRepository repository;
    private RecordingBridge bridge;
    private TerritoryMaterializer materializer;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        Bukkit.createWorld(new WorldCreator("world"));
        repository = new TerritoryRepository(
                new File(System.getProperty("java.io.tmpdir")),
                Logger.getAnonymousLogger());
        bridge = new RecordingBridge();
        materializer = new TerritoryMaterializer(null, repository, bridge, settings(true, true, true));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void needsMaterialization_whenRegionMissing() {
        TerritoryRecord record = localRecord(1, TerritorySyncState.PENDING);
        repository.put(record);

        assertTrue(materializer.needsMaterialization(record));
    }

    @Test
    void needsMaterialization_skipsWhenRegionPresent() {
        TerritoryRecord record = localRecord(2, TerritorySyncState.PENDING);
        repository.put(record);
        bridge.regions.add(record.getRegionId());

        assertFalse(materializer.needsMaterialization(record));
        assertEquals(TerritorySyncState.MATERIALIZED,
                repository.get(2, "local-test", "world").orElseThrow().getSyncState());
    }

    @Test
    void needsMaterialization_skipsFailedWhenRetryDisabled() {
        TerritoryMaterializer noRetry = new TerritoryMaterializer(
                null, repository, bridge, settings(true, true, false));
        TerritoryRecord record = localRecord(3, TerritorySyncState.FAILED);
        repository.put(record);

        assertFalse(noRetry.needsMaterialization(record));
    }

    @Test
    void needsMaterialization_forceFailed_retriesFailedRecord() {
        TerritoryMaterializer noRetry = new TerritoryMaterializer(
                null, repository, bridge, settings(true, true, false));
        TerritoryRecord record = localRecord(5, TerritorySyncState.FAILED);
        repository.put(record);

        assertFalse(noRetry.needsMaterialization(record));
        assertTrue(noRetry.needsMaterialization(record, true));
    }

    @Test
    void countAdminTargets_filtersByGuildAndForce() {
        TerritoryMaterializer counter = new TerritoryMaterializer(
                null, repository, bridge, settings(true, true, false));
        repository.put(localRecord(10, TerritorySyncState.PENDING));
        repository.put(localRecord(11, TerritorySyncState.FAILED));

        assertEquals(1, counter.countAdminTargets(10, null, false));
        assertEquals(1, counter.countAdminTargets(null, null, false));
        assertEquals(0, counter.countAdminTargets(10, "world_nether", false));
        assertEquals(1, counter.countAdminTargets(11, null, true));
        assertEquals(0, counter.countAdminTargets(11, null, false));
    }

    @Test
    void materializeRecordNow_invokesBridge() {
        TerritoryRecord record = localRecord(4, TerritorySyncState.PENDING);
        repository.put(record);
        UUID leader = UUID.randomUUID();

        TerritoryMaterializeOutcome outcome = materializer.materializeRecordNow(
                record, leader, List.of(leader));

        assertEquals(TerritoryMaterializeOutcome.CREATED, outcome);
        assertEquals(1, bridge.materializeCalls);
    }

    private static TerritoryRecord localRecord(int guildId, TerritorySyncState state) {
        long now = System.currentTimeMillis();
        return new TerritoryRecord(
                guildId, "Guild" + guildId, TerritoryRecord.defaultRegionId(guildId),
                "local-test", "world",
                0, 0, 0, 5, 5, 5,
                now, state, now);
    }

    private static TerritorySettings settings(boolean onLoad, boolean onWorldLoad, boolean retryFailed) {
        ModuleContext context = mock(ModuleContext.class);
        ModuleConfigSection config = mock(ModuleConfigSection.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getBoolean("cross-server.materialize-on-load", true)).thenReturn(onLoad);
        when(config.getBoolean("cross-server.materialize-on-world-load", true)).thenReturn(onWorldLoad);
        when(config.getBoolean("cross-server.materialize-retry-failed", true)).thenReturn(retryFailed);
        return new TerritorySettings(context);
    }

    private static final class RecordingBridge implements TerritoryBridge {
        private final Set<String> regions = new HashSet<>();
        private int materializeCalls;

        @Override
        public boolean isOperational() {
            return true;
        }

        @Override
        public Optional<TerritoryRecord> claimTerritory(TerritoryClaimRequest request) {
            return Optional.empty();
        }

        @Override
        public boolean unclaimTerritory(int guildId, String worldName) {
            return false;
        }

        @Override
        public void syncMembers(int guildId, String worldName, Collection<UUID> memberUuids, UUID leaderUuid) {
        }

        @Override
        public Optional<TerritoryRecord> findTerritory(int guildId, String worldName) {
            return Optional.empty();
        }

        @Override
        public Optional<TerritoryRecord> findTerritoryAt(String worldName, int x, int y, int z) {
            return Optional.empty();
        }

        @Override
        public boolean hasRegionForRecord(TerritoryRecord record) {
            return regions.contains(record.getRegionId());
        }

        @Override
        public TerritoryMaterializeOutcome materializeFromRecord(TerritoryRecord record,
                                                                 UUID leaderUuid,
                                                                 Collection<UUID> memberUuids) {
            materializeCalls++;
            regions.add(record.getRegionId());
            return TerritoryMaterializeOutcome.CREATED;
        }
    }
}
