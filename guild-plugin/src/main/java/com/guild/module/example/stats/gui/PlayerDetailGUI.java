package com.guild.module.example.stats.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.module.example.stats.EconomyContributionFetcher;
import com.guild.module.example.stats.GuildStatsModule;
import com.guild.module.example.stats.model.ActivityReport;
import com.guild.module.example.stats.model.PlayerActivity;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerDetailGUI extends AbstractModuleGUI {

    private final GuildStatsModule module;
    private final PlayerActivity playerActivity;
    private final ActivityReport report;
    private final EconomyContributionFetcher.EconomySummary economySummary;

    public PlayerDetailGUI(GuildStatsModule module, PlayerActivity playerActivity,
                            ActivityReport report,
                            EconomyContributionFetcher.EconomySummary economySummary) {
        this.module = module;
        this.playerActivity = playerActivity;
        this.report = report;
        this.economySummary = economySummary;
        this.inventory = org.bukkit.Bukkit.createInventory(null, getSize(),
            ColorUtils.colorize("&6&l" + playerActivity.getPlayerName() + " 的活跃详情"));
        setupInventory(this.inventory);
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6&l" + playerActivity.getPlayerName() + " 的活跃详情");
    }

    @Override
    public void setupInventory(Inventory inv) {
        fillBorder(inv);
        fillInteriorSlots(inv);

        Material avatarIcon = getAvatarIcon();
        String statusColor = playerActivity.getStatusColor();
        String statusText = playerActivity.isOnline() ? "&a&l在线" : statusColor + playerActivity.getStatusText();

        inv.setItem(4, createItem(avatarIcon,
            statusColor + playerActivity.getPlayerName(),
            "&7角色: " + getRoleDisplay(playerActivity.getRole()),
            "&7状态: " + statusText,
            "&7公会排名: &e#" + playerActivity.getRankInGuild()));

        boolean hasRealEconomy = economySummary != null && !economySummary.allContributions.isEmpty();
        double playerNetContrib = hasRealEconomy
            ? economySummary.getPlayerContribution(playerActivity.getPlayerUuid())
            : playerActivity.getContribution();
        double totalForPercent = hasRealEconomy
            ? Math.max(economySummary.netTotal, 1)
            : report.getMembers().stream().mapToDouble(PlayerActivity::getContribution).sum();

        inv.setItem(19, createItem(Material.DIAMOND,
            "&e&l贡献统计",
            "",
            hasRealEconomy ?
                ("&7净贡献金额: &e&l" + String.format("%,.0f", playerNetContrib)) :
                ("&7总贡献值(核心): &e&l" + String.format("%.0f", playerActivity.getContribution())),
            hasRealEconomy ?
                "&8  来源: guild_contributions 表 (真实资金流)" :
                "&8  来源: 公会核心系统 MemberData",
            hasRealEconomy ?
                "&8  (存入-取出, 与排名模块独立)" :
                "&8  特性: 只读，与「成员排名」模块独立",
            "",
            "&7贡献占比: &a" + String.format("%.1f", (totalForPercent > 0 ? playerNetContrib / totalForPercent * 100 : 0)) + "%",
            "&7活跃评分(本模块): " + getScoreColor(playerActivity.getActivityScore()) +
                "&l" + String.format("%.1f", playerActivity.getActivityScore()) + "/100",
            "&8  来源: guild-stats 独立计算"));

        inv.setItem(21, createItem(Material.CLOCK,
            "&b&l在线统计",
            "",
            "&7今日在线: &b" + formatMinutes(playerActivity.getOnlineMinutesToday()),
            "&7本周活跃: &e" + playerActivity.getActiveDaysThisWeek() + " / 7 天",
            "&7最后活跃: " + (playerActivity.isOnline() ? "&a刚刚" :
                statusColor + playerActivity.getStatusText())));

        inv.setItem(23, createItem(Material.NETHER_STAR,
            "&6&l活跃度分析",
            "",
            "&7综合评级: " + getActivityGrade(playerActivity.getActivityScore()),
            "&7总分: " + getScoreColor(playerActivity.getActivityScore()) +
                "&l" + String.format("%.1f", playerActivity.getActivityScore()) + "/100",
            "",
            "&8━━ 得分明细 ━━",
            "&7贡献得分: " + getFactorColor(calcContribScore(), 40) +
                String.format("%.1f", calcContribScore()) + "/40",
            "   &8(" + String.format("%,.0f", getEffectiveContribution()) + " × 0.004)",
            "",
            "&7在线状态: " + getFactorColor(calcOnlineScore(), 30) +
                String.format("%.1f", calcOnlineScore()) + "/30",
            "   &8" + getOnlineStatusDesc(),
            "",
            "&7角色加成: " + getFactorColor(calcRoleScore(), 10) +
                String.format("%.0f", calcRoleScore()) + "/10",
            "   &8" + getRoleDisplay(playerActivity.getRole()),
            "",
            "&7时长加分: " + getFactorColor(calcTimeScore(), 20) +
                String.format("%.1f", calcTimeScore()) + "/20",
            "   &8(今日 " + formatMinutes(playerActivity.getOnlineMinutesToday()) + ")",
            "",
            "&8━━ 排名对比 ━━",
            "&7位次: " + getRankColor() +
                "#" + playerActivity.getRankInGuild() + " &7/ " + report.getTotalMembers(),
            "&8  " + getRankPercentileDesc(),
            "",
            "&7超越成员: &a" + getSurpassedCount() + " &7人 (" +
                calculateSurpassPercent() + ")",
            "&7被超越: &c" + (report.getTotalMembers() - playerActivity.getRankInGuild()) + " 人",
            "&7在线排名: " + (playerActivity.isOnline() ? "&a" : "&7") +
                "#" + getOnlineRank() + " &7/ " + report.getOnlineCount() + " 在线",
            "",
            "&8━━ 分数对比 ━━",
            "&7与均分差距: " + getScoreDiffColor() +
                String.format("%+.1f", playerActivity.getActivityScore() - report.getAvgActivityScore()),
            "&7与最高分差距: " + getMaxDiffColor() +
                String.format("-%.1f", getTopScore() - playerActivity.getActivityScore()),
            "   &8(最高: " + String.format("%.1f", getTopScore()) + ")",
            "&7与最低分差距: " + getMinDiffColor() +
                String.format("%+.1f", playerActivity.getActivityScore() - getBottomScore()),
            "   &8(最低: " + String.format("%.1f", getBottomScore()) + ")",
            "",
            "&8┃ 公式: 贡献40%+在线30%+角色10%+时长20%"));

        inv.setItem(25, createItem(Material.PAPER,
            "&7&l其他信息",
            "",
            "&7公会总人数: &f" + report.getTotalMembers(),
            "&7当前在线: &a" + report.getOnlineCount() + " 人",
            "&7平均评分: &f" + String.format("%.1f", report.getAvgActivityScore())));

        inv.setItem(40, createBackButton("&c&l返回列表", "&7返回成员活跃度统计"));
    }

    private Material getAvatarIcon() {
        if (playerActivity.isOnline()) {
            return Material.PLAYER_HEAD;
        }
        long offlineHours = (System.currentTimeMillis() - playerActivity.getLastActiveTime()) / 3600000L;
        if (offlineHours < 24) return Material.YELLOW_STAINED_GLASS_PANE;
        if (offlineHours < 168) return Material.RED_STAINED_GLASS_PANE;
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    private String getRoleDisplay(String role) {
        if (role == null) return "&7未知";
        switch (role.toUpperCase()) {
            case "LEADER": return "&6&l会长";
            case "OFFICER": return "&e官员";
            default: return "&7成员";
        }
    }

    private String getScoreColor(double score) {
        if (score >= 80) return "&a";
        if (score >= 60) return "&e";
        if (score >= 40) return "&6";
        return "&c";
    }

    private String getActivityGrade(double score) {
        if (score >= 90) return "&6&lS 超级活跃";
        if (score >= 75) return "&e&A 非常活跃";
        if (score >= 60) return "&a&B 活跃";
        if (score >= 40) return "&7&C 一般";
        return "&c&D 不太活跃";
    }

    private String getRankPositionText() {
        int rank = playerActivity.getRankInGuild();
        int total = report.getTotalMembers();
        double percent = (double) rank / total * 100;

        if (percent <= 10) return "&6&l前 10% (精英)";
        if (percent <= 30) return "&e前 30% (优秀)";
        if (percent <= 50) return "&a前 50% (良好)";
        if (percent <= 70) return "&7后 30% (一般)";
        return "&c后 30% (需努力)";
    }

    private String calculateContributionPercent() {
        if (report.getTotalMembers() == 0 || playerActivity.getContribution() == 0) {
            return "0.0";
        }
        double totalContrib = report.getMembers().stream()
            .mapToDouble(PlayerActivity::getContribution)
            .sum();
        if (totalContrib == 0) return "0.0";
        return String.format("%.1f", (playerActivity.getContribution() / totalContrib * 100));
    }

    private String calculateSurpassPercent() {
        int rank = playerActivity.getRankInGuild();
        int total = report.getTotalMembers();
        if (total == 0) return "0.0";
        double surpass = ((double)(total - rank) / total) * 100;
        return String.format("%.1f", surpass);
    }

    private String formatMinutes(long minutes) {
        if (minutes < 60) return minutes + " 分钟";
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (mins == 0) return hours + " 小时";
        return hours + "小时" + mins + "分";
    }

    private double calcContribScore() {
        double contribValue = getEffectiveContribution();
        return Math.min(40.0, contribValue * 0.004);
    }

    private double getEffectiveContribution() {
        if (economySummary != null && !economySummary.allContributions.isEmpty()) {
            double net = economySummary.getPlayerContribution(playerActivity.getPlayerUuid());
            if (net != 0) return net;
        }
        return playerActivity.getContribution();
    }

    private double calcOnlineScore() {
        if (playerActivity.isOnline()) return 30.0;
        long offlineHours = (System.currentTimeMillis() - playerActivity.getLastActiveTime()) / 3600000L;
        if (offlineHours < 1) return 25.0;
        if (offlineHours < 6) return 20.0;
        if (offlineHours < 24) return 15.0;
        if (offlineHours < 72) return 10.0;
        return 5.0;
    }

    private double calcRoleScore() {
        String role = playerActivity.getRole();
        if ("LEADER".equalsIgnoreCase(role)) return 10.0;
        if ("OFFICER".equalsIgnoreCase(role)) return 7.0;
        return 5.0;
    }

    private double calcTimeScore() {
        return Math.min(20.0, playerActivity.getOnlineMinutesToday() * 0.05);
    }

    private String getFactorColor(double value, double max) {
        double ratio = value / max;
        if (ratio >= 0.8) return "&a";
        if (ratio >= 0.5) return "&e";
        if (ratio >= 0.25) return "&6";
        return "&c";
    }

    private String getOnlineStatusDesc() {
        if (playerActivity.isOnline()) return "当前在线 (+30)";
        long offlineHours = (System.currentTimeMillis() - playerActivity.getLastActiveTime()) / 3600000L;
        if (offlineHours < 1) return "离线<1小时 (+25)";
        if (offlineHours < 6) return "离线" + offlineHours + "小时 (+20)";
        if (offlineHours < 24) return "离线" + offlineHours + "h (+15)";
        if (offlineHours < 72) return "离线" + (offlineHours / 24) + "天 (+10)";
        return "离线较久 (+5)";
    }

    private String getScoreDiffColor() {
        double diff = playerActivity.getActivityScore() - report.getAvgActivityScore();
        if (diff >= 10) return "&a";
        if (diff >= 0) return "&e";
        if (diff >= -10) return "&6";
        return "&c";
    }

    private String getRankColor() {
        int rank = playerActivity.getRankInGuild();
        int total = report.getTotalMembers();
        if (total == 0) return "&7";
        double pct = (double) rank / total * 100;
        if (pct <= 10) return "&6&l";
        if (pct <= 25) return "&e&l";
        if (pct <= 50) return "&a";
        return "&7";
    }

    private String getRankPercentileDesc() {
        int rank = playerActivity.getRankInGuild();
        int total = report.getTotalMembers();
        if (total == 0) return "无数据";
        double pct = (double) rank / total * 100;
        if (pct <= 5) return "顶尖 5% (公会精英)";
        if (pct <= 10) return "前 10% (核心成员)";
        if (pct <= 25) return "前 25% (活跃骨干)";
        if (pct <= 50) return "前 50% (表现良好)";
        if (pct <= 75) return "中游位置 (有提升空间)";
        return "后段 (需要加油)";
    }

    private int getSurpassedCount() {
        return Math.max(0, playerActivity.getRankInGuild() - 1);
    }

    private int getOnlineRank() {
        if (!report.getMembers().isEmpty()) {
            int onlineOrder = 1;
            for (var m : report.getMembers()) {
                if (m.getPlayerUuid() != null && m.getPlayerUuid().equals(playerActivity.getPlayerUuid())) {
                    break;
                }
                if (m.isOnline()) onlineOrder++;
            }
            return Math.min(onlineOrder, report.getOnlineCount());
        }
        return playerActivity.isOnline() ? 1 : 999;
    }

    private double getTopScore() {
        return report.getMembers().stream()
            .mapToDouble(PlayerActivity::getActivityScore)
            .max()
            .orElse(100.0);
    }

    private double getBottomScore() {
        return report.getMembers().stream()
            .mapToDouble(PlayerActivity::getActivityScore)
            .min()
            .orElse(0.0);
    }

    private String getMaxDiffColor() {
        double diff = getTopScore() - playerActivity.getActivityScore();
        if (diff <= 5) return "&a";
        if (diff <= 20) return "&e";
        if (diff <= 40) return "&6";
        return "&c";
    }

    private String getMinDiffColor() {
        double diff = playerActivity.getActivityScore() - getBottomScore();
        if (diff >= 60) return "&a";
        if (diff >= 30) return "&e";
        if (diff >= 10) return "&6";
        return "&c";
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 40) {
            module.getContext().openGUI(player, new MemberActivityGUI(module, report, economySummary));
        }
    }
}
