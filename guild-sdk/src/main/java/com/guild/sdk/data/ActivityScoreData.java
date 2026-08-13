package com.guild.sdk.data;

import java.util.UUID;

/**
 * Read-only hybrid activity / contribution score for one guild member (core builtin).
 */
public final class ActivityScoreData {

    private final UUID playerUuid;
    private final String playerName;
    private final double economyPts;
    private final double activityPts;
    private final double totalScore;
    private final int rank;
    private final boolean online;

    public ActivityScoreData(UUID playerUuid, String playerName,
                             double economyPts, double activityPts, double totalScore,
                             int rank, boolean online) {
        this.playerUuid = playerUuid;
        this.playerName = playerName != null ? playerName : "";
        this.economyPts = economyPts;
        this.activityPts = activityPts;
        this.totalScore = totalScore;
        this.rank = rank;
        this.online = online;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public double getEconomyPts() {
        return economyPts;
    }

    public double getActivityPts() {
        return activityPts;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public int getRank() {
        return rank;
    }

    public boolean isOnline() {
        return online;
    }
}
