package com.guild.module.example.territory;

/**
 * 公会 ↔ WG 区域映射（模块本地持久化，不替代 WG 区域文件）。
 */
public final class TerritoryRecord {

    private final int guildId;
    private final String guildName;
    private final String regionId;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final long claimedAtEpochMs;

    public TerritoryRecord(int guildId, String guildName, String regionId, String worldName,
                           int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                           long claimedAtEpochMs) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.regionId = regionId;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.claimedAtEpochMs = claimedAtEpochMs;
    }

    /** Gson 反序列化用。 */
    @SuppressWarnings("unused")
    private TerritoryRecord() {
        this(0, "", "", "", 0, 0, 0, 0, 0, 0, 0L);
    }

    public static String defaultRegionId(int guildId) {
        return "guild_" + guildId;
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
}
