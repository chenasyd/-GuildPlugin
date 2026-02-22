package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.core.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.guild.core.utils.CompatibleScheduler;

/**
 * 创建工会GUI
 */
public class CreateGuildGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private String guildName = "";
    private String guildTag = "";
    private String guildDescription = "";

    public CreateGuildGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
    }

    public CreateGuildGUI(GuildPlugin plugin, Player player, String guildName, String guildTag, String guildDescription) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.guildName = guildName;
        this.guildTag = guildTag;
        this.guildDescription = guildDescription;
    }

    @Override
    public String getTitle() {
        return plugin.getLanguageManager().getGuiColoredMessage(player, "create-guild.title", "&6创建工会");
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("create-guild.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 添加输入按钮
        setupInputButtons(inventory);
        
        // 添加确认/取消按钮
        setupActionButtons(inventory);
        
        // 显示当前输入信息
        displayCurrentInput(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // 工会名称输入
                handleNameInput(player);
                break;
            case 22: // 工会标签输入
                handleTagInput(player);
                break;
            case 24: // 工会描述输入
                handleDescriptionInput(player);
                break;
            case 39: // 确认创建
                handleConfirmCreate(player);
                break;
            case 41: // 取消
                handleCancel(player);
                break;
        }
    }
    
    /**
     * 填充边框
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * 设置输入按钮
     */
    private void setupInputButtons(Inventory inventory) {
        // 工会名称输入按钮
        ItemStack nameInput = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.name-input.name", "&e工会名称")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.name-input.lore.1", "&7点击输入工会名称")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.name-input.lore.2", "&7长度: 3-20 字符"))
        );
        inventory.setItem(20, nameInput);
        
        // 工会标签输入按钮
        ItemStack tagInput = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.name", "&e工会标签")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.lore.1", "&7点击输入工会标签")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.lore.2", "&7长度: 最多6字符")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.lore.3", "&7可选"))
        );
        inventory.setItem(22, tagInput);
        
        // 工会描述输入按钮
        ItemStack descriptionInput = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.name", "&e工会描述")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.lore.1", "&7点击输入工会描述")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.lore.2", "&7长度: 最多100字符")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.lore.3", "&7可选"))
        );
        inventory.setItem(24, descriptionInput);
    }
    
    /**
     * 设置操作按钮
     */
    private void setupActionButtons(Inventory inventory) {
        // 获取创建费用
        double creationCost = plugin.getConfigManager().getMainConfig().getDouble("guild.creation-cost", 1000.0);
        String costText = String.format("%.0f", creationCost);
        
        // 确认创建按钮
        String confirmName = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.name", "&a确认创建");
        String confirmLore1 = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.lore.1", "&7确认创建工会");
        String confirmLore2 = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.lore.2", "&7费用: {cost} 金币");
        String confirmLore3 = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.lore.3", "&7创建者: {player_name}");
        
        // 替换变量
        confirmLore2 = confirmLore2.replace("{cost}", costText);
        confirmLore3 = confirmLore3.replace("{player_name}", "当前玩家");
        
        ItemStack confirm = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(confirmName),
            ColorUtils.colorize(confirmLore1),
            ColorUtils.colorize(confirmLore2),
            ColorUtils.colorize(confirmLore3)
        );
        inventory.setItem(39, confirm);
        
        // 取消按钮
        ItemStack cancel = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.cancel.name", "&c取消")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.cancel.lore.1", "&7取消创建工会"))
        );
        inventory.setItem(41, cancel);
    }
    
    /**
     * 显示当前输入信息
     */
    private void displayCurrentInput(Inventory inventory) {
        // 当前工会名称
        String nameDisplay = guildName.isEmpty() ? "未设置" : guildName;
        ItemStack currentName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize("&e当前工会名称"),
            ColorUtils.colorize("&7" + nameDisplay)
        );
        inventory.setItem(11, currentName);
        
        // 当前工会标签
        String tagDisplay = guildTag.isEmpty() ? "未设置" : "[" + guildTag + "]";
        ItemStack currentTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize("&e当前工会标签"),
            ColorUtils.colorize("&7" + tagDisplay)
        );
        inventory.setItem(13, currentTag);
        
        // 当前工会描述
        String descriptionDisplay = guildDescription.isEmpty() ? "未设置" : guildDescription;
        ItemStack currentDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize("&e当前工会描述"),
            ColorUtils.colorize("&7" + descriptionDisplay)
        );
        inventory.setItem(15, currentDescription);
    }
    
    /**
     * 处理工会名称输入
     */
    private void handleNameInput(Player player) {
        String message = languageManager.getMessage(player, "gui.input-name", "&a请在聊天中输入工会名称（3-20字符）：");
        player.sendMessage(ColorUtils.colorize(message));

        // 强制关闭GUI以便玩家看到输入提示
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);

        // 延迟设置输入模式，确保GUI完全关闭
        CompatibleScheduler.runTaskLater(plugin, () -> {
            // 设置输入模式
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() < 3) {
                    String errorMessage = languageManager.getMessage(player, "create.name-too-short", "&c工会名称太短！最少需要 {min} 个字符。", "{min}", "3");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                if (input.length() > 20) {
                    String errorMessage = languageManager.getMessage(player, "create.name-too-long", "&c工会名称太长！最多只能有 {max} 个字符。", "{max}", "20");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                guildName = input;
                String successMessage = languageManager.getMessage(player, "gui.name-set", "&a工会名称已设置为：{name}", "{name}", guildName);
                player.sendMessage(ColorUtils.colorize(successMessage));

                // 重新打开GUI显示更新后的内容
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, player, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // 延迟2个tick (0.1秒)
    }
    
    /**
     * 处理工会标签输入
     */
    private void handleTagInput(Player player) {
        String message = languageManager.getMessage(player, "gui.input-tag", "&a请在聊天中输入工会标签（最多6字符，可选）：");
        player.sendMessage(ColorUtils.colorize(message));

        // 强制关闭GUI以便玩家看到输入提示
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);

        // 延迟设置输入模式，确保GUI完全关闭
        CompatibleScheduler.runTaskLater(plugin, () -> {
            // 设置输入模式
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() > 6) {
                    String errorMessage = languageManager.getMessage(player, "create.tag-too-long", "&c工会标签太长！最多只能有 {max} 个字符。", "{max}", "6");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                guildTag = input;
                String successMessage = languageManager.getMessage(player, "gui.tag-set", "&a工会标签已设置为：{tag}", "{tag}", guildTag.isEmpty() ? "无" : guildTag);
                player.sendMessage(ColorUtils.colorize(successMessage));

                // 重新打开GUI显示更新后的内容
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, player, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // 延迟2个tick (0.1秒)
    }
    
    /**
     * 处理工会描述输入
     */
    private void handleDescriptionInput(Player player) {
        String message = languageManager.getMessage(player, "gui.input-description", "&a请在聊天中输入工会描述（最多100字符，可选）：");
        player.sendMessage(ColorUtils.colorize(message));

        // 强制关闭GUI以便玩家看到输入提示
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);

        // 延迟设置输入模式，确保GUI完全关闭
        CompatibleScheduler.runTaskLater(plugin, () -> {
            // 设置输入模式
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() > 100) {
                    String errorMessage = languageManager.getMessage(player, "create.description-too-long", "&c工会描述不能超过100个字符！");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                guildDescription = input;
                String successMessage = languageManager.getMessage(player, "gui.description-set", "&a工会描述已设置为：{description}", "{description}", guildDescription.isEmpty() ? "无" : guildDescription);
                player.sendMessage(ColorUtils.colorize(successMessage));

                // 重新打开GUI显示更新后的内容
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, player, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // 延迟2个tick (0.1秒)
    }
    
    /**
     * 处理确认创建
     */
    private void handleConfirmCreate(Player player) {
        // 验证输入
        if (guildName.isEmpty()) {
            String message = languageManager.getMessage(player, "create.name-required", "&c请先输入工会名称！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (guildName.length() < 3) {
            String message = languageManager.getMessage(player, "create.name-too-short", "&c工会名称太短！最少需要 {min} 个字符。", "{min}", "3");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (guildName.length() > 20) {
            String message = languageManager.getMessage(player, "create.name-too-long", "&c工会名称太长！最多只能有 {max} 个字符。", "{max}", "20");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!guildTag.isEmpty() && guildTag.length() > 6) {
            String message = languageManager.getMessage(player, "create.tag-too-long", "&c工会标签太长！最多只能有 {max} 个字符。", "{max}", "6");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!guildDescription.isEmpty() && guildDescription.length() > 100) {
            String message = languageManager.getMessage(player, "create.description-too-long", "&c工会描述不能超过100个字符！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 检查经济系统
        if (!plugin.getEconomyManager().isVaultAvailable()) {
            String message = languageManager.getMessage(player, "create.economy-not-available", "&c经济系统不可用，无法创建工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 获取创建费用
        double creationCost = plugin.getConfigManager().getMainConfig().getDouble("guild.creation-cost", 1000.0);

        // 检查玩家余额
        if (!plugin.getEconomyManager().hasBalance(player, creationCost)) {
            String message = languageManager.getMessage(player, "create.insufficient-funds", "&c您的余额不足！创建工会需要 {cost} 金币。", "{cost}", String.format("%.0f", creationCost));
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 扣除创建费用
        if (!plugin.getEconomyManager().withdraw(player, creationCost)) {
            String message = languageManager.getMessage(player, "create.payment-failed", "&c扣除创建费用失败！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 创建工会
        String finalTag = guildTag.isEmpty() ? null : guildTag;
        String finalDescription = guildDescription.isEmpty() ? null : guildDescription;

        plugin.getGuildService().createGuildAsync(guildName, finalTag, finalDescription, player.getUniqueId(), player.getName()).thenAccept(success -> {
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String message = languageManager.getMessage(player, "create.success", "&a工会 {name} 创建成功！", "{name}", guildName);
                    player.sendMessage(ColorUtils.colorize(message));

                    // 关闭GUI并返回主界面
                    plugin.getGuiManager().closeGUI(player);
                    plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                } else {
                    // 如果创建失败，退还费用
                    plugin.getEconomyManager().deposit(player, creationCost);
                    String refundMessage = languageManager.getMessage(player, "create.payment-refunded", "&e已退还创建费用 {cost} 金币。", "{cost}", String.format("%.0f", creationCost));
                    player.sendMessage(ColorUtils.colorize(refundMessage));

                    String message = languageManager.getMessage(player, "create.failed", "&c工会创建失败！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * 处理取消
     */
    private void handleCancel(Player player) {
        plugin.getGuiManager().closeGUI(player);
        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
    }
    
    /**
     * 创建物品
     */
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
