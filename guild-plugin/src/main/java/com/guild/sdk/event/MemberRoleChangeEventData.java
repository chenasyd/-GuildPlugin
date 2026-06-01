package com.guild.sdk.event;

import java.util.UUID;

/**
 * 成员角色变更事件数据。
 */
public class MemberRoleChangeEventData {
    private final int guildId;
    private final String guildName;
    private final UUID playerUuid;
    private final String playerName;
    private final String oldRole;
    private final String newRole;

    public MemberRoleChangeEventData(int guildId, String guildName, UUID playerUuid,
                                     String playerName, String oldRole, String newRole) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    public int getGuildId() { return guildId; }
    public String getGuildName() { return guildName; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getOldRole() { return oldRole; }
    public String getNewRole() { return newRole; }
}
