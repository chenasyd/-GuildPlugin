package com.guild.sdk.event;

import java.util.UUID;

/**
 * 通用事件数据 - 成员事件
 */
public class MemberEventData {
    private final int guildId;
    private final String guildName;
    private final UUID playerUuid;
    private final String playerName;
    private final String eventType;  // JOIN, LEAVE, KICK

    public MemberEventData(int guildId, String guildName,
                           UUID playerUuid, String playerName,
                           String eventType) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.eventType = eventType;
    }

    public int getGuildId() { return guildId; }
    public String getGuildName() { return guildName; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getEventType() { return eventType; }
}
