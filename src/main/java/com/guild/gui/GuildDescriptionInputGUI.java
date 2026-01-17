package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * 工会描述输入GUI
 */
public class GuildDescriptionInputGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private String currentDescription;
    
    public GuildDescriptionInputGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
        this.currentDescription = guild.getDescription() != null ? guild.getDescription() : "";
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6修改工会描述");
    }
    
    @Override
    public int getSize() {
        return 27;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 显示当前描述
        displayCurrentDescription(inventory);
        
        // 添加操作按钮
        setupButtons(inventory);
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
        ItemStack currentDesc = createItem(
            Material.BOOK,
            ColorUtils.colorize("&e当前描述"),
            ColorUtils.colorize("&7" + (currentDescription.isEmpty() ? "无描述" : currentDescription))
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
            ColorUtils.colorize("&a确认修改"),
            ColorUtils.colorize("&7确认修改工会描述")
        );
        inventory.setItem(15, confirm);
        
        // 取消按钮
        ItemStack cancel = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize("&c取消"),
            ColorUtils.colorize("&7取消修改")
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
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.input-description", "&a请在聊天框中输入新的工会描述（最多100字符）：");
        player.sendMessage(ColorUtils.colorize(message));
        
        // 设置玩家为输入模式
        plugin.getGuiManager().setInputMode(player, input -> {
            if (input.length() > 100) {
                String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.description-too-long", "&c描述过长，最多100字符！");
                player.sendMessage(ColorUtils.colorize(errorMessage));
                return false;
            }
            
            // 更新描述
            currentDescription = input;
            
            // 保存到数据库
            plugin.getGuildService().updateGuildDescriptionAsync(guild.getId(), input).thenAccept(success -> {
                if (success) {
                    String successMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.description-updated", "&a工会描述已更新！");
                    player.sendMessage(ColorUtils.colorize(successMessage));
                    
                    // 安全刷新GUI
                    plugin.getGuiManager().refreshGUI(player);
                } else {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.description-update-failed", "&c工会描述更新失败！");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                }
            });
            
            return true;
        });
    }
    
    /**
     * 处理确认
     */
    private void handleConfirm(Player player) {
        // 返回工会设置GUI
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
    }
    
    /**
     * 处理取消
     */
    private void handleCancel(Player player) {
        // 返回工会设置GUI
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
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
