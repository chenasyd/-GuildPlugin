package com.guild.sdk.data;

import java.util.UUID;

/**
 * 工会成员数据传输对象（只读）
 */
public class MemberData {

    private final UUID playerUuid;
    private final String playerName;
    private final String role;
    private final long joinTime;
    private final double contribution;
    private final boolean online;

    public MemberData(UUID playerUuid, String playerName, String role,
                      long joinTime, double contribution, boolean online) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.role = role;
        this.joinTime = joinTime;
        this.contribution = contribution;
        this.online = online;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getRole() { return role; }
    public long getJoinTime() { return joinTime; }
    public double getContribution() { return contribution; }
    public boolean isOnline() { return online; }

    @Override
    public String toString() {
        return "MemberData{name='" + playerName + "', role=" + role + "}";
    }
}
