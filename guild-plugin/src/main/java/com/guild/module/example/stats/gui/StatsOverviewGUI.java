package com.guild.module.example.stats.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.module.example.stats.EconomyContributionFetcher;
import com.guild.module.example.stats.GuildStatsModule;
import com.guild.module.example.stats.ActivityCalculator;
import com.guild.module.example.stats.model.GuildStatistics;
import com.guild.module.example.stats.model.ActivityReport;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StatsOverviewGUI extends AbstractModuleGUI {

    private final GuildStatsModule module;
    private final Guild guild;
    private final GuildStatistics stats;
    private final EconomyContributionFetcher.EconomySummary economySummary;

    public StatsOverviewGUI(GuildStatsModule module, Guild guild, GuildStatistics stats,
                              EconomyContributionFetcher.EconomySummary economySummary) {
        this.module = module;
        this.guild = guild;
        this.stats = stats;
        this.economySummary = economySummary;
        this.inventory = org.bukkit.Bukkit.createInventory(null, getSize(),
            ColorUtils.colorize("&6&l" + guild.getName() + " - 数据统计"));
        setupInventory(this.inventory);
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6&l" + guild.getName() + " - 数据统计");
    }

    @Override
    public void setupInventory(Inventory inv) {
        fillBorder(inv);
        fillInteriorSlots(inv);

        long exp = stats.getExperience();
        String expColor = exp > 0 ? "&b" : "&7";
        String expDisplay = exp >= 10000 ? String.format("%,.0f", exp / 1000.0) + "k" : String.format("%,d", exp);
        inv.setItem(4, createItem(Material.BOOK,
            "&6&l" + guild.getName() + " 基础信息",
            "",
            "&7等级: &f" + stats.getLevel(),
            "&7成员: &f" + stats.getMemberCount() + "/" + stats.getMaxMembers(),
            "&7资金: &e$" + String.format("%.0f", stats.getBalance()),
            "&7经验: " + expColor + expDisplay,
            "&8  (公会经济累计进度, 来自 guild_economy 表)",
            "",
            "&8┃ 数据来源: &7公会核心系统 (只读)"));

        inv.setItem(19, createItem(Material.EMERALD,
            "&a&l活跃度统计",
            "",
            "&7■ 活跃度评分: " + getActivityScoreColor(stats.getActivityScore()) +
                "&l" + String.format("%.1f", stats.getActivityScore()) + "/100",
            "&8  (基于在线时长+B币+角色综合计算)",
            "",
            "&7■ 活跃成员: &f" + stats.getActiveMemberCount(),
            "&8  (当前在线的成员数量)",
            "",
            "&7⚠ 与「B币」的区别:",
            "&8  B币 = 累积数值(只增不减)",
            "&8  活跃度 = 近期活跃程度(动态变化)",
            "",
            "&8┃ 数据来源: &7本模块独立计算 (在线追踪+评分算法)"));

        boolean hasRealEconomy = economySummary != null && !economySummary.allContributions.isEmpty();
        double displayTotalContrib = hasRealEconomy ? economySummary.netTotal : stats.getTotalBCoin();
        double displayAvgContrib = hasRealEconomy && economySummary.uniqueContributors > 0
            ? (economySummary.netTotal / economySummary.uniqueContributors) : stats.getAvgBCoin();

        inv.setItem(21, createItem(Material.GOLD_INGOT,
            "&6&l经济报表",
            "",
            hasRealEconomy ?
                "&7■ 净B币总额: &e" + String.format("%,.0f", displayTotalContrib) :
                "&7■ 总B币(核心): &e" + String.format("%.0f", displayTotalContrib),
            hasRealEconomy ?
                "&8  (存入-取出, 来自 guild_contributions 表)" :
                "&8  (所有成员累积B币之和)",
            "",
            hasRealEconomy ?
                ("&7■ 人均净B币: &f" + String.format("%,.1f", displayAvgContrib) +
                 "\n&8  (净总额 ÷ " + economySummary.uniqueContributors + " 位贡献者)") :
                ("&7■ 人均B币: &f" + String.format("%.1f", displayAvgContrib) +
                 "\n&8  (总B币 ÷ 成员数)"),
            "",
            hasRealEconomy ?
                ("&7■ 累计存入: &a+$" + String.format("%,.0f", economySummary.totalDeposited) +
                 "\n&7■ 累计取出: &c-$" + String.format("%,.0f", economySummary.totalWithdrawn) +
                 "\n&7■ 贡献人数: &f" + economySummary.uniqueContributors + " 人") :
                ("&7■ 经济增长率: " + getGrowthColor(stats.getEconomyGrowthRate()) +
                 String.format("%+.1f%%", stats.getEconomyGrowthRate()) +
                 "\n&8  (相比上周的变化)"),
            "",
            hasRealEconomy ?
                "&8┃ 数据来源: &7guild_contributions 表 (实时)" :
                "&8┃ 数据来源: &7公会核心 MemberData (只读)",
            hasRealEconomy ? "" : "&8  ⚠ 点击进入查看实时数据"));

        inv.setItem(23, createItem(Material.DIAMOND,
            "&b&l综合评分",
            "",
            "&7■ 综合分: " + getOverallScoreColor(stats.getOverallScore()) +
                "&l" + String.format("%.1f", stats.getOverallScore()) + "/1000",
            "",
            "&8━━ 计算公式 ━━",
            "&7综合分 = 等级×50",
            "&8       + 活跃度×3",
            "&8       + B币×0.05",
            "&8  (满分约1000分)",
            "",
            "&8━━ 评分标准 ━━",
            "&7 900+: &6&lSSS 极品公会",
            "&7 700+: &e&lSS 优秀公会", 
            "&7 500+: &a&aS  良好公会",
            "&7 300+: &7&b  普通公会",
            "&7 <300: &c&C  发展中",
            "",
            "&8┃ 数据来源: &7本模块综合计算"));

        inv.setItem(25, createItem(Material.CLOCK,
            "&d&l系统信息",
            "",
            "&7最后更新: &f" + formatTime(stats.getLastUpdated()),
            "&7更新周期: &b每5分钟自动刷新",
            "",
            "&7模块版本: &av1.0.0",
            "&7SDK版本: &a1.3.6"));

        inv.setItem(40, createBackButton("&c&l返回", "&7返回上级菜单"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 19:
                openMemberActivity(player);
                break;
            case 21:
                openEconomyReport(player);
                break;
            case 23:
                openGuildRanking(player);
                break;
            case 40:
                module.getContext().navigateBack(player);
                break;
        }
    }

    private void openMemberActivity(Player player) {
        player.sendMessage(ColorUtils.colorize(
            "&e[Stats] 正在加载活跃度数据，请稍候..."));

        module.getContext().getLogger().info(
            "[Stats] 玩家 " + player.getName() + 
            " 请求查看公会 " + guild.getName() + " 的活跃度统计"
        );

        GuildPluginAPI api = module.getContext().getApi();

        api.getGuildById(guild.getId())
            .thenCompose(guildData -> {
                if (guildData == null) {
                    player.sendMessage(ColorUtils.colorize(
                        "&c[Stats] 无法获取公会数据！"));
                    return CompletableFuture.completedFuture(null);
                }
                return api.getGuildMembers(guild.getId())
                    .thenApply(members -> new Object[]{guildData, members});
            })
            .thenAccept(result -> {
                if (result == null) return;

                GuildData guildData = (GuildData) result[0];
                List<MemberData> members = (List<MemberData>) result[1];

                if (members == null || members.isEmpty()) {
                    player.sendMessage(ColorUtils.colorize(
                        "&c[Stats] 无法获取成员数据！\n" +
                        "&7可能原因: 公会无成员或API异常"));
                    return;
                }

                module.getContext().getLogger().info(String.format(
                    "[Stats] 成功获取 %d 名成员数据 (经验: %d)",
                    members.size(), guildData.getExperience()));

                ActivityCalculator calculator = module.getActivityCalculator();
                if (calculator == null) {
                    player.sendMessage(ColorUtils.colorize(
                        "&c[Stats] 活跃度计算器未初始化"));
                    return;
                }

                ActivityReport report = calculator.calculate(guildData, members);

                module.getEconomyFetcher().fetchEconomySummary(guild.getId())
                    .thenAccept(econSummary -> {
                        player.sendMessage(ColorUtils.colorize(
                            "&a[Stats] 数据加载完成！正在打开界面..."));

                        try {
                            module.getContext().openGUI(player,
                                new MemberActivityGUI(module, report, econSummary));
                        } catch (Exception e) {
                            player.sendMessage(ColorUtils.colorize(
                                "&c[Stats] 打开界面失败: " + e.getMessage()));
                            module.getContext().getLogger().severe(
                                "[Stats] 打开 MemberActivityGUI 异常");
                            e.printStackTrace();
                        }
                    });
            })
            .exceptionally(ex -> {
                player.sendMessage(ColorUtils.colorize(
                    "&c[Stats] 加载失败: " + ex.getMessage()));
                module.getContext().getLogger().severe(
                    "[Stats] 异步加载成员数据异常: " + ex.toString());
                return null;
            });
    }

    private void openGuildRanking(Player player) {
        player.sendMessage(ColorUtils.colorize("&e[Stats] 正在加载全服排行..."));

        var dataCache = module.getDataCache();
        if (dataCache == null) {
            player.sendMessage(ColorUtils.colorize("&c[Stats] 数据缓存未初始化"));
            return;
        }

        var allStats = dataCache.getAllCachedStats();
        if (allStats.isEmpty()) {
            player.sendMessage(ColorUtils.colorize(
                "&c[Stats] 暂无排行数据\n&7请等待首次统计完成(约5分钟)"));
            return;
        }

        allStats.sort((a, b) ->
            Double.compare(b.getOverallScore(), a.getOverallScore()));

        try {
            module.getContext().openGUI(player,
                new GuildRankingGUI(module, allStats));
        } catch (Exception e) {
            player.sendMessage(ColorUtils.colorize(
                "&c[Stats] 打开排行界面失败: " + e.getMessage()));
        }
    }

    private void openEconomyReport(Player player) {
        player.sendMessage(ColorUtils.colorize("&e[Stats] 正在加载经济报表..."));

        GuildPluginAPI api = module.getContext().getApi();

        api.getGuildById(guild.getId())
            .thenCompose(guildData -> {
                if (guildData == null) {
                    player.sendMessage(ColorUtils.colorize(
                        "&c[Stats] 无法获取公会数据！"));
                    return CompletableFuture.completedFuture(null);
                }
                return api.getGuildMembers(guild.getId())
                    .thenApply(members -> new Object[]{guildData, members});
            })
            .thenAccept(result -> {
                if (result == null) return;

                GuildData guildData = (GuildData) result[0];
                List<MemberData> members = (List<MemberData>) result[1];

                if (members == null || members.isEmpty()) {
                    player.sendMessage(ColorUtils.colorize(
                        "&c[Stats] 无法获取成员数据！\n" +
                        "&7可能原因: 公会无成员或API异常"));
                    return;
                }

                ActivityCalculator calculator = module.getActivityCalculator();
                if (calculator == null) {
                    player.sendMessage(ColorUtils.colorize(
                        "&c[Stats] 活跃度计算器未初始化"));
                    return;
                }

                ActivityReport report = calculator.calculate(guildData, members);

                module.getEconomyFetcher().fetchEconomySummary(guild.getId())
                    .thenAccept(economySummary -> {
                        try {
                            module.getContext().openGUI(player,
                                new EconomyReportGUI(module, stats, report, economySummary));
                        } catch (Exception e) {
                            player.sendMessage(ColorUtils.colorize(
                                "&c[Stats] 打开经济报表失败: " + e.getMessage()));
                            module.getContext().getLogger().severe(
                                "[Stats] 打开 EconomyReportGUI 异常");
                            e.printStackTrace();
                        }
                    });
            })
            .exceptionally(ex -> {
                player.sendMessage(ColorUtils.colorize(
                    "&c[Stats] 加载经济报表失败: " + ex.getMessage()));
                return null;
            });
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "未知";
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date(timestamp));
    }

    private String getScoreColor(double score) {
        if (score >= 80) return "&a";
        if (score >= 60) return "&e";
        if (score >= 40) return "&6";
        return "&c";
    }

    private String getActivityScoreColor(double score) {
        if (score >= 80) return "&a&l";
        if (score >= 60) return "&e";
        if (score >= 40) return "&6";
        return "&c";
    }

    private String getOverallScoreColor(double score) {
        if (score >= 900) return "&6&l";
        if (score >= 700) return "&e&l";
        if (score >= 500) return "&a";
        if (score >= 300) return "&7";
        return "&c";
    }

    private String getGrowthColor(double rate) {
        if (rate > 0) return "&a+";
        if (rate < 0) return "&c";
        return "&7";
    }
}
