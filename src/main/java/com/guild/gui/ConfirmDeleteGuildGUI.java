package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * 确认删除工会GUI
 */
public class ConfirmDeleteGuildGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    
    public ConfirmDeleteGuildGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&4确认删除工会");
    }
    
    @Override
    public int getSize() {
        return 27;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 显示确认信息
        displayConfirmInfo(inventory);
        
        // 添加确认和取消按钮
        setupButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // 确认删除
                handleConfirmDelete(player);
                break;
            case 15: // 取消
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
     * 显示确认信息
     */
    private void displayConfirmInfo(Inventory inventory) {
        ItemStack info = createItem(
            Material.BOOK,
            ColorUtils.colorize("&4确认删除工会"),
            ColorUtils.colorize("&7工会: &e" + guild.getName()),
            ColorUtils.colorize("&7你确定要删除这个工会吗？"),
            ColorUtils.colorize("&c此操作将永久删除工会！"),
            ColorUtils.colorize("&c所有成员将被移除！"),
            ColorUtils.colorize("&c此操作不可撤销！")
        );
        inventory.setItem(13, info);
    }
    
    /**
     * 设置按钮
     */
    private void setupButtons(Inventory inventory) {
        // 确认删除按钮
        ItemStack confirm = createItem(
            Material.TNT,
            ColorUtils.colorize("&4确认删除"),
            ColorUtils.colorize("&7点击确认删除工会"),
            ColorUtils.colorize("&c此操作不可撤销！")
        );
        inventory.setItem(11, confirm);
        
        // 取消按钮
        ItemStack cancel = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize("&a取消"),
            ColorUtils.colorize("&7取消删除工会")
        );
        inventory.setItem(15, cancel);
    }
    
    /**
     * 处理确认删除
     */
    private void handleConfirmDelete(Player player) {
        // 检查权限（只有当前工会会长可以删除）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getGuildId() != guild.getId() || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 删除工会
        plugin.getGuildService().deleteGuildAsync(guild.getId(), player.getUniqueId()).thenAccept(success -> {
            if (success) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("delete.success", "&a工会 &e{guild} &a已被删除！")
                    .replace("{guild}", guild.getName());
                // 回到主线程进行界面操作
                CompatibleScheduler.runTask(plugin, () -> {
                    player.sendMessage(ColorUtils.colorize(message));
                    // 使用GUIManager以确保主线程安全关闭与打开
                    plugin.getGuiManager().closeGUI(player);
                    plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                });
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("delete.failed", "&c删除工会失败！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
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
