package com.guild.module.example.stats.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.module.example.stats.GuildStatsModule;
import com.guild.module.example.stats.model.GuildStatistics;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.gui.AbstractModuleGUI;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GuildRankingGUI extends AbstractModuleGUI {

    private final GuildStatsModule module;
    private final List<GuildStatistics> allStats;
    private int currentPage = 1;
    private final Map<Integer, GuildStatistics> slotDataMap = new HashMap<>();

    public GuildRankingGUI(GuildStatsModule module, List<GuildStatistics> allStats) {
        this.module = module;
        this.allStats = allStats;
        this.inventory = org.bukkit.Bukkit.createInventory(null, getSize(),
            ColorUtils.colorize("&6&l全服公会排行榜"));
        setupInventory(this.inventory);
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6&l全服公会排行榜");
    }

    @Override
    public void setupInventory(Inventory inv) {
        fillBorder(inv);
        fillInteriorSlots(inv);

        slotDataMap.clear();

        int totalGuilds = allStats.size();
        int totalPages = getTotalPages(totalGuilds);
        int startIndex = (currentPage - 1) * PER_PAGE;
        int endIndex = Math.min(startIndex + PER_PAGE, totalGuilds);

        inv.setItem(4, createItem(Material.BOOK,
            "&6&l全服公会排行榜",
            "",
            "&7基于综合评分对全服公会排名",
            "&8  综合分 = 等级×50 + 活跃度×3 + 贡献×0.05",
            "",
            "&8┃ 数据来源: &7本模块独立计算 (定时刷新)",
            "&8┃ 更新周期: &b每5分钟自动刷新",
            "",
            "&7共 &f" + totalGuilds + " &7个公会"));

        for (int i = startIndex; i < endIndex; i++) {
            GuildStatistics s = allStats.get(i);
            int rank = i + 1;
            Material icon = getRankIcon(rank);
            String color = getRankColor(rank);

            int slot = mapToSlot(i - startIndex);
            if (slot != -1) {
                inv.setItem(slot, createItem(icon,
                    color + "&l#" + rank + " " + s.getGuildName(),
                    "&7综合分: &e" + String.format("%.1f", s.getOverallScore()),
                    "&7等级: &f" + s.getLevel(),
                    "&7成员: &f" + s.getMemberCount(),
                    "&7资金: &f$ " + String.format("%,.0f", s.getBalance()),
                    "&7活跃度: &f" + String.format("%.1f", s.getActivityScore()),
                    "",
                    "&8┃ 来自 guild-stats 模块缓存"));
                slotDataMap.put(slot, s);
            }
        }

        setupPagination(inv, currentPage, totalPages,
            "&e&l上一页", "&e&l下一页");

        inv.setItem(49, createItem(Material.PAPER,
            "&7第 &f" + currentPage + " &7/ &f" + totalPages + " &7页",
            "&7共 &f" + totalGuilds + " &7个公会"));
    }

    private Material getRankIcon(int rank) {
        if (rank == 1) return Material.DIAMOND;
        if (rank == 2) return Material.GOLD_BLOCK;
        if (rank == 3) return Material.IRON_BLOCK;
        return Material.BOOK;
    }

    private String getRankColor(int rank) {
        if (rank == 1) return "&6";
        if (rank == 2) return "&e";
        if (rank == 3) return "&7";
        return "&f";
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        int totalPages = getTotalPages(allStats.size());
        if (slot == 45 && currentPage > 1) {
            currentPage--;
            refresh(player);
        } else if (slot == 53 && currentPage < totalPages) {
            currentPage++;
            refresh(player);
        }

        GuildStatistics selected = slotDataMap.get(slot);
        if (selected != null) {
            try {
                Guild guildObj = new Guild();
                guildObj.setId(selected.getGuildId());
                guildObj.setName(selected.getGuildName());
                guildObj.setLevel(selected.getLevel());
                Map<String, Object> overviewData = new HashMap<>();
                overviewData.put("guild", guildObj);
                overviewData.put("stats", selected);
                overviewData.put("economySummary", null);
                GuildPluginAPI api = module.getContext().getApi();
                api.openCustomGUI("stats-overview", player, overviewData);
            } catch (Exception e) {
                player.sendMessage(ColorUtils.colorize(
                    "&c[Stats] 打开总览失败: " + e.getMessage()));
            }
        }
    }
}
