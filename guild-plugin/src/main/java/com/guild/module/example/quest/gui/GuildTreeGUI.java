package com.guild.module.example.quest.gui;

import com.guild.core.module.ModuleContext;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.module.example.quest.GuildQuestModule;
import com.guild.module.example.quest.QuestTexts;
import com.guild.module.example.quest.tree.GuildTreeService;
import com.guild.module.example.quest.tree.GuildTreeState;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Guild Quest Tree GUI — pixel tree; click tree then type EXP amount in chat.
 */
public class GuildTreeGUI extends AbstractModuleGUI {

    private static final int[] LEAF_SLOTS = {
        3, 4, 5,
        11, 12, 13, 14, 15,
        20, 21, 22, 23, 24,
        29, 30, 32, 33
    };

    private static final int[] TRUNK_SLOTS = {31, 40, 49};

    private static final int SLOT_UPGRADE = 48;
    private static final int SLOT_BACK = 50;

    private final GuildQuestModule module;
    private final ModuleContext context;
    private final QuestTexts tx;
    private final int guildId;
    private final int guildLevel;
    private final UUID playerUuid;
    private final Set<Integer> treeSlots = new HashSet<>();

    public GuildTreeGUI(GuildQuestModule module, int guildId, int guildLevel, UUID playerUuid) {
        super();
        this.module = module;
        this.context = module.getContext();
        this.tx = module.texts();
        this.guildId = guildId;
        this.guildLevel = guildLevel;
        this.playerUuid = playerUuid;
        for (int s : LEAF_SLOTS) treeSlots.add(s);
        for (int s : TRUNK_SLOTS) treeSlots.add(s);
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(tx.t("module.quest.tree.gui-title", "&2&lGuild Tree"));
    }

    @Override
    public void setupInventory(Inventory inv) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        GuildTreeService tree = module.getTreeService();
        GuildTreeState state = tree.getOrCreate(guildId);
        int treeLevel = state.getTreeLevel();
        long storedExp = state.getVirtualExp();
        double rate = tree.getConversionRate(treeLevel);
        int dailyCap = tree.getDailyWithdrawCap(treeLevel);
        int maxSingle = tree.getMaxSingleWithdraw(treeLevel);
        int dailyUsed = playerUuid != null
            ? tree.getDailyWithdrawn(guildId, playerUuid) : 0;
        long upgradeCost = tree.getUpgradeCost(treeLevel);
        boolean guildLevelOk = guildLevel >= treeLevel;
        boolean expOk = storedExp >= upgradeCost;
        boolean canUpgrade = guildLevelOk && expOk;

        String[] treeLore = {
            "",
            tx.tf("module.quest.tree.level", "&7Tree Level: &f{0}", treeLevel),
            tx.tf("module.quest.tree.guild-level", "&7Guild Level: &f{0}", guildLevel),
            tx.tf("module.quest.tree.stored", "&7Stored EXP: &f{0}", storedExp),
            "",
            tx.t("module.quest.tree.click-withdraw", "&eClick to enter withdraw amount")
        };

        ItemStack leaf = createItem(Material.LIME_STAINED_GLASS_PANE,
            tx.t("module.quest.tree.header", "&a&lGuild Experience Tree"),
            treeLore);
        ItemStack trunk = createItem(Material.BROWN_STAINED_GLASS_PANE,
            tx.t("module.quest.tree.header", "&a&lGuild Experience Tree"),
            treeLore);

        for (int s : LEAF_SLOTS) {
            inv.setItem(s, leaf.clone());
        }
        for (int s : TRUNK_SLOTS) {
            inv.setItem(s, trunk.clone());
        }

        inv.setItem(SLOT_UPGRADE, createItem(canUpgrade ? Material.EMERALD : Material.REDSTONE,
            tx.tf("module.quest.tree.upgrade", "&6&lUpgrade Tree (Lv {0} → {1})",
                treeLevel, treeLevel + 1),
            "",
            tx.tf("module.quest.tree.upgrade-cost-progress",
                "&7Cost: &f{0}&7/&f{1}",
                storedExp, upgradeCost),
            canUpgrade
                ? tx.t("module.quest.tree.upgrade-ok", "&aCan upgrade — click to confirm")
                : (!guildLevelOk
                    ? tx.t("module.quest.tree.upgrade-blocked", "&cGuild level too low")
                    : tx.t("module.quest.tree.insufficient-exp-lore", "&cNot enough stored EXP")),
            "",
            tx.tf("module.quest.tree.rate", "&7Conversion Rate: &f{0}",
                String.format(Locale.US, "%.2f", rate)),
            tx.tf("module.quest.tree.daily-cap-progress",
                "&7Daily Withdraw: &f{0}&7/&f{1}", dailyUsed, dailyCap),
            tx.tf("module.quest.tree.single-cap", "&7Max Per Withdraw: &f{0}", maxSingle),
            tx.tf("module.quest.tree.upgrade-req",
                "&7Requires guild level ≥ &f{0} &7(current: &f{1}&7)",
                treeLevel, guildLevel)));

        inv.setItem(SLOT_BACK, createBackButton(
            tx.t("module.quest.back", "&cBack"),
            tx.t("module.quest.back-hint", "&7Return to quest list")));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == SLOT_BACK) {
            context.openGUI(player, new QuestListGUI(module, guildId, player.getUniqueId()));
            return;
        }

        GuildTreeService tree = module.getTreeService();
        if (tree == null) return;

        if (slot == SLOT_UPGRADE) {
            GuildTreeService.UpgradeResult result = tree.tryUpgrade(player, guildId);
            switch (result) {
                case SUCCESS -> context.sendMessage(player, "module.quest.tree.upgrade-success",
                    "&a[Guild Tree] Tree upgraded!");
                case NO_PERMISSION -> context.sendMessage(player, "module.quest.tree.no-permission",
                    "&c[Guild Tree] No permission to upgrade");
                case GUILD_LEVEL_TOO_LOW -> context.sendMessage(player, "module.quest.tree.guild-level-low",
                    "&c[Guild Tree] Guild level is too low to upgrade");
                case INSUFFICIENT_EXP -> context.sendMessage(player, "module.quest.tree.insufficient-exp",
                    "&c[Guild Tree] Not enough virtual EXP");
                default -> context.sendMessage(player, "module.quest.tree.error",
                    "&c[Guild Tree] Operation failed");
            }
            refresh(player);
            return;
        }

        if (treeSlots.contains(slot)) {
            startWithdrawInput(player, tree);
        }
    }

    private void startWithdrawInput(Player player, GuildTreeService tree) {
        GuildTreeState state = tree.getOrCreate(guildId);
        int treeLevel = state.getTreeLevel();
        int maxSingle = tree.getMaxSingleWithdraw(treeLevel);
        int dailyCap = tree.getDailyWithdrawCap(treeLevel);
        int used = tree.getDailyWithdrawn(guildId, player.getUniqueId());
        int remaining = Math.max(0, dailyCap - used);
        int maxAllowed = Math.min(maxSingle, remaining);

        context.getGuiManager().closeGUI(player);

        String prompt = tx.tf("module.quest.tree.withdraw-prompt",
            "&e[Guild Tree] Enter EXP amount to withdraw (max &f{0}&e). Type &ccancel &eto abort.",
            maxAllowed);
        player.sendMessage(ColorUtils.colorize(prompt));

        final int guildIdFinal = guildId;
        final int guildLevelFinal = guildLevel;
        context.getGuiManager().setInputMode(player, input -> {
            String trimmed = input.trim();
            if (trimmed.equalsIgnoreCase("cancel") || trimmed.equalsIgnoreCase("c")
                || trimmed.equalsIgnoreCase("取消")) {
                player.sendMessage(ColorUtils.colorize(tx.t("module.quest.tree.withdraw-cancelled",
                    "&e[Guild Tree] Withdraw cancelled")));
                reopenTree(player, guildIdFinal, guildLevelFinal);
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                player.sendMessage(ColorUtils.colorize(tx.t("module.quest.tree.withdraw-invalid",
                    "&c[Guild Tree] Please enter a positive integer (or cancel)")));
                return false; // keep listening
            }

            if (amount <= 0) {
                player.sendMessage(ColorUtils.colorize(tx.t("module.quest.tree.withdraw-invalid",
                    "&c[Guild Tree] Please enter a positive integer (or cancel)")));
                return false;
            }

            CompatibleScheduler.runTask(context.getPlugin(), player, () -> {
                handleWithdraw(player, tree, amount);
                reopenTree(player, guildIdFinal, guildLevelFinal);
            });
            return true;
        });
    }

    private void reopenTree(Player player, int gId, int gLevel) {
        CompatibleScheduler.runTask(context.getPlugin(), player, () -> {
            if (!player.isOnline()) return;
            context.getGuiManager().openGUI(player,
                new GuildTreeGUI(module, gId, gLevel, player.getUniqueId()));
        });
    }

    private void handleWithdraw(Player player, GuildTreeService tree, int amount) {
        GuildTreeService.WithdrawResult result = tree.withdraw(player, guildId, amount);
        switch (result) {
            case SUCCESS -> context.sendMessage(player, "module.quest.tree.withdraw-success",
                "&a[Guild Tree] Vanilla EXP withdrawn");
            case INSUFFICIENT_BALANCE -> context.sendMessage(player, "module.quest.tree.insufficient-exp",
                "&c[Guild Tree] Not enough virtual EXP");
            case DAILY_CAP -> context.sendMessage(player, "module.quest.tree.daily-cap-reached",
                "&c[Guild Tree] Daily withdraw cap reached");
            case INVALID_AMOUNT -> context.sendMessage(player, "module.quest.tree.withdraw-invalid",
                "&c[Guild Tree] Please enter a positive integer (or cancel)");
            case NO_PERMISSION -> context.sendMessage(player, "module.quest.tree.no-permission",
                "&c[Guild Tree] No permission");
            default -> context.sendMessage(player, "module.quest.tree.error",
                "&c[Guild Tree] Operation failed");
        }
    }
}
