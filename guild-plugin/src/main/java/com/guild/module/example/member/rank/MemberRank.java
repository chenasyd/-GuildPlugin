package com.guild.module.example.member.rank;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 成员A币排名数据模型
 * <p>
 * 记录工会成员的累积A币和最近活跃时间，
 * 用于在 GUI 中展示A币排行榜。
 */
public class MemberRank {

    private UUID playerUuid;
    private String playerName;
    private int guildId;
    private long aCoin;        // 累积A币
    private LocalDateTime lastActive; // 最近活跃时间

    /** 无参构造器（JSON 反序列化使用） */
    public MemberRank() {}

    public MemberRank(UUID playerUuid, String playerName, int guildId) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.guildId = guildId;
        this.aCoin = 0;
        this.lastActive = LocalDateTime.now();
    }

    public MemberRank(UUID playerUuid, String playerName, int guildId, long aCoin) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.guildId = guildId;
        this.aCoin = aCoin;
        this.lastActive = LocalDateTime.now();
    }

    // ==================== Getters & Setters ====================

    public UUID getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getGuildId() { return guildId; }
    public void setGuildId(int guildId) { this.guildId = guildId; }

    public long getACoin() { return aCoin; }
    public void setACoin(long aCoin) { this.aCoin = aCoin; }

    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }

    // ==================== 业务方法 ====================

    /** 增加A币 */
    public void addACoin(long amount) {
        this.aCoin += amount;
        this.lastActive = LocalDateTime.now();
    }

    /** 记录活跃（不改变A币） */
    public void touchActive() {
        this.lastActive = LocalDateTime.now();
    }
}
