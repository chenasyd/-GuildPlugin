package com.guild.core.permissions;

import com.guild.GuildPlugin;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 公会成员操作权限统一入口：Bukkit 权限节点 + config 角色矩阵 + 是否在公会。
 * 命令、GUI、Service 层应通过此类鉴权，避免直接调用 {@code PermissionManager} 成员矩阵或 {@code Role} 硬编码。
 */
public final class GuildMembershipRules {

    private final GuildPlugin plugin;
    private final PermissionManager permissions;

    public GuildMembershipRules(GuildPlugin plugin) {
        this.plugin = plugin;
        this.permissions = plugin.getPermissionManager();
    }

    public boolean canInvite(Player player) {
        return permissions.canInviteMembers(player);
    }

    public boolean canInvite(GuildMember member) {
        if (member == null) {
            return false;
        }
        Player online = plugin.getServer().getPlayer(member.getPlayerUuid());
        if (online != null && online.isOnline()) {
            return canInvite(online);
        }
        return permissions.roleCanInvite(member.getRole());
    }

    public boolean canKick(Player player) {
        return permissions.canKickMembers(player);
    }

    public boolean canKick(GuildMember member) {
        if (member == null) {
            return false;
        }
        Player online = plugin.getServer().getPlayer(member.getPlayerUuid());
        if (online != null && online.isOnline()) {
            return canKick(online);
        }
        return permissions.roleCanKick(member.getRole());
    }

    public boolean canPromote(Player player) {
        return permissions.canPromoteMembers(player);
    }

    public boolean canDemote(Player player) {
        return permissions.canDemoteMembers(player);
    }

    public boolean canPromote(GuildMember member) {
        if (member == null) {
            return false;
        }
        Player online = plugin.getServer().getPlayer(member.getPlayerUuid());
        if (online != null && online.isOnline()) {
            return canPromote(online);
        }
        return permissions.roleCanPromote(member.getRole());
    }

    public boolean canDemote(GuildMember member) {
        if (member == null) {
            return false;
        }
        Player online = plugin.getServer().getPlayer(member.getPlayerUuid());
        if (online != null && online.isOnline()) {
            return canDemote(online);
        }
        return permissions.roleCanDemote(member.getRole());
    }

    public boolean canDeleteGuild(Player player) {
        return permissions.canDeleteGuild(player);
    }

    public boolean canDeleteGuild(GuildMember member) {
        if (member == null) {
            return false;
        }
        Player online = plugin.getServer().getPlayer(member.getPlayerUuid());
        if (online != null && online.isOnline()) {
            return canDeleteGuild(online);
        }
        return permissions.roleCanDeleteGuild(member.getRole());
    }

    /** 官员级管理（邀请/踢人/关系/设 home/日志等），由 config 矩阵驱动 */
    public boolean canManageGuild(Player player) {
        return permissions.hasGuildManagementPermission(player);
    }

    public boolean canManageGuild(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        Player online = plugin.getServer().getPlayer(playerUuid);
        if (online != null && online.isOnline()) {
            return canManageGuild(online);
        }
        GuildService guildService = plugin.getGuildService();
        if (guildService == null) {
            return false;
        }
        GuildMember member = guildService.getGuildMember(playerUuid);
        if (member == null) {
            return false;
        }
        return permissions.roleCanInvite(member.getRole()) || permissions.roleCanKick(member.getRole());
    }

    public boolean isLeaderOf(Player player, int guildId) {
        return player != null && isLeaderOf(player.getUniqueId(), guildId);
    }

    public boolean isLeaderOf(UUID playerUuid, int guildId) {
        if (playerUuid == null) {
            return false;
        }
        GuildService guildService = plugin.getGuildService();
        return guildService != null && guildService.isGuildLeader(playerUuid, guildId);
    }

    /** Config 矩阵查询（仅角色，不含 Bukkit 节点 / 是否在公会），供 GUI 展示与离线路径使用。 */
    public boolean roleMatrixCanInvite(GuildMember.Role role) {
        return permissions.roleCanInvite(role);
    }

    public boolean roleMatrixCanKick(GuildMember.Role role) {
        return permissions.roleCanKick(role);
    }

    public boolean roleMatrixCanPromote(GuildMember.Role role) {
        return permissions.roleCanPromote(role);
    }

    public boolean roleMatrixCanDemote(GuildMember.Role role) {
        return permissions.roleCanDemote(role);
    }

    public boolean roleMatrixCanDeleteGuild(GuildMember.Role role) {
        return permissions.roleCanDeleteGuild(role);
    }

    public boolean roleMatrixCanWarehouse(GuildMember.Role role) {
        return permissions.getDefaultCanWarehouse(role);
    }

    public boolean roleMatrixCanManage(GuildMember.Role role) {
        return roleMatrixCanInvite(role) || roleMatrixCanKick(role);
    }
}
