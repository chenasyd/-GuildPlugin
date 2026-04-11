package com.guild.module.example.stats.model;

import java.time.LocalDate;

public class GuildStatistics {
    private int guildId;
    private String guildName;
    private String snapshotDate;
    private long lastUpdated;

    private int level;
    private int memberCount;
    private int maxMembers;
    private double balance;
    private long experience;

    private double activityScore;
    private int activeMemberCount;
    private double avgDailyOnline;
    private int loginFrequency;

    private double totalBCoin;
    private double avgBCoin;
    private double economyGrowthRate;
    private int topContributorCount;

    private double overallScore;
    private int rankingPosition;
    private int rankingChange;

    public GuildStatistics(int guildId) {
        this.guildId = guildId;
        this.snapshotDate = LocalDate.now().toString();
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getGuildId() { return guildId; }
    public void setGuildId(int guildId) { this.guildId = guildId; }
    public String getGuildName() { return guildName; }
    public void setGuildName(String guildName) { this.guildName = guildName; }
    public String getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(String snapshotDate) { this.snapshotDate = snapshotDate; }
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public long getExperience() { return experience; }
    public void setExperience(long experience) { this.experience = experience; }
    public double getActivityScore() { return activityScore; }
    public void setActivityScore(double activityScore) { this.activityScore = activityScore; }
    public int getActiveMemberCount() { return activeMemberCount; }
    public void setActiveMemberCount(int activeMemberCount) { this.activeMemberCount = activeMemberCount; }
    public double getAvgDailyOnline() { return avgDailyOnline; }
    public void setAvgDailyOnline(double avgDailyOnline) { this.avgDailyOnline = avgDailyOnline; }
    public int getLoginFrequency() { return loginFrequency; }
    public void setLoginFrequency(int loginFrequency) { this.loginFrequency = loginFrequency; }
    public double getTotalBCoin() { return totalBCoin; }
    public void setTotalBCoin(double totalBCoin) { this.totalBCoin = totalBCoin; }
    public double getAvgBCoin() { return avgBCoin; }
    public void setAvgBCoin(double avgBCoin) { this.avgBCoin = avgBCoin; }
    public double getEconomyGrowthRate() { return economyGrowthRate; }
    public void setEconomyGrowthRate(double economyGrowthRate) { this.economyGrowthRate = economyGrowthRate; }
    public int getTopContributorCount() { return topContributorCount; }
    public void setTopContributorCount(int topContributorCount) { this.topContributorCount = topContributorCount; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
    public int getRankingPosition() { return rankingPosition; }
    public void setRankingPosition(int rankingPosition) { this.rankingPosition = rankingPosition; }
    public int getRankingChange() { return rankingChange; }
    public void setRankingChange(int rankingChange) { this.rankingChange = rankingChange; }
}
