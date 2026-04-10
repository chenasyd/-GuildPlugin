package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * 工会权限设置GUI
 */
public class GuildPermissionsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;

    public GuildPermissionsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getMessage(player, "guild-permissions.title",
                "&6工会权限设置"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 显示权限信息
        displayPermissions(inventory);
        
        // 添加返回按钮
        setupButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            // 返回
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
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
     * 显示权限信息
     */
    private void displayPermissions(Inventory inventory) {
        // 会长权限
        ItemStack leaderPerms = createItem(
            Material.GOLDEN_HELMET,
            ColorUtils.colorize("&6会长权限"),
            ColorUtils.colorize("&7• 所有权限"),
            ColorUtils.colorize("&7• 管理成员"),
            ColorUtils.colorize("&7• 修改设置"),
            ColorUtils.colorize("&7• 删除工会")
        );
        inventory.setItem(10, leaderPerms);
        
        // 官员权限
        ItemStack officerPerms = createItem(
            Material.IRON_HELMET,
            ColorUtils.colorize("&e官员权限"),
            ColorUtils.colorize("&7• 邀请成员"),
            ColorUtils.colorize("&7• 踢出成员"),
            ColorUtils.colorize("&7• 处理申请"),
            ColorUtils.colorize("&7• 设置工会家")
        );
        inventory.setItem(12, officerPerms);
        
        // 成员权限
        ItemStack memberPerms = createItem(
            Material.LEATHER_HELMET,
            ColorUtils.colorize("&7成员权限"),
            ColorUtils.colorize("&7• 查看工会信息"),
            ColorUtils.colorize("&7• 传送到工会家"),
            ColorUtils.colorize("&7• 申请加入其他工会")
        );
        inventory.setItem(14, memberPerms);
        
        // 权限说明
        ItemStack info = createItem(
            Material.BOOK,
            ColorUtils.colorize("&e权限说明"),
            ColorUtils.colorize("&7权限系统基于角色分配"),
            ColorUtils.colorize("&7会长可以提升/降级成员"),
            ColorUtils.colorize("&7官员可以管理普通成员"),
            ColorUtils.colorize("&7成员拥有基础权限")
        );
        inventory.setItem(16, info);
        
        // 当前权限状态
        ItemStack currentStatus = createItem(
            Material.SHIELD,
            ColorUtils.colorize("&a当前权限状态"),
            ColorUtils.colorize("&7工会: &e" + guild.getName()),
            ColorUtils.colorize("&7权限系统: &a正常运行"),
            ColorUtils.colorize("&7权限检查: &a已启用")
        );
        inventory.setItem(22, currentStatus);
    }
    
    /**
     * 设置按钮
     */
    private void setupButtons(Inventory inventory) {
        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize("&7返回"),
            ColorUtils.colorize("&7返回工会设置")
        );
        inventory.setItem(49, back);
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
