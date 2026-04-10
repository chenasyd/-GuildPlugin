package com.guild.module.example.stats.model;

import java.util.UUID;

public class PlayerActivity {
    private UUID playerUuid;
    private String playerName;
    private String role;

    private boolean online;
    private long lastActiveTime;
    private long loginTime;
    private long logoutTime;

    private double contribution;
    private int rankInGuild;

    private double activityScore;
    private long onlineMinutesToday;
    private int activeDaysThisWeek;

    public PlayerActivity(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.onlineMinutesToday = 0;
        this.activeDaysThisWeek = 0;
        this.activityScore = 0;
        this.lastActiveTime = System.currentTimeMillis();
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(UUID playerUuid) { this.playerUuid = playerUuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public long getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(long lastActiveTime) { this.lastActiveTime = lastActiveTime; }
    public long getLoginTime() { return loginTime; }
    public void setLoginTime(long loginTime) { this.loginTime = loginTime; }
    public long getLogoutTime() { return logoutTime; }
    public void setLogoutTime(long logoutTime) { this.logoutTime = logoutTime; }

    public double getContribution() { return contribution; }
    public void setContribution(double contribution) { this.contribution = contribution; }
    public int getRankInGuild() { return rankInGuild; }
    public void setRankInGuild(int rankInGuild) { this.rankInGuild = rankInGuild; }

    public double getActivityScore() { return activityScore; }
    public void setActivityScore(double activityScore) { this.activityScore = activityScore; }
    public long getOnlineMinutesToday() { return onlineMinutesToday; }
    public void setOnlineMinutesToday(long onlineMinutesToday) { this.onlineMinutesToday = onlineMinutesToday; }
    public void addOnlineMinutes(long minutes) { this.onlineMinutesToday += minutes; }
    public int getActiveDaysThisWeek() { return activeDaysThisWeek; }
    public void setActiveDaysThisWeek(int activeDaysThisWeek) { this.activeDaysThisWeek = activeDaysThisWeek; }

    public String getStatusColor() {
        if (online) return "&a";
        long offlineMillis = System.currentTimeMillis() - lastActiveTime;
        long offlineHours = offlineMillis / 3600000L;
        if (offlineHours < 1) return "&e";
        if (offlineHours < 24) return "&6";
        if (offlineHours < 168) return "&c";
        return "&7";
    }

    public String getStatusText() {
        if (online) return "在线";
        long offlineMillis = System.currentTimeMillis() - lastActiveTime;
        long offlineMinutes = offlineMillis / 60000L;
        if (offlineMinutes < 60) return offlineMinutes + " 分钟前";
        long offlineHours = offlineMinutes / 60;
        if (offlineHours < 24) return offlineHours + " 小时前";
        long offlineDays = offlineHours / 24;
        return offlineDays + " 天前";
    }
}
