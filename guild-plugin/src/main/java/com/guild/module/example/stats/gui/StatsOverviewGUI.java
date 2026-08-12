package com.guild.module.example.stats.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.module.example.stats.EconomyContributionFetcher;
import com.guild.module.example.stats.GuildStatsModule;
import com.guild.module.example.stats.model.GuildStatistics;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Single overview panel: guild stats at a glance. No nested submenus.
 * Server ranking is a separate entry ({@link GuildRankingGUI} / {@code /guild stats top}).
 */
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
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6&l" + guild.getName() + " - Stats");
    }

    @Override
    public void setupInventory(Inventory inv) {
        this.inventory = inv;
        fillBorder(inv);

        inv.setItem(4, createItem(Material.BOOK,
            "&6&l" + guild.getName(),
            "&7Level: &f" + stats.getLevel(),
            "&7Members: &f" + stats.getMemberCount() + "/" + stats.getMaxMembers(),
            "&7Balance: &e$" + String.format("%.0f", stats.getBalance()),
            "&7Exp: &b" + formatExp(stats.getExperience())));

        inv.setItem(20, createItem(Material.EMERALD,
            "&a&lActivity",
            "&7Score: " + scoreColor(stats.getActivityScore())
                + String.format("%.1f", stats.getActivityScore()) + "&7/100",
            "&7Online now: &f" + stats.getActiveMemberCount()));

        boolean hasEconomy = economySummary != null && !economySummary.allContributions.isEmpty();
        double net = hasEconomy ? economySummary.netTotal : stats.getTotalBCoin();
        inv.setItem(22, createItem(Material.GOLD_INGOT,
            "&6&lEconomy",
            hasEconomy
                ? "&7Net B-Coin: &e" + String.format("%,.0f", net)
                : "&7Total B-Coin: &e" + String.format("%.0f", net),
            hasEconomy
                ? "&7Deposit: &a+$" + String.format("%,.0f", economySummary.totalDeposited)
                : "&7Avg B-Coin: &f" + String.format("%.1f", stats.getAvgBCoin()),
            hasEconomy
                ? "&7Withdraw: &c-$" + String.format("%,.0f", economySummary.totalWithdrawn)
                : "&7Growth: " + growthColor(stats.getEconomyGrowthRate())
                    + String.format("%+.1f%%", stats.getEconomyGrowthRate())));

        inv.setItem(24, createItem(Material.DIAMOND,
            "&b&lOverall Score",
            "&7Score: " + overallColor(stats.getOverallScore())
                + String.format("%.1f", stats.getOverallScore()) + "&7/1000",
            "&8level×50 + activity×3 + B-Coin×0.05",
            "",
            "&7Use &e/guild stats top &7for rankings"));

        inv.setItem(31, createItem(Material.CLOCK,
            "&d&lUpdated",
            "&7" + formatTime(stats.getLastUpdated()),
            "&7Auto-refresh every 5 minutes"));

        inv.setItem(49, createBackButton("&c&lBack", "&7Return"));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            module.getContext().navigateBack(player);
        }
    }

    private static String formatExp(long exp) {
        if (exp >= 10000) {
            return String.format("%,.0f", exp / 1000.0) + "k";
        }
        return String.format("%,d", exp);
    }

    private static String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "n/a";
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
            .format(new java.util.Date(timestamp));
    }

    private static String scoreColor(double score) {
        if (score >= 80) return "&a";
        if (score >= 60) return "&e";
        if (score >= 40) return "&6";
        return "&c";
    }

    private static String overallColor(double score) {
        if (score >= 900) return "&6&l";
        if (score >= 700) return "&e&l";
        if (score >= 500) return "&a";
        if (score >= 300) return "&7";
        return "&c";
    }

    private static String growthColor(double rate) {
        if (rate > 0) return "&a";
        if (rate < 0) return "&c";
        return "&7";
    }
}
