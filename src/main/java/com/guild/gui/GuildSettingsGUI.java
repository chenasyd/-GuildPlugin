package com.guild.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guildplugin.util.FoliaTeleportUtils;

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

        // 用灰色玻璃填充内部空槽以美化界面（不覆盖已有物品）
        fillInteriorSlots(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 11: // 文本编辑：左=名称 右=描述 Shift+左=标签
                if (clickType == ClickType.LEFT) handleChangeName(player);
                else if (clickType == ClickType.RIGHT) handleChangeDescription(player);
                else if (clickType == ClickType.SHIFT_LEFT) handleChangeTag(player);
                break;
            case 15: // 成员管理：左=邀请 右=踢出 Shift+左=提升/降级
                if (clickType == ClickType.LEFT) handleInviteMember(player);
                else if (clickType == ClickType.RIGHT) handleKickMember(player);
                else if (clickType == ClickType.SHIFT_LEFT) plugin.getGuiManager().openGUI(player, new PromoteMemberGUI(plugin, guild));
                break;
            case 13: // 设置工会家
                handleSetHome(player);
                break;
            case 28: // 申请管理
                if (clickType == ClickType.LEFT) handleApplications(player);
                break;
            case 29: // 关系管理
                if (clickType == ClickType.LEFT) handleRelations(player);
                break;
            case 31: // 工会日志
                if (clickType == ClickType.LEFT) handleGuildLogs(player);
                break;
            case 33: // 工会家传送
                handleHomeTeleport(player);
                break;
            case 34: // 离开
                handleLeaveGuild(player);
                break;
            case 36: // 删除
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
     * 设置设置按钮（简约化）
     */
    private void setupSettingsButtons(Inventory inventory) {
        // 文本编辑（说明改为 Shift+左键）
        ItemStack textEdit = createItem(Material.WRITABLE_BOOK, "&e文本编辑", false,
            "&7左键 &f修改名称",
            "&7右键 &f修改描述",
            "&7Shift+左键 &f修改标签");
        inventory.setItem(11, textEdit);

        // 成员管理（说明改为 Shift+左键）
        ItemStack memberMgmt = createItem(Material.SHIELD, "&a成员管理", false,
            "&7左键 &f邀请成员",
            "&7右键 &f踢出成员",
            "&7Shift+左键 &f提升/降级");
        inventory.setItem(15, memberMgmt);

        // 设置工会家 - 单独格
        ItemStack setHome = createItem(Material.COMPASS, "&b设置工会家", false,
            "&7单击 &f设置工会家");
        inventory.setItem(13, setHome);
    }

    /**
     * 设置功能按钮
     */
    private void setupFunctionButtons(Inventory inventory) {
        // 申请管理（单独槽位 28）
        ItemStack applications = createItem(Material.PAPER, "&e申请管理", false,
            "&7单击 &f处理加入申请");
        inventory.setItem(28, applications);

        // 关系管理（单独槽位 29）
        ItemStack relations = createItem(Material.RED_WOOL, "&e关系管理", false,
            "&7单击 &f管理工会关系");
        inventory.setItem(29, relations);

        // 工会日志（单独槽位 31）
        ItemStack guildLogs = createItem(Material.BOOK, "&6工会日志", false,
            "&7单击 &f查看工会日志");
        inventory.setItem(31, guildLogs);

        // 工会家传送
        ItemStack homeTeleport = createItem(Material.ENDER_PEARL, "&d工会家传送", false,
            "&7单击 &f传送到工会家");
        inventory.setItem(33, homeTeleport);

        // 离开工会
        ItemStack leaveGuild = createItem(Material.BARRIER, "&c离开工会", false,
            "&7单击 &f离开当前工会");
        inventory.setItem(34, leaveGuild);

        // 删除工会
        ItemStack deleteGuild = createItem(Material.TNT, "&4删除工会", false,
            "&7单击 &f删除当前工会");
        inventory.setItem(36, deleteGuild);

        // 返回主菜单
        ItemStack back = createItem(Material.ARROW, "&7返回", false,
            "&7单击 &f返回主菜单");
        inventory.setItem(49, back);
    }
    
    /**
     * 显示当前设置信息（简洁概览）
     */
    private void displayCurrentSettings(Inventory inventory) {
        String name = guild.getName() != null ? guild.getName() : "无名称";
        String tag = guild.getTag() != null ? "[" + guild.getTag() + "]" : "无标签";
        String desc = guild.getDescription() != null ? guild.getDescription() : "无描述";
        String homeStatus = guild.hasHome() ? "&a已设置" : "&c未设置";

        ItemStack overview = createItem(Material.PAPER, "&6工会概览", false,
            "&7名称: &e" + name,
            "&7标签: &e" + tag,
            "&7描述: &7" + desc,
            "&7工会家: " + homeStatus
        );
        inventory.setItem(10, overview);
    }

    // 填充内部空槽（灰色玻璃，不覆盖已有物品）
    private void fillInteriorSlots(Inventory inventory) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 9; slot <= 44; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue; // 左右边框
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }

    private void handleChangeName(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildNameInputGUI(plugin, guild, player));
    }

    private void handleChangeDescription(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildDescriptionInputGUI(plugin, guild));
    }

    private void handleChangeTag(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildTagInputGUI(plugin, guild));
    }

    private void handleInviteMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&c需要官员或更高权限");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new InviteMemberGUI(plugin, guild));
    }

    private void handleKickMember(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&c需要官员或更高权限");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new KickMemberGUI(plugin, guild));
    }

    private void handleSetHome(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuildService().setGuildHomeAsync(guild.getId(), player.getLocation(), player.getUniqueId()).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("sethome.success", "&a工会家设置成功！");
                    player.sendMessage(ColorUtils.colorize(message));
                    plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("sethome.failed", "&c工会家设置失败！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void handleApplications(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.officer-or-higher", "&c需要官员或更高权限");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new ApplicationManagementGUI(plugin, guild));
    }

    private void handleRelations(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("relation.only-leader", "&c只有工会会长才能管理工会关系！");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildRelationsGUI(plugin, guild, player));
    }

    private void handleGuildLogs(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&c权限不足");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new GuildLogsGUI(plugin, guild, player));
    }

    private void handleHomeTeleport(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&c权限不足");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuildService().getGuildHomeAsync(guild.getId()).thenAccept(location -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (location != null) {
                    FoliaTeleportUtils.safeTeleport(plugin, player, location);
                    String message = plugin.getConfigManager().getMessagesConfig().getString("home.success", "&a已传送到工会家！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("home.not-set", "&c工会家未设置！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void handleLeaveGuild(Player player) {
        plugin.getGuiManager().openGUI(player, new ConfirmLeaveGuildGUI(plugin, guild));
    }

    private void handleDeleteGuild(Player player) {
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String msg = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
            player.sendMessage(ColorUtils.colorize(msg));
            return;
        }
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild));
    }

    // createItem 重载：支持不传 glowing 参数的旧调用
    private ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, false, lore);
    }

    // 修正发光魔咒为 LURE（兼容 API）
    private ItemStack createItem(Material material, String name, boolean glowing, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtils.colorize(line));
            }
            meta.setLore(coloredLore);
            if (glowing) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
