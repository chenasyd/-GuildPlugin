package com.guild.services;

import com.guild.core.module.ModuleManager;
import com.guild.core.time.TimeProvider;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.sdk.GuildPluginAPI;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

abstract class GuildServiceSupport {
    protected final GuildServiceContext ctx;

    GuildServiceSupport(GuildServiceContext ctx) {
        this.ctx = ctx;
    }

    // ==================== 模块事件分发辅助 ====================

    protected void fireModuleEvent(String eventName, Consumer<GuildPluginAPI> action) {
        try {
            ModuleManager moduleManager = ctx.plugin.getServiceContainer().get(ModuleManager.class);
            if (moduleManager == null) {
                return;
            }
            action.accept(moduleManager.getSharedApi());
        } catch (Exception e) {
            ctx.logger.log(Level.WARNING,
                    "Failed to dispatch module event '" + eventName + "': " + e.getMessage(), e);
        }
    }

    protected void refreshPlayerPermissions(UUID playerUuid) {
        try {
            ctx.plugin.getPermissionManager().updatePlayerPermissions(playerUuid);
        } catch (Exception e) {
            ctx.logger.log(Level.FINE,
                    "Failed to refresh permissions for " + playerUuid + ": " + e.getMessage());
        }
    }

    protected void fireGuildCreate(int guildId, String guildName, String leaderName) {
        fireModuleEvent("GuildCreate", api -> api.fireGuildCreate(guildId, guildName, leaderName));
    }

    protected void fireGuildDelete(int guildId, String guildName, String leaderName) {
        fireModuleEvent("GuildDelete", api -> api.fireGuildDelete(guildId, guildName, leaderName));
    }

    protected void fireMemberJoin(int guildId, String guildName, UUID playerUuid, String playerName) {
        fireModuleEvent("MemberJoin", api -> api.fireMemberJoin(guildId, guildName, playerUuid, playerName));
    }

    protected void fireMemberLeave(int guildId, String guildName, UUID playerUuid, String playerName, String eventType) {
        fireModuleEvent("MemberLeave",
                api -> api.fireMemberLeave(guildId, guildName, playerUuid, playerName, eventType));
    }

    public void notifyEconomyDeposit(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        fireModuleEvent("EconomyDeposit",
                api -> api.fireEconomyDeposit(guildId, guildName, playerUuid, playerName, amount));
    }

    public void notifyEconomyWithdraw(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        fireModuleEvent("EconomyWithdraw",
                api -> api.fireEconomyWithdraw(guildId, guildName, playerUuid, playerName, amount));
    }

    protected void fireMemberRoleChange(int guildId, String guildName, UUID playerUuid, String playerName,
                                      String oldRole, String newRole) {
        fireModuleEvent("MemberRoleChange", api -> api.fireMemberRoleChange(guildId, guildName, playerUuid,
                playerName, oldRole, newRole));
    }
    
    // 时间工具：统一使用操作系统本地时间字符串（yyyy-MM-dd HH:mm:ss）
    protected String nowString() { return TimeProvider.nowString(); }
    protected String plusMinutesString(int minutes) { return TimeProvider.plusMinutesString(minutes); }
    protected String plusDaysString(int days) { return TimeProvider.plusDaysString(days); }
    void deleteWarehouseData(int guildId) throws SQLException {
        try (Connection conn = ctx.databaseManager.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM guild_warehouse_items WHERE guild_id = ?")) {
                stmt.setInt(1, guildId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM guild_warehouse_role_perms WHERE guild_id = ?")) {
                stmt.setInt(1, guildId);
                stmt.executeUpdate();
            }
        }
    }
    /** 在线玩家是否拥有 guild.admin */
    public boolean isGuildAdmin(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        Player player = ctx.plugin.getServer().getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return false;
        }
        return ctx.plugin.getPermissionManager().hasPermission(player, "guild.admin");
    }

    protected boolean canTransferLeadership(Guild guild, UUID requesterUuid) {
        if (requesterUuid == null || guild == null) {
            return false;
        }
        if (isGuildAdmin(requesterUuid)) {
            return true;
        }
        GuildMember requester = ctx.serviceRef.getGuildMember(requesterUuid);
        return requester != null
                && requester.getGuildId() == guild.getId()
                && requester.getRole() == GuildMember.Role.LEADER
                && requesterUuid.equals(guild.getLeaderUuid());
    }

    /** Service 层 CompletableFuture 失败时的结构化日志（不通知玩家）。 */
    protected <T> Function<Throwable, T> logAsyncFailure(String operation) {
        return throwable -> {
            Throwable cause = throwable;
            if (throwable instanceof CompletionException && throwable.getCause() != null) {
                cause = throwable.getCause();
            }
            ctx.logger.log(Level.SEVERE, "Guild service failed: " + operation, cause);
            return null;
        };
    }

}
