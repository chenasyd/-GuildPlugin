package com.guild.module.example.stats;

import com.guild.models.Guild;
import com.guild.module.example.stats.gui.GuildRankingGUI;
import com.guild.module.example.stats.gui.StatsOverviewGUI;
import com.guild.module.example.stats.model.GuildStatistics;
import com.guild.sdk.GuildPluginAPI;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** /guild stats 子命令与统计 GUI 入口。 */
final class StatsCommandHandler {

    private final GuildStatsModule module;
    private final StatsAggregator aggregator;

    StatsCommandHandler(GuildStatsModule module, StatsAggregator aggregator) {
        this.module = module;
        this.aggregator = aggregator;
    }

    void handleStatsCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "top" -> openGuildRanking(player);
                case "refresh" -> {
                    if (player.hasPermission("guild.stats.admin.refresh")) {
                        module.getContext().sendMessage(player, "stats.refreshing", "&a[Stats] 正在刷新...");
                        forceRefresh(player);
                    } else {
                        module.getContext().sendMessage(player, "stats.no-permission", "&c权限不足");
                    }
                }
                default -> {
                    if (args.length > 1 && args[0].equalsIgnoreCase("view")) {
                        openGuildStatsByName(player, args[1]);
                    } else {
                        showHelp(player);
                    }
                }
            }
        } else {
            openMyGuildStats(player);
        }
    }

    void openStatsOverview(Player player, Object... ctx) {
        Guild guild = extractGuild(ctx);
        if (guild == null) {
            module.getContext().sendMessage(player, "stats.error.no-guild", "&c无法获取公会信息");
            return;
        }

        module.getContext().sendMessage(player, "stats.loading", "&e[Stats] 正在加载最新统计数据...");

        CompletableFuture<GuildStatistics> statsFuture = aggregator.updateSingleGuild(guild.getId());
        CompletableFuture<EconomyContributionFetcher.EconomySummary> economyFuture =
                module.getEconomyFetcher().fetchEconomySummary(guild.getId())
                        .exceptionally(ex -> {
                            module.getContext().getLogger().warning(
                                    "[Stats] 经济数据加载失败(非致命): " + ex.getMessage());
                            return null;
                        });

        statsFuture.thenAcceptBoth(economyFuture, (stats, econSummary) -> {
            if (stats != null) {
                module.getDataCache().updateCache(guild.getId(), stats);
                module.getContext().getLogger().info(String.format(
                        "[Stats] 刷新公会 %s 统计: 活跃度=%.1f 贡献=%.0f 经济=%s",
                        guild.getName(),
                        stats.getActivityScore(),
                        stats.getTotalBCoin(),
                        econSummary != null ? String.format("净$%,.0f", econSummary.netTotal) : "无"));
                try {
                    module.getContext().openGUI(player,
                            new StatsOverviewGUI(module, guild, stats, econSummary));
                } catch (Exception e) {
                    module.getContext().sendMessage(player, "stats.error.gui-fail",
                            "&c[Stats] 打开界面失败: " + e.getMessage());
                    module.getContext().getLogger().log(java.util.logging.Level.SEVERE,
                            "[Stats] Failed to open overview GUI", e);
                }
            } else {
                GuildStatistics fallback = module.getDataCache().getCachedStats(guild.getId());
                if (fallback != null) {
                    module.getContext().openGUI(player,
                            new StatsOverviewGUI(module, guild, fallback, econSummary));
                } else {
                    module.getContext().sendMessage(player, "stats.error.no-data",
                            "&c[Stats] 无法加载统计数据");
                }
            }
        }).exceptionally(ex -> {
            GuildStatistics fallback = module.getDataCache().getCachedStats(guild.getId());
            if (fallback != null) {
                module.getContext().openGUI(player, new StatsOverviewGUI(module, guild, fallback, null));
            } else {
                module.getContext().sendMessage(player, "stats.error.load-fail",
                        "&c[Stats] 加载失败: " + ex.getMessage());
            }
            return null;
        });
    }

    void openGuildRanking(Player player) {
        List<GuildStatistics> allStats = module.getDataCache().getAllCachedStats();
        allStats.sort((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()));
        module.getContext().openGUI(player, new GuildRankingGUI(module, allStats));
    }

    private void openGuildStatsByName(Player player, String guildName) {
        module.getContext().sendMessage(player, "stats.loading", "&e[Stats] 正在查询公会: &f" + guildName);
        GuildPluginAPI api = module.getContext().getApi();
        api.getGuildByName(guildName)
                .thenCompose(guildData -> {
                    if (guildData == null) {
                        module.getContext().sendMessage(player, "stats.error.guild-not-found",
                                "&c未找到名为 \"" + guildName + "\" 的公会");
                        return CompletableFuture.completedFuture(null);
                    }
                    return aggregator.updateSingleGuild(guildData.getId())
                            .thenCombine(module.getEconomyFetcher().fetchEconomySummary(guildData.getId())
                                    .exceptionally(ex -> null), (stats, econSummary) -> {
                                if (stats != null) {
                                    module.getDataCache().updateCache(guildData.getId(), stats);
                                    try {
                                        Guild guild = new Guild(
                                                guildData.getName(), "", "", guildData.getMasterUuid(),
                                                guildData.getMasterName());
                                        guild.setId(guildData.getId());
                                        guild.setLevel(guildData.getLevel());
                                        module.getContext().openGUI(player,
                                                new StatsOverviewGUI(module, guild, stats, econSummary));
                                    } catch (Exception e) {
                                        module.getContext().sendMessage(player, "stats.error.gui-fail",
                                                "&c[Stats] 打开界面失败: " + e.getMessage());
                                    }
                                } else {
                                    module.getContext().sendMessage(player, "stats.error.no-data",
                                            "&c[Stats] 无法加载该公会的统计数据");
                                }
                                return null;
                            });
                })
                .exceptionally(ex -> {
                    module.getContext().sendMessage(player, "stats.error.load-fail",
                            "&c[Stats] 查询失败: " + ex.getMessage());
                    return null;
                });
    }

    private void openMyGuildStats(Player player) {
        UUID uuid = player.getUniqueId();
        GuildPluginAPI api = module.getContext().getApi();
        api.getPlayerGuild(uuid).thenAccept(guildData -> {
            if (guildData != null) {
                Guild guild = new Guild(
                        guildData.getName(), "", "", guildData.getMasterUuid(), guildData.getMasterName());
                guild.setId(guildData.getId());
                guild.setLevel(guildData.getLevel());
                module.getContext().runSync(() -> openStatsOverview(player, guild));
            } else {
                module.getContext().runSync(() -> module.getContext().sendMessage(player,
                        "stats.error.no-guild-member", "&c你不在任何公会中"));
            }
        }).exceptionally(ex -> {
            module.getContext().runSync(() -> module.getContext().sendMessage(player,
                    "stats.error.load-fail", "&c[Stats] 查询失败: " + ex.getMessage()));
            return null;
        });
    }

    private void forceRefresh(Player player) {
        aggregator.updateAllGuildsStatistics();
        module.getContext().sendMessage(player, "stats.refresh-triggered",
                "&a[Stats] 已触发全量刷新，请稍后查看");
    }

    private void showHelp(Player player) {
        module.getContext().sendMessage(player, "stats.help.header", "&6&l=== 公会统计命令 ===");
        module.getContext().sendMessage(player, "stats.help.view", "&e/guild stats &7- 查看自己公会");
        module.getContext().sendMessage(player, "stats.help.top", "&e/guild stats top &7- 全服排行");
        module.getContext().sendMessage(player, "stats.help.refresh", "&e/guild stats refresh &7- 刷新数据(OP)");
        module.getContext().sendMessage(player, "stats.help.view-name",
                "&e/guild stats view <名称> &7- 按名查询其他公会");
    }

    private Guild extractGuild(Object... ctx) {
        if (ctx != null && ctx.length > 0 && ctx[0] instanceof Guild guild) {
            return guild;
        }
        return null;
    }
}
