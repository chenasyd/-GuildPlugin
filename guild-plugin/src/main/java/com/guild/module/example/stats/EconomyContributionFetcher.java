package com.guild.module.example.stats;

import com.guild.models.GuildContribution;
import com.guild.services.GuildService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class EconomyContributionFetcher {

    private final GuildStatsModule module;
    private final Logger logger;

    public EconomyContributionFetcher(GuildStatsModule module, Logger logger) {
        this.module = module;
        this.logger = logger;
    }

    private GuildService getGuildService() {
        return module.getContext().getServiceContainer().get(GuildService.class);
    }

    public CompletableFuture<Map<UUID, Double>> fetchGuildEconomyContributions(int guildId) {
        GuildService guildService = getGuildService();
        return guildService.getGuildContributionsAsync(guildId)
            .thenApply(this::aggregateByPlayer);
    }

    public CompletableFuture<Double> fetchPlayerNetContribution(int guildId, UUID playerUuid) {
        GuildService guildService = getGuildService();
        return guildService.getPlayerContributionsAsync(playerUuid)
            .thenApply(contributions -> {
                double net = 0;
                for (GuildContribution c : contributions) {
                    if (c.getGuildId() == guildId) {
                        net += c.getNetContribution();
                    }
                }
                return net;
            });
    }

    public CompletableFuture<EconomySummary> fetchEconomySummary(int guildId) {
        GuildService guildService = getGuildService();
        return guildService.getGuildContributionsAsync(guildId)
            .thenApply(contributions -> {
                Map<UUID, Double> byPlayer = aggregateByPlayer(contributions);
                double totalDeposited = 0;
                double totalWithdrawn = 0;
                int uniqueContributors = 0;

                for (GuildContribution c : contributions) {
                    if (c.getType() == GuildContribution.ContributionType.DEPOSIT) {
                        totalDeposited += c.getAmount();
                    } else if (c.getType() == GuildContribution.ContributionType.WITHDRAW) {
                        totalWithdrawn += c.getAmount();
                    }
                }

                uniqueContributors = byPlayer.size();

                List<PlayerContribution> topList = byPlayer.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                    .limit(10)
                    .map(e -> new PlayerContribution(e.getKey(), e.getValue()))
                    .collect(java.util.stream.Collectors.toList());

                return new EconomySummary(
                    totalDeposited, totalWithdrawn,
                    totalDeposited - totalWithdrawn,
                    uniqueContributors, topList, byPlayer
                );
            });
    }

    private Map<UUID, Double> aggregateByPlayer(List<GuildContribution> contributions) {
        Map<UUID, Double> map = new HashMap<>();
        for (GuildContribution c : contributions) {
            UUID uuid = c.getPlayerUuid();
            if (uuid == null) continue;
            map.merge(uuid, c.getNetContribution(), Double::sum);
        }
        return map;
    }

    public static class EconomySummary {
        public final double totalDeposited;
        public final double totalWithdrawn;
        public final double netTotal;
        public final int uniqueContributors;
        public final List<PlayerContribution> topContributors;
        public final Map<UUID, Double> allContributions;

        public EconomySummary(double totalDeposited, double totalWithdrawn,
                               double netTotal, int uniqueContributors,
                               List<PlayerContribution> topContributors,
                               Map<UUID, Double> allContributions) {
            this.totalDeposited = totalDeposited;
            this.totalWithdrawn = totalWithdrawn;
            this.netTotal = netTotal;
            this.uniqueContributors = uniqueContributors;
            this.topContributors = topContributors;
            this.allContributions = allContributions;
        }

        public double getPlayerContribution(UUID uuid) {
            return allContributions.getOrDefault(uuid, 0.0);
        }
    }

    public static class PlayerContribution {
        public final UUID playerUuid;
        public final double netAmount;

        public PlayerContribution(UUID playerUuid, double netAmount) {
            this.playerUuid = playerUuid;
            this.netAmount = netAmount;
        }
    }
}
