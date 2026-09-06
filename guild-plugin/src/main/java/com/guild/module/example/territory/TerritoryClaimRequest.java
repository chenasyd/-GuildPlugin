package com.guild.module.example.territory;

import java.util.Collection;
import java.util.UUID;

/** 创建领地时的输入参数。 */
public final class TerritoryClaimRequest {

    private final int guildId;
    private final String guildName;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final UUID leaderUuid;
    private final Collection<UUID> memberUuids;
    private final String serverId;

    public TerritoryClaimRequest(int guildId, String guildName, String worldName,
                                 int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                 UUID leaderUuid, Collection<UUID> memberUuids, String serverId) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.leaderUuid = leaderUuid;
        this.memberUuids = memberUuids;
        this.serverId = serverId != null ? serverId : "";
    }

    public TerritoryClaimRequest(int guildId, String guildName, String worldName,
                                 int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                 UUID leaderUuid, Collection<UUID> memberUuids) {
        this(guildId, guildName, worldName, minX, minY, minZ, maxX, maxY, maxZ, leaderUuid, memberUuids, "");
    }

    public int getGuildId() {
        return guildId;
    }

    public String getGuildName() {
        return guildName;
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

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public Collection<UUID> getMemberUuids() {
        return memberUuids;
    }

    public String getServerId() {
        return serverId;
    }

    public String regionId() {
        return TerritoryRecord.defaultRegionId(guildId);
    }
}
