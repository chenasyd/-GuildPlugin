package com.guild.module.example.territory;

import java.util.logging.Logger;

/** 按运行时可用性创建 {@link TerritoryBridge} 实例。 */
public final class TerritoryBridgeFactory {

    private TerritoryBridgeFactory() {
    }

    public static TerritoryBridge create(WorldGuardProbe.Availability availability,
                                         TerritoryRepository repository,
                                         Logger logger) {
        if (availability.fullyReady()) {
            return new WorldGuardTerritoryBridge(repository, logger);
        }
        return new NoOpTerritoryBridge(logger, "missing " + availability.describeMissing());
    }
}
