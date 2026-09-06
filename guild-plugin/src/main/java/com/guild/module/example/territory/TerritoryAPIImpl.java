package com.guild.module.example.territory;

import com.guild.sdk.territory.TerritoryAPI;
import com.guild.sdk.territory.TerritorySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** {@link TerritoryAPI} 实现。 */
public final class TerritoryAPIImpl implements TerritoryAPI {

    private final TerritoryModule module;

    public TerritoryAPIImpl(TerritoryModule module) {
        this.module = module;
    }

    @Override
    public String getLocalServerId() {
        return module.getRepository().getLocalServerId();
    }

    @Override
    public boolean isWorldGuardReady() {
        return module.isWorldGuardReady();
    }

    @Override
    public Optional<TerritorySnapshot> getLocalTerritory(int guildId, String worldName) {
        return module.getRepository().getLocal(guildId, worldName).map(TerritorySnapshots::toSnapshot);
    }

    @Override
    public List<TerritorySnapshot> listByGuild(int guildId) {
        List<TerritorySnapshot> snapshots = new ArrayList<>();
        for (TerritoryRecord record : module.getRepository().findByGuildId(guildId)) {
            snapshots.add(TerritorySnapshots.toSnapshot(record));
        }
        return List.copyOf(snapshots);
    }

    @Override
    public List<TerritorySnapshot> listLocalByGuild(int guildId) {
        List<TerritorySnapshot> snapshots = new ArrayList<>();
        for (TerritoryRecord record : module.getRepository().findByGuildIdLocal(guildId)) {
            snapshots.add(TerritorySnapshots.toSnapshot(record));
        }
        return List.copyOf(snapshots);
    }

    @Override
    public Optional<TerritorySnapshot> findAt(String worldName, int x, int y, int z) {
        if (!module.getBridge().isOperational()) {
            return Optional.empty();
        }
        return module.getBridge().findTerritoryAt(worldName, x, y, z)
                .map(TerritorySnapshots::toSnapshot);
    }
}
