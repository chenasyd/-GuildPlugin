package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;

import org.geysermc.cumulus.form.CustomForm;
import com.guild.core.language.LanguageManager;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * 公会描述输入GUI
 */
public class GuildDescriptionInputGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_CURRENT_DESC = "CURRENT_DESC";
    public static final String FUNC_CONFIRM = "CONFIRM";
    public static final String FUNC_CANCEL = "CANCEL";

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;
    private String currentDescription;

    public GuildDescriptionInputGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.currentDescription = guild.getDescription() != null ? guild.getDescription() : "";
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-description-input.title",
                "&6Modify Guild Description"));
    }
    
    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        int maxLength = plugin.getConfigManager().getMainConfig()
                .getInt("guild.max-description-length", 100);

        CustomForm form = CustomForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.guild-description-input.bedrock-title", "&6Modify Guild Description"))
                .input(languageManager.getGuiColoredMessage(player, "gui.guild-description-input.bedrock-input-label", "&fEnter new guild description"),
                        languageManager.getGuiColoredMessage(player, "gui.guild-description-input.bedrock-input-placeholder", "Max {max} characters", "{max}", String.valueOf(maxLength)),
                        currentDescription)
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    String input = response.getInput(0);
                    if (input == null) input = "";
                    if (input.length() > maxLength) {
                        player.sendMessage(ColorUtils.colorize(
                                languageManager.getGuiMessage(player,
                                        "gui.common.description-too-long",
                                        "&cDescription too long, max {max} characters!",
                                        "{max}", String.valueOf(maxLength))));
                        openBedrockForm(player);
                        return;
                    }
                    currentDescription = input;
                    plugin.getGuildService().updateGuildDescriptionAsync(guild.getId(), input)
                            .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                                if (success) {
                                    player.sendMessage(ColorUtils.colorize(
                                            languageManager.getGuiMessage(player,
                                                    "gui.common.description-updated",
                                                    "&aGuild description updated!")));
                                    plugin.getGuiManager().openGUI(player,
                                            new GuildSettingsGUI(plugin, guild, player));
                                } else {
                                    player.sendMessage(ColorUtils.colorize(
                                            languageManager.getGuiMessage(player,
                                                    "gui.common.description-update-failed",
                                                    "&cFailed to update guild description!")));
                                }
                            }));
                }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(plugin, player, () ->
                        plugin.getGuiManager().openGUI(player,
                                new GuildSettingsGUI(plugin, guild, player))))
                .build();

        return BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 显示当前描述
        displayCurrentDescription(inventory);
        
        // 添加操作按钮
        setupButtons(inventory);

        // 应用图像模式
        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // 输入描述
                handleInputDescription(player);
                break;
            case 15: // 确认
                handleConfirm(player);
                break;
            case 13: // 取消
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
            inventory.setItem(i + 18, border);
        }
        for (int i = 9; i < 18; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * 显示当前描述
     */
    private void displayCurrentDescription(Inventory inventory) {
        String descText = currentDescription.isEmpty() ?
            languageManager.getGuiMessage(player, "gui.common.no-description", "No description") : currentDescription;
        ItemStack currentDesc = createItem(
            Material.BOOK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-description-input.current-description", "&eCurrent Description")),
            ColorUtils.colorize("&7" + descText)
        );
        inventory.setItem(11, currentDesc);
    }
    
    /**
     * 设置按钮
     */
    private void setupButtons(Inventory inventory) {
        // 确认按钮
        ItemStack confirm = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-description-input.confirm-button", "&aConfirm Edit")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-description-input.confirm-lore", "&7Confirm guild description edit"))
        );
        inventory.setItem(15, confirm);
        
        // 取消按钮
        ItemStack cancel = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-description-input.cancel-button", "&cCancel")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-description-input.cancel-lore", "&7Cancel edit"))
        );
        inventory.setItem(13, cancel);
    }
    
    /**
     * 处理输入描述
     */
    private void handleInputDescription(Player player) {
        // 关闭GUI
        player.closeInventory();

        // 发送消息提示输入
        int maxLength = plugin.getConfigManager().getMainConfig().getInt("guild.max-description-length", 100);
        String message = languageManager.getGuiMessage(player, "gui.common.input-description", "&aPlease enter guild description in chat (max 100 characters, optional):", "{max}", String.valueOf(maxLength));
        player.sendMessage(ColorUtils.colorize(message));

        // 设置玩家为输入模式
        final int finalMaxLength = maxLength; // 使用final变量避免lambda中的变量冲突
        plugin.getGuiManager().setInputMode(player, input -> {
            if (input.length() > finalMaxLength) {
                String errorMessage = languageManager.getGuiMessage(player, "gui.common.description-too-long", "&cDescription too long, max {max} characters!", "{max}", String.valueOf(finalMaxLength));
                player.sendMessage(ColorUtils.colorize(errorMessage));
                return false;
            }

            // 更新描述
            currentDescription = input;

            // 保存到数据库
            plugin.getGuildService().updateGuildDescriptionAsync(guild.getId(), input).thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String successMessage = languageManager.getGuiMessage(player, "gui.common.description-updated", "&aGuild description updated!");
                        player.sendMessage(ColorUtils.colorize(successMessage));

                        // 安全刷新GUI
                        plugin.getGuiManager().refreshGUI(player);
                    } else {
                        String errorMessage = languageManager.getGuiMessage(player, "gui.common.description-update-failed", "&cFailed to update guild description!");
                        player.sendMessage(ColorUtils.colorize(errorMessage));
                    }
                });
            });

            return true;
        });
    }
    
    /**
     * 处理确认
     */
    private void handleConfirm(Player player) {
        // 返回公会设置GUI
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
    }

    /**
     * 处理取消
     */
    private void handleCancel(Player player) {
        // 返回公会设置GUI
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
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
