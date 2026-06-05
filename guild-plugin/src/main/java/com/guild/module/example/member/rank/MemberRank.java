package com.guild.module.example.member.rank;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Member A-Coin ranking data model.
 * <p>
 * Records a guild member's accumulated A-Coins and last active time,
 * used to display the A-Coin leaderboard in the GUI.
 */
public class MemberRank {

    private UUID playerUuid;
    private String playerName;
    private int guildId;
    private long aCoin;        // Accumulated A-Coins
    private LocalDateTime lastActive; // Last active time

    /** No-arg constructor (used by JSON deserialization) */
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

    // ==================== Business Methods ====================

    /** Add A-Coins */
    public void addACoin(long amount) {
        this.aCoin += amount;
        this.lastActive = LocalDateTime.now();
    }

    /** Record activity (without changing A-Coins) */
    public void touchActive() {
        this.lastActive = LocalDateTime.now();
    }
}
