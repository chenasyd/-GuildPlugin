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

import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.CompatibleScheduler;

import org.geysermc.cumulus.form.CustomForm;

/**
 * 创建公会GUI
 */
public class CreateGuildGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_CURRENT_NAME = "CURRENT_NAME";
    public static final String FUNC_CURRENT_TAG = "CURRENT_TAG";
    public static final String FUNC_CURRENT_DESC = "CURRENT_DESC";
    public static final String FUNC_NAME_INPUT = "NAME_INPUT";
    public static final String FUNC_TAG_INPUT = "TAG_INPUT";
    public static final String FUNC_DESC_INPUT = "DESC_INPUT";
    public static final String FUNC_CONFIRM = "CONFIRM";
    public static final String FUNC_CANCEL = "CANCEL";

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
        return ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.create-guild.title", "&6Create Guild"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        CustomForm form = CustomForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.create-guild.bedrock-title", "&6Create Guild"))
                .input(languageManager.getGuiColoredMessage(player, "gui.create-guild.bedrock-name-label", "&fGuild Name (3-20 characters)"), languageManager.getGuiColoredMessage(player, "gui.create-guild.bedrock-name-placeholder", "Enter guild name"), guildName)
                .input(languageManager.getGuiColoredMessage(player, "gui.create-guild.bedrock-tag-label", "&fGuild Tag (max 6 characters, optional)"), languageManager.getGuiColoredMessage(player, "gui.create-guild.bedrock-tag-placeholder", "Enter guild tag"), guildTag)
                .input(languageManager.getGuiColoredMessage(player, "gui.create-guild.bedrock-desc-label", "&fGuild Description (max 100 characters, optional)"), languageManager.getGuiColoredMessage(player, "gui.create-guild.bedrock-desc-placeholder", "Enter guild description"), guildDescription)
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    String name = response.getInput(0);
                    String tag = response.getInput(1);
                    String desc = response.getInput(2);
                    guildName = name != null ? name.trim() : "";
                    guildTag = tag != null ? tag.trim() : "";
                    guildDescription = desc != null ? desc.trim() : "";
                    handleConfirmCreate(player);
                }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(plugin, player, () ->
                        handleCancel(player)))
                .build();

        return BedrockFormSender.sendForm(player.getUniqueId(), form);
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

        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // 公会名称输入
                handleNameInput(player);
                break;
            case 22: // 公会标签输入
                handleTagInput(player);
                break;
            case 24: // 公会描述输入
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
        // 公会名称输入按钮
        ItemStack nameInput = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-name-input-name", "&eGuild Name")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-name-input-lore-1", "&7Click to enter guild name")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-name-input-lore-2", "&7Length: 3-20 characters"))
        );
        inventory.setItem(20, nameInput);

        // 公会标签输入按钮
        ItemStack tagInput = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-tag-input-name", "&eGuild Tag")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-tag-input-lore-1", "&7Click to enter guild tag")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-tag-input-lore-2", "&7Length: Max 6 characters")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-tag-input-lore-3", "&7Optional"))
        );
        inventory.setItem(22, tagInput);

        // 公会描述输入按钮
        ItemStack descriptionInput = createItem(
            Material.BOOK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-description-input-name", "&eGuild Description")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-description-input-lore-1", "&7Click to enter guild description")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-description-input-lore-2", "&7Length: Max 100 characters")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-description-input-lore-3", "&7Optional"))
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
        String confirmName = languageManager.getGuiMessage(player, "gui.create-guild.create-guild-confirm-name", "&aConfirm Create");
        String confirmLore1 = languageManager.getGuiMessage(player, "gui.create-guild.create-guild-confirm-lore-1", "&7Confirm guild creation");
        String confirmLore2 = languageManager.getGuiMessage(player, "gui.create-guild.create-guild-confirm-lore-2", "&7Cost: {cost} coins");
        String confirmLore3 = languageManager.getGuiMessage(player, "gui.create-guild.create-guild-confirm-lore-3", "&7Creator: {player_name}");

        // 替换变量
        confirmLore2 = confirmLore2.replace("{cost}", costText);
        confirmLore3 = confirmLore3.replace("{player_name}", player.getName());

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
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-cancel-name", "&cCancel")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-cancel-lore-1", "&7Cancel guild creation"))
        );
        inventory.setItem(41, cancel);
    }
    
    /**
     * 显示当前输入信息
     */
    private void displayCurrentInput(Inventory inventory) {
        // 当前公会名称
        String nameDisplay = guildName.isEmpty() ?
            languageManager.getGuiMessage(player, "gui.common.not-set", "Not set") : guildName;
        ItemStack currentName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-current-name", "&eCurrent Guild Name")),
            ColorUtils.colorize("&7" + nameDisplay)
        );
        inventory.setItem(11, currentName);
        
        // 当前公会标签
        String tagDisplay = guildTag.isEmpty() ?
            languageManager.getGuiMessage(player, "gui.common.not-set", "Not set") : "&7[" + guildTag + "&7]";
        ItemStack currentTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-current-tag", "&eCurrent Guild Tag")),
            ColorUtils.colorize("&7" + tagDisplay)
        );
        inventory.setItem(13, currentTag);
        
        // 当前公会描述
        String descriptionDisplay = guildDescription.isEmpty() ?
            languageManager.getGuiMessage(player, "gui.common.not-set", "Not set") : guildDescription;
        ItemStack currentDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.create-guild.create-guild-current-description", "&eCurrent Guild Description")),
            ColorUtils.colorize("&7" + descriptionDisplay)
        );
        inventory.setItem(15, currentDescription);
    }
    
    /**
     * 处理公会名称输入
     */
    private void handleNameInput(Player player) {
        String message = languageManager.getGuiMessage(player, "gui.common.input-name", "&aPlease enter guild name in chat (3-20 characters):");
        player.sendMessage(ColorUtils.colorize(message));

        // 强制关闭GUI以便玩家看到输入提示
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);

        // 延迟设置输入模式，确保GUI完全关闭
        CompatibleScheduler.runTaskLater(plugin, player, () -> {
            // 设置输入模式
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() < 3) {
                    String errorMessage = languageManager.getGuiMessage(player, "gui.create-guild.create.name-too-short", "&cGuild name is too short! Minimum {min} characters required.", "{min}", "3");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                if (input.length() > 20) {
                    String errorMessage = languageManager.getGuiMessage(player, "gui.create-guild.create.name-too-long", "&cGuild name is too long! Maximum {max} characters allowed.", "{max}", "20");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                guildName = input;
                String successMessage = languageManager.getGuiMessage(player, "gui.common.name-set", "&aGuild name set to: &e{name}", "{name}", guildName);
                player.sendMessage(ColorUtils.colorize(successMessage));

                // 重新打开GUI显示更新后的内容
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, player, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // 延迟2个tick (0.1秒)
    }
    
    /**
     * 处理公会标签输入
     */
    private void handleTagInput(Player player) {
        String message = languageManager.getGuiMessage(player, "gui.common.input-tag", "&aPlease enter guild tag in chat (max 6 characters, optional):");
        player.sendMessage(ColorUtils.colorize(message));

        // 强制关闭GUI以便玩家看到输入提示
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);

        // 延迟设置输入模式，确保GUI完全关闭
        CompatibleScheduler.runTaskLater(plugin, player, () -> {
            // 设置输入模式
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() > 6) {
                    String errorMessage = languageManager.getGuiMessage(player, "gui.create-guild.create.tag-too-long", "&cGuild tag is too long! Maximum {max} characters allowed.", "{max}", "6");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                guildTag = input;
                String successMessage = languageManager.getGuiMessage(player, "gui.common.tag-set", "&aGuild tag set to: &e{tag}", "{tag}", guildTag.isEmpty() ? "无" : guildTag);
                player.sendMessage(ColorUtils.colorize(successMessage));

                // 重新打开GUI显示更新后的内容
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, player, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // 延迟2个tick (0.1秒)
    }
    
    /**
     * 处理公会描述输入
     */
    private void handleDescriptionInput(Player player) {
        String message = languageManager.getGuiMessage(player, "gui.common.input-description", "&aPlease enter guild description in chat (max 100 characters, optional):");
        player.sendMessage(ColorUtils.colorize(message));

        // 强制关闭GUI以便玩家看到输入提示
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);

        // 延迟设置输入模式，确保GUI完全关闭
        CompatibleScheduler.runTaskLater(plugin, player, () -> {
            // 设置输入模式
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() > 100) {
                    String errorMessage = languageManager.getGuiMessage(player, "gui.create-guild.create.description-too-long", "&cGuild description cannot exceed 100 characters!");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }

                guildDescription = input;
                String successMessage = languageManager.getGuiMessage(player, "gui.common.description-set", "&aGuild description set to: &e{description}", "{description}", guildDescription.isEmpty() ? "无" : guildDescription);
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
            String message = languageManager.getGuiMessage(player, "gui.create-guild.create.name-required", "&cPlease enter a guild name first!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (guildName.length() < 3) {
            String message = languageManager.getGuiMessage(player, "gui.create-guild.create.name-too-short", "&cGuild name is too short! Minimum {min} characters required.", "{min}", "3");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (guildName.length() > 20) {
            String message = languageManager.getGuiMessage(player, "gui.create-guild.create.name-too-long", "&cGuild name is too long! Maximum {max} characters allowed.", "{max}", "20");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!guildTag.isEmpty() && guildTag.length() > 6) {
            String message = languageManager.getGuiMessage(player, "gui.create-guild.create.tag-too-long", "&cGuild tag is too long! Maximum {max} characters allowed.", "{max}", "6");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!guildDescription.isEmpty() && guildDescription.length() > 100) {
            String message = languageManager.getGuiMessage(player, "gui.create-guild.create.description-too-long", "&cGuild description cannot exceed 100 characters!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 检查经济系统
        boolean vaultAvailable = plugin.getEconomyManager().isVaultAvailable();
        boolean noEconomyMode = plugin.getEconomyManager().isNoEconomyMode();

        if (!vaultAvailable && !noEconomyMode) {
            String message = languageManager.getGuiMessage(player, "gui.create-guild.create.economy-not-available", "&cEconomy system is not available, cannot create guild!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 获取创建费用（无经济模式下费用为0）
        double creationCost = vaultAvailable
            ? plugin.getConfigManager().getMainConfig().getDouble("guild.creation-cost", 1000.0)
            : 0.0;

        // 仅在有经济系统时检查余额并扣费
        if (vaultAvailable) {
            if (!plugin.getEconomyManager().hasBalance(player, creationCost)) {
                String message = languageManager.getGuiMessage(player, "gui.create-guild.create.insufficient-funds", "&cInsufficient balance! Creating a guild requires {amount}!", "{amount}", plugin.getEconomyManager().format(creationCost));
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }

            if (!plugin.getEconomyManager().withdraw(player, creationCost)) {
                String message = languageManager.getGuiMessage(player, "gui.create-guild.create.payment-failed", "&cFailed to deduct creation fee!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
        }

        // 创建公会
        String finalTag = guildTag.isEmpty() ? null : guildTag;
        String finalDescription = guildDescription.isEmpty() ? null : guildDescription;
        final double finalCost = creationCost;

        plugin.getGuildService().createGuildAsync(guildName, finalTag, finalDescription, player.getUniqueId(), player.getName()).thenAccept(success -> {
            // 确保在玩家所在区域线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    String message = languageManager.getGuiMessage(player, "gui.create-guild.create.success", "&aGuild {name} created successfully!", "{name}", guildName);
                    player.sendMessage(ColorUtils.colorize(message));

                    // 关闭GUI并返回主界面
                    plugin.getGuiManager().closeGUI(player);
                    plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                } else {
                    // 如果创建失败且有扣费，退还费用
                    if (vaultAvailable && finalCost > 0) {
                        plugin.getEconomyManager().deposit(player, finalCost);
                        String refundMessage = languageManager.getGuiMessage(player, "gui.create-guild.create.payment-refunded", "&eCreation fee {amount} has been refunded.", "{amount}", plugin.getEconomyManager().format(finalCost));
                        player.sendMessage(ColorUtils.colorize(refundMessage));
                    }

                    String message = languageManager.getGuiMessage(player, "gui.create-guild.create.failed", "&cGuild creation failed! Possible reasons:");
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
