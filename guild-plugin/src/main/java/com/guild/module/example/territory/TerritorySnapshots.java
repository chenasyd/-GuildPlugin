package com.guild.module.example.territory;

import com.guild.sdk.territory.TerritorySnapshot;

/** 模块 Record / Outcome 与 guild-sdk DTO 互转。 */
final class TerritorySnapshots {

    private TerritorySnapshots() {
    }

    static TerritorySnapshot toSnapshot(TerritoryRecord record) {
        if (record == null) {
            return null;
        }
        return new TerritorySnapshot(
                record.getGuildId(),
                record.getGuildName(),
                record.getRegionId(),
                record.getServerId(),
                record.getWorldName(),
                record.getMinX(),
                record.getMinY(),
                record.getMinZ(),
                record.getMaxX(),
                record.getMaxY(),
                record.getMaxZ(),
                record.getClaimedAtEpochMs(),
                toSdkSyncState(record.getSyncState()),
                record.getUpdatedAtEpochMs());
    }

    static com.guild.sdk.territory.TerritorySyncState toSdkSyncState(TerritorySyncState internal) {
        if (internal == null) {
            return com.guild.sdk.territory.TerritorySyncState.MATERIALIZED;
        }
        return com.guild.sdk.territory.TerritorySyncState.valueOf(internal.name());
    }

    static com.guild.sdk.territory.TerritoryMaterializeOutcome toSdkOutcome(
            TerritoryMaterializeOutcome outcome) {
        if (outcome == null) {
            return com.guild.sdk.territory.TerritoryMaterializeOutcome.SKIPPED;
        }
        return com.guild.sdk.territory.TerritoryMaterializeOutcome.valueOf(outcome.name());
    }
}
