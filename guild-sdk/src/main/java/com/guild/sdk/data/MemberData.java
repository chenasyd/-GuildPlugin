package com.guild.sdk.data;

import java.util.UUID;

public class MemberData {
    private final UUID playerUuid;
    private final String playerName;
    private final String role;
    private final long joinTime;
    private final double contribution;
    private final boolean online;
    private final double investedBalance;

    /** 向后兼容构造器 —— investedBalance 默认 0 */
    public MemberData(UUID playerUuid, String playerName, String role,
                      long joinTime, double contribution, boolean online) {
        this(playerUuid, playerName, role, joinTime, contribution, online, 0.0);
    }

    /** 全参构造器（新增 investedBalance） */
    public MemberData(UUID playerUuid, String playerName, String role,
                      long joinTime, double contribution, boolean online,
                      double investedBalance) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.role = role;
        this.joinTime = joinTime;
        this.contribution = contribution;
        this.online = online;
        this.investedBalance = investedBalance;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getRole() { return role; }
    public long getJoinTime() { return joinTime; }
    public double getContribution() { return contribution; }
    public boolean isOnline() { return online; }
    /** 玩家在该公会累计投入的金币总额 */
    public double getInvestedBalance() { return investedBalance; }
}
