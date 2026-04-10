package com.guild.module.example.stats;

import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import com.guild.module.example.stats.model.ActivityReport;
import com.guild.module.example.stats.model.PlayerActivity;

import java.util.List;

public class ActivityCalculator {

    private final ActivityTracker tracker;

    public ActivityCalculator(ActivityTracker tracker) {
        this.tracker = tracker;
    }

    public ActivityReport calculate(GuildData guild, List<MemberData> members) {
        ActivityReport report = new ActivityReport(guild.getId());
        report.setGuildName(guild.getName());

        for (MemberData member : members) {
            PlayerActivity activity = calculateMemberActivity(member);
            report.addMemberActivity(activity);
        }

        report.sortByActivityScore();
        report.calculateSummary();

        return report;
    }

    private PlayerActivity calculateMemberActivity(MemberData member) {
        PlayerActivity activity = new PlayerActivity(member.getPlayerUuid());

        activity.setPlayerName(member.getPlayerName());
        activity.setRole(member.getRole());
        activity.setContribution(member.getContribution());

        boolean isOnline = tracker.isPlayerOnline(member.getPlayerUuid());
        activity.setOnline(isOnline);

        long onlineMinutesToday = tracker.getOnlineMinutesToday(member.getPlayerUuid());
        activity.setOnlineMinutesToday(onlineMinutesToday);

        int activeDaysThisWeek = tracker.getActiveDaysThisWeek(member.getPlayerUuid());
        activity.setActiveDaysThisWeek(activeDaysThisWeek);

        long lastActive = tracker.getLastSeen(member.getPlayerUuid());
        if (lastActive > 0) {
            activity.setLastActiveTime(lastActive);
        } else if (member.getJoinTime() > 0) {
            activity.setLastActiveTime(member.getJoinTime() * 1000L);
        } else {
            activity.setLastActiveTime(System.currentTimeMillis() - 86400000L);
        }

        double score = calculateActivityScore(member, isOnline, onlineMinutesToday, activeDaysThisWeek);
        activity.setActivityScore(score);

        return activity;
    }

    private double calculateActivityScore(MemberData member, boolean isOnline,
                                          long onlineMinutesToday, int activeDaysThisWeek) {
        double score = 0.0;

        double contribScore = Math.min(40.0, member.getContribution() * 0.004);
        score += contribScore;

        if (isOnline) {
            score += 30.0;
        } else {
            long lastActive = tracker.getLastSeen(member.getPlayerUuid());
            long offlineHours = (System.currentTimeMillis() - lastActive) / 3600000L;
            if (offlineHours < 1) score += 25.0;
            else if (offlineHours < 6) score += 20.0;
            else if (offlineHours < 24) score += 15.0;
            else if (offlineHours < 72) score += 10.0;
            else score += 5.0;
        }

        String role = member.getRole();
        if ("LEADER".equalsIgnoreCase(role)) score += 10.0;
        else if ("OFFICER".equalsIgnoreCase(role)) score += 7.0;
        else score += 5.0;

        double onlineScore = Math.min(20.0, onlineMinutesToday * 0.05);
        score += onlineScore;

        return Math.min(100.0, Math.max(0.0, score));
    }
}
