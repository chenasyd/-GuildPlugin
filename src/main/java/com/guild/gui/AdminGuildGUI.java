package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.gui.SystemSettingsGUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 管理员工会GUI
 */
public class AdminGuildGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Player player;

    public AdminGuildGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public String getTitle() {
        return plugin.getLanguageManager().getGuiColoredMessage(player, "admin-gui.title", "&4工会管理");
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("admin-gui.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 工会列表管理
        ItemStack guildList = createItem(
            Material.BOOKSHELF,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.guild-list.name", "&e工会列表管理")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.guild-list.lore.1", "&7查看和管理所有工会")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.guild-list.lore.2", "&7包括删除、冻结等操作"))
        );
        inventory.setItem(20, guildList);
        
        // 经济管理
        ItemStack economy = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.economy.name", "&e经济管理")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.economy.lore.1", "&7管理工会经济系统")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.economy.lore.2", "&7设置资金、查看贡献等"))
        );
        inventory.setItem(22, economy);
        
        // 关系管理
        ItemStack relations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.relations.name", "&e关系管理")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.relations.lore.1", "&7管理工会关系")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.relations.lore.2", "&7盟友、敌对、开战等"))
        );
        inventory.setItem(24, relations);
        
        // 统计信息
        ItemStack statistics = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.statistics.name", "&e统计信息")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.statistics.lore.1", "&7查看工会统计信息")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.statistics.lore.2", "&7成员数量、经济状况等"))
        );
        inventory.setItem(29, statistics);
        
        // 系统设置
        ItemStack settings = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.settings.name", "&e系统设置")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.settings.lore.1", "&7管理系统设置")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.settings.lore.2", "&7重载配置、权限设置等"))
        );
        inventory.setItem(31, settings);
        
        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.back.name", "&c返回")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("admin-gui.items.back.lore.1", "&7返回主菜单"))
        );
        inventory.setItem(49, back);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // 工会列表管理
                openGuildListManagement(player);
                break;
            case 22: // 经济管理
                openEconomyManagement(player);
                break;
            case 24: // 关系管理
                openRelationManagement(player);
                break;
            case 29: // 统计信息
                openStatistics(player);
                break;
            case 31: // 系统设置
                openSystemSettings(player);
                break;
            case 49: // 返回
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                break;
        }
    }
    
    private void openGuildListManagement(Player player) {
        // 打开工会列表管理GUI
        GuildListManagementGUI guildListGUI = new GuildListManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, guildListGUI);
    }
    
    private void openEconomyManagement(Player player) {
        // 打开经济管理GUI
        EconomyManagementGUI economyGUI = new EconomyManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, economyGUI);
    }
    
    private void openRelationManagement(Player player) {
        // 打开关系管理GUI
        RelationManagementGUI relationGUI = new RelationManagementGUI(plugin, player);
        plugin.getGuiManager().openGUI(player, relationGUI);
    }
    
    private void openStatistics(Player player) {
        // 显示统计信息
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            player.sendMessage(ColorUtils.colorize("&6=== 工会统计信息 ==="));
            player.sendMessage(ColorUtils.colorize("&e总工会数量: &f" + guilds.size()));
            
            if (!guilds.isEmpty()) {
                final double[] totalBalance = {0};
                final int[] frozenCount = {0};
                
                for (Guild guild : guilds) {
                    totalBalance[0] += guild.getBalance();
                    if (guild.isFrozen()) {
                        frozenCount[0]++;
                    }
                }
                
                // 获取总成员数
                CompletableFuture<Integer>[] memberCountFutures = new CompletableFuture[guilds.size()];
                for (int i = 0; i < guilds.size(); i++) {
                    memberCountFutures[i] = plugin.getGuildService().getGuildMemberCountAsync(guilds.get(i).getId());
                }
                
                CompletableFuture.allOf(memberCountFutures).thenRun(() -> {
                    final int[] totalMembers = {0};
                    for (CompletableFuture<Integer> future : memberCountFutures) {
                        try {
                            totalMembers[0] += future.get();
                        } catch (Exception e) {
                            plugin.getLogger().severe("获取成员数量时发生错误: " + e.getMessage());
                        }
                    }
                    
                    player.sendMessage(ColorUtils.colorize("&e总成员数量: &f" + totalMembers[0]));
                    player.sendMessage(ColorUtils.colorize("&e总资金: &f" + totalBalance[0]));
                    player.sendMessage(ColorUtils.colorize("&e冻结工会数: &f" + frozenCount[0]));
                    player.sendMessage(ColorUtils.colorize("&e正常工会数: &f" + (guilds.size() - frozenCount[0])));
                });
            }
        });
    }
    
    private void openSystemSettings(Player player) {
        // 打开系统设置GUI
        plugin.getGuiManager().openGUI(player, new SystemSettingsGUI(plugin, player));
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
        // 刷新GUI
    }
}
