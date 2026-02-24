package com.guild.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;

/**
 * 工会信息GUI
 */
public class GuildInfoGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Player player;
    private final Guild guild;
    private Inventory inventory;
    
    public GuildInfoGUI(GuildPlugin plugin, Player player, Guild guild) {
        this.plugin = plugin;
        this.player = player;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getLanguageManager().getMessage(player, "guild-info.title", "&6工会信息"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;

        // 保证外框可见
        fillBorder(inventory);

        // 直接使用默认物品设置
        setupDefaultItems();
    }

    private void setupDefaultItems() {
        // 合并展示：名称/标签/描述/创建时间/会长 一格显示
        String createdTime = guild.getCreatedAt() != null
            ? guild.getCreatedAt().format(com.guild.core.time.TimeProvider.FULL_FORMATTER)
            : "未知";

        List<String> summaryLore = new ArrayList<>();
        String lang = plugin.getLanguageManager().getPlayerLanguage(player);
        summaryLore.add(ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.tag", "标签") + ": " + (guild.getTag() != null ? "[" + guild.getTag() + "]" : plugin.getLanguageManager().getMessage(player, "guild-info.no-tag", "无"))));
        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            summaryLore.add(ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.description", "描述") + ": " + guild.getDescription()));
        }
        summaryLore.add(ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.leader", "会长") + ": &e" + guild.getLeaderName()));
        summaryLore.add(ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.created-time", "创建时间") + ": " + createdTime));
        ItemStack summaryItem = createItem(Material.PAPER, ColorUtils.colorize("&6" + guild.getName()), summaryLore.toArray(new String[0]));
        inventory.setItem(10, summaryItem);

        // 统计（等级 + 成员占位 + 进度）
        ItemStack statsItem = createItem(
            Material.EXPERIENCE_BOTTLE,
            ColorUtils.colorize("&e" + plugin.getLanguageManager().getMessage(player, "guild-info.stats-title", "工会统计")),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.level", "等级") + ": &e" + guild.getLevel()),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.members", "成员") + ": &e" + plugin.getLanguageManager().getMessage(player, "guild-info.loading", "加载中...")),
            getProgressBar(guild.getLevel(), guild.getBalance(), 8)
        );
        inventory.setItem(19, statsItem);

        // 经济（余额 + 升级需求 + 可视化进度）
        ItemStack economyItem = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize("&6" + plugin.getLanguageManager().getMessage(player, "guild-info.economy-title", "经济信息")),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.balance", "资金") + ": &a" + plugin.getEconomyManager().format(guild.getBalance())),
            ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.next-level-requirement", "下级所需") + ": " + getNextLevelRequirement(guild.getLevel())),
            getProgressBar(guild.getLevel(), guild.getBalance(), 8)
        );
        inventory.setItem(28, economyItem);

        // 状态（单格）
        String status = guild.isFrozen()
            ? "§c" + plugin.getLanguageManager().getMessage(player, "guild-info.status-frozen", "已冻结")
            : "§a" + plugin.getLanguageManager().getMessage(player, "guild-info.status-normal", "正常");
        ItemStack statusItem = createItem(Material.BEACON, "§6" + plugin.getLanguageManager().getMessage(player, "guild-info.status", "工会状态"), status);
        inventory.setItem(36, statusItem);

        // 返回按钮（保留原位）
        ItemStack backItem = createItem(
            Material.ARROW,
            "§c" + plugin.getLanguageManager().getMessage(player, "guild-info.back", "返回"),
            "§e" + plugin.getLanguageManager().getMessage(player, "guild-info.back-hint", "点击返回主菜单")
        );
        inventory.setItem(49, backItem);

        // 填充内部空槽以饱和界面（不覆盖已有物品）
        fillInteriorSlots(inventory);

        // 异步刷新动态信息（成员数、经济显示）
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (inventory == null) return;

                // 更新统计（成员数与进度）
                ItemStack updatedStats = createItem(
                    Material.EXPERIENCE_BOTTLE,
                    ColorUtils.colorize("&e" + plugin.getLanguageManager().getMessage(player, "guild-info.stats-title", "工会统计")),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.level", "等级") + ": &e" + guild.getLevel()),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.members", "成员") + ": &e" + memberCount + "/" + guild.getMaxMembers() + " " + plugin.getLanguageManager().getMessage(player, "guild-info.people", "人")),
                    getProgressBar(guild.getLevel(), guild.getBalance(), 8)
                );
                inventory.setItem(19, updatedStats);

                // 更新经济信息（以防余额变化）
                ItemStack updatedEconomy = createItem(
                    Material.GOLD_INGOT,
                    ColorUtils.colorize("&6" + plugin.getLanguageManager().getMessage(player, "guild-info.economy-title", "经济信息")),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.balance", "资金") + ": &a" + plugin.getEconomyManager().format(guild.getBalance())),
                    ColorUtils.colorize("&7" + plugin.getLanguageManager().getMessage(player, "guild-info.next-level-requirement", "下级所需") + ": " + getNextLevelRequirement(guild.getLevel())),
                    getProgressBar(guild.getLevel(), guild.getBalance(), 8)
                );
                inventory.setItem(28, updatedEconomy);
            });
        });
    }

    // 在内部空槽放灰色玻璃（不覆盖已有物品）
    private void fillInteriorSlots(Inventory inventory) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 9; slot <= 44; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue; // 跳过左右边框
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ColorUtils.colorize(line));
            }
            meta.setLore(loreList);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private String replacePlaceholders(String text) {
        return PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
    }

    private String replacePlaceholdersAsync(String text, int memberCount) {
        // 先使用PlaceholderUtils处理基础变量
        String result = PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);
        
        // 然后处理动态变量
        return result
            .replace("{member_count}", String.valueOf(memberCount))
            .replace("{online_member_count}", String.valueOf(memberCount)); // 暂时使用总成员数
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            // 返回主菜单
            plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
        }
    }
    
    @Override
    public void onClose(Player player) {
        // 关闭时的处理
    }
    
    @Override
    public void refresh(Player player) {
        setupInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * 获取下一级升级所需资金
     */
    private String getNextLevelRequirement(int currentLevel) {
        if (currentLevel >= 10) {
            return plugin.getLanguageManager().getMessage(player, "guild-info.max-level-reached", "已达到最高等级");
        }
        
        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }
        
        return plugin.getEconomyManager().format(required);
    }

    /**
     * 获取当前等级进度
     */
    private String getLevelProgress(int currentLevel, double currentBalance) {
        if (currentLevel >= 10) {
            return "100%";
        }

        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }

        if (required <= 0) return "0.0%";
        double percentage = (currentBalance / required) * 100;
        if (percentage > 100) percentage = 100;
        return String.format("%.1f%%", percentage);
    }

    /**
     * 获取可视化进度条
     */
    private String getProgressBar(int currentLevel, double currentBalance, int length) {
        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
            default: required = 1; break;
        }
        if (required <= 0) required = 1;
        double percent = Math.min(100.0, (currentBalance / required) * 100.0);
        int filled = (int) Math.round((percent / 100.0) * length);
        StringBuilder sb = new StringBuilder();
        sb.append(ColorUtils.colorize("&7["));
        for (int i = 0; i < length; i++) {
            if (i < filled) sb.append("§a■");
            else sb.append("§7■");
        }
        sb.append(ColorUtils.colorize("&7] "));
        sb.append(String.format("%.1f%%", percent));
        return sb.toString();
    }

    // 在类中添加缺失的边框绘制方法，行为与其它 GUI 保持一致
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
}
