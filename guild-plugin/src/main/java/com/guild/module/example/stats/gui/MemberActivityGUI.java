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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.guild.sdk.GuildPluginAPI;

public class MemberActivityGUI extends AbstractModuleGUI {

    private final GuildStatsModule module;
    private final ActivityReport report;
    private final EconomyContributionFetcher.EconomySummary economySummary;
    private int currentPage = 1;
    private final Map<Integer, PlayerActivity> slotDataMap = new HashMap<>();

    public MemberActivityGUI(GuildStatsModule module, ActivityReport report,
                             EconomyContributionFetcher.EconomySummary economySummary) {
        this.module = module;
        this.report = report;
        this.economySummary = economySummary;
        this.inventory = org.bukkit.Bukkit.createInventory(null, getSize(),
            ColorUtils.colorize("&a&l成员活跃度统计 - " + report.getGuildName()));
        setupInventory(this.inventory);
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&a&l成员活跃度统计 - " + report.getGuildName());
    }

    @Override
    public void setupInventory(Inventory inv) {
        fillBorder(inv);
        fillInteriorSlots(inv);

        slotDataMap.clear();

        String scoreColor = getScoreColor(report.getOverallScore());
        inv.setItem(4, createItem(Material.BOOK,
            "&6&l" + report.getGuildName() + " 活跃度总览",
            "",
            "&7■ 综合评分: " + scoreColor + "&l" + String.format("%.1f", report.getOverallScore()) + "/100",
            "&7  基于活跃度、在线率、贡献综合计算",
            "",
            "&7■ 在线成员: &a" + report.getOnlineCount() + "/" + report.getTotalMembers(),
            "&7■ 今日活跃: &e" + report.getActiveTodayCount() + " 人 (在线>10分钟)",
            "&7■ 平均评分: &f" + String.format("%.1f", report.getAvgActivityScore()),
            "",
            "&8┃ 贡献值来源: &7核心系统 MemberData (只读)",
            "&8┃ 活跃度来源: &7本模块独立计算"));

        List<PlayerActivity> pageMembers = report.getPage(currentPage, PER_PAGE);
        for (int i = 0; i < pageMembers.size(); i++) {
            PlayerActivity member = pageMembers.get(i);
            int slot = mapToSlot(i);
            if (slot != -1) {
                inv.setItem(slot, createMemberItem(member));
                slotDataMap.put(slot, member);  // 记录槽位映射
            }
        }

        int totalPages = report.getTotalPages(PER_PAGE);
        setupPagination(inv, currentPage, totalPages,
            "&e&l上一页", "&e&l下一页");

        inv.setItem(49, createItem(Material.PAPER,
            "&7第 &f" + currentPage + " &7/ &f" + totalPages + " &7页  |  共 &f" + report.getTotalMembers() + " &7名成员"));
    }

    private ItemStack createMemberItem(PlayerActivity member) {
        Material icon = getPlayerIcon(member);
        String statusColor = member.getStatusColor();
        String statusText = member.getStatusText();

        return createItem(icon,
            statusColor + "#" + member.getRankInGuild() + " " + member.getPlayerName(),
            "",
            "&7角色: " + getRoleDisplay(member.getRole()),
            "&7状态: " + statusColor + "● " + statusText,
            "&7贡献(核心): &e" + String.format("%.0f", member.getContribution()),
            "&8  来自 MemberData，与排名模块独立",
            "&7活跃度(本模块): " + getScoreColor(member.getActivityScore()) +
                "&l" + String.format("%.1f", member.getActivityScore()) + "/100",
            "&7今日在线: &b" + formatMinutes(member.getOnlineMinutesToday()),
            "",
            "&e▶ 左键查看详情");
    }

    private Material getPlayerIcon(PlayerActivity member) {
        if (member.isOnline()) {
            if ("LEADER".equalsIgnoreCase(member.getRole())) return Material.DIAMOND_BLOCK;
            if ("OFFICER".equalsIgnoreCase(member.getRole())) return Material.GOLD_BLOCK;
            return Material.EMERALD_BLOCK;
        }
        long offlineHours = (System.currentTimeMillis() - member.getLastActiveTime()) / 3600000L;
        if (offlineHours < 24) return Material.YELLOW_WOOL;
        if (offlineHours < 168) return Material.RED_WOOL;
        return Material.GRAY_WOOL;
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

    private String formatMinutes(long minutes) {
        if (minutes < 60) return minutes + " 分钟";
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (mins == 0) return hours + " 小时";
        return hours + "小时" + mins + "分";
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        int totalPages = report.getTotalPages(PER_PAGE);

        if (slot == 45 && currentPage > 1) {
            currentPage--;
            refresh(player);
            return;
        }

        if (slot == 53 && currentPage < totalPages) {
            currentPage++;
            refresh(player);
            return;
        }

        // 使用槽位映射表查找数据
        PlayerActivity selected = slotDataMap.get(slot);
        if (selected != null) {
            var guiManager = module.getContext().getGuiManager();
            module.getContext().getLogger().info(
                "[Stats] 玩家 " + player.getName()
                + " 查看成员详情: " + selected.getPlayerName()
                + " (导航深度: " + guiManager.getOpenGUICount() + ")");

            try {
                Map<String, Object> detailData = new HashMap<>();
                detailData.put("targetUuid", selected.getPlayerUuid());
                detailData.put("report", report);
                detailData.put("economySummary", economySummary);
                GuildPluginAPI api = module.getContext().getApi();
                api.openCustomGUI("stats-player-detail", player, detailData);
            } catch (Exception e) {
                player.sendMessage(ColorUtils.colorize(
                    "&c[Stats] 打开详情失败: " + e.getMessage()));
                module.getContext().getLogger().severe(
                    "[Stats] 打开 PlayerDetailGUI 异常: " + e.toString());
                e.printStackTrace();
            }
        } else if (slot >= CONTENT_START && slot <= CONTENT_END) {
            // 该位置没有成员数据（可能是空槽或边框）
            // 不显示消息，避免刷屏
        }
    }
}
