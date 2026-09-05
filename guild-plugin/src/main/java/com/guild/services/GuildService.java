package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildApplication;
import com.guild.models.GuildInvitation;
import com.guild.models.GuildRelation;
import com.guild.models.GuildEconomy;
import com.guild.models.GuildContribution;
import com.guild.models.GuildLog;
import com.guild.services.repository.GuildRepositories;
import com.guild.util.NotifyUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import com.guild.core.time.TimeProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.DebugLog;
import com.guild.core.utils.QuietLog;
import com.guild.core.module.ModuleManager;
import com.guild.sdk.GuildPluginAPI;

import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * 公会业务门面：编排鉴权、事件、日志与通知，数据访问委托 {@link GuildRepositories}。
 */
public class GuildService {
    
    private final GuildPlugin plugin;
    private final DatabaseManager databaseManager;
    private final GuildRepositories repos;
    private final Logger logger;
    
    public GuildService(GuildPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.logger = plugin.getLogger();
        this.repos = new GuildRepositories(databaseManager, logger);
    }

    // ==================== 模块事件分发辅助 ====================

    private void fireModuleEvent(String eventName, Consumer<GuildPluginAPI> action) {
        try {
            ModuleManager moduleManager = plugin.getServiceContainer().get(ModuleManager.class);
            if (moduleManager == null) {
                return;
            }
            action.accept(moduleManager.getSharedApi());
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "Failed to dispatch module event '" + eventName + "': " + e.getMessage(), e);
        }
    }

    private void refreshPlayerPermissions(UUID playerUuid) {
        try {
            plugin.getPermissionManager().updatePlayerPermissions(playerUuid);
        } catch (Exception e) {
            logger.log(Level.FINE,
                    "Failed to refresh permissions for " + playerUuid + ": " + e.getMessage());
        }
    }

    private void fireGuildCreate(int guildId, String guildName, String leaderName) {
        fireModuleEvent("GuildCreate", api -> api.fireGuildCreate(guildId, guildName, leaderName));
    }

    private void fireGuildDelete(int guildId, String guildName, String leaderName) {
        fireModuleEvent("GuildDelete", api -> api.fireGuildDelete(guildId, guildName, leaderName));
    }

    private void fireMemberJoin(int guildId, String guildName, UUID playerUuid, String playerName) {
        fireModuleEvent("MemberJoin", api -> api.fireMemberJoin(guildId, guildName, playerUuid, playerName));
    }

    private void fireMemberLeave(int guildId, String guildName, UUID playerUuid, String playerName, String eventType) {
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

    private void fireMemberRoleChange(int guildId, String guildName, UUID playerUuid, String playerName,
                                      String oldRole, String newRole) {
        fireModuleEvent("MemberRoleChange", api -> api.fireMemberRoleChange(guildId, guildName, playerUuid,
                playerName, oldRole, newRole));
    }
    
    // 时间工具：统一使用操作系统本地时间字符串（yyyy-MM-dd HH:mm:ss）
    private String nowString() { return TimeProvider.nowString(); }
    private String plusMinutesString(int minutes) { return TimeProvider.plusMinutesString(minutes); }
    private String plusDaysString(int days) { return TimeProvider.plusDaysString(days); }
    
    /**
     * 创建公会 (异步)
     */
    public CompletableFuture<Boolean> createGuildAsync(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        return getGuildByNameAsync(name).thenCompose(existingGuildByName -> {
            if (existingGuildByName != null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildByTagAsync(tag).thenCompose(existingGuildByTag -> {
                if (existingGuildByTag != null) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return repos.guilds().insertAsync(name, tag, description, leaderUuid, leaderName,
                        nowString(), nowString()).thenApply(guildId -> {
                    if (guildId > 0) {
                        QuietLog.system("Guild created successfully: " + name + " (ID: " + guildId + ")");
                    }
                    return guildId;
                }).thenCompose(guildId -> {
                    if ((Integer) guildId > 0) {
                        // 添加会长为公会成员（避免重复查询）
                        return addGuildMemberDirectAsync((Integer) guildId, leaderUuid, leaderName, GuildMember.Role.LEADER)
                            .thenCompose(success -> {
                                if (success) {
                                    fireGuildCreate((Integer) guildId, name, leaderName);
                                    // 记录公会创建日志
                                    return logGuildActionAsync((Integer) guildId, name, leaderUuid.toString(), leaderName,
                                        GuildLog.LogType.GUILD_CREATED, "创建公会", "公会名称: " + name + ", 标签: " + tag)
                                        .thenApply(logSuccess -> success);
                                }
                                return CompletableFuture.completedFuture(success);
                            });
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
        });
    }
    
    /**
     * 创建公会 (同步包装器)
     */
    public boolean createGuild(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        try {
            return createGuildAsync(name, tag, description, leaderUuid, leaderName).get();
        } catch (Exception e) {
            logger.severe("Exception creating guild: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 删除公会 (异步)
     */
    public CompletableFuture<Boolean> deleteGuildAsync(int guildId, UUID requesterUuid) {
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildMemberAsync(requesterUuid).thenCompose(member -> {
                // 检查权限
                if (member == null || member.getGuildId() != guildId || member.getRole() != GuildMember.Role.LEADER) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        // 获取公会余额用于退款
                        double guildBalance = guild.getBalance();
                        
                        // 删除公会成员
                        try (Connection conn = databaseManager.getConnection()) {
                            repos.members().deleteAllByGuildId(conn, guildId);
                        }

                        deleteWarehouseData(guildId);

                        if (repos.guilds().deleteById(guildId)) {
                            QuietLog.system("Guild deleted successfully: " + guild.getName() + " (ID: " + guildId + ")");

                            // 退款给会长（如果经济系统可用）
                            if (guildBalance > 0 && plugin.getEconomyManager().isVaultAvailable()) {
                                try {
                                    org.bukkit.entity.Player leaderPlayer = org.bukkit.Bukkit.getPlayer(guild.getLeaderUuid());
                                    if (leaderPlayer != null && leaderPlayer.isOnline()) {
                                        plugin.getEconomyManager().deposit(leaderPlayer, guildBalance);
                                        String message = plugin.getLanguageManager().getCoreMessage(leaderPlayer, "economy.disband-compensation", "&a公会解散，您获得了 {amount} 金币补偿！", "{amount}", plugin.getEconomyManager().format(guildBalance));
                                        leaderPlayer.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                                    }
                                } catch (Exception e) {
                                    logger.warning("Error refunding leader: " + e.getMessage());
                                }
                            }

                            // 记录公会解散日志
                            logGuildActionAsync(guildId, guild.getName(), guild.getLeaderUuid().toString(), guild.getLeaderName(),
                                GuildLog.LogType.GUILD_DISSOLVED, "公会解散", "公会余额: " + guildBalance + " 金币");

                            // 分发公会解散事件给模块
                            fireGuildDelete(guildId, guild.getName(), guild.getLeaderName());

                            return true;
                        }
                    } catch (SQLException e) {
                        logger.severe("Error deleting guild: " + e.getMessage());
                    }
                    return false;
                });
            });
        });
    }
    
    /**
     * 删除公会 (同步包装器)
     */
    public boolean deleteGuild(int guildId, UUID requesterUuid) {
        try {
            return deleteGuildAsync(guildId, requesterUuid).get();
        } catch (Exception e) {
            logger.severe("Exception deleting guild: " + e.getMessage());
            return false;
        }
    }

    private void deleteWarehouseData(int guildId) throws SQLException {
        try (Connection conn = databaseManager.getConnection()) {
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

    /**
     * 管理员强制删除公会 (异步) — 跳过会长身份验证，但资金仍退还至会长
     * @param guildId 公会ID
     * @param adminUuid 管理员UUID（执行删除的人，非会长）
     */
    public CompletableFuture<Boolean> forceDeleteGuildAsync(int guildId, UUID adminUuid) {
        if (adminUuid != null && !isGuildAdmin(adminUuid)) {
            return CompletableFuture.completedFuture(false);
        }
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // 获取公会余额用于退款（退款给会长，而非管理员）
                    double guildBalance = guild.getBalance();
                    
                    // 删除所有公会成员
                    try (Connection conn = databaseManager.getConnection()) {
                        repos.members().deleteAllByGuildId(conn, guildId);
                    }

                    deleteWarehouseData(guildId);

                    if (repos.guilds().deleteById(guildId)) {
                        QuietLog.system("Admin force-deleted guild: " + guild.getName() + " (ID: " + guildId + ", by: " + adminUuid + ")");

                        // 退款给会长（如果经济系统可用）— 注意：退款给会长而非管理员
                        if (guildBalance > 0 && plugin.getEconomyManager().isVaultAvailable()) {
                            try {
                                org.bukkit.entity.Player leaderPlayer = org.bukkit.Bukkit.getPlayer(guild.getLeaderUuid());
                                if (leaderPlayer != null && leaderPlayer.isOnline()) {
                                    plugin.getEconomyManager().deposit(leaderPlayer, guildBalance);
                                    String message = plugin.getLanguageManager().getCoreMessage(leaderPlayer, "economy.disband-compensation", "&a公会解散，您获得了 {amount} 金币补偿！", "{amount}", plugin.getEconomyManager().format(guildBalance));
                                    leaderPlayer.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                                }
                            } catch (Exception e) {
                                logger.warning("Error refunding to guild leader: " + e.getMessage());
                            }
                        }

                        // 记录公会强制解散日志
                        logGuildActionAsync(guildId, guild.getName(), guild.getLeaderUuid().toString(), guild.getLeaderName(),
                            GuildLog.LogType.GUILD_DISSOLVED, "Admin force deleted",
                            "By: " + adminUuid + ", balance: " + guildBalance + " coins");

                        // 分发公会解散事件给模块
                        fireGuildDelete(guildId, guild.getName(), guild.getLeaderName());

                        return true;
                    }
                } catch (SQLException e) {
                    logger.severe("Error during admin force-delete: " + e.getMessage());
                }
                return false;
            });
        });
    }
    
    /**
     * 更新公会信息 (异步)
     */
    public CompletableFuture<Boolean> updateGuildAsync(int guildId, String name, String tag, String description, UUID requesterUuid) {
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildMemberAsync(requesterUuid).thenCompose(member -> {
                // 检查权限
                if (member == null || member.getGuildId() != guildId || 
                    (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // 检查名称和标签是否与其他公会冲突
                CompletableFuture<Boolean> nameCheck = CompletableFuture.completedFuture(true);
                if (name != null && !name.equals(guild.getName())) {
                    nameCheck = getGuildByNameAsync(name).thenApply(existingGuild -> existingGuild == null);
                }
                
                CompletableFuture<Boolean> tagCheck = CompletableFuture.completedFuture(true);
                if (tag != null && !tag.equals(guild.getTag())) {
                    tagCheck = getGuildByTagAsync(tag).thenApply(existingGuild -> existingGuild == null);
                }
                
                return nameCheck.thenCombine(tagCheck, (nameValid, tagValid) -> nameValid && tagValid)
                    .thenCompose(valid -> {
                        if (!valid) {
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        return CompletableFuture.supplyAsync(() -> {
                            if (repos.guilds().updateInfo(guildId, name, tag, description, nowString())) {
                                QuietLog.system("Guild info updated successfully: " + guild.getName() + " (ID: " + guildId + ")");
                                return true;
                            }
                            return false;
                        });
                    });
            });
        });
    }
    
    /**
     * 更新公会信息 (同步包装器)
     */
    public boolean updateGuild(int guildId, String name, String tag, String description, UUID requesterUuid) {
        try {
            return updateGuildAsync(guildId, name, tag, description, requesterUuid).get();
        } catch (Exception e) {
            logger.severe("Exception updating guild info: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 添加公会成员 (异步)
     * 包含人数上限检查：查询目标公会当前成员数，与有效上限比较。
     */
    public CompletableFuture<Boolean> addGuildMemberAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        DebugLog.info(logger, "[AddMember-Debug] Starting member add: guildId=" + guildId + ", player=" + playerName + ", uuid=" + playerUuid);
        
        return getPlayerGuildAsync(playerUuid).thenCompose(existingGuild -> {
            if (existingGuild != null) {
                logger.warning("[AddMember-Debug] Player " + playerName + " is already in guild " + existingGuild.getName());
                return CompletableFuture.completedFuture(false);
            }
            
            DebugLog.info(logger, "[AddMember-Debug] Player not in any guild, checking capacity");
            
            // 人数上限检查：获取目标公会并比较当前成员数与有效上限
            return getGuildByIdAsync(guildId).thenCompose(targetGuild -> {
                if (targetGuild == null) {
                    logger.warning("[AddMember-Debug] Target guild not found: guildId=" + guildId);
                    return CompletableFuture.completedFuture(false);
                }
                
                return getGuildMemberCountAsync(guildId).thenCompose(memberCount -> {
                    int effectiveMax = getEffectiveMaxMembers(targetGuild);
                    if (memberCount >= effectiveMax) {
                        DebugLog.info(logger, "[AddMember-Debug] Guild " + targetGuild.getName() + " is full (" 
                            + memberCount + "/" + effectiveMax + "), rejecting " + playerName);
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    DebugLog.info(logger, "[AddMember-Debug] Capacity OK (" + memberCount + "/" + effectiveMax + "), preparing database insert");
                    
                    return CompletableFuture.supplyAsync(() -> {
                        if (repos.members().insert(guildId, playerUuid, playerName, role, nowString())) {
                                DebugLog.info(logger, "[AddMember-Debug] Player " + playerName + " successfully joined guild (ID: " + guildId + ")");
                                refreshPlayerPermissions(playerUuid);
                                logGuildActionAsync(guildId, targetGuild.getName(), playerUuid.toString(), playerName,
                                    GuildLog.LogType.MEMBER_JOINED, "成员加入", "玩家: " + playerName + ", 职位: " + role.getDisplayName());
                                fireMemberJoin(targetGuild.getId(), targetGuild.getName(), playerUuid, playerName);
                                return true;
                        }
                        logger.warning("[AddMember-Debug] INSERT did not affect any rows");
                        return false;
                    });
                });
            });
        });
    }
    
    /**
     * 添加公会成员 (同步包装器)
     */
    public boolean addGuildMember(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        try {
            return addGuildMemberAsync(guildId, playerUuid, playerName, role).get();
        } catch (Exception e) {
            logger.severe("Exception adding guild member: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 移除公会成员 (异步)
     */
    public CompletableFuture<Boolean> removeGuildMemberAsync(UUID playerUuid, UUID requesterUuid) {
        return getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildMemberAsync(requesterUuid).thenCompose(requester -> {
                // 检查权限
                if (requester == null || requester.getGuildId() != member.getGuildId()) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // 会长不能被踢出，除非是自我离开
                if (member.getRole() == GuildMember.Role.LEADER && !playerUuid.equals(requesterUuid)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // 只有会长和官员可以踢出成员
                if (!playerUuid.equals(requesterUuid) && 
                    requester.getRole() != GuildMember.Role.LEADER && 
                    requester.getRole() != GuildMember.Role.OFFICER) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    if (repos.members().deleteByPlayerUuid(playerUuid)) {
                                QuietLog.system("Player " + member.getPlayerName() + " left guild (ID: " + member.getGuildId() + ")");
                                refreshPlayerPermissions(playerUuid);
                                getGuildByIdAsync(member.getGuildId()).thenAccept(guild -> {
                                    if (guild != null) {
                                        GuildLog.LogType logType = playerUuid.equals(requesterUuid) ? 
                                            GuildLog.LogType.MEMBER_LEFT : GuildLog.LogType.MEMBER_KICKED;
                                        String description = playerUuid.equals(requesterUuid) ? "成员主动离开" : "成员被踢出";
                                        String details = "玩家: " + member.getPlayerName() + 
                                            (playerUuid.equals(requesterUuid) ? "" : ", 操作者: " + requester.getPlayerName());
                                        
                                        logGuildActionAsync(member.getGuildId(), guild.getName(), 
                                            requesterUuid.toString(), requester.getPlayerName(),
                                            logType, description, details);
                                        
                                        // 分发成员离开事件给模块
                                        String eventType = playerUuid.equals(requesterUuid) ? "leave" : "kicked";
                                        fireMemberLeave(guild.getId(), guild.getName(), playerUuid, member.getPlayerName(), eventType);
                                    }
                                });
                                return true;
                    }
                    return false;
                });
            });
        });
    }
    
    /**
     * 移除公会成员 (同步包装器)
     */
    public boolean removeGuildMember(UUID playerUuid, UUID requesterUuid) {
        try {
            return removeGuildMemberAsync(playerUuid, requesterUuid).get();
        } catch (Exception e) {
            logger.severe("Exception removing guild member: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新成员角色 (异步)
     */
    public CompletableFuture<Boolean> updateMemberRoleAsync(UUID playerUuid, GuildMember.Role newRole, UUID requesterUuid) {
        return getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null) {
                return CompletableFuture.completedFuture(false);
            }

            if (newRole == GuildMember.Role.LEADER) {
                return CompletableFuture.completedFuture(false);
            }
            if (member.getRole() == newRole) {
                return CompletableFuture.completedFuture(false);
            }
            if (member.getRole() == GuildMember.Role.LEADER) {
                return CompletableFuture.completedFuture(false);
            }
            if (newRole == GuildMember.Role.OFFICER && member.getRole() != GuildMember.Role.MEMBER) {
                return CompletableFuture.completedFuture(false);
            }
            if (newRole == GuildMember.Role.MEMBER && member.getRole() != GuildMember.Role.OFFICER) {
                return CompletableFuture.completedFuture(false);
            }

            return getGuildMemberAsync(requesterUuid).thenCompose(requester -> {
                if (requester == null || requester.getGuildId() != member.getGuildId()
                        || requester.getRole() != GuildMember.Role.LEADER) {
                    return CompletableFuture.completedFuture(false);
                }

                String oldRole = member.getRole().name();
                int guildId = member.getGuildId();

                return CompletableFuture.supplyAsync(() -> {
                    if (repos.members().updateRole(playerUuid, guildId, newRole)) {
                                QuietLog.system("Player " + member.getPlayerName() + " role updated to: " + newRole.name());
                                refreshPlayerPermissions(playerUuid);
                                getGuildByIdAsync(guildId).thenAccept(guild -> {
                                    if (guild != null) {
                                        GuildLog.LogType logType = newRole == GuildMember.Role.OFFICER
                                                ? GuildLog.LogType.MEMBER_PROMOTED
                                                : GuildLog.LogType.MEMBER_DEMOTED;
                                        String description = newRole == GuildMember.Role.OFFICER ? "成员升职" : "成员降职";
                                        String details = "玩家: " + member.getPlayerName() + ", 新职位: " + newRole.getDisplayName()
                                                + ", 操作者: " + requester.getPlayerName();

                                        logGuildActionAsync(guildId, guild.getName(),
                                                requesterUuid.toString(), requester.getPlayerName(),
                                                logType, description, details);
                                        fireMemberRoleChange(guildId, guild.getName(), playerUuid, member.getPlayerName(),
                                                oldRole, newRole.name());
                                    }
                                });
                                return true;
                    }
                    return false;
                });
            });
        });
    }
    
    /**
     * 更新成员角色 (同步包装器)
     */
    public boolean updateMemberRole(UUID playerUuid, GuildMember.Role newRole, UUID requesterUuid) {
        try {
            return updateMemberRoleAsync(playerUuid, newRole, requesterUuid).get();
        } catch (Exception e) {
            logger.severe("Exception updating member role: " + e.getMessage());
            return false;
        }
    }

    // ==================== SDK 直接管理方法（v1.5 新增，跳过权限检查） ====================

    /**
     * 直接移除成员（供 SDK 模块调用，跳过操作者权限检查）。
     * 不影响现有的 removeGuildMemberAsync(playerUuid, requesterUuid) 方法。
     */
    public CompletableFuture<Boolean> removeGuildMemberDirectAsync(int guildId, UUID playerUuid) {
        return getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null || member.getGuildId() != guildId) {
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.supplyAsync(() -> {
                if (repos.members().deleteByPlayerUuid(playerUuid)) {
                            refreshPlayerPermissions(playerUuid);
                            getGuildByIdAsync(guildId).thenAccept(guild -> {
                                if (guild != null) {
                                    logGuildActionAsync(guildId, guild.getName(), playerUuid.toString(), member.getPlayerName(),
                                            GuildLog.LogType.MEMBER_KICKED, "成员被API移除", "玩家: " + member.getPlayerName());
                                    fireMemberLeave(guildId, guild.getName(), playerUuid, member.getPlayerName(), "kicked");
                                }
                            });
                            return true;
                }
                return false;
            });
        });
    }

    /**
     * 直接修改成员角色（供 SDK 模块调用，跳过操作者权限检查）。
     * 不影响现有的 updateMemberRoleAsync(playerUuid, newRole, requesterUuid) 方法。
     */
    public CompletableFuture<Boolean> updateMemberRoleDirectAsync(int guildId, UUID playerUuid, String roleName) {
        return getGuildMemberAsync(playerUuid).thenCompose(member -> {
            if (member == null || member.getGuildId() != guildId) {
                return CompletableFuture.completedFuture(false);
            }
            GuildMember.Role newRole;
            try {
                newRole = GuildMember.Role.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return CompletableFuture.completedFuture(false);
            }
            String oldRole = member.getRole().name();
            return CompletableFuture.supplyAsync(() -> {
                if (repos.members().updateRoleByPlayerUuid(playerUuid, newRole)) {
                            refreshPlayerPermissions(playerUuid);
                            getGuildByIdAsync(guildId).thenAccept(guild -> {
                                if (guild != null) {
                                    fireMemberRoleChange(guildId, guild.getName(), playerUuid, member.getPlayerName(), oldRole, newRole.name());
                                }
                            });
                            return true;
                }
                return false;
            });
        });
    }
    
    /**
     * 转移会长职位 (异步)
     * 将 guild_id 的会长改为 newLeaderUuid，同时将原会长降为成员。
     */
    public CompletableFuture<Boolean> transferGuildLeadershipAsync(int guildId, UUID newLeaderUuid, String newLeaderName) {
        return transferGuildLeadershipAsync(guildId, newLeaderUuid, newLeaderName, null);
    }

    /**
     * 转移会长职位 (异步，带操作者记录)
     * <p>服务层校验：目标必须为本公会成员且非当前会长；写库检查影响行数。
     */
    public CompletableFuture<Boolean> transferGuildLeadershipAsync(int guildId, UUID newLeaderUuid, String newLeaderName,
                                                                   UUID requesterUuid) {
        if (newLeaderUuid == null) {
            return CompletableFuture.completedFuture(false);
        }
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            if (requesterUuid != null && !canTransferLeadership(guild, requesterUuid)) {
                return CompletableFuture.completedFuture(false);
            }
            UUID oldLeaderUuid = guild.getLeaderUuid();
            if (newLeaderUuid.equals(oldLeaderUuid)) {
                return CompletableFuture.completedFuture(false);
            }
            return getGuildMemberAsync(guildId, newLeaderUuid).thenCompose(newMember -> {
                if (newMember == null) {
                    return CompletableFuture.completedFuture(false);
                }
                String resolvedName = (newLeaderName != null && !newLeaderName.isEmpty())
                        ? newLeaderName : newMember.getPlayerName();
                return CompletableFuture.supplyAsync(() -> {
                    try (Connection conn = databaseManager.getConnection()) {
                        conn.setAutoCommit(false);
                        try {
                            // 1. 更新 guilds 表新会长
                            int guildUpdated = repos.guilds().updateLeader(conn, guildId, newLeaderUuid, resolvedName);
                            if (guildUpdated <= 0) {
                                conn.rollback();
                                return false;
                            }
                            // 2. 原会长降级为成员
                            repos.members().demoteLeaderToMember(conn, guildId, oldLeaderUuid);
                            // 3. 新会长升级
                            int newLeaderUpdated = repos.members().promoteMemberToLeader(conn, guildId, newLeaderUuid);
                            if (newLeaderUpdated <= 0) {
                                conn.rollback();
                                return false;
                            }
                            conn.commit();

                            // 刷新权限缓存
                            refreshPlayerPermissions(oldLeaderUuid);
                            refreshPlayerPermissions(newLeaderUuid);

                            // 触发角色变更事件
                            String oldLeaderName = guild.getLeaderName();
                            fireMemberRoleChange(guildId, guild.getName(), oldLeaderUuid, oldLeaderName, "LEADER", "MEMBER");
                            fireMemberRoleChange(guildId, guild.getName(), newLeaderUuid, resolvedName,
                                    newMember.getRole().name(), "LEADER");

                            // 记录日志
                            String actor = newLeaderUuid.toString();
                            String actorName = resolvedName;
                            if (requesterUuid != null) {
                                actor = requesterUuid.toString();
                                try {
                                    org.bukkit.OfflinePlayer requester = plugin.getServer().getOfflinePlayer(requesterUuid);
                                    if (requester.getName() != null) {
                                        actorName = requester.getName();
                                    }
                                } catch (Exception e) {
                                    logger.log(Level.FINE,
                                            "Could not resolve requester name for " + requesterUuid + ": " + e.getMessage());
                                }
                            }
                            String desc = "会长由 " + oldLeaderName + " 转移给 " + resolvedName;
                            logGuildActionAsync(guildId, guild.getName(), actor, actorName,
                                    GuildLog.LogType.LEADER_TRANSFERRED, desc, desc);

                            return true;
                        } catch (SQLException e) {
                            conn.rollback();
                            logger.severe("Error transferring leadership: " + e.getMessage());
                            return false;
                        }
                    } catch (SQLException e) {
                        logger.severe("Database error transferring leadership: " + e.getMessage());
                        return false;
                    }
                });
            });
        });
    }

    /**
     * 获取玩家公会 (异步)
     */
    public CompletableFuture<Guild> getPlayerGuildAsync(UUID playerUuid) {
        return repos.guilds().findByPlayerUuidAsync(playerUuid);
    }
    
    /**
     * 获取玩家公会 (同步包装器；优先短 TTL 缓存)
     */
    public Guild getPlayerGuild(UUID playerUuid) {
        try {
            var cache = plugin.getGuildPlayerDataCache();
            if (cache != null) {
                var hit = cache.getIfPresent(playerUuid);
                if (hit != null) {
                    return hit.guild;
                }
                // Load-through populates guild+member in one round-trip pair.
                return cache.getGuild(playerUuid);
            }
            return getPlayerGuildAsync(playerUuid).get();
        } catch (Exception e) {
            logger.severe("Exception fetching player guild: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取公会成员 (异步)
     */
    public CompletableFuture<GuildMember> getGuildMemberAsync(UUID playerUuid) {
        return repos.members().findByPlayerUuidAsync(playerUuid);
    }
    
    /**
     * 获取公会成员 (同步包装器；优先短 TTL 缓存)
     */
    public GuildMember getGuildMember(UUID playerUuid) {
        try {
            var cache = plugin.getGuildPlayerDataCache();
            if (cache != null) {
                var hit = cache.getIfPresent(playerUuid);
                if (hit != null) {
                    return hit.member;
                }
                return cache.getMember(playerUuid);
            }
            return getGuildMemberAsync(playerUuid).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild members: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取公会成员数量 (异步)
     */
    public CompletableFuture<Integer> getGuildMemberCountAsync(int guildId) {
        return repos.members().countByGuildIdAsync(guildId);
    }
    
    /**
     * 获取公会成员数量 (同步包装器)
     */
    public int getGuildMemberCount(int guildId) {
        try {
            return getGuildMemberCountAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild member count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取公会所有成员 (异步)
     */
    public CompletableFuture<List<GuildMember>> getGuildMembersAsync(int guildId) {
        return repos.members().findAllByGuildIdAsync(guildId);
    }
    
    /**
     * 获取公会所有成员 (同步包装器)
     */
    public List<GuildMember> getGuildMembers(int guildId) {
        try {
            return getGuildMembersAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild member list: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据ID获取公会 (异步)
     */
    public CompletableFuture<Guild> getGuildByIdAsync(int guildId) {
        return repos.guilds().findByIdAsync(guildId);
    }
    
    /**
     * 根据ID获取公会 (同步包装器)
     */
    public Guild getGuildById(int guildId) {
        try {
            return getGuildByIdAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild by ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据名称获取公会 (异步)
     */
    public CompletableFuture<Guild> getGuildByNameAsync(String name) {
        return repos.guilds().findByNameAsync(name);
    }
    
    /**
     * 根据名称获取公会 (同步包装器)
     */
    public Guild getGuildByName(String name) {
        try {
            return getGuildByNameAsync(name).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild by name: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据标签获取公会 (异步)
     */
    public CompletableFuture<Guild> getGuildByTagAsync(String tag) {
        return repos.guilds().findByTagAsync(tag);
    }
    
    /**
     * 根据标签获取公会 (同步包装器)
     */
    public Guild getGuildByTag(String tag) {
        try {
            return getGuildByTagAsync(tag).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild by tag: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取所有公会 (异步)
     */
    public CompletableFuture<List<Guild>> getAllGuildsAsync() {
        return repos.guilds().findAllAsync();
    }
    
    /**
     * 获取所有公会 (同步包装器)
     */
    public List<Guild> getAllGuilds() {
        try {
            return getAllGuildsAsync().get();
        } catch (Exception e) {
            logger.severe("Exception fetching all guilds: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查是否为公会会长
     */
    public boolean isGuildLeader(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getRole() == GuildMember.Role.LEADER;
    }
    
    /**
     * 检查是否为指定公会的会长
     */
    public boolean isGuildLeader(UUID playerUuid, int guildId) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getGuildId() == guildId && member.getRole() == GuildMember.Role.LEADER;
    }
    
    /**
     * 检查是否为公会官员
     */
    public boolean isGuildOfficer(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getRole() == GuildMember.Role.OFFICER;
    }
    
    /**
     * 检查是否有公会权限
     */
    public boolean hasGuildPermission(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER);
    }

    /** 在线玩家是否拥有 guild.admin */
    public boolean isGuildAdmin(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        Player player = plugin.getServer().getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return false;
        }
        return plugin.getPermissionManager().hasPermission(player, "guild.admin");
    }

    private boolean canTransferLeadership(Guild guild, UUID requesterUuid) {
        if (requesterUuid == null || guild == null) {
            return false;
        }
        if (isGuildAdmin(requesterUuid)) {
            return true;
        }
        GuildMember requester = getGuildMember(requesterUuid);
        return requester != null
                && requester.getGuildId() == guild.getId()
                && requester.getRole() == GuildMember.Role.LEADER
                && requesterUuid.equals(guild.getLeaderUuid());
    }
    
    /**
     * 提交申请 (异步)
     */
    public CompletableFuture<Boolean> submitApplicationAsync(int guildId, UUID playerUuid, String playerName, String message) {
        return repos.applications().hasPendingAsync(playerUuid, guildId).thenCompose(hasPending -> {
            if (hasPending) {
                return CompletableFuture.completedFuture(false);
            }
            return repos.applications().insertAsync(guildId, playerUuid, playerName, message,
                    GuildApplication.ApplicationStatus.PENDING, nowString()).thenApply(inserted -> {
                if (inserted) {
                    QuietLog.system("Player " + playerName + " submitted a join application (guild ID: " + guildId + ")");
                    getGuildByIdAsync(guildId).thenAccept(guild -> {
                        if (guild != null) {
                            logGuildActionAsync(guildId, guild.getName(), playerUuid.toString(), playerName,
                                    GuildLog.LogType.APPLICATION_SUBMITTED, "申请提交", "申请消息: " + message);
                            GuildApplication application = new GuildApplication(guildId, playerUuid, playerName, message);
                            NotifyUtils.notifyLeaderNewApplication(plugin, guild, application);
                        }
                    });
                }
                return inserted;
            });
        });
    }
    
    /**
     * 提交申请 (同步包装器)
     */
    public boolean submitApplication(int guildId, UUID playerUuid, String playerName, String message) {
        try {
            return submitApplicationAsync(guildId, playerUuid, playerName, message).get();
        } catch (Exception e) {
            logger.severe("Exception submitting application: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 处理申请 (异步)
     * 审批通过前先检查公会是否满员，避免申请状态已更新但成员无法加入的情况。
     */
    public CompletableFuture<Boolean> processApplicationAsync(int applicationId, GuildApplication.ApplicationStatus status, UUID processorUuid) {
        return getApplicationByIdAsync(applicationId).thenCompose(application -> {
            if (application == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return getGuildMemberAsync(processorUuid).thenCompose(processor -> {
                // 检查处理者权限
                if (processor == null || processor.getGuildId() != application.getGuildId() || 
                    (processor.getRole() != GuildMember.Role.LEADER && processor.getRole() != GuildMember.Role.OFFICER)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // 审批通过前预检查公会人数上限
                if (status == GuildApplication.ApplicationStatus.APPROVED) {
                    return isGuildFullAsync(application.getGuildId()).thenCompose(isFull -> {
                        if (isFull) {
                            QuietLog.system("Application approval rejected: guild " + application.getGuildId() 
                                + " is at member capacity, applicant=" + application.getPlayerName());
                            return CompletableFuture.completedFuture(false);
                        }
                        return doProcessApplication(applicationId, application, status, processor, processorUuid);
                    });
                }
                
                return doProcessApplication(applicationId, application, status, processor, processorUuid);
            });
        });
    }
    
    /**
     * 执行申请处理的数据库操作（内部方法）
     */
    private CompletableFuture<Boolean> doProcessApplication(int applicationId, GuildApplication application,
            GuildApplication.ApplicationStatus status, GuildMember processor, UUID processorUuid) {
        return repos.applications().updateStatusAsync(applicationId, status).thenApply(updated -> {
            if (updated) {
                QuietLog.system("Application processed: " + application.getPlayerName() + " -> " + status.name());
                getGuildByIdAsync(application.getGuildId()).thenAccept(guild -> {
                    if (guild != null) {
                        GuildLog.LogType logType = status == GuildApplication.ApplicationStatus.APPROVED ?
                                GuildLog.LogType.APPLICATION_ACCEPTED : GuildLog.LogType.APPLICATION_REJECTED;
                        String description = status == GuildApplication.ApplicationStatus.APPROVED ? "申请接受" : "申请拒绝";
                        String details = "申请人: " + application.getPlayerName() + ", 处理者: " + processor.getPlayerName();
                        logGuildActionAsync(application.getGuildId(), guild.getName(),
                                processorUuid.toString(), processor.getPlayerName(),
                                logType, description, details);
                    }
                });
            }
            return updated;
        }).thenCompose(success -> {
            if (success && status == GuildApplication.ApplicationStatus.APPROVED) {
                // 如果申请被通过，自动添加成员（addGuildMemberAsync 内部有二次容量校验）
                return addGuildMemberAsync(application.getGuildId(), application.getPlayerUuid(), 
                                          application.getPlayerName(), GuildMember.Role.MEMBER);
            }
            return CompletableFuture.completedFuture(success);
        });
    }
    
    /**
     * 处理申请 (同步包装器)
     */
    public boolean processApplication(int applicationId, GuildApplication.ApplicationStatus status, UUID processorUuid) {
        try {
            return processApplicationAsync(applicationId, status, processorUuid).get();
        } catch (Exception e) {
            logger.severe("Exception processing application: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查是否有待处理的申请 (异步)
     */
    public CompletableFuture<Boolean> hasPendingApplicationAsync(UUID playerUuid, int guildId) {
        return repos.applications().hasPendingAsync(playerUuid, guildId);
    }
    
    /**
     * 检查是否有待处理的申请 (同步包装器)
     */
    public boolean hasPendingApplication(UUID playerUuid, int guildId) {
        try {
            return hasPendingApplicationAsync(playerUuid, guildId).get();
        } catch (Exception e) {
            logger.severe("Exception checking pending applications: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取公会申请列表 (异步)
     */
    public CompletableFuture<List<GuildApplication>> getGuildApplicationsAsync(int guildId) {
        return repos.applications().findAllByGuildIdAsync(guildId);
    }
    
    /**
     * 获取公会申请列表 (同步包装器)
     */
    public List<GuildApplication> getGuildApplications(int guildId) {
        try {
            return getGuildApplicationsAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild application list: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取玩家申请列表 (异步)
     */
    public CompletableFuture<List<GuildApplication>> getPlayerApplicationsAsync(UUID playerUuid) {
        return repos.applications().findAllByPlayerUuidAsync(playerUuid);
    }
    
    /**
     * 获取玩家申请列表 (同步包装器)
     */
    public List<GuildApplication> getPlayerApplications(UUID playerUuid) {
        try {
            return getPlayerApplicationsAsync(playerUuid).get();
        } catch (Exception e) {
            logger.severe("Exception fetching player application list: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据ID获取申请 (异步)
     */
    public CompletableFuture<GuildApplication> getApplicationByIdAsync(int applicationId) {
        return repos.applications().findByIdAsync(applicationId);
    }
    
    /**
     * 根据ID获取申请 (同步包装器)
     */
    public GuildApplication getApplicationById(int applicationId) {
        try {
            return getApplicationByIdAsync(applicationId).get();
        } catch (Exception e) {
            logger.severe("Exception fetching application by ID: " + e.getMessage());
            return null;
        }
    }
    
     /**
      * 设置公会家 (异步)
      */
     public CompletableFuture<Boolean> setGuildHomeAsync(int guildId, org.bukkit.Location location, UUID requesterUuid) {
         return getGuildByIdAsync(guildId).thenCompose(guild -> {
             if (guild == null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return getGuildMemberAsync(requesterUuid).thenCompose(member -> {
                 // 检查权限 - 只有会长可以设置家
                 if (member == null || member.getGuildId() != guildId || member.getRole() != GuildMember.Role.LEADER) {
                     return CompletableFuture.completedFuture(false);
                 }
                 
                 return CompletableFuture.supplyAsync(() -> {
                     if (repos.guilds().updateHome(guildId, location.getWorld().getName(),
                             location.getX(), location.getY(), location.getZ(),
                             location.getYaw(), location.getPitch(), nowString())) {
                         QuietLog.system("Guild home set successfully: " + guild.getName() + " (ID: " + guildId + ")");
                         return true;
                     }
                     return false;
                 });
             });
         });
     }
     
     /**
      * 设置公会家 (同步包装器)
      */
     public boolean setGuildHome(int guildId, org.bukkit.Location location, UUID requesterUuid) {
         try {
             return setGuildHomeAsync(guildId, location, requesterUuid).get();
         } catch (Exception e) {
             logger.severe("Exception setting guild home: " + e.getMessage());
             return false;
         }
     }
     
     /**
      * 获取公会家位置 (异步)
      */
     public CompletableFuture<org.bukkit.Location> getGuildHomeAsync(int guildId) {
         return getGuildByIdAsync(guildId).thenApply(guild -> {
             if (guild == null || !guild.hasHome()) {
                 return null;
             }
             
             org.bukkit.World world = plugin.getServer().getWorld(guild.getHomeWorld());
             if (world == null) {
                 logger.warning("Guild home world does not exist: " + guild.getHomeWorld());
                 return null;
             }
             
             return guild.getHomeLocation(world);
         });
     }
     
     /**
      * 获取公会家位置 (同步包装器)
      */
     public org.bukkit.Location getGuildHome(int guildId) {
         try {
             return getGuildHomeAsync(guildId).get();
         } catch (Exception e) {
             logger.severe("Exception fetching guild home: " + e.getMessage());
             return null;
         }
     }
     
     // ==================== 邀请系统 ====================
     
     /**
      * 发送邀请 (异步)
      */
     public CompletableFuture<Boolean> sendInvitationAsync(int guildId, UUID inviterUuid, String inviterName, UUID targetUuid, String targetName) {
         return getGuildMemberAsync(guildId, inviterUuid).thenCompose(inviterMember -> {
             if (inviterMember == null || !inviterMember.getRole().canInvite()) {
                 return CompletableFuture.completedFuture(false);
             }
             return getPlayerGuildAsync(targetUuid).thenCompose(existingGuild -> {
             if (existingGuild != null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return getPendingInvitationAsync(targetUuid, guildId).thenCompose(existingInvitation -> {
                 if (existingInvitation != null) {
                     return CompletableFuture.completedFuture(false);
                 }
                 
                 return repos.invitations().insertAsync(guildId, targetUuid, targetName, inviterUuid, inviterName,
                         "PENDING", plusMinutesString(30), nowString()).thenCompose(ok -> {
                     if (!Boolean.TRUE.equals(ok)) {
                         return CompletableFuture.completedFuture(false);
                     }
                     QuietLog.system("Invitation sent successfully: " + inviterName + " -> " + targetName + " (guild ID: " + guildId + ")");
                     return getGuildByIdAsync(guildId).thenCompose(g -> {
                         String guildName = g != null ? g.getName() : ("#" + guildId);
                         return logGuildActionAsync(guildId, guildName,
                                 inviterUuid.toString(), inviterName,
                                 GuildLog.LogType.INVITATION_SENT,
                                 "Invitation sent",
                                 "target: " + targetName + " (" + targetUuid + ")")
                                 .thenApply(v -> true);
                     });
                 });
             });
         });
         });
     }
     
     /**
      * 发送邀请 (同步包装器)
      */
     public boolean sendInvitation(int guildId, UUID inviterUuid, String inviterName, UUID targetUuid, String targetName) {
         try {
             return sendInvitationAsync(guildId, inviterUuid, inviterName, targetUuid, targetName).get();
         } catch (Exception e) {
             logger.severe("Exception sending invitation: " + e.getMessage());
             return false;
         }
     }
     
    /**
     * 处理邀请 (异步) - 通过inviterUuid查找
     */
    public CompletableFuture<Boolean> processInvitationAsync(UUID targetUuid, UUID inviterUuid, boolean accept) {
        return getPendingInvitationAsync(targetUuid, inviterUuid).thenCompose(invitation -> {
            if (invitation == null) {
                return CompletableFuture.completedFuture(false);
            }
            return processInvitationDirectAsync(invitation, accept);
        });
    }
    
    /**
     * 处理邀请 (异步) - 直接处理邀请对象
     * 接受邀请前先检查公会是否满员，避免邀请状态已更新但成员无法加入的情况。
     */
    public CompletableFuture<Boolean> processInvitationDirectAsync(GuildInvitation invitation, boolean accept) {
        DebugLog.info(logger, "[Process-Debug] Starting invitation processing: id=" + invitation.getId() + ", guildId=" + invitation.getGuildId() + ", target=" + invitation.getTargetUuid() + ", accept=" + accept);
        
        // 接受邀请前预检查公会人数上限
        if (accept) {
            return isGuildFullAsync(invitation.getGuildId()).thenCompose(isFull -> {
                if (isFull) {
                    DebugLog.info(logger, "[Process-Debug] Invitation accept rejected: guild " + invitation.getGuildId() 
                        + " is at member capacity, target=" + invitation.getTargetUuid());
                    return CompletableFuture.completedFuture(false);
                }
                return doProcessInvitation(invitation, accept);
            });
        }
        
        return doProcessInvitation(invitation, accept);
    }
    
    /**
     * 执行邀请处理的数据库操作（内部方法）
     */
    private CompletableFuture<Boolean> doProcessInvitation(GuildInvitation invitation, boolean accept) {
        String status = accept ? "ACCEPTED" : "DECLINED";
        return repos.invitations().updateStatusAsync(invitation.getId(), status).thenApply(updated -> {
            if (updated) {
                DebugLog.info(logger, "[Process-Debug] Invitation status updated: " + invitation.getTargetUuid() + " -> " + status);
            } else {
                logger.warning("[Process-Debug] Invitation status update failed, no rows affected: id=" + invitation.getId());
            }
            return updated;
        }).thenCompose(success -> {
            if (!Boolean.TRUE.equals(success)) {
                return CompletableFuture.completedFuture(false);
            }
            GuildLog.LogType inviteLog = accept
                    ? GuildLog.LogType.INVITATION_ACCEPTED
                    : GuildLog.LogType.INVITATION_REJECTED;
            return getGuildByIdAsync(invitation.getGuildId()).thenCompose(g -> {
                String guildName = g != null ? g.getName() : ("#" + invitation.getGuildId());
                return logGuildActionAsync(invitation.getGuildId(), guildName,
                        invitation.getTargetUuid().toString(), invitation.getTargetName(),
                        inviteLog,
                        accept ? "Invitation accepted" : "Invitation declined",
                        "inviter: " + invitation.getInviterName()
                                + " (" + invitation.getInviterUuid() + ")")
                        .thenApply(v -> g);
            }).thenCompose(g -> {
                if (!accept) {
                    return CompletableFuture.completedFuture(true);
                }
                DebugLog.info(logger, "[Process-Debug] Preparing to add player to guild: guildId="
                        + invitation.getGuildId() + ", player=" + invitation.getTargetUuid());
                return addGuildMemberAsync(invitation.getGuildId(), invitation.getTargetUuid(),
                        invitation.getTargetName(), GuildMember.Role.MEMBER)
                        .thenCompose(addSuccess -> {
                            if (addSuccess) {
                                DebugLog.info(logger, "[Process-Debug] Player added, dispatching event");
                                if (g != null) {
                                    fireMemberJoin(g.getId(), g.getName(),
                                            invitation.getTargetUuid(), invitation.getTargetName());
                                }
                                return CompletableFuture.completedFuture(true);
                            }
                            logger.warning("[Process-Debug] Failed to add player to guild");
                            return CompletableFuture.completedFuture(false);
                        });
            });
        });
    }
     
     /**
      * 处理邀请 (同步包装器)
      */
     public boolean processInvitation(UUID targetUuid, UUID inviterUuid, boolean accept) {
         try {
             return processInvitationAsync(targetUuid, inviterUuid, accept).get();
         } catch (Exception e) {
             logger.severe("Exception processing invitation: " + e.getMessage());
             return false;
         }
     }
     
     /**
      * 获取待处理邀请 (异步)
      */
     public CompletableFuture<GuildInvitation> getPendingInvitationAsync(UUID targetUuid, UUID inviterUuid) {
         return repos.invitations().findPendingByTargetAndInviterAsync(targetUuid, inviterUuid, nowString());
     }
     
     /**
      * 获取待处理邀请 (同步包装器)
      */
     public GuildInvitation getPendingInvitation(UUID targetUuid, UUID inviterUuid) {
         try {
             return getPendingInvitationAsync(targetUuid, inviterUuid).get();
         } catch (Exception e) {
             logger.severe("Exception fetching invitation: " + e.getMessage());
             return null;
         }
     }
     
     /**
      * 获取玩家的待处理邀请 (异步)
      */
    public CompletableFuture<GuildInvitation> getPendingInvitationAsync(UUID targetUuid, int guildId) {
        return repos.invitations().findPendingByTargetAndGuildAsync(targetUuid, guildId, nowString());
    }
     
     /**
      * 获取玩家的待处理邀请 (同步包装器)
      */
     public GuildInvitation getPendingInvitation(UUID targetUuid, int guildId) {
         try {
             return getPendingInvitationAsync(targetUuid, guildId).get();
         } catch (Exception e) {
             logger.severe("Exception fetching invitation: " + e.getMessage());
             return null;
         }
     }
     
     /**
      * 获取待处理申请 (异步)
      */
     public CompletableFuture<List<GuildApplication>> getPendingApplicationsAsync(int guildId) {
         return repos.applications().findPendingByGuildIdAsync(guildId);
     }
     
     /**
      * 获取申请历史 (异步)
      */
     public CompletableFuture<List<GuildApplication>> getApplicationHistoryAsync(int guildId) {
         return repos.applications().findHistoryByGuildIdAsync(guildId);
     }
    
    /**
     * 获取玩家所有待处理的邀请 (异步)
     */
    public CompletableFuture<List<GuildInvitation>> getPendingInvitationsAsync(UUID playerUuid) {
        return repos.invitations().findAllPendingByPlayerAsync(playerUuid, nowString());
    }
    
    /**
     * 清理过期的公会邀请 (异步) - 将过期邀请状态更新为EXPIRED
     * 建议定时调用，避免数据库中积累过多过期邀请
     */
    public CompletableFuture<Integer> cleanupExpiredInvitationsAsync() {
        return repos.invitations().markExpiredBeforeAsync(nowString()).thenApply(affectedRows -> {
            if (affectedRows > 0) {
                QuietLog.system("Cleaned up " + affectedRows + " expired guild invitations");
            }
            return affectedRows;
        });
    }
    
    /**
     * 清理旧的已处理邀请 (异步) - 删除已过期超过指定天数的邀请记录
     * @param days 保留天数，超过此天数的已处理邀请（ACCEPTED/DECLINED/EXPIRED）将被删除
     */
    public CompletableFuture<Integer> cleanupOldProcessedInvitationsAsync(int days) {
        return repos.invitations().deleteOldProcessedAsync(days).thenApply(affectedRows -> {
            if (affectedRows > 0) {
                QuietLog.system("Cleaned up " + affectedRows + " old processed invitation records");
            }
            return affectedRows;
        });
    }
    
    /**
     * 获取公会成员 (异步) - 重载方法，接受guildId参数
     */
    public CompletableFuture<GuildMember> getGuildMemberAsync(int guildId, UUID playerUuid) {
         return repos.members().findByGuildAndPlayerUuidAsync(guildId, playerUuid);
     }
     
     /**
      * 更新公会描述 (异步)
      */
     public CompletableFuture<Boolean> updateGuildDescriptionAsync(int guildId, String description) {
         return CompletableFuture.supplyAsync(() -> repos.guilds().updateDescription(guildId, description));
     }
     
     // ==================== 公会关系系统 ====================
     
     /**
      * 创建公会关系 (异步)
      */
     public CompletableFuture<Boolean> createGuildRelationAsync(int guild1Id, int guild2Id, String guild1Name, String guild2Name,
                                                              GuildRelation.RelationType type, UUID initiatorUuid, String initiatorName) {
         return repos.relations().insertAsync(guild1Id, guild2Id, guild1Name, guild2Name, type,
                 initiatorUuid, initiatorName, plusDaysString(7)).thenCompose(ok -> {
             if (!Boolean.TRUE.equals(ok)) {
                 return CompletableFuture.completedFuture(false);
             }
             String details = type.name() + " <-> " + guild2Name;
             String detailsPeer = type.name() + " <-> " + guild1Name;
             CompletableFuture<Boolean> a = logGuildActionAsync(guild1Id, guild1Name,
                     initiatorUuid.toString(), initiatorName,
                     GuildLog.LogType.RELATION_CREATED, "Relation request created", details);
             CompletableFuture<Boolean> b = logGuildActionAsync(guild2Id, guild2Name,
                     initiatorUuid.toString(), initiatorName,
                     GuildLog.LogType.RELATION_CREATED, "Relation request received", detailsPeer);
             return a.thenCombine(b, (x, y) -> true);
         });
     }
     
     /**
      * 更新公会关系状态 (异步)
      */
     public CompletableFuture<Boolean> updateGuildRelationStatusAsync(int relationId, GuildRelation.RelationStatus status) {
         return getGuildRelationByIdAsync(relationId).thenCompose(relation -> {
             if (relation == null) {
                 return CompletableFuture.completedFuture(false);
             }
             return repos.relations().updateStatusAsync(relationId, status, nowString()).thenCompose(ok -> {
                 if (!Boolean.TRUE.equals(ok)) {
                     return CompletableFuture.completedFuture(false);
                 }
                 GuildLog.LogType logType = status == GuildRelation.RelationStatus.ACTIVE
                         ? GuildLog.LogType.RELATION_ACCEPTED
                         : GuildLog.LogType.RELATION_REJECTED;
                 String details = relation.getType().name() + " status=" + status.name()
                         + " peers=" + relation.getGuild1Name() + "/" + relation.getGuild2Name();
                 String actor = relation.getInitiatorUuid() != null
                         ? relation.getInitiatorUuid().toString() : "SYSTEM";
                 String actorName = relation.getInitiatorName() != null
                         ? relation.getInitiatorName() : "system";
                 CompletableFuture<Boolean> a = logGuildActionAsync(relation.getGuild1Id(), relation.getGuild1Name(),
                         actor, actorName, logType, "Relation status updated", details);
                 CompletableFuture<Boolean> b = logGuildActionAsync(relation.getGuild2Id(), relation.getGuild2Name(),
                         actor, actorName, logType, "Relation status updated", details);
                 return a.thenCombine(b, (x, y) -> true);
             });
         });
     }
     
     /**
      * 获取公会关系 (异步)
      */
     public CompletableFuture<GuildRelation> getGuildRelationAsync(int guild1Id, int guild2Id) {
         return repos.relations().findByGuildPairAsync(guild1Id, guild2Id);
     }
     
     /**
      * 获取公会的所有关系 (异步)
      */
     public CompletableFuture<List<GuildRelation>> getGuildRelationsAsync(int guildId) {
         return repos.relations().findAllByGuildIdAsync(guildId);
     }
     
     /**
      * 按 ID 获取公会关系 (异步)
      */
     public CompletableFuture<GuildRelation> getGuildRelationByIdAsync(int relationId) {
         return repos.relations().findByIdAsync(relationId);
     }

     /**
      * 删除公会关系 (异步)
      */
     public CompletableFuture<Boolean> deleteGuildRelationAsync(int relationId) {
         return getGuildRelationByIdAsync(relationId).thenCompose(relation -> {
             if (relation == null) {
                 return CompletableFuture.completedFuture(false);
             }
             return repos.relations().deleteByIdAsync(relationId).thenCompose(ok -> {
                 if (!Boolean.TRUE.equals(ok)) {
                     return CompletableFuture.completedFuture(false);
                 }
                 String details = relation.getType().name()
                         + " peers=" + relation.getGuild1Name() + "/" + relation.getGuild2Name();
                 String actor = relation.getInitiatorUuid() != null
                         ? relation.getInitiatorUuid().toString() : "SYSTEM";
                 String actorName = relation.getInitiatorName() != null
                         ? relation.getInitiatorName() : "system";
                 CompletableFuture<Boolean> a = logGuildActionAsync(relation.getGuild1Id(), relation.getGuild1Name(),
                         actor, actorName, GuildLog.LogType.RELATION_DELETED, "Relation deleted", details);
                 CompletableFuture<Boolean> b = logGuildActionAsync(relation.getGuild2Id(), relation.getGuild2Name(),
                         actor, actorName, GuildLog.LogType.RELATION_DELETED, "Relation deleted", details);
                 return a.thenCombine(b, (x, y) -> true);
             });
         });
     }
     
     // ==================== 公会经济系统 ====================
     
     /**
      * 初始化公会经济 (异步)
      */
     public CompletableFuture<Boolean> initializeGuildEconomyAsync(int guildId) {
         return repos.economy().insertAsync(guildId);
     }
     
     /**
      * 获取公会经济信息 (异步)
      */
     public CompletableFuture<GuildEconomy> getGuildEconomyAsync(int guildId) {
         return repos.economy().findByGuildIdAsync(guildId);
     }
     
     /**
      * 更新公会经济 (异步)
      */
     public CompletableFuture<Boolean> updateGuildEconomyAsync(int guildId, double balance, int level, double experience, double maxExperience, int maxMembers) {
         return repos.economy().updateAsync(guildId, balance, level, experience, maxExperience, maxMembers, nowString());
     }
     
     /**
      * 添加公会贡献记录 (异步)
      */
     public CompletableFuture<Boolean> addGuildContributionAsync(int guildId, UUID playerUuid, String playerName,
                                                               double amount, GuildContribution.ContributionType type, String description) {
         return repos.contributions().insertAsync(guildId, playerUuid, playerName, amount, type, description);
     }
     
     /**
      * 获取公会贡献记录 (异步)
      */
     public CompletableFuture<List<GuildContribution>> getGuildContributionsAsync(int guildId) {
         return repos.contributions().findAllByGuildIdAsync(guildId);
     }
     
     /**
      * 获取玩家贡献记录 (异步)
      */
     public CompletableFuture<List<GuildContribution>> getPlayerContributionsAsync(UUID playerUuid) {
         return repos.contributions().findAllByPlayerUuidAsync(playerUuid);
     }

    /**
     * 按玩家聚合公会净贡献（WITHDRAW 为负，其余为正）。
     * 返回 Map&lt;playerUuid, netAmount&gt;。
     */
    public CompletableFuture<Map<UUID, Double>> getGuildContributionNetByPlayerAsync(int guildId) {
        return repos.contributions().computeNetByPlayerAsync(guildId);
    }

    /**
     * 获取公会中各成员的存款总额（聚合查询，仅 DEPOSIT 类型）
     * 返回 List<GuildContribution>，每个玩家一条，amount 为累计存款总额。
     */
    public CompletableFuture<List<GuildContribution>> getGuildContributionTotalsAsync(int guildId) {
        return repos.contributions().computeDepositTotalsAsync(guildId);
    }
    
     // ==================== 公会经济管理方法 ====================
     
     /**
      * 更新公会余额 (异步)
      */
     public CompletableFuture<Boolean> updateGuildBalanceAsync(int guildId, double balance) {
        return updateGuildBalanceAsync(guildId, balance, null, null);
    }

    /**
     * 管理员调整公会余额（须 guild.admin；GUI Confirm 与纵深防御入口）
     */
    public CompletableFuture<Boolean> updateGuildBalanceByAdminAsync(int guildId, double balance,
                                                                    UUID adminUuid, String adminName) {
        if (!isGuildAdmin(adminUuid)) {
            return CompletableFuture.completedFuture(false);
        }
        return updateGuildBalanceAsync(guildId, balance,
                adminUuid != null ? adminUuid.toString() : null, adminName);
    }

    public CompletableFuture<Boolean> updateGuildBalanceAsync(int guildId, double balance,
                                                               String operatorUuid, String operatorName) {
         return getGuildByIdAsync(guildId).thenCompose(guild -> {
             if (guild == null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return CompletableFuture.supplyAsync(() -> {
                 if (repos.guilds().updateBalance(guildId, balance, nowString())) {
                     QuietLog.system("Guild balance updated: " + guild.getName() + " (ID: " + guildId + ") new balance: " + balance);

                     // 异步检查是否需要自动升级，不阻塞当前操作
                     CompletableFuture.runAsync(() -> {
                         checkAndUpgradeGuildLevel(guildId, balance);
                     });

                     // 记录资金变更日志
                     double oldBalance = guild.getBalance();
                    double change = balance - oldBalance;
                    if (change != 0) {
                        GuildLog.LogType logType = change > 0 ? GuildLog.LogType.FUND_DEPOSITED : GuildLog.LogType.FUND_WITHDRAWN;
                        String description = change > 0 ? "Fund deposited" : "Fund withdrawn";
                        String details = "Change: " + (change > 0 ? "+" : "") + change + " coins, New balance: " + balance + " coins";

                        String logUuid = (operatorUuid != null && !operatorUuid.isEmpty()) ? operatorUuid : "SYSTEM";
                        String logName = (operatorName != null && !operatorName.isEmpty()) ? operatorName : "System";
                        logGuildActionAsync(guildId, guild.getName(), logUuid, logName,
                            logType, description, details);
                    }

                     return true;
                 }
                 return false;
             });
         });
     }
    
    /**
     * 更新公会等级 (异步)
     */
    public CompletableFuture<Boolean> updateGuildLevelAsync(int guildId, int level) {
        return CompletableFuture.supplyAsync(() -> repos.guilds().updateLevel(guildId, level));
    }
    
    /**
     * 更新公会最大成员数 (异步)
     */
    public CompletableFuture<Boolean> updateGuildMaxMembersAsync(int guildId, int maxMembers) {
        return CompletableFuture.supplyAsync(() -> repos.guilds().updateMaxMembers(guildId, maxMembers));
    }
    
    /**
     * 更新公会冻结状态 (异步)
     */
    public CompletableFuture<Boolean> updateGuildFrozenStatusAsync(int guildId, boolean frozen) {
        return updateGuildFrozenStatusAsync(guildId, frozen, null);
    }

    public CompletableFuture<Boolean> updateGuildFrozenStatusAsync(int guildId, boolean frozen, UUID operatorUuid) {
        if (operatorUuid != null && !isGuildAdmin(operatorUuid)) {
            return CompletableFuture.completedFuture(false);
        }
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return CompletableFuture.supplyAsync(() -> {
                if (repos.guilds().updateFrozen(guildId, frozen)) {
                    GuildLog.LogType logType = frozen ? GuildLog.LogType.GUILD_FROZEN : GuildLog.LogType.GUILD_UNFROZEN;
                    String description = frozen ? "公会冻结" : "公会解冻";
                    String operatorId = operatorUuid != null ? operatorUuid.toString() : "SYSTEM";
                    String operatorName = "System";
                    if (operatorUuid != null) {
                        Player op = plugin.getServer().getPlayer(operatorUuid);
                        if (op != null && op.getName() != null) {
                            operatorName = op.getName();
                        }
                    }

                    logGuildActionAsync(guildId, guild.getName(), operatorId, operatorName,
                        logType, description, "操作: " + (frozen ? "冻结" : "解冻"));

                    return true;
                }
                return false;
            });
        });
    }

    /**
     * 直接插入公会成员（不做已有公会检查）。
     * 仅用于建会后插入会长，以避免额外读库造成的连接争用。
     */
    private CompletableFuture<Boolean> addGuildMemberDirectAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        return repos.members().insertAsync(guildId, playerUuid, playerName, role, nowString())
                .thenApply(success -> {
                    if (success) {
                        refreshPlayerPermissions(playerUuid);
                    }
                    return success;
                });
    }

    /**
     * 检查并自动升级公会等级
     */
    private void checkAndUpgradeGuildLevel(int guildId, double currentBalance) {
        getGuildByIdAsync(guildId).thenAccept(guild -> {
            if (guild == null) return;
            
            int currentLevel = guild.getLevel();
            if (currentLevel >= 10) return; // 已达到最高等级
            
            // 检查是否满足升级条件
            double requiredBalance = getRequiredBalanceForLevel(currentLevel);
            if (currentBalance >= requiredBalance) {
                // 自动升级
                int newLevel = currentLevel + 1;
                int newMaxMembers = getMaxMembersForLevel(newLevel);
                
                CompletableFuture.supplyAsync(() -> {
                    if (repos.guilds().updateLevelMaxMembersAndPeak(guildId, newLevel, newMaxMembers, nowString())) {
                        QuietLog.system("Guild auto-upgraded successfully: " + guild.getName() + " (ID: " + guildId + ") level: " + currentLevel + " -> " + newLevel);

                        // 记录升级日志
                        logGuildActionAsync(guildId, guild.getName(), "SYSTEM", "系统",
                            GuildLog.LogType.GUILD_LEVEL_UP, "公会升级", "新等级: " + newLevel + ", 新最大成员数: " + newMaxMembers);

                        return true;
                    }
                    return false;
                });
            }
        }).exceptionally(throwable -> {
            logger.severe("Error checking guild upgrade: " + throwable.getMessage());
            return null;
        });
    }
    
    /**
     * 获取指定等级所需的资金
     */
    private double getRequiredBalanceForLevel(int level) {
        switch (level) {
            case 1: return 5000;
            case 2: return 10000;
            case 3: return 20000;
            case 4: return 35000;
            case 5: return 50000;
            case 6: return 75000;
            case 7: return 100000;
            case 8: return 150000;
            case 9: return 200000;
            default: return Double.MAX_VALUE;
        }
    }
    
    /**
     * 获取指定等级的最大成员数
     */
    private int getMaxMembersForLevel(int level) {
        switch (level) {
            case 1: return 6;
            case 2: return 12;
            case 3: return 18;
            case 4: return 25;
            case 5: return 35;
            case 6: return 45;
            case 7: return 60;
            case 8: return 75;
            case 9: return 90;
            case 10: return 100;
            default: return 100;
        }
    }
    
    /**
     * 获取全局最大成员数上限（从 config.yml 的 guild.max-members 读取）。
     * 作为所有公会的绝对上限，即使等级系统允许更多成员也不能超过此值。
     */
    private int getGlobalMaxMembers() {
        try {
            return plugin.getConfigManager().getMainConfig().getInt("guild.max-members", 100);
        } catch (Exception e) {
            return 100;
        }
    }
    
    /**
     * 获取公会的有效最大成员数。
     * 取公会自身存储的 max_members（由等级决定）与全局配置上限的较小值。
     * 对于已超限的存量公会，此方法仅影响新成员加入，不会踢出已有成员。
     */
    public int getEffectiveMaxMembers(Guild guild) {
        int guildMax = guild.getMaxMembers();
        int globalMax = getGlobalMaxMembers();
        return Math.min(guildMax, globalMax);
    }
    
    /**
     * 检查公会是否已满员 (异步)
     * 用于在审批申请/接受邀请前预检查，避免状态已更新但成员无法加入的情况。
     */
    public CompletableFuture<Boolean> isGuildFullAsync(int guildId) {
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(true);
            }
            return getGuildMemberCountAsync(guildId).thenApply(count -> count >= getEffectiveMaxMembers(guild));
        });
    }
    
    /**
     * 通知公会成员升级成功
     */
    private void notifyGuildMembersOfUpgrade(int guildId, int newLevel, int newMaxMembers) {
        getGuildMembersAsync(guildId).thenAccept(members -> {
            // 在每位成员所在区域线程中发送消息（Folia 实体调度）
            for (GuildMember member : members) {
                Player player = Bukkit.getPlayer(member.getPlayerUuid());
                if (player != null && player.isOnline()) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = plugin.getLanguageManager().getCoreMessage(player, "economy.level-up", "&a公会升级成功！当前等级：{level}", "{level}", String.valueOf(newLevel), "{max_members}", String.valueOf(newMaxMembers));
                        player.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                    });
                }
            }
        }).exceptionally(throwable -> {
            logger.warning("Error notifying guild members of upgrade: " + throwable.getMessage());
            return null;
        });
    }
    
    // ==================== 公会日志系统 ====================
    
    /**
     * 记录公会日志 (异步)
     */
    public CompletableFuture<Boolean> logGuildActionAsync(int guildId, String guildName, String playerUuid, 
                                                        String playerName, GuildLog.LogType logType, 
                                                        String description, String details) {
        return repos.logs().insertAsync(guildId, guildName, playerUuid, playerName, logType, description, details, nowString());
    }
    
    /**
     * 记录公会日志 (同步包装器)
     */
    public boolean logGuildAction(int guildId, String guildName, String playerUuid, String playerName, 
                                GuildLog.LogType logType, String description, String details) {
        try {
            return logGuildActionAsync(guildId, guildName, playerUuid, playerName, logType, description, details).get();
        } catch (Exception e) {
            logger.severe("Exception recording guild log: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取公会日志列表 (异步)
     */
    public CompletableFuture<List<GuildLog>> getGuildLogsAsync(int guildId, int limit, int offset) {
        return repos.logs().findByGuildIdAsync(guildId, limit, offset);
    }
    
    /**
     * 获取公会日志列表 (同步包装器)
     */
    public List<GuildLog> getGuildLogs(int guildId, int limit, int offset) {
        try {
            return getGuildLogsAsync(guildId, limit, offset).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild logs: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取公会日志总数 (异步)
     */
    public CompletableFuture<Integer> getGuildLogsCountAsync(int guildId) {
        return repos.logs().countByGuildIdAsync(guildId);
    }
    
    /**
     * 获取公会日志总数 (同步包装器)
     */
    public int getGuildLogsCount(int guildId) {
        try {
            return getGuildLogsCountAsync(guildId).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild log count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 清理旧日志 (异步)
     */
    public CompletableFuture<Integer> cleanOldLogsAsync(int daysToKeep) {
        String threshold = TimeProvider.nowLocalDateTime().minusDays(daysToKeep)
                .format(TimeProvider.FULL_FORMATTER);
        return repos.logs().deleteOlderThanAsync(threshold).thenApply(affectedRows -> {
            QuietLog.system("Cleaned up " + affectedRows + " old log records");
            return affectedRows;
        });
    }
    
    /**
     * 清理旧日志 (同步包装器)
     */
    public int cleanOldLogs(int daysToKeep) {
        try {
            return cleanOldLogsAsync(daysToKeep).get();
        } catch (Exception e) {
            logger.severe("Exception cleaning up old logs: " + e.getMessage());
            return 0;
        }
    }
}
