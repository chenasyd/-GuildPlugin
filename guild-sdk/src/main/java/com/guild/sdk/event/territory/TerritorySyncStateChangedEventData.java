package com.guild.sdk.event.territory;

import com.guild.sdk.territory.TerritoryEventSource;
import com.guild.sdk.territory.TerritorySnapshot;
import com.guild.sdk.territory.TerritorySyncState;

/** 领地 sync_state 变更（如 PENDING → MATERIALIZED / FAILED）。 */
public final class TerritorySyncStateChangedEventData {

    private final TerritorySnapshot territory;
    private final TerritorySyncState previousState;
    private final TerritorySyncState newState;
    private final TerritoryEventSource source;

    public TerritorySyncStateChangedEventData(TerritorySnapshot territory,
                                              TerritorySyncState previousState,
                                              TerritorySyncState newState,
                                              TerritoryEventSource source) {
        this.territory = territory;
        this.previousState = previousState;
        this.newState = newState;
        this.source = source != null ? source : TerritoryEventSource.SYSTEM;
    }

    public TerritorySnapshot getTerritory() {
        return territory;
    }

    public TerritorySyncState getPreviousState() {
        return previousState;
    }

    public TerritorySyncState getNewState() {
        return newState;
    }

    public TerritoryEventSource getSource() {
        return source;
    }
}
