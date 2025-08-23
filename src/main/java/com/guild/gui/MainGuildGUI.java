package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.gui.GUIManager;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.guild.core.utils.CompatibleScheduler;

/**
 * 主工会GUI - 六个主要入口
 */
public class MainGuildGUI implements GUI {
    
    private final GuildPlugin plugin;
    
    public MainGuildGUI(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.title", "&6工会系统"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("main-menu.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 工会信息按钮
        ItemStack guildInfo = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-info.name", "&e工会信息")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-info.lore.1", "&7查看工会详细信息")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-info.lore.2", "&7包括基本信息、统计等"))
        );
        inventory.setItem(20, guildInfo);
        
        // 成员管理按钮
        ItemStack memberManagement = createItem(
            Material.PLAYER_HEAD,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.member-management.name", "&e成员管理")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.member-management.lore.1", "&7管理工会成员")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.member-management.lore.2", "&7邀请、踢出、权限管理"))
        );
        inventory.setItem(22, memberManagement);
        
        // 申请管理按钮
        ItemStack applicationManagement = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.application-management.name", "&e申请管理")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.application-management.lore.1", "&7处理加入申请")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.application-management.lore.2", "&7查看申请历史"))
        );
        inventory.setItem(24, applicationManagement);
        
        // 工会设置按钮
        ItemStack guildSettings = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-settings.name", "&e工会设置")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-settings.lore.1", "&7修改工会设置")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-settings.lore.2", "&7描述、标签、权限等"))
        );
        inventory.setItem(29, guildSettings);
        
        // 工会列表按钮
        ItemStack guildList = createItem(
            Material.BOOKSHELF,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-list.name", "&e工会列表")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-list.lore.1", "&7查看所有工会")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-list.lore.2", "&7搜索、筛选功能"))
        );
        inventory.setItem(31, guildList);
        
        // 工会关系按钮
        ItemStack guildRelations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-relations.name", "&e工会关系")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-relations.lore.1", "&7管理工会关系")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.guild-relations.lore.2", "&7盟友、敌对、开战等"))
        );
        inventory.setItem(33, guildRelations);
        
        // 创建工会按钮
        ItemStack createGuild = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.create-guild.name", "&a创建工会")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.create-guild.lore.1", "&7创建新的工会")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("main-menu.items.create-guild.lore.2", "&7需要消耗金币"))
        );
        inventory.setItem(4, createGuild);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // 工会信息
                openGuildInfoGUI(player);
                break;
            case 22: // 成员管理
                openMemberManagementGUI(player);
                break;
            case 24: // 申请管理
                openApplicationManagementGUI(player);
                break;
            case 29: // 工会设置
                openGuildSettingsGUI(player);
                break;
            case 31: // 工会列表
                openGuildListGUI(player);
                break;
            case 33: // 工会关系
                openGuildRelationsGUI(player);
                break;
            case 4: // 创建工会
                openCreateGuildGUI(player);
                break;
        }
    }
    
    /**
     * 打开工会信息GUI
     */
    private void openGuildInfoGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&c您还没有工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 打开工会信息GUI
                GuildInfoGUI guildInfoGUI = new GuildInfoGUI(plugin, player, guild);
                plugin.getGuiManager().openGUI(player, guildInfoGUI);
            });
        });
    }
    
    /**
     * 打开成员管理GUI
     */
    private void openMemberManagementGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&c您还没有工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 打开成员管理GUI
                MemberManagementGUI memberManagementGUI = new MemberManagementGUI(plugin, guild);
                plugin.getGuiManager().openGUI(player, memberManagementGUI);
            });
        });
    }
    
    /**
     * 打开申请管理GUI
     */
    private void openApplicationManagementGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&c您还没有工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查权限
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // 确保在主线程中执行GUI操作
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || !member.getRole().canInvite()) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&c权限不足");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        
                        // 打开申请管理GUI
                        ApplicationManagementGUI applicationManagementGUI = new ApplicationManagementGUI(plugin, guild);
                        plugin.getGuiManager().openGUI(player, applicationManagementGUI);
                    });
                });
            });
        });
    }
    
    /**
     * 打开工会设置GUI
     */
    private void openGuildSettingsGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&c您还没有工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查权限
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // 确保在主线程中执行GUI操作
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || member.getRole() != com.guild.models.GuildMember.Role.LEADER) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        
                        // 打开工会设置GUI
                        GuildSettingsGUI guildSettingsGUI = new GuildSettingsGUI(plugin, guild);
                        plugin.getGuiManager().openGUI(player, guildSettingsGUI);
                    });
                });
            });
        });
    }
    
    /**
     * 打开工会列表GUI
     */
    private void openGuildListGUI(Player player) {
        // 打开工会列表GUI
        GuildListGUI guildListGUI = new GuildListGUI(plugin);
        plugin.getGuiManager().openGUI(player, guildListGUI);
    }
    
    /**
     * 打开工会关系GUI
     */
    private void openGuildRelationsGUI(Player player) {
        // 检查玩家是否有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guild", "&c您还没有工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查权限
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // 确保在主线程中执行GUI操作
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || member.getRole() != com.guild.models.GuildMember.Role.LEADER) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能管理关系");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        
                        // 打开工会关系GUI
                        GuildRelationsGUI guildRelationsGUI = new GuildRelationsGUI(plugin, guild, player);
                        plugin.getGuiManager().openGUI(player, guildRelationsGUI);
                    });
                });
            });
        });
    }
    
    /**
     * 打开创建工会GUI
     */
    private void openCreateGuildGUI(Player player) {
        // 检查玩家是否已有工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            // 确保在主线程中执行GUI操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild != null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("create.already-in-guild", "&c您已经在一个工会中了！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 打开创建工会GUI
                CreateGuildGUI createGuildGUI = new CreateGuildGUI(plugin);
                plugin.getGuiManager().openGUI(player, createGuildGUI);
            });
        });
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
