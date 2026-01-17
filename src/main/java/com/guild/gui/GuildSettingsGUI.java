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
import java.util.concurrent.CompletableFuture;

/**
 * 工会设置GUI
 */
public class GuildSettingsGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    
    public GuildSettingsGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.title", "&6工会设置 - {guild_name}")
            .replace("{guild_name}", guild.getName() != null ? guild.getName() : "未知工会"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-settings.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 添加设置按钮
        setupSettingsButtons(inventory);
        
        // 显示当前设置信息
        displayCurrentSettings(inventory);
        
        // 添加功能按钮
        setupFunctionButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 10: // 修改名称
                handleChangeName(player);
                break;
            case 11: // 修改描述
                handleChangeDescription(player);
                break;
            case 12: // 修改标签
                handleChangeTag(player);
                break;
            case 14: // 设置工会家
                handleSetHome(player);
                break;
            case 16: // 权限设置
                handlePermissions(player);
                break;
            case 20: // 邀请成员
                handleInviteMember(player);
                break;
            case 22: // 踢出成员
                handleKickMember(player);
                break;
            case 24: // 提升成员
                handlePromoteMember(player);
                break;
            case 26: // 降级成员
                handleDemoteMember(player);
                break;
            case 30: // 处理申请
                handleApplications(player);
                break;
            case 31: // 工会关系管理
                handleRelations(player);
                break;
            case 32: // 工会日志
                handleGuildLogs(player);
                break;
            case 33: // 工会家传送
                handleHomeTeleport(player);
                break;
            case 34: // 离开工会
                handleLeaveGuild(player);
                break;
            case 36: // 删除工会
                handleDeleteGuild(player);
                break;
            case 49: // 返回
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
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
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * 设置设置按钮
     */
    private void setupSettingsButtons(Inventory inventory) {
        // 修改名称按钮
        ItemStack changeName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-name.name", "&e修改名称")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-name.lore.1", "&7修改工会名称"))
        );
        inventory.setItem(10, changeName);
        
        // 修改描述按钮
        ItemStack changeDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-description.name", "&e修改描述")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-description.lore.1", "&7修改工会描述"))
        );
        inventory.setItem(11, changeDescription);
        
        // 修改标签按钮
        ItemStack changeTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-tag.name", "&e修改标签")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.change-tag.lore.1", "&7修改工会标签"))
        );
        inventory.setItem(12, changeTag);
        
        // 设置工会家按钮
        ItemStack setHome = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.set-home.name", "&e设置工会家")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.set-home.lore.1", "&7设置工会传送点"))
        );
        inventory.setItem(14, setHome);
        
        // 权限设置按钮
        ItemStack permissions = createItem(
            Material.SHIELD,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.permissions.name", "&e权限设置")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.permissions.lore.1", "&7管理成员权限"))
        );
        inventory.setItem(16, permissions);
    }
    
    /**
     * 设置功能按钮
     */
    private void setupFunctionButtons(Inventory inventory) {
        // 邀请成员按钮
        ItemStack inviteMember = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.invite-member", "&a邀请成员")),
            ColorUtils.colorize("&7邀请新成员加入工会")
        );
        inventory.setItem(20, inviteMember);
        
        // 踢出成员按钮
        ItemStack kickMember = createItem(
            Material.REDSTONE,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.kick-member", "&c踢出成员")),
            ColorUtils.colorize("&7踢出工会成员")
        );
        inventory.setItem(22, kickMember);
        
        // 提升成员按钮
        ItemStack promoteMember = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.promote-member", "&6提升成员")),
            ColorUtils.colorize("&7提升成员职位")
        );
        inventory.setItem(24, promoteMember);
        
        // 降级成员按钮
        ItemStack demoteMember = createItem(
            Material.IRON_INGOT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.demote-member", "&7降级成员")),
            ColorUtils.colorize("&7降级成员职位")
        );
        inventory.setItem(26, demoteMember);
        
        // 处理申请按钮
        ItemStack applications = createItem(
            Material.PAPER,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.application-management", "&e申请管理")),
            ColorUtils.colorize("&7处理加入申请")
        );
        inventory.setItem(30, applications);
        
        // 工会关系管理按钮
        ItemStack relations = createItem(
            Material.RED_WOOL,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.name", "&e工会关系管理")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.lore.1", "&7管理工会关系")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations-management.lore.2", "&7盟友、敌对等"))
        );
        inventory.setItem(31, relations);
        
        // 工会日志按钮
        ItemStack guildLogs = createItem(
            Material.BOOK,
            ColorUtils.colorize("&6工会日志"),
            ColorUtils.colorize("&7查看工会操作历史"),
            ColorUtils.colorize("&7记录所有重要操作")
        );
        inventory.setItem(32, guildLogs);
        
        // 工会家传送按钮
        ItemStack homeTeleport = createItem(
            Material.ENDER_PEARL,
            ColorUtils.colorize("&b传送到工会家"),
            ColorUtils.colorize("&7传送到工会设置的家")
        );
        inventory.setItem(33, homeTeleport);
        
        // 离开工会按钮
        ItemStack leaveGuild = createItem(
            Material.BARRIER,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.leave-guild", "&c离开工会")),
            ColorUtils.colorize("&7离开当前工会")
        );
        inventory.setItem(34, leaveGuild);
        
        // 删除工会按钮
        ItemStack deleteGuild = createItem(
            Material.TNT,
            ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("gui.delete-guild", "&4删除工会")),
            ColorUtils.colorize("&7删除整个工会"),
            ColorUtils.colorize("&c此操作不可撤销！")
        );
        inventory.setItem(36, deleteGuild);
        
        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.back.name", "&7返回")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-settings.items.back.lore.1", "&7返回主菜单"))
        );
        inventory.setItem(49, back);
    }
    
    /**
     * 显示当前设置信息
     */
    private void displayCurrentSettings(Inventory inventory) {
        // 当前名称
        ItemStack currentName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize("&e当前名称"),
            ColorUtils.colorize("&7" + (guild.getName() != null ? guild.getName() : "无名称"))
        );
        inventory.setItem(10, currentName);
        
        // 当前描述
        ItemStack currentDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize("&e当前描述"),
            ColorUtils.colorize("&7" + (guild.getDescription() != null ? guild.getDescription() : "无描述"))
        );
        inventory.setItem(11, currentDescription);
        
        // 当前标签
        ItemStack currentTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize("&e当前标签"),
            ColorUtils.colorize("&7" + (guild.getTag() != null ? "[" + guild.getTag() + "]" : "无标签"))
        );
        inventory.setItem(13, currentTag);
        
        // 当前工会家状态
        String homeStatus = guild.hasHome() ? "&a已设置" : "&c未设置";
        ItemStack currentHome = createItem(
            Material.COMPASS,
            ColorUtils.colorize("&e工会家状态"),
            ColorUtils.colorize("&7状态: " + homeStatus)
        );
        inventory.setItem(15, currentHome);
        
        // 当前权限设置
        ItemStack currentPermissions = createItem(
            Material.SHIELD,
            ColorUtils.colorize("&e当前权限设置"),
            ColorUtils.colorize("&7会长: 所有权限"),
            ColorUtils.colorize("&7官员: 邀请、踢出"),
            ColorUtils.colorize("&7成员: 基础权限")
        );
        inventory.setItem(17, currentPermissions);
    }
    
    /**
     * 处理修改名称
     */
    private void handleChangeName(Player player) {
        // 检查权限（只有会长可以修改名称）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开名称输入GUI
        plugin.getGuiManager().openGUI(player, new GuildNameInputGUI(plugin, guild, player));
    }
    
    /**
     * 处理修改描述
     */
    private void handleChangeDescription(Player player) {
        // 检查权限（只有会长可以修改描述）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开描述输入GUI
        plugin.getGuiManager().openGUI(player, new GuildDescriptionInputGUI(plugin, guild));
    }
    
    /**
     * 处理修改标签
     */
    private void handleChangeTag(Player player) {
        // 检查权限（只有会长可以修改标签）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开标签输入GUI
        plugin.getGuiManager().openGUI(player, new GuildTagInputGUI(plugin, guild));
    }
    
    /**
     * 处理设置工会家
     */
    private void handleSetHome(Player player) {
        // 检查权限（只有会长可以设置工会家）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 设置工会家
        plugin.getGuildService().setGuildHomeAsync(guild.getId(), player.getLocation(), player.getUniqueId()).thenAccept(success -> {
            if (success) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("sethome.success", "&a工会家设置成功！");
                player.sendMessage(ColorUtils.colorize(message));
                
                // 刷新GUI
                plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("sethome.failed", "&c工会家设置失败！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    /**
     * 处理权限设置
     */
    private void handlePermissions(Player player) {
        // 检查权限（只有会长可以管理权限）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开权限设置GUI
        plugin.getGuiManager().openGUI(player, new GuildPermissionsGUI(plugin, guild));
    }
    
    /**
     * 处理邀请成员
     */
    private void handleInviteMember(Player player) {
        // 检查权限（官员或会长可以邀请成员）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&c需要官员或更高权限");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开邀请成员GUI
        plugin.getGuiManager().openGUI(player, new InviteMemberGUI(plugin, guild));
    }
    
    /**
     * 处理踢出成员
     */
    private void handleKickMember(Player player) {
        // 检查权限（官员或会长可以踢出成员）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&c需要官员或更高权限");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开踢出成员GUI
        plugin.getGuiManager().openGUI(player, new KickMemberGUI(plugin, guild));
    }
    
    /**
     * 处理提升成员
     */
    private void handlePromoteMember(Player player) {
        // 检查权限（只有会长可以提升成员）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开提升成员GUI
        plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild));
    }
    
    /**
     * 处理降级成员
     */
    private void handleDemoteMember(Player player) {
        // 检查权限（只有会长可以降级成员）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开降级成员GUI
        plugin.getGuiManager().openGUI(player, new DemoteMemberGUI(plugin, guild));
    }
    
    /**
     * 处理申请管理
     */
    private void handleApplications(Player player) {
        // 检查权限（官员或会长可以处理申请）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&c需要官员或更高权限");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开申请管理GUI
        plugin.getGuiManager().openGUI(player, new ApplicationManagementGUI(plugin, guild));
    }
    
    /**
     * 处理工会关系管理
     */
    private void handleRelations(Player player) {
        // 检查权限（只有会长可以管理关系）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.only-leader", "&c只有工会会长才能管理工会关系！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开工会关系管理GUI
        plugin.getGuiManager().openGUI(player, new GuildRelationsGUI(plugin, guild, player));
    }
    
    /**
     * 处理工会日志查看
     */
    private void handleGuildLogs(Player player) {
        // 检查权限（工会成员可以查看日志）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&c权限不足");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开工会日志GUI
        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player));
    }
    
    /**
     * 处理工会家传送
     */
    private void handleHomeTeleport(Player player) {
        // 检查权限（工会成员可以传送到工会家）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&c权限不足");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 传送到工会家
        plugin.getGuildService().getGuildHomeAsync(guild.getId()).thenAccept(location -> {
            // 确保在主线程中执行传送操作
            CompatibleScheduler.runTask(plugin, () -> {
                if (location != null) {
                    player.teleport(location);
                    String message = plugin.getConfigManager().getMessagesConfig().getString("home.success", "&a已传送到工会家！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("home.not-set", "&c工会家未设置！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * 处理离开工会
     */
    private void handleLeaveGuild(Player player) {
        // 打开确认离开GUI
        plugin.getGuiManager().openGUI(player, new ConfirmLeaveGuildGUI(plugin, guild));
    }
    
    /**
     * 处理删除工会
     */
    private void handleDeleteGuild(Player player) {
        // 检查权限（只有会长可以删除工会）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 打开确认删除GUI
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild));
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
