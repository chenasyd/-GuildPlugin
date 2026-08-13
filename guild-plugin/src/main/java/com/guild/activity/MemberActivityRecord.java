package com.guild.activity;

import java.util.UUID;

/**
 * Persisted per-member activity counters for a guild.
 */
public final class MemberActivityRecord {

    private final int guildId;
    private final UUID playerUuid;
    private String playerName;
    private int onlineMinutesToday;
    private int onlineMinutesTotal;
    private int activeDaysWeek;
    private String activeDayDate;
    private String weekStartDate;
    private String lastLoginDate;
    private long lastSeen;
    private String todayDate;

    public MemberActivityRecord(int guildId, UUID playerUuid, String playerName) {
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.playerName = playerName != null ? playerName : "";
    }

    public int getGuildId() {
        return guildId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName != null ? playerName : "";
    }

    public int getOnlineMinutesToday() {
        return onlineMinutesToday;
    }

    public void setOnlineMinutesToday(int onlineMinutesToday) {
        this.onlineMinutesToday = Math.max(0, onlineMinutesToday);
    }

    public int getOnlineMinutesTotal() {
        return onlineMinutesTotal;
    }

    public void setOnlineMinutesTotal(int onlineMinutesTotal) {
        this.onlineMinutesTotal = Math.max(0, onlineMinutesTotal);
    }

    public int getActiveDaysWeek() {
        return activeDaysWeek;
    }

    public void setActiveDaysWeek(int activeDaysWeek) {
        this.activeDaysWeek = Math.max(0, activeDaysWeek);
    }

    public String getActiveDayDate() {
        return activeDayDate;
    }

    public void setActiveDayDate(String activeDayDate) {
        this.activeDayDate = activeDayDate;
    }

    public String getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(String weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public String getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(String lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getTodayDate() {
        return todayDate;
    }

    public void setTodayDate(String todayDate) {
        this.todayDate = todayDate;
    }
}
