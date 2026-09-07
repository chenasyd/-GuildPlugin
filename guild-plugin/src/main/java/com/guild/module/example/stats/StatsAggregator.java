package com.guild.module.example.stats;

import com.guild.core.module.ModuleContext;
import com.guild.module.example.stats.model.ActivityReport;
import com.guild.module.example.stats.model.GuildStatistics;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 公会统计聚合：拉取成员/经济/活跃度并计算综合分。 */
final class StatsAggregator {

    private final ModuleContext context;
    private final GuildStatsManager statsManager;
    private final StatsDataCache dataCache;
    private final ActivityCalculator activityCalculator;

    StatsAggregator(ModuleContext context, GuildStatsManager statsManager,
                    StatsDataCache dataCache, ActivityCalculator activityCalculator) {
        this.context = context;
        this.statsManager = statsManager;
        this.dataCache = dataCache;
        this.activityCalculator = activityCalculator;
    }

    void updateAllGuildsStatistics() {
        GuildPluginAPI api = context.getApi();
        api.getAllGuilds()
                .thenAccept(guilds -> {
                    List<CompletableFuture<Void>> tasks = new ArrayList<>();
                    for (GuildData guild : guilds) {
                        tasks.add(updateSingleGuild(guild.getId())
                                .thenAccept(stats -> {
                                    dataCache.updateCache(guild.getId(), stats);
                                    statsManager.put(stats);
                                }));
                    }
                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                            .thenRun(() -> {
                                context.getLogger().info(
                                        "[Stats] 已更新 " + guilds.size() + " 个公会的统计数据");
                                context.getEventBus().publish(new GuildStatsModule.StatsRefreshedEvent(
                                        0, "ALL", guilds.size(), 0));
                            })
                            .exceptionally(ex -> {
                                context.getLogger().warning("[Stats] 批量更新失败: " + ex.getMessage());
                                return null;
                            });
                })
                .exceptionally(ex -> {
                    context.getLogger().severe("[Stats] 获取公会列表失败: " + ex.getMessage());
                    return null;
                });
    }

    CompletableFuture<GuildStatistics> updateSingleGuild(int guildId) {
        GuildPluginAPI api = context.getApi();
        var guildService = context.getServiceContainer().get(com.guild.services.GuildService.class);
        CompletableFuture<Long> expFuture = guildService != null
                ? guildService.getGuildEconomyAsync(guildId)
                .thenApply(econ -> econ != null ? (long) econ.getExperience() : 0L)
                : CompletableFuture.completedFuture(0L);
        return api.getGuildById(guildId)
                .thenCompose(guild -> {
                    if (guild == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return api.getGuildMembers(guildId).thenCompose(members ->
                            expFuture.thenCompose(experience ->
                                    api.getMemberActivityScores(guildId)
                                            .exceptionally(ex -> List.of())
                                            .thenApply(scores -> calculateStats(guild, members, experience, scores))));
                });
    }

    GuildStatistics calculateStats(GuildData guild, List<MemberData> members, long experience,
                                   List<com.guild.sdk.data.ActivityScoreData> coreScores) {
        GuildStatistics stats = new GuildStatistics(guild.getId());
        stats.setGuildName(guild.getName());
        stats.setLevel(guild.getLevel());
        stats.setMemberCount(guild.getMemberCount());
        stats.setMaxMembers(guild.getMaxMembers());
        stats.setBalance(guild.getBalance());
        stats.setExperience(experience);

        double totalContrib = 0;
        int onlineCount = 0;
        for (MemberData m : members) {
            totalContrib += m.getContribution();
            if (m.isOnline()) {
                onlineCount++;
            }
        }
        if (coreScores != null && !coreScores.isEmpty()) {
            totalContrib = 0;
            for (var s : coreScores) {
                totalContrib += s.getEconomyPts();
            }
        }

        stats.setTotalBCoin(totalContrib);
        stats.setAvgBCoin(members.isEmpty() ? 0 : totalContrib / members.size());
        stats.setActiveMemberCount(onlineCount);

        double activityScore;
        if (coreScores != null && !coreScores.isEmpty()) {
            double sum = 0;
            for (var s : coreScores) {
                sum += s.getActivityPts();
            }
            activityScore = sum / coreScores.size();
        } else if (!members.isEmpty() && activityCalculator != null) {
            ActivityReport report = activityCalculator.calculate(guild, members);
            if (report != null && !report.getMembers().isEmpty()) {
                double totalScore = 0;
                for (var m : report.getMembers()) {
                    totalScore += m.getActivityScore();
                }
                activityScore = totalScore / members.size();
            } else {
                activityScore = calculateFallbackActivityScore(onlineCount, totalContrib, members.size());
            }
        } else {
            activityScore = members.isEmpty() ? 0
                    : calculateFallbackActivityScore(onlineCount, totalContrib, members.size());
        }
        stats.setActivityScore(activityScore);

        double overallScore = guild.getLevel() * 50 + activityScore * 3 +
                Math.min(totalContrib, 10000) * 0.05;
        stats.setOverallScore(overallScore);

        return stats;
    }

    private static double calculateFallbackActivityScore(int onlineCount, double totalContrib, int memberCount) {
        double onlineScore = Math.min(30.0, onlineCount * 30.0 / Math.max(memberCount, 1));
        double contribScore = Math.min(40.0, (memberCount > 0 ? (totalContrib / memberCount) * 0.004 : 0));
        double baseScore = Math.min(20.0, memberCount * 2.0);
        return Math.min(100.0, contribScore + onlineScore + baseScore + 10.0);
    }
}
