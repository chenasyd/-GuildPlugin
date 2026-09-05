package com.guild.module.example.territory;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * WG 不可用或未实现时的占位桥接器：不执行任何 WG 操作。
 */
public final class NoOpTerritoryBridge implements TerritoryBridge {

    private final Logger logger;
    private final String reason;

    public NoOpTerritoryBridge(Logger logger, String reason) {
        this.logger = logger;
        this.reason = reason;
    }

    @Override
    public boolean isOperational() {
        return false;
    }

    @Override
    public Optional<TerritoryRecord> claimTerritory(TerritoryClaimRequest request) {
        logger.fine(() -> "Territory claim skipped (no-op): " + reason);
        return Optional.empty();
    }

    @Override
    public boolean unclaimTerritory(int guildId, String worldName) {
        logger.fine(() -> "Territory unclaim skipped (no-op): " + reason);
        return false;
    }

    @Override
    public void syncMembers(int guildId, String worldName, Collection<UUID> memberUuids, UUID leaderUuid) {
        logger.fine(() -> "Member sync skipped (no-op): " + reason);
    }

    @Override
    public Optional<TerritoryRecord> findTerritory(int guildId, String worldName) {
        return Optional.empty();
    }

    @Override
    public Optional<TerritoryRecord> findTerritoryAt(String worldName, int x, int y, int z) {
        return Optional.empty();
    }
}
