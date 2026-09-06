package com.guild.sdk.event.territory;

import com.guild.sdk.territory.TerritoryEventSource;
import com.guild.sdk.territory.TerritorySnapshot;

import java.util.UUID;

/** 本机成功放弃领地后发布。 */
public final class TerritoryUnclaimedEventData {

    private final TerritorySnapshot territory;
    private final UUID actorUuid;
    private final TerritoryEventSource source;

    public TerritoryUnclaimedEventData(TerritorySnapshot territory, UUID actorUuid,
                                       TerritoryEventSource source) {
        this.territory = territory;
        this.actorUuid = actorUuid;
        this.source = source != null ? source : TerritoryEventSource.PLAYER;
    }

    public TerritorySnapshot getTerritory() {
        return territory;
    }

    public UUID getActorUuid() {
        return actorUuid;
    }

    public TerritoryEventSource getSource() {
        return source;
    }
}
