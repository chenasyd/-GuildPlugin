package com.guild.module.example.stats.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.module.example.stats.GuildStatsModule;
import com.guild.module.example.stats.model.GuildStatistics;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Server-wide guild ranking (paginated). Display-only — no drill-down into other GUIs.
 */
public class GuildRankingGUI extends AbstractModuleGUI {

    private final GuildStatsModule module;
    private final List<GuildStatistics> allStats;
    private int currentPage = 1;

    public GuildRankingGUI(GuildStatsModule module, List<GuildStatistics> allStats) {
        this.module = module;
        this.allStats = allStats;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6&lGuild Ranking");
    }

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        fillBorder(inv);
        fillInteriorSlots(inv);

        int totalGuilds = allStats.size();
        int totalPages = getTotalPages(totalGuilds);
        int startIndex = (currentPage - 1) * PER_PAGE;
        int endIndex = Math.min(startIndex + PER_PAGE, totalGuilds);

        inv.setItem(4, createItem(Material.GOLD_BLOCK,
            "&6&lServer Ranking",
            "&7By overall score",
            "&7Guilds: &f" + totalGuilds));

        for (int i = startIndex; i < endIndex; i++) {
            GuildStatistics s = allStats.get(i);
            int rank = i + 1;
            int slot = mapToSlot(i - startIndex);
            if (slot == -1) {
                continue;
            }
            inv.setItem(slot, createItem(rankIcon(rank),
                rankColor(rank) + "&l#" + rank + " " + s.getGuildName(),
                "&7Score: &e" + String.format("%.1f", s.getOverallScore()),
                "&7Level: &f" + s.getLevel() + "  &7Members: &f" + s.getMemberCount(),
                "&7Activity: &f" + String.format("%.1f", s.getActivityScore())));
        }

        setupPagination(inv, currentPage, totalPages, "&e&lPrev", "&e&lNext");
        inv.setItem(49, createItem(Material.PAPER,
            "&7Page &f" + currentPage + " &7/ &f" + totalPages));
        inv.setItem(40, createBackButton("&c&lBack", "&7Return"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        int totalPages = getTotalPages(allStats.size());
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
        if (slot == 40) {
            module.getContext().navigateBack(player);
        }
    }

    private static Material rankIcon(int rank) {
        if (rank == 1) return Material.DIAMOND;
        if (rank == 2) return Material.GOLD_BLOCK;
        if (rank == 3) return Material.IRON_BLOCK;
        return Material.BOOK;
    }

    private static String rankColor(int rank) {
        if (rank == 1) return "&6";
        if (rank == 2) return "&e";
        if (rank == 3) return "&7";
        return "&f";
    }
}
