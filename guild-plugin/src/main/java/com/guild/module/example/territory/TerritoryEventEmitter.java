package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.sdk.event.territory.TerritoryClaimedEventData;
import com.guild.sdk.event.territory.TerritoryGuildClearedEventData;
import com.guild.sdk.event.territory.TerritoryMaterializedEventData;
import com.guild.sdk.event.territory.TerritorySyncStateChangedEventData;
import com.guild.sdk.event.territory.TerritoryUnclaimedEventData;
import com.guild.sdk.territory.TerritoryEventSource;
import com.guild.sdk.territory.TerritorySnapshot;

import java.util.UUID;

/**
 * 向 {@link com.guild.core.events.EventBus} 发布 guild-sdk 领地事件。
 */
public final class TerritoryEventEmitter {

    private final ModuleContext context;
    private final TerritoryRepository repository;
    private final TerritorySettings settings;

    public TerritoryEventEmitter(ModuleContext context, TerritoryRepository repository,
                                 TerritorySettings settings) {
        this.context = context;
        this.repository = repository;
        this.settings = settings;
    }

    public void fireClaimed(TerritoryRecord record, UUID actorUuid, TerritoryEventSource source,
                            double costPaid) {
        if (!isEnabled() || record == null) {
            return;
        }
        context.getEventBus().publish(new TerritoryClaimedEventData(
                TerritorySnapshots.toSnapshot(record), actorUuid, source, costPaid));
    }

    public void fireUnclaimed(TerritoryRecord record, UUID actorUuid, TerritoryEventSource source) {
        if (!isEnabled() || record == null) {
            return;
        }
        context.getEventBus().publish(new TerritoryUnclaimedEventData(
                TerritorySnapshots.toSnapshot(record), actorUuid, source));
    }

    public void fireMaterialized(TerritoryRecord record, TerritoryMaterializeOutcome moduleOutcome,
                                 TerritoryEventSource source) {
        if (!isEnabled() || record == null) {
            return;
        }
        TerritoryRecord latest = resolveLatest(record);
        context.getEventBus().publish(new TerritoryMaterializedEventData(
                TerritorySnapshots.toSnapshot(latest),
                TerritorySnapshots.toSdkOutcome(moduleOutcome),
                source));
    }

    public void fireSyncStateChanged(TerritoryRecord before, TerritoryRecord after,
                                     TerritoryEventSource source) {
        if (!isEnabled() || before == null || after == null) {
            return;
        }
        var previous = TerritorySnapshots.toSdkSyncState(before.getSyncState());
        var next = TerritorySnapshots.toSdkSyncState(after.getSyncState());
        if (previous == next) {
            return;
        }
        context.getEventBus().publish(new TerritorySyncStateChangedEventData(
                TerritorySnapshots.toSnapshot(after), previous, next, source));
    }

    public void fireGuildCleared(int guildId, String guildName, int removedCount,
                                 TerritoryEventSource source) {
        if (!isEnabled() || removedCount <= 0) {
            return;
        }
        context.getEventBus().publish(new TerritoryGuildClearedEventData(
                guildId, guildName, removedCount, source));
    }

    private TerritoryRecord resolveLatest(TerritoryRecord record) {
        if (repository == null) {
            return record;
        }
        return repository.get(record.getGuildId(), record.getServerId(), record.getWorldName())
                .orElse(record);
    }

    private boolean isEnabled() {
        return context != null && settings != null && settings.isEventsEnabled();
    }
}
