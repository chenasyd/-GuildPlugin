package com.guild.sdk.event.territory;

import com.guild.sdk.territory.TerritoryEventSource;
import com.guild.sdk.territory.TerritoryMaterializeOutcome;
import com.guild.sdk.territory.TerritorySnapshot;

/** DB 元数据 materialize 为 WG 区域后的结果。 */
public final class TerritoryMaterializedEventData {

    private final TerritorySnapshot territory;
    private final TerritoryMaterializeOutcome outcome;
    private final TerritoryEventSource source;

    public TerritoryMaterializedEventData(TerritorySnapshot territory,
                                          TerritoryMaterializeOutcome outcome,
                                          TerritoryEventSource source) {
        this.territory = territory;
        this.outcome = outcome != null ? outcome : TerritoryMaterializeOutcome.SKIPPED;
        this.source = source != null ? source : TerritoryEventSource.SYSTEM;
    }

    public TerritorySnapshot getTerritory() {
        return territory;
    }

    public TerritoryMaterializeOutcome getOutcome() {
        return outcome;
    }

    public TerritoryEventSource getSource() {
        return source;
    }
}
