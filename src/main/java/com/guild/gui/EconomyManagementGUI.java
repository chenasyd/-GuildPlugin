package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 经济管理GUI
 */
public class EconomyManagementGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private int currentPage = 0;
    private final int itemsPerPage = 28; // 7列 × 4行
    private List<Guild> allGuilds = new ArrayList<>();

    public EconomyManagementGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        loadGuilds();
    }

    @Override
    public String getTitle() {
        return languageManager.getGuiColoredMessage(player, "economy-management.title",
                ColorUtils.colorize("&e经济管理"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 设置工会列表
        setupGuildList(inventory);
        
        // 设置分页按钮
        setupPaginationButtons(inventory);
        
        // 设置操作按钮
        setupActionButtons(inventory);
    }
    
    private void setupGuildList(Inventory inventory) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allGuilds.size());
        
        for (int i = 0; i < itemsPerPage; i++) {
            if (startIndex + i < endIndex) {
                Guild guild = allGuilds.get(startIndex + i);
                
                // 计算在2-8列，2-5行的位置 (slots 10-43)
                int row = (i / 7) + 1; // 2-5行
                int col = (i % 7) + 1; // 2-8列
                int slot = row * 9 + col;
                
                inventory.setItem(slot, createGuildItem(guild));
            }
        }
    }
    
    private ItemStack createGuildItem(Guild guild) {
        Material material = Material.GOLD_INGOT;
        
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7工会: " + guild.getName()));
        lore.add(ColorUtils.colorize("&7会长: " + guild.getLeaderName()));
        lore.add(ColorUtils.colorize("&7等级: " + guild.getLevel()));
        lore.add(ColorUtils.colorize("&7当前资金: " + plugin.getEconomyManager().format(guild.getBalance())));
        lore.add(ColorUtils.colorize("&7最大成员: " + guild.getMaxMembers()));
        lore.add("");
        lore.add(ColorUtils.colorize("&e左键: 设置资金"));
        lore.add(ColorUtils.colorize("&a右键: 增加资金"));
        lore.add(ColorUtils.colorize("&c中键: 减少资金"));
        
        return createItem(material, ColorUtils.colorize("&6" + guild.getName()), lore.toArray(new String[0]));
    }
    
    private void setupPaginationButtons(Inventory inventory) {
        int totalPages = (int) Math.ceil((double) allGuilds.size() / itemsPerPage);
        
        // 上一页按钮
        if (currentPage > 0) {
            inventory.setItem(45, createItem(Material.ARROW, ColorUtils.colorize("&a上一页"), 
                ColorUtils.colorize("&7第 " + (currentPage) + " 页")));
        }
        
        // 页码信息
        inventory.setItem(49, createItem(Material.PAPER, ColorUtils.colorize("&e第 " + (currentPage + 1) + " 页，共 " + totalPages + " 页")));
        
        // 下一页按钮
        if (currentPage < totalPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, ColorUtils.colorize("&a下一页"), 
                ColorUtils.colorize("&7第 " + (currentPage + 2) + " 页")));
        }
    }
    
    private void setupActionButtons(Inventory inventory) {
        // 返回按钮
        inventory.setItem(46, createItem(Material.BARRIER, ColorUtils.colorize("&c返回")));
        
        // 刷新按钮
        inventory.setItem(52, createItem(Material.EMERALD, ColorUtils.colorize("&a刷新列表")));
    }
    
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        
        // 填充边框
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    private void loadGuilds() {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            this.allGuilds = guilds;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    refresh(player);
                }
            });
        });
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 46) {
            // 返回
            plugin.getGuiManager().openGUI(player, new AdminGuildGUI(plugin, player));
        } else if (slot == 52) {
            // 刷新
            loadGuilds();
        } else if (slot == 45 && currentPage > 0) {
            // 上一页
            currentPage--;
            refresh(player);
        } else if (slot == 53 && currentPage < (int) Math.ceil((double) allGuilds.size() / itemsPerPage) - 1) {
            // 下一页
            currentPage++;
            refresh(player);
        } else if (slot >= 10 && slot <= 43) {
            // 工会项目 - 检查是否在2-8列，2-5行范围内
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIndex = (row - 1) * 7 + (col - 1);
                int guildIndex = (currentPage * itemsPerPage) + relativeIndex;
                if (guildIndex < allGuilds.size()) {
                    Guild guild = allGuilds.get(guildIndex);
                    handleGuildClick(player, guild, clickType);
                }
            }
        }
    }
    
    private void handleGuildClick(Player player, Guild guild, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // 设置资金
            openSetBalanceGUI(player, guild);
        } else if (clickType == ClickType.RIGHT) {
            // 增加资金
            openAddBalanceGUI(player, guild);
        } else if (clickType == ClickType.MIDDLE) {
            // 减少资金
            openRemoveBalanceGUI(player, guild);
        }
    }
    
    private void openSetBalanceGUI(Player player, Guild guild) {
        // TODO: 实现设置资金GUI
        player.sendMessage(ColorUtils.colorize("&e设置资金功能开发中..."));
        player.sendMessage(ColorUtils.colorize("&e使用命令: &f/guildadmin economy set " + guild.getName() + " <金额>"));
    }
    
    private void openAddBalanceGUI(Player player, Guild guild) {
        // TODO: 实现增加资金GUI
        player.sendMessage(ColorUtils.colorize("&e增加资金功能开发中..."));
        player.sendMessage(ColorUtils.colorize("&e使用命令: &f/guildadmin economy add " + guild.getName() + " <金额>"));
    }
    
    private void openRemoveBalanceGUI(Player player, Guild guild) {
        // TODO: 实现减少资金GUI
        player.sendMessage(ColorUtils.colorize("&e减少资金功能开发中..."));
        player.sendMessage(ColorUtils.colorize("&e使用命令: &f/guildadmin economy remove " + guild.getName() + " <金额>"));
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            
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
        if (player.isOnline()) {
            plugin.getGuiManager().refreshGUI(player);
        }
    }
}
