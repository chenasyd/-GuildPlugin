package com.guild.module.example.territory;

/** {@link TerritoryBridge#materializeFromRecord} 结果。 */
public enum TerritoryMaterializeOutcome {

    CREATED,
    ALREADY_PRESENT,
    SKIPPED,
    WORLD_NOT_LOADED,
    CONFLICT,
    FAILED
}
