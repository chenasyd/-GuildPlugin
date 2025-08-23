package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
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
 * 工会名称输入GUI
 */
public class GuildNameInputGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private String currentName;
    
    public GuildNameInputGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.currentName = guild.getName() != null ? guild.getName() : "";
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6修改工会名称");
    }
    
    @Override
    public int getSize() {
        return 27;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 显示当前名称
        displayCurrentName(inventory);
        
        // 添加操作按钮
        setupButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // 输入名称
                handleInputName(player);
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
     * 显示当前名称
     */
    private void displayCurrentName(Inventory inventory) {
        ItemStack currentNameItem = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize("&e当前工会名称"),
            ColorUtils.colorize("&7" + (currentName.isEmpty() ? "无名称" : currentName))
        );
        inventory.setItem(11, currentNameItem);
    }
    
    /**
     * 设置按钮
     */
    private void setupButtons(Inventory inventory) {
        // 确认按钮
        ItemStack confirmButton = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&a确认修改"),
            ColorUtils.colorize("&7点击确认修改工会名称"),
            ColorUtils.colorize("&7注意：工会名称修改后需要重新登录才能生效")
        );
        inventory.setItem(15, confirmButton);
        
        // 取消按钮
        ItemStack cancelButton = createItem(
            Material.REDSTONE,
            ColorUtils.colorize("&c取消"),
            ColorUtils.colorize("&7返回上一级菜单")
        );
        inventory.setItem(13, cancelButton);
    }
    
    /**
     * 处理输入名称
     */
    private void handleInputName(Player player) {
        // 关闭GUI并进入输入模式
        plugin.getGuiManager().closeGUI(player);
        plugin.getGuiManager().setInputMode(player, "guild_name_input", this);
        
        // 发送输入提示
        player.sendMessage(ColorUtils.colorize("&6请输入新的工会名称:"));
        player.sendMessage(ColorUtils.colorize("&7当前名称: &f" + currentName));
        player.sendMessage(ColorUtils.colorize("&7输入 &c取消 &7来取消操作"));
        player.sendMessage(ColorUtils.colorize("&7支持颜色字符，例如: &a&l绿色粗体 &7或 &c&o红色斜体"));
        player.sendMessage(ColorUtils.colorize("&7注意：工会名称不能与其他工会重复"));
    }
    
    /**
     * 处理确认
     */
    private void handleConfirm(Player player) {
        // 检查权限（只有会长可以修改工会名称）
        if (!plugin.getGuildService().isGuildLeader(player.getUniqueId(), guild.getId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 如果当前名称为空，提示输入
        if (currentName.isEmpty()) {
            handleInputName(player);
            return;
        }
        
        // 执行改名操作
        executeNameChange(player, currentName);
    }
    
    /**
     * 处理取消
     */
    public void handleCancel(Player player) {
        // 返回到工会设置GUI
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
    }
    
    /**
     * 处理输入完成
     */
    public void handleInputComplete(Player player, String input) {
        if (input == null || input.trim().isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&c工会名称不能为空！"));
            plugin.getGuiManager().openGUI(player, this);
            return;
        }
        
        String newName = input.trim();
        
        // 检查名称长度（基于清理后的名称，不包括颜色字符）
        String cleanName = newName.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
        if (cleanName.length() < 2) {
            player.sendMessage(ColorUtils.colorize("&c工会名称至少需要2个字符（不包括颜色字符）！"));
            plugin.getGuiManager().openGUI(player, this);
            return;
        }
        
        if (cleanName.length() > 16) {
            player.sendMessage(ColorUtils.colorize("&c工会名称不能超过16个字符（不包括颜色字符）！"));
            plugin.getGuiManager().openGUI(player, this);
            return;
        }
        
        // 检查是否与当前名称相同
        if (newName.equalsIgnoreCase(currentName)) {
            player.sendMessage(ColorUtils.colorize("&c新名称与当前名称相同！"));
            plugin.getGuiManager().openGUI(player, this);
            return;
        }
        
        // 检查名称格式（允许中文、英文、数字和颜色字符）
        if (!cleanName.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9]+$")) {
            player.sendMessage(ColorUtils.colorize("&c工会名称只能包含中文、英文和数字！"));
            plugin.getGuiManager().openGUI(player, this);
            return;
        }
        
        // 执行改名操作
        executeNameChange(player, newName);
    }
    
    /**
     * 执行改名操作
     */
    private void executeNameChange(Player player, String newName) {
        // 异步检查名称是否可用
        plugin.getGuildService().getGuildByNameAsync(newName).thenAccept(existingGuild -> {
            if (existingGuild != null) {
                // 名称已存在
                CompatibleScheduler.runTask(plugin, () -> {
                    player.sendMessage(ColorUtils.colorize("&c工会名称 &f" + newName + " &c已被使用！"));
                    plugin.getGuiManager().openGUI(player, this);
                });
                return;
            }
            
            // 名称可用，执行更新
            plugin.getGuildService().updateGuildAsync(guild.getId(), newName, guild.getTag(), guild.getDescription(), player.getUniqueId())
                .thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (success) {
                            // 更新成功
                            player.sendMessage(ColorUtils.colorize("&a工会名称修改成功！"));
                            player.sendMessage(ColorUtils.colorize("&7新名称: &f" + newName));
                            
                            // 记录日志
                            plugin.getGuildService().logGuildActionAsync(
                                guild.getId(),
                                guild.getName(),
                                player.getUniqueId().toString(),
                                player.getName(),
                                com.guild.models.GuildLog.LogType.GUILD_RENAMED,
                                "工会名称从 " + currentName + " 修改为 " + newName,
                                "原名称: " + currentName + ", 新名称: " + newName
                            );
                            
                            // 重新获取最新的工会信息
                            plugin.getGuildService().getGuildByIdAsync(guild.getId()).thenAccept(updatedGuild -> {
                                if (updatedGuild != null) {
                                    // 返回到工会设置GUI（使用最新的工会信息）
                                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, updatedGuild));
                                } else {
                                    // 如果获取失败，使用本地更新的对象
                                    guild.setName(newName);
                                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
                                }
                            });
                        } else {
                            // 更新失败
                            player.sendMessage(ColorUtils.colorize("&c工会名称修改失败！请重试"));
                            plugin.getGuiManager().openGUI(player, this);
                        }
                    });
                });
        });
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
    
    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        // 刷新GUI
        plugin.getGuiManager().openGUI(player, this);
    }
}
