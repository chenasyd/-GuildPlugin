package com.guild.sdk.event.territory;

import com.guild.sdk.territory.TerritoryEventSource;
import com.guild.sdk.territory.TerritorySnapshot;

import java.util.UUID;

/** 本机成功声明领地后发布（同步、不可取消）。 */
public final class TerritoryClaimedEventData {

    private final TerritorySnapshot territory;
    private final UUID actorUuid;
    private final TerritoryEventSource source;
    private final double costPaid;

    public TerritoryClaimedEventData(TerritorySnapshot territory, UUID actorUuid,
                                     TerritoryEventSource source, double costPaid) {
        this.territory = territory;
        this.actorUuid = actorUuid;
        this.source = source != null ? source : TerritoryEventSource.PLAYER;
        this.costPaid = costPaid;
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

    public double getCostPaid() {
        return costPaid;
    }
}
