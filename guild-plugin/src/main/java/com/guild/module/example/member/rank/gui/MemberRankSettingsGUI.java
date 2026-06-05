package com.guild.module.example.member.rank.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.module.example.member.rank.MemberRankModule;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * A-Coin growth rules configuration GUI.
 * <p>
 * Allows guild leaders to configure A-Coin auto-growth rules,
 * including online time, award intervals, and other parameters.
 */
public class MemberRankSettingsGUI extends AbstractModuleGUI {

    private final MemberRankModule module;
    private final Guild guild;
    private final Player viewer;

    public MemberRankSettingsGUI(MemberRankModule module, Guild guild, Player viewer) {
        this.module = module;
        this.guild = guild;
        this.viewer = viewer;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(module.getContext().getMessage(
                "module.member-rank.settings.title", "&6&lA-Coin Growth Rules Config"));
    }

    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;
        inventory.clear();
        fillBorder(inventory);

        // Title
        inventory.setItem(4, createItem(Material.BOOK,
                "&6&lA-Coin Growth Rules Config",
                "",
                "&7Configure A-Coin auto-growth rules",
                "&7Includes online time, award intervals, etc.",
                "",
                "&7Data source: &eMember Ranking module config",
                "&7Update mode: &aSaved in real-time"));

        // Online check interval
        int checkInterval = module.getContext().getConfig().getInt("activity.online.check-interval-minutes", 1);
        inventory.setItem(11, createItem(Material.CLOCK,
                "&e&lOnline Check Interval",
                "&7Current: &f" + checkInterval + " min(s)",
                "&7How often the system checks player online status",
                "&7Recommended: 1-5 minutes",
                "",
                "&eLeft-click + | Right-click -"));

        // Award interval
        int awardInterval = module.getContext().getConfig().getInt("activity.online.award-every-minutes", 5);
        inventory.setItem(12, createItem(Material.GOLD_INGOT,
                "&e&lAward Interval",
                "&7Current: &f" + awardInterval + " min(s)",
                "&7Time between A-Coin award ticks",
                "&7Recommended: 5-15 minutes",
                "",
                "&eLeft-click + | Right-click -"));

        // Active window
        long activeWindow = module.getContext().getConfig().getLong("activity.online.active-window-seconds", 120);
        inventory.setItem(13, createItem(Material.REDSTONE,
                "&e&lActive Window",
                "&7Current: &f" + activeWindow + " sec(s)",
                "&7How long a player stays considered active",
                "&7Recommended: 60-300 seconds",
                "",
                "&eLeft-click + | Right-click -"));

        // Award points per tick
        int awardPoints = module.getContext().getConfig().getInt("activity.online.award-points", 2);
        inventory.setItem(14, createItem(Material.DIAMOND,
                "&e&lAward Per Tick",
                "&7Current: &f" + awardPoints + " A-Coin(s)",
                "&7How many A-Coins awarded per tick",
                "&7Recommended: 1-5",
                "",
                "&eLeft-click + | Right-click -"));

        // Daily cap
        int dailyCap = module.getContext().getConfig().getInt("activity.online.daily-cap", 60);
        inventory.setItem(15, createItem(Material.EMERALD,
                "&e&lDaily A-Coin Cap",
                "&7Current: &f" + dailyCap + " A-Coin(s)",
                "&7Maximum A-Coins a player can earn per day",
                "&7Recommended: 30-120",
                "",
                "&eLeft-click + | Right-click -"));

        // Initial A-Coins on join
        int defaultContribution = module.getContext().getConfig().getInt("default-contribution-on-join", 0);
        inventory.setItem(20, createItem(Material.GOLD_NUGGET,
                "&e&lInitial A-Coins",
                "&7Current: &f" + defaultContribution + " A-Coin(s)",
                "&7A-Coins granted to new members on join",
                "&7Recommended: 0-50",
                "",
                "&eLeft-click + | Right-click -"));

        // Auto deposit toggle
        boolean autoDepositEnabled = module.getContext().getConfig().getBoolean("auto-deposit.enabled", true);
        inventory.setItem(21, createItem(autoDepositEnabled ? Material.LIME_WOOL : Material.RED_WOOL,
                "&e&lAuto Award",
                "&7Status: &f" + (autoDepositEnabled ? "Enabled" : "Disabled"),
                "&7Whether to auto-award A-Coins",
                "&7When enabled, awards based on online time",
                "",
                "&eClick to toggle"));

        // Auto deposit amount
        double autoDepositAmount = module.getContext().getConfig().getDouble("auto-deposit.amount", 10);
        inventory.setItem(22, createItem(Material.GOLD_BLOCK,
                "&e&lAuto Award Amount",
                "&7Current: &f" + autoDepositAmount + " coins",
                "&7Amount of coins awarded automatically",
                "&7Recommended: 5-50 coins",
                "",
                "&eLeft-click + | Right-click -"));

        // Back button
        inventory.setItem(49, createBackButton(
                module.getContext().getMessage("module.member-rank.gui.back", "&cBack"),
                module.getContext().getMessage("module.member-rank.gui.back-hint", "&7Click to return")));

        fillInteriorSlots(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Back button
        if (slot == 49) {
            module.getContext().navigateBack(player);
            return;
        }

        // Config item click hint
        if (slot >= 11 && slot <= 22) {
            player.sendMessage(ColorUtils.colorize("&e[MemberRank] Config editing is not yet available"));
            return;
        }
    }
}
