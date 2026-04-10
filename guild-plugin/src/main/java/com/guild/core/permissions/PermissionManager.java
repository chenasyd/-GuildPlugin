package com.guild.core.permissions;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 权限管理器 - 提供插件独立的权限功能
 */
public class PermissionManager {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<UUID, PlayerPermissions> playerPermissions = new HashMap<>();
    // 配置驱动的角色权限矩阵
    private RolePermissions defaultPermissions;
    private RolePermissions memberPermissions;
    private RolePermissions officerPermissions;
    private RolePermissions leaderPermissions;
    
    public PermissionManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reloadFromConfig();
    }
    
    /**
     * 检查玩家是否有指定权限
     */
    public boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        // 首先检查Bukkit权限系统
        if (player.hasPermission(permission)) {
            return true;
        }
        
        // 检查插件内置权限
        return hasInternalPermission(player, permission);
    }
    
    /**
     * 检查插件内置权限
     */
    private boolean hasInternalPermission(Player player, String permission) {
        UUID playerUuid = player.getUniqueId();
        
        // 获取玩家权限
        PlayerPermissions permissions = getPlayerPermissions(playerUuid);
        
        // 检查具体权限
        switch (permission) {
            case "guild.use":
                return true; // 所有玩家都可以使用工会系统
                
            case "guild.create":
                return permissions.canCreateGuild();
                
            case "guild.invite":
                return permissions.canInviteMembers();
                
            case "guild.kick":
                return permissions.canKickMembers();
                
            case "guild.promote":
                return permissions.canPromoteMembers();
                
            case "guild.demote":
                return permissions.canDemoteMembers();
                
            case "guild.delete":
                return permissions.canDeleteGuild();
                
            case "guild.admin":
                return permissions.isAdmin();
                
            default:
                return false;
        }
    }
    
    /**
     * 获取玩家权限
     */
    private PlayerPermissions getPlayerPermissions(UUID playerUuid) {
        return playerPermissions.computeIfAbsent(playerUuid, uuid -> {
            PlayerPermissions resolved = new PlayerPermissions();
            GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
            GuildMember.Role role = null;
            if (guildService != null) {
                Guild guild = guildService.getPlayerGuild(uuid);
                if (guild != null) {
                    GuildMember member = guildService.getGuildMember(uuid);
                    if (member != null) {
                        role = member.getRole();
                    }
                }
            }
            RolePermissions rp = resolveRolePermissions(role);
            resolved.setCanCreateGuild(rp.canCreate);
            resolved.setCanInviteMembers(rp.canInvite);
            resolved.setCanKickMembers(rp.canKick);
            resolved.setCanDeleteGuild(rp.canDelete);
            resolved.setCanPromoteMembers(rp.canPromote);
            resolved.setCanDemoteMembers(rp.canDemote);
            // isAdmin 仍由 Bukkit 权限系统控制
            return resolved;
        });
    }

    private RolePermissions resolveRolePermissions(GuildMember.Role role) {
        if (role == null) {
            return defaultPermissions;
        }
        switch (role) {
            case LEADER:
                return leaderPermissions;
            case OFFICER:
                return officerPermissions;
            case MEMBER:
            default:
                return memberPermissions;
        }
    }
    
    /**
     * 更新玩家权限（当工会状态改变时调用）
     */
    public void updatePlayerPermissions(UUID playerUuid) {
        playerPermissions.remove(playerUuid);
        // 重新计算权限
        getPlayerPermissions(playerUuid);
    }

    /**
     * 从配置重载权限矩阵，并清空缓存
     */
    public void reloadFromConfig() {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        this.defaultPermissions = readRolePermissions(cfg, "permissions.default",
                new RolePermissions(false, false, false, false, false, false));
        this.memberPermissions = readRolePermissions(cfg, "permissions.member",
                new RolePermissions(true, false, false, false, false, false));
        this.officerPermissions = readRolePermissions(cfg, "permissions.officer",
                new RolePermissions(true, true, true, false, false, false));
        // leader 未配置时，采用全开作为回退
        RolePermissions leaderFallback = new RolePermissions(true, true, true, true, true, true);
        this.leaderPermissions = readRolePermissions(cfg, "permissions.leader", leaderFallback);
        playerPermissions.clear();
        logger.info("权限矩阵已从配置重载，并清空玩家权限缓存");
    }

    private RolePermissions readRolePermissions(FileConfiguration cfg, String path, RolePermissions fallback) {
        if (cfg == null) return fallback;
        boolean canCreate = cfg.getBoolean(path + ".can-create", fallback.canCreate);
        boolean canInvite = cfg.getBoolean(path + ".can-invite", fallback.canInvite);
        boolean canKick = cfg.getBoolean(path + ".can-kick", fallback.canKick);
        boolean canPromote = cfg.getBoolean(path + ".can-promote", fallback.canPromote);
        boolean canDemote = cfg.getBoolean(path + ".can-demote", fallback.canDemote);
        boolean canDelete = cfg.getBoolean(path + ".can-delete", fallback.canDelete);
        return new RolePermissions(canCreate, canInvite, canKick, canPromote, canDemote, canDelete);
    }
    
    /**
     * 检查玩家是否可以邀请成员
     */
    public boolean canInviteMembers(Player player) {
        if (!hasPermission(player, "guild.invite")) {
            return false;
        }
        
        // 检查玩家是否在工会中
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }
        
        return getPlayerPermissions(player.getUniqueId()).canInviteMembers();
    }
    
    /**
     * 检查玩家是否可以踢出成员
     */
    public boolean canKickMembers(Player player) {
        if (!hasPermission(player, "guild.kick")) {
            return false;
        }
        
        // 检查玩家是否在工会中
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }
        
        return getPlayerPermissions(player.getUniqueId()).canKickMembers();
    }
    
    /**
     * 检查玩家是否可以删除工会
     */
    public boolean canDeleteGuild(Player player) {
        if (!hasPermission(player, "guild.delete")) {
            return false;
        }
        
        // 检查玩家是否在工会中
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }
        
        return getPlayerPermissions(player.getUniqueId()).canDeleteGuild();
    }
    
    /**
     * 检查玩家是否可以创建工会
     */
    public boolean canCreateGuild(Player player) {
        if (!hasPermission(player, "guild.create")) {
            return false;
        }
        
        // 检查玩家是否已有工会
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        return guildService.getPlayerGuild(player.getUniqueId()) == null;
    }
    
    /**
     * 玩家权限类
     */
    private static class PlayerPermissions {
        private boolean canCreateGuild = false;
        private boolean canInviteMembers = false;
        private boolean canKickMembers = false;
        private boolean canDeleteGuild = false;
        private boolean canPromoteMembers = false;
        private boolean canDemoteMembers = false;
        private boolean isAdmin = false;
        
        // Getters and Setters
        public boolean canCreateGuild() { return canCreateGuild; }
        public void setCanCreateGuild(boolean canCreateGuild) { this.canCreateGuild = canCreateGuild; }
        
        public boolean canInviteMembers() { return canInviteMembers; }
        public void setCanInviteMembers(boolean canInviteMembers) { this.canInviteMembers = canInviteMembers; }
        
        public boolean canKickMembers() { return canKickMembers; }
        public void setCanKickMembers(boolean canKickMembers) { this.canKickMembers = canKickMembers; }
        
        public boolean canDeleteGuild() { return canDeleteGuild; }
        public void setCanDeleteGuild(boolean canDeleteGuild) { this.canDeleteGuild = canDeleteGuild; }
        
        public boolean canPromoteMembers() { return canPromoteMembers; }
        public void setCanPromoteMembers(boolean canPromoteMembers) { this.canPromoteMembers = canPromoteMembers; }
        
        public boolean canDemoteMembers() { return canDemoteMembers; }
        public void setCanDemoteMembers(boolean canDemoteMembers) { this.canDemoteMembers = canDemoteMembers; }
        
        public boolean isAdmin() { return isAdmin; }
        public void setAdmin(boolean admin) { isAdmin = admin; }
    }

    // 角色权限矩阵（配置驱动）
    private static class RolePermissions {
        final boolean canCreate;
        final boolean canInvite;
        final boolean canKick;
        final boolean canPromote;
        final boolean canDemote;
        final boolean canDelete;
        RolePermissions(boolean canCreate, boolean canInvite, boolean canKick, boolean canPromote, boolean canDemote, boolean canDelete) {
            this.canCreate = canCreate;
            this.canInvite = canInvite;
            this.canKick = canKick;
            this.canPromote = canPromote;
            this.canDemote = canDemote;
            this.canDelete = canDelete;
        }
    }
}
