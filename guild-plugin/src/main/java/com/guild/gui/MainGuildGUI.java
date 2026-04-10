package com.guild.gui;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;

/**
 * 主工会GUI - 六个主要入口
 */
public class MainGuildGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;

    public MainGuildGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getMessage(player, "main-menu.title", "&6工会系统"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);

        // 工会信息按钮
        ItemStack guildInfo = createItem(
            Material.BOOK,
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-info.name", "&eGuild Info")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-info.lore.1", "&7View detailed guild information")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-info.lore.2", "&7Including basic info, statistics, etc."))
        );
        inventory.setItem(20, guildInfo);

        // 成员管理按钮
        ItemStack memberManagement = createItem(
            Material.PLAYER_HEAD,
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.member-management.name", "&eMember Management")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.member-management.lore.1", "&7Manage guild members")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.member-management.lore.2", "&7Invite, kick, permission management"))
        );
        inventory.setItem(22, memberManagement);

        // 申请管理按钮
        ItemStack applicationManagement = createItem(
            Material.PAPER,
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.application-management.name", "&eApplication Management")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.application-management.lore.1", "&7Handle join applications")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.application-management.lore.2", "&7View application history"))
        );
        inventory.setItem(24, applicationManagement);

        // 工会设置按钮
        ItemStack guildSettings = createItem(
            Material.COMPASS,
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-settings.name", "&eGuild Settings")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-settings.lore.1", "&7Modify guild settings")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-settings.lore.2", "&7Description, tag, permissions, etc."))
        );
        inventory.setItem(29, guildSettings);

        // 工会列表按钮
        ItemStack guildList = createItem(
            Material.BOOKSHELF,
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-list.name", "&eGuild List")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-list.lore.1", "&7View all guilds")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-list.lore.2", "&7Search, filter functions"))
        );
        inventory.setItem(31, guildList);

        // 工会关系按钮
        ItemStack guildRelations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-relations.name", "&eGuild Relations")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-relations.lore.1", "&7Manage guild relations")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.guild-relations.lore.2", "&7Allies, enemies, etc."))
        );
        inventory.setItem(33, guildRelations);

        // 创建工会按钮
        ItemStack createGuild = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.create-guild.name", "&aCreate Guild")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.create-guild.lore.1", "&7Create a new guild")),
            ColorUtils.colorize(languageManager.getMessage(player, "main-menu.create-guild.lore.2", "&7Requires coins"))
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
                    String message = languageManager.getMessage(player, "gui.no-guild", "&c您还没有工会");
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
                    String message = languageManager.getMessage(player, "gui.no-guild", "&c您还没有工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 打开成员管理GUI
                MemberManagementGUI memberManagementGUI = new MemberManagementGUI(plugin, guild, player);
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
                    String message = languageManager.getMessage(player, "gui.no-guild", "&c您还没有工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 检查权限
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // 确保在主线程中执行GUI操作
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || !member.getRole().canInvite()) {
                            String message = languageManager.getMessage(player, "gui.no-permission", "&c权限不足");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        // 打开申请管理GUI
                        ApplicationManagementGUI applicationManagementGUI = new ApplicationManagementGUI(plugin, guild, player);
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
                    String message = languageManager.getMessage(player, "gui.no-guild", "&c您还没有工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 检查权限
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // 确保在主线程中执行GUI操作
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || member.getRole() != com.guild.models.GuildMember.Role.LEADER) {
                            String message = languageManager.getMessage(player, "gui.leader-only", "&c只有工会会长才能执行此操作");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        // 打开工会设置GUI
                        GuildSettingsGUI guildSettingsGUI = new GuildSettingsGUI(plugin, guild, player);
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
        GuildListGUI guildListGUI = new GuildListGUI(plugin, player);
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
                    String message = languageManager.getMessage(player, "gui.no-guild", "&c您还没有工会");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 检查权限
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    // 确保在主线程中执行GUI操作
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (member == null || member.getRole() != com.guild.models.GuildMember.Role.LEADER) {
                            String message = languageManager.getMessage(player, "gui.manage-relations-leader-only", "&c只有工会会长才能管理关系");
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
                    String message = languageManager.getMessage(player, "create.already-in-guild", "&c您已经在一个工会中了！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // 打开创建工会GUI
                CreateGuildGUI createGuildGUI = new CreateGuildGUI(plugin, player);
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
