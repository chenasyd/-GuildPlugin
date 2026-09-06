package com.guild.module.example.territory;

/**
 * 公会 ↔ WG 区域映射（跨服时存共享 DB；WG 区域文件仍在本机）。
 */
public final class TerritoryRecord {

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

    /** 兼容旧版 JSON / 单服：serverId 默认为空，由迁移或本机 identity 填充。 */
    public TerritoryRecord(int guildId, String guildName, String regionId, String worldName,
                           int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                           long claimedAtEpochMs) {
        this(guildId, guildName, regionId, "", worldName,
                minX, minY, minZ, maxX, maxY, maxZ,
                claimedAtEpochMs, TerritorySyncState.MATERIALIZED, claimedAtEpochMs);
    }

    public TerritoryRecord(int guildId, String guildName, String regionId, String serverId, String worldName,
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
        this.updatedAtEpochMs = updatedAtEpochMs > 0 ? updatedAtEpochMs : claimedAtEpochMs;
    }

    /** Gson 反序列化用。 */
    @SuppressWarnings("unused")
    private TerritoryRecord() {
        this(0, "", "", "", "", 0, 0, 0, 0, 0, 0, 0L, TerritorySyncState.MATERIALIZED, 0L);
    }

    public static String defaultRegionId(int guildId) {
        return "guild_" + guildId;
    }

    public TerritoryRecord withServerId(String newServerId) {
        return new TerritoryRecord(
                guildId, guildName, regionId, newServerId, worldName,
                minX, minY, minZ, maxX, maxY, maxZ,
                claimedAtEpochMs, syncState, updatedAtEpochMs);
    }

    public TerritoryRecord withSyncState(TerritorySyncState newState, long newUpdatedAt) {
        return new TerritoryRecord(
                guildId, guildName, regionId, serverId, worldName,
                minX, minY, minZ, maxX, maxY, maxZ,
                claimedAtEpochMs, newState, newUpdatedAt);
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
}
