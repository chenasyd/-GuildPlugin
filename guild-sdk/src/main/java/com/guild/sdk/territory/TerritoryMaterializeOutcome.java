package com.guild.sdk.territory;

/** {@code materializeFromRecord} 结果。 */
public enum TerritoryMaterializeOutcome {
    CREATED,
    ALREADY_PRESENT,
    SKIPPED,
    WORLD_NOT_LOADED,
    CONFLICT,
    FAILED
}
