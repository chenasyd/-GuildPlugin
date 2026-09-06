package com.guild.sdk.territory;

/**
 * 领地只读快照（跨模块/API 边界使用，勿与模块内部 Record 混用）。
 */
public final class TerritorySnapshot {

    private final int guildId;
    private final String guildName;
    private final String regionId;
    private final String serverId;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final long claimedAtEpochMs;
    private final TerritorySyncState syncState;
    private final long updatedAtEpochMs;

    public TerritorySnapshot(int guildId, String guildName, String regionId, String serverId,
                             String worldName,
                             int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                             long claimedAtEpochMs, TerritorySyncState syncState, long updatedAtEpochMs) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.regionId = regionId;
        this.serverId = serverId != null ? serverId : "";
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.claimedAtEpochMs = claimedAtEpochMs;
        this.syncState = syncState != null ? syncState : TerritorySyncState.MATERIALIZED;
        this.updatedAtEpochMs = updatedAtEpochMs;
    }

    public int getGuildId() {
        return guildId;
    }

    public String getGuildName() {
        return guildName;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getServerId() {
        return serverId;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public long getClaimedAtEpochMs() {
        return claimedAtEpochMs;
    }

    public TerritorySyncState getSyncState() {
        return syncState;
    }

    public long getUpdatedAtEpochMs() {
        return updatedAtEpochMs;
    }

    public long getVolume() {
        long dx = (long) maxX - minX + 1;
        long dy = (long) maxY - minY + 1;
        long dz = (long) maxZ - minZ + 1;
        return dx * dy * dz;
    }
}
