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
 * 公会标签输入GUI
 */
public class GuildTagInputGUI implements GUI {

    // ── 图像模式功能常量 ──
    public static final String FUNC_CURRENT_TAG = "CURRENT_TAG";
    public static final String FUNC_CONFIRM = "CONFIRM";
    public static final String FUNC_CANCEL = "CANCEL";

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;
    private String currentTag;

    public GuildTagInputGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.currentTag = guild.getTag() != null ? guild.getTag() : "";
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-tag-input.title",
                "&6Modify Guild Tag"));
    }
    
    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;

        int maxLength = plugin.getConfigManager().getMainConfig()
                .getInt("guild.max-tag-length", 6);

        CustomForm form = CustomForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.guild-tag-input.bedrock-title", "&6Modify Guild Tag"))
                .input(languageManager.getGuiColoredMessage(player, "gui.guild-tag-input.bedrock-input-label", "&fEnter new guild tag"),
                        languageManager.getGuiColoredMessage(player, "gui.guild-tag-input.bedrock-input-placeholder", "Max {max} characters", "{max}", String.valueOf(maxLength)),
                        currentTag)
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    String input = response.getInput(0);
                    if (input == null) input = "";
                    if (input.length() > maxLength) {
                        player.sendMessage(ColorUtils.colorize(
                                languageManager.getGuiMessage(player,
                                        "gui.common.tag-too-long",
                                        "&cTag too long, max {max} characters!",
                                        "{max}", String.valueOf(maxLength))));
                        openBedrockForm(player);
                        return;
                    }
                    currentTag = input;
                    plugin.getGuildService().updateGuildAsync(
                            guild.getId(), guild.getName(), input,
                            guild.getDescription(), player.getUniqueId())
                            .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                                if (success) {
                                    player.sendMessage(ColorUtils.colorize(
                                            languageManager.getGuiMessage(player,
                                                    "gui.common.tag-updated",
                                                    "&aGuild tag updated!")));
                                    plugin.getGuiManager().openGUI(player,
                                            new GuildSettingsGUI(plugin, guild, player));
                                } else {
                                    player.sendMessage(ColorUtils.colorize(
                                            languageManager.getGuiMessage(player,
                                                    "gui.common.tag-update-failed",
                                                    "&cFailed to update guild tag!")));
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
        
        // 显示当前标签
        displayCurrentTag(inventory);
        
        // 添加操作按钮
        setupButtons(inventory);

        // 应用图像模式
        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // 输入标签
                handleInputTag(player);
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
     * 显示当前标签
     */
    private void displayCurrentTag(Inventory inventory) {
        String tagText = currentTag.isEmpty() ?
            languageManager.getGuiMessage(player, "gui.common.no-tag", "No tag") : "[" + currentTag + "]";
        ItemStack currentTagItem = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-tag-input.current-tag", "&eCurrent Tag")),
            ColorUtils.colorize("&7" + tagText)
        );
        inventory.setItem(11, currentTagItem);
    }
    
    /**
     * 设置按钮
     */
    private void setupButtons(Inventory inventory) {
        // 确认按钮
        ItemStack confirm = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-tag-input.confirm-button", "&aConfirm Edit")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-tag-input.confirm-lore", "&7Confirm guild tag edit"))
        );
        inventory.setItem(15, confirm);
        
        // 取消按钮
        ItemStack cancel = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-tag-input.cancel-button", "&cCancel")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-tag-input.cancel-lore", "&7Cancel edit"))
        );
        inventory.setItem(13, cancel);
    }
    
    /**
     * 处理输入标签
     */
    private void handleInputTag(Player player) {
        // 关闭GUI
        player.closeInventory();

        // 发送消息提示输入
        int maxLength = plugin.getConfigManager().getMainConfig().getInt("guild.max-tag-length", 6);
        String message = languageManager.getGuiMessage(player, "gui.common.input-tag", "&aPlease enter guild tag in chat (max 6 characters, optional):", "{max}", String.valueOf(maxLength));
        player.sendMessage(ColorUtils.colorize(message));

        // 设置玩家为输入模式
        final int finalMaxLength = maxLength; // 使用final变量避免lambda中的变量冲突
        plugin.getGuiManager().setInputMode(player, input -> {
            if (input.length() > finalMaxLength) {
                String errorMessage = languageManager.getGuiMessage(player, "gui.common.tag-too-long", "&cTag too long, max {max} characters!", "{max}", String.valueOf(finalMaxLength));
                player.sendMessage(ColorUtils.colorize(errorMessage));
                return false;
            }

            // 更新标签
            currentTag = input;

            // 保存到数据库
            plugin.getGuildService().updateGuildAsync(guild.getId(), guild.getName(), input, guild.getDescription(), player.getUniqueId()).thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (success) {
                        String successMessage = languageManager.getGuiMessage(player, "gui.common.tag-updated", "&aGuild tag updated!");
                        player.sendMessage(ColorUtils.colorize(successMessage));

                        // 安全刷新GUI
                        plugin.getGuiManager().refreshGUI(player);
                    } else {
                        String errorMessage = languageManager.getGuiMessage(player, "gui.common.tag-update-failed", "&cFailed to update guild tag!");
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
