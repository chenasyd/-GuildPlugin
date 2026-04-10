package com.guild.module.example.stats.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ActivityReport {
    private int guildId;
    private String guildName;

    private int totalMembers;
    private int onlineCount;
    private int activeTodayCount;
    private double avgActivityScore;
    private double overallScore;

    private List<PlayerActivity> members;

    public ActivityReport(int guildId) {
        this.guildId = guildId;
        this.members = new ArrayList<>();
    }

    public int getGuildId() { return guildId; }
    public void setGuildId(int guildId) { this.guildId = guildId; }
    public String getGuildName() { return guildName; }
    public void setGuildName(String guildName) { this.guildName = guildName; }

    public int getTotalMembers() { return totalMembers; }
    public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }
    public int getOnlineCount() { return onlineCount; }
    public void setOnlineCount(int onlineCount) { this.onlineCount = onlineCount; }
    public int getActiveTodayCount() { return activeTodayCount; }
    public void setActiveTodayCount(int activeTodayCount) { this.activeTodayCount = activeTodayCount; }
    public double getAvgActivityScore() { return avgActivityScore; }
    public void setAvgActivityScore(double avgActivityScore) { this.avgActivityScore = avgActivityScore; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

    public List<PlayerActivity> getMembers() { return members; }
    public void setMembers(List<PlayerActivity> members) { this.members = members; }

    public void addMemberActivity(PlayerActivity activity) {
        members.add(activity);
    }

    public void sortByActivityScore() {
        members.sort(Comparator.comparingDouble(PlayerActivity::getActivityScore).reversed());
        for (int i = 0; i < members.size(); i++) {
            members.get(i).setRankInGuild(i + 1);
        }
    }

    public void calculateSummary() {
        totalMembers = members.size();

        onlineCount = 0;
        double scoreSum = 0;
        int todayActive = 0;

        for (PlayerActivity member : members) {
            if (member.isOnline()) onlineCount++;
            scoreSum += member.getActivityScore();
            if (member.getOnlineMinutesToday() > 10 || member.isOnline()) {
                todayActive++;
            }
        }

        avgActivityScore = totalMembers > 0 ? scoreSum / totalMembers : 0;
        activeTodayCount = todayActive;

        overallScore = avgActivityScore * 0.6 +
                      (onlineCount * 100.0 / Math.max(totalMembers, 1)) * 0.25 +
                      (todayActive * 100.0 / Math.max(totalMembers, 1)) * 0.15;
    }

    public List<PlayerActivity> getPage(int page, int pageSize) {
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, members.size());
        if (fromIndex >= members.size()) {
            return new ArrayList<>();
        }
        return members.subList(fromIndex, toIndex);
    }

    public int getTotalPages(int pageSize) {
        return (int) Math.ceil((double) members.size() / pageSize);
    }
}
