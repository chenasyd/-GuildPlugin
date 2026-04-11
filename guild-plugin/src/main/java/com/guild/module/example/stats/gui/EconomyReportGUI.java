package com.guild.module.example.stats.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.module.example.stats.EconomyContributionFetcher;
import com.guild.module.example.stats.GuildStatsModule;
import com.guild.module.example.stats.model.ActivityReport;
import com.guild.module.example.stats.model.GuildStatistics;
import com.guild.module.example.stats.model.PlayerActivity;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomyReportGUI extends AbstractModuleGUI {

    private final GuildStatsModule module;
    private final GuildStatistics stats;
    private final ActivityReport report;
    private final EconomyContributionFetcher.EconomySummary economySummary;
    private double growthRate;
    private static final int TOP_COUNT = 5;

    public EconomyReportGUI(GuildStatsModule module, GuildStatistics stats, ActivityReport report,
                            EconomyContributionFetcher.EconomySummary economySummary) {
        this.module = module;
        this.stats = stats;
        this.report = report;
        this.economySummary = economySummary;
        this.growthRate = calculateGrowthRate();
        this.inventory = org.bukkit.Bukkit.createInventory(null, getSize(),
            ColorUtils.colorize("&6&l" + stats.getGuildName() + " - 经济报表"));
        setupInventory(this.inventory);
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6&l" + stats.getGuildName() + " - 经济报表");
    }

    @Override
    public void setupInventory(Inventory inv) {
        fillBorder(inv);
        fillInteriorSlots(inv);

        boolean hasRealData = economySummary != null && !economySummary.allContributions.isEmpty();

        inv.setItem(4, createItem(Material.GOLD_INGOT,
            "&6&l" + stats.getGuildName() + " 经济报表",
            "",
            hasRealData ?
                "&8┃ 数据来源: 核心系统 guild_contributions 表 (真实资金记录)" :
                "&8┃ 数据来源: 核心系统 MemberData (暂无存取款记录)",
            "&8┃ 与「成员排名」模块相互独立"));

        setupBalanceOverview(inv, hasRealData);
        setupTopContributors(inv, hasRealData);
        setupDistribution(inv, hasRealData);
        setupGrowthTrend(inv);

        inv.setItem(40, createBackButton("&c&l返回", "&7返回数据统计总览"));
    }

    private void setupBalanceOverview(Inventory inv, boolean hasRealData) {
        double balance = stats.getBalance();
        int memberCount = stats.getMemberCount();

        String balanceColor = balance >= 100000 ? "&6" : balance >= 10000 ? "&e" : "&a";

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(""));
        lore.add(ColorUtils.colorize("&7公会资金余额: " + balanceColor + "$" + String.format("%,.0f", balance)));
        lore.add(ColorUtils.colorize("&8  来自核心系统 GuildData.balance"));
        lore.add(ColorUtils.colorize(""));

        if (hasRealData) {
            lore.add(ColorUtils.colorize("&7累计存入: &a+$" + String.format("%,.0f", economySummary.totalDeposited)));
            lore.add(ColorUtils.colorize("&7累计取出: &c-$" + String.format("%,.0f", economySummary.totalWithdrawn)));
            lore.add(ColorUtils.colorize("&7净流入: &e" + (economySummary.netTotal >= 0 ? "+" : "") +
                "$" + String.format("%,.0f", economySummary.netTotal)));
            lore.add(ColorUtils.colorize("&7贡献人数: &f" + economySummary.uniqueContributors + " 人"));
            lore.add(ColorUtils.colorize(""));
            lore.add(ColorUtils.colorize("&8┃ 基于 /guild deposit 和 /guild withdraw 记录"));
        } else {
            lore.add(ColorUtils.colorize("&7B币总值(核心): &e&l" + String.format("%,.0f", stats.getTotalBCoin())));
            lore.add(ColorUtils.colorize("&8  暂无存取款记录，显示 MemberData 值"));
            lore.add(ColorUtils.colorize("&7人均B币: &f" + String.format("%,.1f", stats.getAvgBCoin())));
        }
        lore.add(ColorUtils.colorize("&7成员总数: &f" + memberCount + " 人"));

        inv.setItem(19, createItem(Material.GOLD_BLOCK,
            "&e&l资金概览",
            lore.toArray(new String[0])));
    }

    private void setupTopContributors(Inventory inv, boolean hasRealData) {
        if (!hasRealData || economySummary.topContributors.isEmpty()) {
            List<PlayerActivity> fallback = getTopContributorsFromReport();
            if (fallback.isEmpty()) {
                inv.setItem(21, createItem(Material.BARRIER,
                    "&e&lTop 贡献者",
                    "",
                    "&c暂无贡献数据",
                    "&7等待成员统计数据刷新..."));
                return;
            }
            setupTopContributorsFallback(inv, fallback);
            return;
        }

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(""));
        lore.add(ColorUtils.colorize("&7基于实际存入/取出金额净额排序"));
        lore.add(ColorUtils.colorize("&8(存款为正，取款为负)"));

        Map<UUID, String> nameMap = buildPlayerNameMap();
        int rank = 1;
        for (EconomyContributionFetcher.PlayerContribution pc : economySummary.topContributors) {
            Material icon = rank == 1 ? Material.GOLD_BLOCK :
                             rank == 2 ? Material.IRON_BLOCK :
                             rank == 3 ? Material.LAPIS_BLOCK : Material.EMERALD;

            String prefix = rank <= 3 ?
                (rank == 1 ? "&6&l[1] " : rank == 2 ? "&f&l[2] " : "&9&l[3] ") :
                "&7[" + rank + "] ";

            String playerName = nameMap.getOrDefault(pc.playerUuid, "未知玩家(" + pc.playerUuid.toString().substring(0, 8) + ")");
            PlayerActivity pa = findPlayerActivity(pc.playerUuid);
            String statusTag = (pa != null && pa.isOnline()) ? "&a●" : "&7●";
            double contribPercent = calculateContribPercent(pc.netAmount);

            lore.add(ColorUtils.colorize(""));
            lore.add(ColorUtils.colorize(prefix + statusTag + " &f" + playerName));
            lore.add(ColorUtils.colorize("   &7净贡献: &e$" + String.format("%,.0f", pc.netAmount) +
                "  &8(" + String.format("%.1f", contribPercent) + "%)"));
            if (pa != null) {
                lore.add(ColorUtils.colorize("   &7角色: " + getRoleDisplay(pa.getRole())));
            }
            rank++;
        }

        lore.add(ColorUtils.colorize(""));
        lore.add(ColorUtils.colorize("&8┃ 来源: guild_contributions 表 (真实资金流)"));

        ItemStack item = new ItemStack(Material.DIAMOND);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&b&lTop " +
                Math.min(TOP_COUNT, economySummary.topContributors.size()) + " 资金贡献者"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(21, item);
    }

    private void setupTopContributorsFallback(Inventory inv, List<PlayerActivity> fallback) {
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(""));
        lore.add(ColorUtils.colorize("&7基于核心系统 MemberData.contribution 排序"));
        lore.add(ColorUtils.colorize("&c⚠ 暂无存取款记录，显示替代数据"));

        int rank = 1;
        for (PlayerActivity member : fallback) {
            Material icon = rank == 1 ? Material.GOLD_BLOCK :
                             rank == 2 ? Material.IRON_BLOCK :
                             rank == 3 ? Material.LAPIS_BLOCK : Material.EMERALD;
            String prefix = rank <= 3 ?
                (rank == 1 ? "&6&l[1] " : rank == 2 ? "&f&l[2] " : "&9&l[3] ") :
                "&7[" + rank + "] ";
            String statusTag = member.isOnline() ? "&a●" : "&7●";
            double contribPercent = calculateContribPercent(member.getContribution());

            lore.add(ColorUtils.colorize(""));
            lore.add(ColorUtils.colorize(prefix + statusTag + " &f" + member.getPlayerName()));
            lore.add(ColorUtils.colorize("   &7贡献(核心): &e" + String.format("%,.0f", member.getContribution()) +
                "  &8(" + String.format("%.1f", contribPercent) + "%)"));
            lore.add(ColorUtils.colorize("   &7角色: " + getRoleDisplay(member.getRole())));
            rank++;
        }

        lore.add(ColorUtils.colorize(""));
        lore.add(ColorUtils.colorize("&8┃ 数据来源: 核心系统 MemberData (只读)"));

        ItemStack item = new ItemStack(Material.DIAMOND);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&b&lTop " + Math.min(TOP_COUNT, fallback.size()) + " 贡献者"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(21, item);
    }

    private void setupDistribution(Inventory inv, boolean hasRealData) {
        if (!hasRealData || economySummary.allContributions.isEmpty()) {
            setupDistributionFallback(inv);
            return;
        }

        double totalNet = economySummary.netTotal;
        if (totalNet == 0) {
            inv.setItem(23, createItem(Material.PAPER,
                "&7&l贡献分布",
                "",
                "&7存入与取出金额相等，净值为0"));
            return;
        }

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7各成员净贡献占比分布 (Top 6)"));

        Map<UUID, String> nameMap = buildPlayerNameMap();
        List<Map.Entry<UUID, Double>> sorted = economySummary.allContributions.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .collect(java.util.stream.Collectors.toList());

        int showCount = Math.min(6, sorted.size());
        int barWidth = 20;

        for (int i = 0; i < showCount; i++) {
            Map.Entry<UUID, Double> entry = sorted.get(i);
            String name = nameMap.getOrDefault(entry.getKey(),
                entry.getKey().toString().substring(0, 8));
            double percent = entry.getValue() / totalNet * 100;
            int filled = (int) Math.round(Math.abs(percent) / 100 * barWidth);
            int empty = barWidth - filled;

            StringBuilder bar = new StringBuilder("&7");
            bar.append(name.length() > 12 ? name.substring(0, 12) : name);
            while (bar.length() < 14) bar.append(" ");
            bar.append(entry.getValue() >= 0 ? "&a" : "&c");
            for (int b = 0; b < filled && b < barWidth; b++) bar.append("█");
            bar.append("&8");
            for (int b = 0; b < empty && b < barWidth; b++) bar.append("░");
            bar.append("&7 ").append(String.format("%.1f%%", percent));

            lore.add(ColorUtils.colorize(bar.toString()));
        }

        if (sorted.size() > showCount) {
            double othersTotal = 0;
            for (int i = showCount; i < sorted.size(); i++) {
                othersTotal += sorted.get(i).getValue();
            }
            double othersPercent = othersTotal / totalNet * 100;
            lore.add(ColorUtils.colorize("&7其他 " + (sorted.size() - showCount) + "人     &8" +
                "░".repeat(barWidth) + " &7" + String.format("%.1f%%", othersPercent)));
        }

        lore.add(ColorUtils.colorize(""));
        lore.add(ColorUtils.colorize("&8┃ 基于 guild_contributions 表真实资金流"));

        ItemStack item = new ItemStack(Material.MAP);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&d&l资金贡献分布占比"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(23, item);
    }

    private void setupDistributionFallback(Inventory inv) {
        List<PlayerActivity> members = report.getMembers();
        if (members.isEmpty()) {
            inv.setItem(23, createItem(Material.PAPER, "&7&l贡献分布", "", "&c无成员数据"));
            return;
        }

        double totalContrib = stats.getTotalBCoin();
        if (totalContrib <= 0) {
            inv.setItem(23, createItem(Material.PAPER, "&7&l贡献分布", "", "&7总贡献值为 0"));
            return;
        }

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7各成员贡献占比分布 (Top 6) [替代数据]"));

        List<PlayerActivity> sorted = new ArrayList<>(members);
        sorted.sort((a, b) -> Double.compare(b.getContribution(), a.getContribution()));

        int showCount = Math.min(6, sorted.size());
        int barWidth = 20;

        for (int i = 0; i < showCount; i++) {
            PlayerActivity m = sorted.get(i);
            double percent = m.getContribution() / totalContrib * 100;
            int filled = (int) Math.round(percent / 100 * barWidth);
            int empty = barWidth - filled;

            StringBuilder bar = new StringBuilder("&7");
            bar.append(m.getPlayerName());
            while (bar.length() < 14) bar.append(" ");
            bar.append("&a");
            for (int b = 0; b < filled && b < barWidth; b++) bar.append("█");
            bar.append("&8");
            for (int b = 0; b < empty && b < barWidth; b++) bar.append("░");
            bar.append("&7 ").append(String.format("%.1f%%", percent));

            lore.add(ColorUtils.colorize(bar.toString()));
        }

        lore.add(ColorUtils.colorize(""));
        lore.add(ColorUtils.colorize("&8┃ 基于 MemberData.contribution (非真实资金)"));

        ItemStack item = new ItemStack(Material.MAP);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&d&l贡献分布占比"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(23, item);
    }

    private void setupGrowthTrend(Inventory inv) {
        String rateText;
        String rateColor;
        String trendIcon;

        if (growthRate == Double.MAX_VALUE) {
            rateText = "首次统计";
            rateColor = "&7";
            trendIcon = "━";
        } else if (growthRate > 0) {
            rateText = String.format("+%.1f%%", growthRate);
            rateColor = "&a";
            trendIcon = "↗";
        } else if (growthRate < 0) {
            rateText = String.format("%.1f%%", growthRate);
            rateColor = "&c";
            trendIcon = "↘";
        } else {
            rateText = "0.0%";
            rateColor = "&7";
            trendIcon = "→";
        }

        long updateInterval = System.currentTimeMillis() - stats.getLastUpdated();
        String timeAgo = formatTimeAgo(updateInterval);

        double displayTotalContrib = (economySummary != null && !economySummary.allContributions.isEmpty())
            ? economySummary.netTotal : stats.getTotalBCoin();

        inv.setItem(25, createItem(Material.CLOCK,
            "&d&l增长趋势",
            "",
            "&7综合增长率: " + rateColor + trendIcon + " " + rateText,
            "&8  (对比上次快照的差值)",
            "",
            "&7当前总贡献: &e" + String.format("%,.0f", displayTotalContrib),
            "&7数据更新: &f" + timeAgo + "前",
            "",
            "&8┃ 更新周期: 每5分钟自动刷新"));
    }

    private List<PlayerActivity> getTopContributorsFromReport() {
        List<PlayerActivity> allMembers = report.getMembers();
        if (allMembers.isEmpty()) return List.of();

        List<PlayerActivity> sorted = new ArrayList<>(allMembers);
        sorted.sort(Comparator.comparingDouble(PlayerActivity::getContribution).reversed());
        int count = Math.min(TOP_COUNT, sorted.size());
        return sorted.subList(0, count);
    }

    private Map<java.util.UUID, String> buildPlayerNameMap() {
        Map<java.util.UUID, String> map = new java.util.HashMap<>();
        if (report == null || report.getMembers() == null) return map;
        for (PlayerActivity pa : report.getMembers()) {
            if (pa.getPlayerUuid() != null) {
                map.put(pa.getPlayerUuid(), pa.getPlayerName());
            }
        }
        return map;
    }

    private PlayerActivity findPlayerActivity(java.util.UUID uuid) {
        if (report == null || report.getMembers() == null) return null;
        for (PlayerActivity pa : report.getMembers()) {
            if (uuid.equals(pa.getPlayerUuid())) return pa;
        }
        return null;
    }

    private double calculateContribPercent(double contribution) {
        double total = stats.getTotalBCoin();
        if (total <= 0) return 0;
        return contribution / total * 100;
    }

    private double calculateGrowthRate() {
        var dataCache = module.getDataCache();
        if (dataCache == null) return Double.MAX_VALUE;

        GuildStatistics prevStats = dataCache.getCachedStats(stats.getGuildId());
        if (prevStats == null || prevStats.getLastUpdated() >= stats.getLastUpdated()) {
            return Double.MAX_VALUE;
        }

        double prevContrib = prevStats.getTotalBCoin();
        double currContrib = stats.getTotalBCoin();

        if (prevContrib <= 0) {
            return currContrib > 0 ? 100.0 : 0.0;
        }

        return ((currContrib - prevContrib) / prevContrib) * 100;
    }

    private String getRoleDisplay(String role) {
        if (role == null) return "&7未知";
        switch (role.toUpperCase()) {
            case "LEADER": return "&6&l会长";
            case "OFFICER": return "&e官员";
            default: return "&7成员";
        }
    }

    private String formatTimeAgo(long millis) {
        if (millis < 60000) return "刚刚";
        if (millis < 3600000) return (millis / 60000) + "分钟";
        if (millis < 86400000) return (millis / 3600000) + "小时";
        return (millis / 86400000) + "天";
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 40) {
            module.getContext().navigateBack(player);
        }
    }
}
