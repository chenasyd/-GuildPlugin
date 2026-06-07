package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildContribution;
import com.guild.models.GuildLog;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 确认变更工会资金 GUI
 * <p>
 * 显示：工会名称、当前资金、操作类型、变更金额、新资金
 * 操作类型：set（设置）、add（增加）、remove（减少）
 */
public class ConfirmChangeFundsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;
    private final String operationType; // "set", "add", "remove"
    private final double amount;

    public ConfirmChangeFundsGUI(GuildPlugin plugin, Guild guild, Player player,
                                 String operationType, double amount) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.operationType = operationType;
        this.amount = amount;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player,
                "confirm-funds.title", "&6确认资金变更"));
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // 边框
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 18, border);
        }
        for (int i = 9; i < 18; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }

        // 工会信息
        double currentBalance = guild.getBalance();
        double newBalance = calculateNewBalance(currentBalance);
        String operationName = getOperationName();
        Material operationMaterial = getOperationMaterial();

        List<String> infoLore = new ArrayList<>();
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                "confirm-funds.guild", "工会") + ": &e" + guild.getName()));
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                "confirm-funds.operation", "操作") + ": " + operationName));
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                "confirm-funds.current-balance", "当前资金") + ": &6" + plugin.getEconomyManager().format(currentBalance)));
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                "confirm-funds.amount", "金额") + ": &f" + plugin.getEconomyManager().format(amount)));
        infoLore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                "confirm-funds.new-balance", "新资金") + ": &a" + plugin.getEconomyManager().format(newBalance)));

        inventory.setItem(13, createItem(operationMaterial,
                ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "confirm-funds.info-title", "&6变更详情")),
                infoLore.toArray(new String[0])));

        // 确认按钮 (slot 11)
        inventory.setItem(11, createItem(Material.EMERALD_BLOCK,
                ColorUtils.colorize("&a" + languageManager.getGuiMessage(player,
                        "confirm-funds.confirm", "确认变更")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                        "confirm-funds.confirm-desc", "执行资金变更"))));

        // 取消按钮 (slot 15)
        inventory.setItem(15, createItem(Material.REDSTONE_BLOCK,
                ColorUtils.colorize("&c" + languageManager.getGuiMessage(player,
                        "confirm-funds.cancel", "取消")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                        "confirm-funds.cancel-desc", "取消变更"))));
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        if (slot == 11) {
            // 确认变更
            executeChange(player);
        } else if (slot == 15) {
            // 取消，返回经济管理
            returnToEconomyManagement(player);
        }
    }

    private void executeChange(Player player) {
        double currentBalance = guild.getBalance();
        double newBalance = calculateNewBalance(currentBalance);

        // 执行资金变更
        plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), newBalance,
                player.getUniqueId().toString(), player.getName())
                .thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (success) {
                            String formattedAmount = String.format("%.2f", amount);
                            String playerName = player.getName();
                            String guildName = guild.getName();

                            // 写入 guild_contributions 表
                            GuildContribution.ContributionType contribType;
                            String contribDesc;
                            GuildLog.LogType logType;
                            String logDesc;

                            switch (operationType) {
                                case "add":
                                    contribType = GuildContribution.ContributionType.DEPOSIT;
                                    contribDesc = languageManager.getGuiMessage(player,
                                            "funds.change.add-contrib",
                                            "{player}增加了{amount}")
                                            .replace("{player}", playerName)
                                            .replace("{amount}", formattedAmount);
                                    logType = GuildLog.LogType.FUND_DEPOSITED;
                                    logDesc = languageManager.getGuiMessage(player,
                                            "funds.change.add-log",
                                            "{player}为{guild}增加资金{amount}")
                                            .replace("{player}", playerName)
                                            .replace("{guild}", guildName)
                                            .replace("{amount}", formattedAmount);
                                    break;
                                case "remove":
                                    contribType = GuildContribution.ContributionType.WITHDRAW;
                                    contribDesc = languageManager.getGuiMessage(player,
                                            "funds.change.remove-contrib",
                                            "{player}扣除了{amount}")
                                            .replace("{player}", playerName)
                                            .replace("{amount}", formattedAmount);
                                    logType = GuildLog.LogType.FUND_WITHDRAWN;
                                    logDesc = languageManager.getGuiMessage(player,
                                            "funds.change.remove-log",
                                            "{player}从{guild}扣除资金{amount}")
                                            .replace("{player}", playerName)
                                            .replace("{guild}", guildName)
                                            .replace("{amount}", formattedAmount);
                                    break;
                                default: // set
                                    double diff = newBalance - currentBalance;
                                    String diffStr = String.format("%.2f", Math.abs(diff));
                                    contribType = GuildContribution.ContributionType.ADMIN;
                                    logType = diff >= 0 ? GuildLog.LogType.FUND_DEPOSITED : GuildLog.LogType.FUND_WITHDRAWN;
                                    String newBalanceStr = String.format("%.2f", newBalance);

                                    if (diff >= 0) {
                                        contribDesc = languageManager.getGuiMessage(player,
                                                "funds.change.set-contrib-increase",
                                                "{player}设置资金为{new}(+{diff})")
                                                .replace("{player}", playerName)
                                                .replace("{new}", newBalanceStr)
                                                .replace("{diff}", diffStr);
                                        logDesc = languageManager.getGuiMessage(player,
                                                "funds.change.set-log-increase",
                                                "{player}将{guild}资金设置为{new}")
                                                .replace("{player}", playerName)
                                                .replace("{guild}", guildName)
                                                .replace("{new}", newBalanceStr);
                                    } else {
                                        contribDesc = languageManager.getGuiMessage(player,
                                                "funds.change.set-contrib-decrease",
                                                "{player}设置资金为{new}(-{diff})")
                                                .replace("{player}", playerName)
                                                .replace("{new}", newBalanceStr)
                                                .replace("{diff}", diffStr);
                                        logDesc = languageManager.getGuiMessage(player,
                                                "funds.change.set-log-decrease",
                                                "{player}将{guild}资金设置为{new}")
                                                .replace("{player}", playerName)
                                                .replace("{guild}", guildName)
                                                .replace("{new}", newBalanceStr);
                                    }
                                    break;
                            }

                            // 记录贡献
                            plugin.getGuildService().addGuildContributionAsync(
                                    guild.getId(), player.getUniqueId(), player.getName(),
                                    amount, contribType, contribDesc);

                            // 记录日志
                            plugin.getGuildService().logGuildActionAsync(
                                    guild.getId(), guild.getName(),
                                    player.getUniqueId().toString(), player.getName(),
                                    logType, logDesc,
                                    languageManager.getGuiMessage(player,
                                            "funds.change.log-details",
                                            "金额:{amount}")
                                            .replace("{amount}", formattedAmount));

                            player.sendMessage(ColorUtils.colorize(
                                    "&a" + languageManager.getGuiMessage(player,
                                            "confirm-funds.success", "资金变更成功！")));
                        } else {
                            player.sendMessage(ColorUtils.colorize(
                                    "&c" + languageManager.getGuiMessage(player,
                                            "confirm-funds.failed", "资金变更失败！")));
                        }
                        returnToEconomyManagement(player);
                    });
                });
    }

    private void returnToEconomyManagement(Player player) {
        EconomyManagementGUI ecoGUI = new EconomyManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, ecoGUI);
    }

    private double calculateNewBalance(double currentBalance) {
        switch (operationType) {
            case "set":
                return amount;
            case "add":
                return currentBalance + amount;
            case "remove":
                return Math.max(0, currentBalance - amount);
            default:
                return currentBalance;
        }
    }

    private String getOperationName() {
        String key = "confirm-funds.operation-" + operationType;
        switch (operationType) {
            case "set":
                return languageManager.getGuiMessage(player, key, "&e设置资金");
            case "add":
                return languageManager.getGuiMessage(player, key, "&a增加资金");
            case "remove":
                return languageManager.getGuiMessage(player, key, "&c扣除资金");
            default:
                return operationType;
        }
    }

    private Material getOperationMaterial() {
        switch (operationType) {
            case "set":
                return Material.GOLD_BLOCK;
            case "add":
                return Material.EMERALD_BLOCK;
            case "remove":
                return Material.LAVA_BUCKET;
            default:
                return Material.PAPER;
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
