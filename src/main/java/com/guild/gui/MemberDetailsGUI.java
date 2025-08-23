package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 成员详情GUI
 */
public class MemberDetailsGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final GuildMember member;
    private final Player viewer;
    
    public MemberDetailsGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player viewer) {
        this.plugin = plugin;
        this.guild = guild;
        this.member = member;
        this.viewer = viewer;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-details.title", "&6成员详情")
            .replace("{member_name}", member.getPlayerName()));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("member-details.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);
        
        // 设置成员头像
        setupMemberHead(inventory);
        
        // 设置基本信息
        setupBasicInfo(inventory);
        
        // 设置权限信息
        setupPermissionInfo(inventory);
        
        // 设置操作按钮
        setupActionButtons(inventory);
        
        // 设置返回按钮
        setupBackButton(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 检查是否是操作按钮
        if (isActionButton(slot)) {
            handleActionButton(player, slot);
            return;
        }
        
        // 检查是否是返回按钮
        if (slot == 49) {
            plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild));
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
     * 设置成员头像
     */
    private void setupMemberHead(Inventory inventory) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            // 根据角色设置不同的显示名称
            String displayName;
            switch (member.getRole()) {
                case LEADER:
                    displayName = ColorUtils.colorize("&c" + member.getPlayerName() + " &7(会长)");
                    break;
                case OFFICER:
                    displayName = ColorUtils.colorize("&6" + member.getPlayerName() + " &7(官员)");
                    break;
                default:
                    displayName = ColorUtils.colorize("&f" + member.getPlayerName() + " &7(成员)");
                    break;
            }
            
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7UUID: &f" + member.getPlayerUuid()));
            lore.add(ColorUtils.colorize("&7角色: &f" + member.getRole().getDisplayName()));
            
            // 格式化加入时间
            if (member.getJoinedAt() != null) {
                String joinTime = member.getJoinedAt().format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
                lore.add(ColorUtils.colorize("&7加入时间: &f" + joinTime));
            } else {
                lore.add(ColorUtils.colorize("&7加入时间: &f未知"));
            }
            
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        
        inventory.setItem(13, head);
    }
    
    /**
     * 设置基本信息
     */
    private void setupBasicInfo(Inventory inventory) {
        // 基本信息标题
        ItemStack infoTitle = createItem(
            Material.BOOK,
            ColorUtils.colorize("&6基本信息"),
            ColorUtils.colorize("&7成员的详细信息")
        );
        inventory.setItem(20, infoTitle);
        
        // 角色信息
        ItemStack roleInfo = createItem(
            Material.GOLDEN_HELMET,
            ColorUtils.colorize("&e角色信息"),
            ColorUtils.colorize("&7当前角色: &f" + member.getRole().getDisplayName()),
            ColorUtils.colorize("&7角色等级: &f" + getRoleLevel(member.getRole())),
            ColorUtils.colorize("&7是否在线: &f" + (isPlayerOnline(member.getPlayerUuid()) ? "&a是" : "&c否"))
        );
        inventory.setItem(21, roleInfo);
        
        // 时间信息
        ItemStack timeInfo = createItem(
            Material.CLOCK,
            ColorUtils.colorize("&e时间信息"),
            ColorUtils.colorize("&7加入时间: &f" + formatTime(member.getJoinedAt())),
            ColorUtils.colorize("&7在工会时长: &f" + getGuildDuration(member.getJoinedAt()))
        );
        inventory.setItem(22, timeInfo);
        
        // 贡献信息
        ItemStack contributionInfo = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&e贡献信息"),
            ColorUtils.colorize("&7工会贡献: &f" + getMemberContribution()),
            ColorUtils.colorize("&7活跃度: &f" + getMemberActivity())
        );
        inventory.setItem(23, contributionInfo);
    }
    
    /**
     * 设置权限信息
     */
    private void setupPermissionInfo(Inventory inventory) {
        // 权限信息标题
        ItemStack permissionTitle = createItem(
            Material.SHIELD,
            ColorUtils.colorize("&6权限信息"),
            ColorUtils.colorize("&7当前拥有的权限")
        );
        inventory.setItem(29, permissionTitle);
        
        // 具体权限列表
        List<String> permissions = getRolePermissions(member.getRole());
        ItemStack permissionList = createItem(
            Material.PAPER,
            ColorUtils.colorize("&e权限列表"),
            permissions.toArray(new String[0])
        );
        inventory.setItem(30, permissionList);
        
        // 权限等级
        ItemStack permissionLevel = createItem(
            Material.EXPERIENCE_BOTTLE,
            ColorUtils.colorize("&e权限等级"),
            ColorUtils.colorize("&7当前等级: &f" + getPermissionLevel(member.getRole())),
            ColorUtils.colorize("&7可执行操作: &f" + getExecutableActions(member.getRole()))
        );
        inventory.setItem(31, permissionLevel);
    }
    
    /**
     * 设置操作按钮
     */
    private void setupActionButtons(Inventory inventory) {
        // 检查当前玩家是否有权限执行操作
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), viewer.getUniqueId()).thenAccept(viewerMember -> {
            if (viewerMember == null) return;
            
            // 不能操作自己
            if (member.getPlayerUuid().equals(viewer.getUniqueId())) {
                return;
            }
            
            // 不能操作会长
            if (member.getRole() == GuildMember.Role.LEADER) {
                return;
            }
            
            // 踢出按钮（需要踢出权限）
            if (viewerMember.getRole().canKick()) {
                ItemStack kickButton = createItem(
                    Material.REDSTONE_BLOCK,
                    ColorUtils.colorize("&c踢出成员"),
                    ColorUtils.colorize("&7将成员踢出工会"),
                    ColorUtils.colorize("&7点击确认踢出")
                );
                inventory.setItem(37, kickButton);
            }
            
            // 提升/降级按钮（只有会长可以）
            if (viewerMember.getRole() == GuildMember.Role.LEADER) {
                if (member.getRole() == GuildMember.Role.OFFICER) {
                    // 降级按钮
                    ItemStack demoteButton = createItem(
                        Material.IRON_INGOT,
                        ColorUtils.colorize("&7降级成员"),
                        ColorUtils.colorize("&7将官员降级为普通成员"),
                        ColorUtils.colorize("&7点击确认降级")
                    );
                    inventory.setItem(39, demoteButton);
                } else {
                    // 提升按钮
                    ItemStack promoteButton = createItem(
                        Material.GOLD_INGOT,
                        ColorUtils.colorize("&6提升成员"),
                        ColorUtils.colorize("&7将成员提升为官员"),
                        ColorUtils.colorize("&7点击确认提升")
                    );
                    inventory.setItem(39, promoteButton);
                }
            }
            
            // 发送消息按钮
            ItemStack messageButton = createItem(
                Material.PAPER,
                ColorUtils.colorize("&e发送消息"),
                ColorUtils.colorize("&7向该成员发送私信"),
                ColorUtils.colorize("&7点击打开聊天")
            );
            inventory.setItem(41, messageButton);
        });
    }
    
    /**
     * 设置返回按钮
     */
    private void setupBackButton(Inventory inventory) {
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize("&7返回"),
            ColorUtils.colorize("&7返回成员管理")
        );
        inventory.setItem(49, back);
    }
    
    /**
     * 检查是否是操作按钮
     */
    private boolean isActionButton(int slot) {
        return slot == 37 || slot == 39 || slot == 41;
    }
    
    /**
     * 处理操作按钮点击
     */
    private void handleActionButton(Player player, int slot) {
        switch (slot) {
            case 37: // 踢出成员
                handleKickMember(player);
                break;
            case 39: // 提升/降级成员
                handlePromoteDemoteMember(player);
                break;
            case 41: // 发送消息
                handleSendMessage(player);
                break;
        }
    }
    
    /**
     * 处理踢出成员
     */
    private void handleKickMember(Player player) {
        // 检查权限
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || !executor.getRole().canKick()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&c权限不足");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 确认踢出
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-kick", "&c确定要踢出成员 {member} 吗？输入 &f/guild kick {member} confirm &c确认")
                .replace("{member}", member.getPlayerName());
            player.sendMessage(ColorUtils.colorize(message));
            player.closeInventory();
        });
    }
    
    /**
     * 处理提升/降级成员
     */
    private void handlePromoteDemoteMember(Player player) {
        // 检查权限
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || executor.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&c只有工会会长才能执行此操作");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            if (member.getRole() == GuildMember.Role.OFFICER) {
                // 降级
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-demote", "&c确定要降级成员 {member} 吗？输入 &f/guild demote {member} confirm &c确认")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            } else {
                // 提升
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-promote", "&a确定要提升成员 {member} 为官员吗？输入 &f/guild promote {member} confirm &a确认")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            }
            player.closeInventory();
        });
    }
    
    /**
     * 处理发送消息
     */
    private void handleSendMessage(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.open-chat", "&e请输入要发送给 {member} 的消息:")
            .replace("{member}", member.getPlayerName());
        player.sendMessage(ColorUtils.colorize(message));
        player.closeInventory();
        
        // 这里可以集成聊天系统，暂时只是提示
        // TODO: 实现私信系统
    }
    
    /**
     * 获取角色等级
     */
    private String getRoleLevel(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "最高级";
            case OFFICER:
                return "高级";
            default:
                return "普通";
        }
    }
    
    /**
     * 检查玩家是否在线
     */
    private boolean isPlayerOnline(java.util.UUID playerUuid) {
        Player player = plugin.getServer().getPlayer(playerUuid);
        return player != null && player.isOnline();
    }
    
    /**
     * 格式化时间
     */
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "未知";
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }
    
    /**
     * 获取在工会时长
     */
    private String getGuildDuration(java.time.LocalDateTime joinDateTime) {
        if (joinDateTime == null) return "未知";
        
        java.time.LocalDateTime currentTime = java.time.LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(joinDateTime, currentTime);
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
            return days + "天" + hours + "小时";
        } else if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }
    
    /**
     * 获取成员贡献
     */
    private String getMemberContribution() {
        // TODO: 实现贡献统计系统
        return "待统计";
    }
    
    /**
     * 获取成员活跃度
     */
    private String getMemberActivity() {
        // TODO: 实现活跃度统计系统
        return "待统计";
    }
    
    /**
     * 获取角色权限列表
     */
    private List<String> getRolePermissions(GuildMember.Role role) {
        List<String> permissions = new ArrayList<>();
        
        switch (role) {
            case LEADER:
                permissions.add(ColorUtils.colorize("&7✓ 所有权限"));
                permissions.add(ColorUtils.colorize("&7✓ 邀请成员"));
                permissions.add(ColorUtils.colorize("&7✓ 踢出成员"));
                permissions.add(ColorUtils.colorize("&7✓ 提升/降级"));
                permissions.add(ColorUtils.colorize("&7✓ 管理工会"));
                permissions.add(ColorUtils.colorize("&7✓ 解散工会"));
                break;
            case OFFICER:
                permissions.add(ColorUtils.colorize("&7✓ 邀请成员"));
                permissions.add(ColorUtils.colorize("&7✓ 踢出成员"));
                permissions.add(ColorUtils.colorize("&7✗ 提升/降级"));
                permissions.add(ColorUtils.colorize("&7✗ 管理工会"));
                permissions.add(ColorUtils.colorize("&7✗ 解散工会"));
                break;
            default:
                permissions.add(ColorUtils.colorize("&7✗ 邀请成员"));
                permissions.add(ColorUtils.colorize("&7✗ 踢出成员"));
                permissions.add(ColorUtils.colorize("&7✗ 提升/降级"));
                permissions.add(ColorUtils.colorize("&7✗ 管理工会"));
                permissions.add(ColorUtils.colorize("&7✗ 解散工会"));
                break;
        }
        
        return permissions;
    }
    
    /**
     * 获取权限等级
     */
    private String getPermissionLevel(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "最高级 (3级)";
            case OFFICER:
                return "高级 (2级)";
            default:
                return "普通 (1级)";
        }
    }
    
    /**
     * 获取可执行操作
     */
    private String getExecutableActions(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "所有操作";
            case OFFICER:
                return "邀请、踢出";
            default:
                return "基础操作";
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
