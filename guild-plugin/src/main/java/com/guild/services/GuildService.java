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
import com.guild.util.NotifyUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.time.LocalDateTime;
import com.guild.core.time.TimeProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.guild.core.utils.CompatibleScheduler;

public class GuildService {
    
    private final GuildPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    
    public GuildService(GuildPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.logger = plugin.getLogger();
    }

    // ==================== 模块事件分发辅助 ====================

    private void fireGuildCreate(int guildId, String guildName, String leaderName) {
        try {
            com.guild.core.module.ModuleManager mm = plugin.getServiceContainer().get(com.guild.core.module.ModuleManager.class);
            if (mm != null) mm.getSharedApi().fireGuildCreate(guildId, guildName, leaderName);
        } catch (Exception ignored) {}
    }

    private void fireGuildDelete(int guildId, String guildName, String leaderName) {
        try {
            com.guild.core.module.ModuleManager mm = plugin.getServiceContainer().get(com.guild.core.module.ModuleManager.class);
            if (mm != null) mm.getSharedApi().fireGuildDelete(guildId, guildName, leaderName);
        } catch (Exception ignored) {}
    }

    private void fireMemberJoin(int guildId, String guildName, UUID playerUuid, String playerName) {
        try {
            com.guild.core.module.ModuleManager mm = plugin.getServiceContainer().get(com.guild.core.module.ModuleManager.class);
            if (mm != null) mm.getSharedApi().fireMemberJoin(guildId, guildName, playerUuid, playerName);
        } catch (Exception ignored) {}
    }

    private void fireMemberLeave(int guildId, String guildName, UUID playerUuid, String playerName, String eventType) {
        try {
            com.guild.core.module.ModuleManager mm = plugin.getServiceContainer().get(com.guild.core.module.ModuleManager.class);
            if (mm != null) mm.getSharedApi().fireMemberLeave(guildId, guildName, playerUuid, playerName, eventType);
        } catch (Exception ignored) {}
    }

    public void notifyEconomyDeposit(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        try {
            com.guild.core.module.ModuleManager mm = plugin.getServiceContainer().get(com.guild.core.module.ModuleManager.class);
            if (mm != null) mm.getSharedApi().fireEconomyDeposit(guildId, guildName, playerUuid, playerName, amount);
        } catch (Exception ignored) {}
    }

    public void notifyEconomyWithdraw(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        try {
            com.guild.core.module.ModuleManager mm = plugin.getServiceContainer().get(com.guild.core.module.ModuleManager.class);
            if (mm != null) mm.getSharedApi().fireEconomyWithdraw(guildId, guildName, playerUuid, playerName, amount);
        } catch (Exception ignored) {}
    }

    private void fireMemberRoleChange(int guildId, String guildName, UUID playerUuid, String playerName,
                                       String oldRole, String newRole) {
        try {
            com.guild.core.module.ModuleManager mm = plugin.getServiceContainer().get(com.guild.core.module.ModuleManager.class);
            if (mm != null) mm.getSharedApi().fireMemberRoleChange(guildId, guildName, playerUuid, playerName, oldRole, newRole);
        } catch (Exception ignored) {}
    }
    
    // 时间工具：统一使用操作系统本地时间字符串（yyyy-MM-dd HH:mm:ss）
    private String nowString() { return TimeProvider.nowString(); }
    private String plusMinutesString(int minutes) { return TimeProvider.plusMinutesString(minutes); }
    private String plusDaysString(int days) { return TimeProvider.plusDaysString(days); }
    
    /**
     * 创建工会 (异步)
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
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        String sql = "INSERT INTO guilds (name, tag, description, leader_uuid, leader_name, balance, level, peak_level, max_members, frozen, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 0.0, 1, 1, 6, 0, ?, ?)";
                        
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            
                            stmt.setString(1, name);
                            stmt.setString(2, tag);
                            stmt.setString(3, description);
                            stmt.setString(4, leaderUuid.toString());
                            stmt.setString(5, leaderName);
                            
                            stmt.setString(6, nowString());
                            stmt.setString(7, nowString());
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                try (ResultSet rs = stmt.getGeneratedKeys()) {
                                    if (rs.next()) {
                                        int guildId = rs.getInt(1);
                                        logger.info("Guild created successfully: " + name + " (ID: " + guildId + ")");
                                        return guildId;
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Error creating guild: " + e.getMessage());
                    }
                    return -1;
                }).thenCompose(guildId -> {
                    if ((Integer) guildId > 0) {
                        // 添加会长为工会成员（避免重复查询）
                        return addGuildMemberDirectAsync((Integer) guildId, leaderUuid, leaderName, GuildMember.Role.LEADER)
                            .thenCompose(success -> {
                                if (success) {
                                    fireGuildCreate((Integer) guildId, name, leaderName);
                                    // 记录工会创建日志
                                    return logGuildActionAsync((Integer) guildId, name, leaderUuid.toString(), leaderName,
                                        GuildLog.LogType.GUILD_CREATED, "创建工会", "工会名称: " + name + ", 标签: " + tag)
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
     * 创建工会 (同步包装器)
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
     * 删除工会 (异步)
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
                        // 获取工会余额用于退款
                        double guildBalance = guild.getBalance();
                        
                        // 删除工会成员
                        String deleteMembersSql = "DELETE FROM guild_members WHERE guild_id = ?";
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(deleteMembersSql)) {
                            stmt.setInt(1, guildId);
                            stmt.executeUpdate();
                        }

                        deleteWarehouseData(guildId);
                        
                        // 删除工会
                        String deleteGuildSql = "DELETE FROM guilds WHERE id = ?";
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(deleteGuildSql)) {
                            stmt.setInt(1, guildId);
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                logger.info("Guild deleted successfully: " + guild.getName() + " (ID: " + guildId + ")");
                                
                                // 退款给会长（如果经济系统可用）
                                if (guildBalance > 0 && plugin.getEconomyManager().isVaultAvailable()) {
                                    try {
                                        org.bukkit.entity.Player leaderPlayer = org.bukkit.Bukkit.getPlayer(guild.getLeaderUuid());
                                        if (leaderPlayer != null && leaderPlayer.isOnline()) {
                                            plugin.getEconomyManager().deposit(leaderPlayer, guildBalance);
                                            String message = plugin.getLanguageManager().getCoreMessage(leaderPlayer, "economy.disband-compensation", "&a工会解散，您获得了 {amount} 金币补偿！", "{amount}", plugin.getEconomyManager().format(guildBalance));
                                            leaderPlayer.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                                        }
                                    } catch (Exception e) {
                                        logger.warning("Error refunding leader: " + e.getMessage());
                                    }
                                }
                                
                                // 记录工会解散日志
                                logGuildActionAsync(guildId, guild.getName(), guild.getLeaderUuid().toString(), guild.getLeaderName(),
                                    GuildLog.LogType.GUILD_DISSOLVED, "工会解散", "工会余额: " + guildBalance + " 金币");
                                
                                // 分发工会解散事件给模块
                                fireGuildDelete(guildId, guild.getName(), guild.getLeaderName());
                                
                                return true;
                            }
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
     * 删除工会 (同步包装器)
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
     * 管理员强制删除工会 (异步) — 跳过会长身份验证，但资金仍退还至会长
     * @param guildId 工会ID
     * @param adminUuid 管理员UUID（执行删除的人，非会长）
     */
    public CompletableFuture<Boolean> forceDeleteGuildAsync(int guildId, UUID adminUuid) {
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // 获取工会余额用于退款（退款给会长，而非管理员）
                    double guildBalance = guild.getBalance();
                    
                    // 删除所有工会成员
                    String deleteMembersSql = "DELETE FROM guild_members WHERE guild_id = ?";
                    try (Connection conn = databaseManager.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(deleteMembersSql)) {
                        stmt.setInt(1, guildId);
                        stmt.executeUpdate();
                    }

                    deleteWarehouseData(guildId);
                    // 删除工会
                    String deleteGuildSql = "DELETE FROM guilds WHERE id = ?";
                    try (Connection conn = databaseManager.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(deleteGuildSql)) {
                        stmt.setInt(1, guildId);
                        int affectedRows = stmt.executeUpdate();
                        if (affectedRows > 0) {
                            logger.info("Admin force-deleted guild: " + guild.getName() + " (ID: " + guildId + ", by: " + adminUuid + ")");
                            
                            // 退款给会长（如果经济系统可用）— 注意：退款给会长而非管理员
                            if (guildBalance > 0 && plugin.getEconomyManager().isVaultAvailable()) {
                                try {
                                    org.bukkit.entity.Player leaderPlayer = org.bukkit.Bukkit.getPlayer(guild.getLeaderUuid());
                                    if (leaderPlayer != null && leaderPlayer.isOnline()) {
                                        plugin.getEconomyManager().deposit(leaderPlayer, guildBalance);
                                        String message = plugin.getLanguageManager().getCoreMessage(leaderPlayer, "economy.disband-compensation", "&a工会解散，您获得了 {amount} 金币补偿！", "{amount}", plugin.getEconomyManager().format(guildBalance));
                                        leaderPlayer.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                                    }
                                } catch (Exception e) {
                                    logger.warning("Error refunding to guild leader: " + e.getMessage());
                                }
                            }
                            
                            // 记录工会强制解散日志
                            logGuildActionAsync(guildId, guild.getName(), guild.getLeaderUuid().toString(), guild.getLeaderName(),
                                GuildLog.LogType.GUILD_DISSOLVED, "Admin force deleted",
                                "By: " + adminUuid + ", balance: " + guildBalance + " coins");
                            
                            // 分发工会解散事件给模块
                            fireGuildDelete(guildId, guild.getName(), guild.getLeaderName());
                            
                            return true;
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("Error during admin force-delete: " + e.getMessage());
                }
                return false;
            });
        });
    }
    
    /**
     * 更新工会信息 (异步)
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
                
                // 检查名称和标签是否与其他工会冲突
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
                            try {
                                String sql = "UPDATE guilds SET name = COALESCE(?, name), tag = COALESCE(?, tag), description = COALESCE(?, description), updated_at = ? WHERE id = ?";
                                
                                try (Connection conn = databaseManager.getConnection();
                                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                                    
                                    stmt.setString(1, name);
                                    stmt.setString(2, tag);
                                    stmt.setString(3, description);
                                    stmt.setString(4, nowString());
                                    stmt.setInt(5, guildId);
                                    
                                    int affectedRows = stmt.executeUpdate();
                                    if (affectedRows > 0) {
                                        logger.info("Guild info updated successfully: " + guild.getName() + " (ID: " + guildId + ")");
                                        return true;
                                    }
                                }
                            } catch (SQLException e) {
                                logger.severe("Error updating guild info: " + e.getMessage());
                            }
                            return false;
                        });
                    });
            });
        });
    }
    
    /**
     * 更新工会信息 (同步包装器)
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
     * 添加工会成员 (异步)
     * 包含人数上限检查：查询目标工会当前成员数，与有效上限比较。
     */
    public CompletableFuture<Boolean> addGuildMemberAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        logger.info("[AddMember-Debug] Starting member add: guildId=" + guildId + ", player=" + playerName + ", uuid=" + playerUuid);
        
        return getPlayerGuildAsync(playerUuid).thenCompose(existingGuild -> {
            if (existingGuild != null) {
                logger.warning("[AddMember-Debug] Player " + playerName + " is already in guild " + existingGuild.getName());
                return CompletableFuture.completedFuture(false);
            }
            
            logger.info("[AddMember-Debug] Player not in any guild, checking capacity");
            
            // 人数上限检查：获取目标工会并比较当前成员数与有效上限
            return getGuildByIdAsync(guildId).thenCompose(targetGuild -> {
                if (targetGuild == null) {
                    logger.warning("[AddMember-Debug] Target guild not found: guildId=" + guildId);
                    return CompletableFuture.completedFuture(false);
                }
                
                return getGuildMemberCountAsync(guildId).thenCompose(memberCount -> {
                    int effectiveMax = getEffectiveMaxMembers(targetGuild);
                    if (memberCount >= effectiveMax) {
                        logger.info("[AddMember-Debug] Guild " + targetGuild.getName() + " is full (" 
                            + memberCount + "/" + effectiveMax + "), rejecting " + playerName);
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    logger.info("[AddMember-Debug] Capacity OK (" + memberCount + "/" + effectiveMax + "), preparing database insert");
                    
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                        
                        String sql = "INSERT INTO guild_members (guild_id, player_uuid, player_name, role, joined_at) VALUES (?, ?, ?, ?, ?)";
                        
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql)) {
                            
                            stmt.setInt(1, guildId);
                            stmt.setString(2, playerUuid.toString());
                            stmt.setString(3, playerName);
                            stmt.setString(4, role.name());
                            stmt.setString(5, nowString());
                            
                            logger.info("[AddMember-Debug] Executing INSERT: guildId=" + guildId + ", uuid=" + playerUuid + ", name=" + playerName + ", role=" + role.name());
                            
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                logger.info("[AddMember-Debug] Player " + playerName + " successfully joined guild (ID: " + guildId + ")");
                                // 更新内置权限缓存
                                try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                                
                                // 记录成员加入日志
                                logGuildActionAsync(guildId, targetGuild.getName(), playerUuid.toString(), playerName,
                                    GuildLog.LogType.MEMBER_JOINED, "成员加入", "玩家: " + playerName + ", 职位: " + role.getDisplayName());
                                // 分发成员加入事件给模块
                                fireMemberJoin(targetGuild.getId(), targetGuild.getName(), playerUuid, playerName);
                                
                                return true;
                            } else {
                                logger.warning("[AddMember-Debug] INSERT did not affect any rows");
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("[AddMember-Debug] Error adding guild member: " + e.getMessage());
                    }
                    return false;
                });
                });
            });
        });
    }
    
    /**
     * 添加工会成员 (同步包装器)
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
     * 移除工会成员 (异步)
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
                    try {
                        String sql = "DELETE FROM guild_members WHERE player_uuid = ?";
                        
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql)) {
                            
                            stmt.setString(1, playerUuid.toString());
                            
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                logger.info("Player " + member.getPlayerName() + " left guild (ID: " + member.getGuildId() + ")");
                                // 更新内置权限缓存
                                try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                                
                                // 记录成员离开日志
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
                        }
                    } catch (SQLException e) {
                        logger.severe("Error removing guild member: " + e.getMessage());
                    }
                    return false;
                });
            });
        });
    }
    
    /**
     * 移除工会成员 (同步包装器)
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
            
            return getGuildMemberAsync(requesterUuid).thenCompose(requester -> {
                // 检查权限 - 只有会长可以更改角色
                if (requester == null || requester.getGuildId() != member.getGuildId() || 
                    requester.getRole() != GuildMember.Role.LEADER) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        String sql = "UPDATE guild_members SET role = ? WHERE player_uuid = ?";
                        
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql)) {
                            
                            stmt.setString(1, newRole.name());
                            stmt.setString(2, playerUuid.toString());
                            
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                logger.info("Player " + member.getPlayerName() + " role updated to: " + newRole.name());
                                // 更新内置权限缓存
                                try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                                
                                // 记录角色变更日志
                                getGuildByIdAsync(member.getGuildId()).thenAccept(guild -> {
                                    if (guild != null) {
                                        GuildLog.LogType logType = newRole == GuildMember.Role.LEADER ? 
                                            GuildLog.LogType.LEADER_TRANSFERRED : 
                                            (newRole == GuildMember.Role.OFFICER ? GuildLog.LogType.MEMBER_PROMOTED : GuildLog.LogType.MEMBER_DEMOTED);
                                        String description = newRole == GuildMember.Role.LEADER ? "会长转让" : 
                                            (newRole == GuildMember.Role.OFFICER ? "成员升职" : "成员降职");
                                        String details = "玩家: " + member.getPlayerName() + ", 新职位: " + newRole.getDisplayName() + 
                                            ", 操作者: " + requester.getPlayerName();
                                        
                                        logGuildActionAsync(member.getGuildId(), guild.getName(), 
                                            requesterUuid.toString(), requester.getPlayerName(),
                                            logType, description, details);
                                    }
                                });
                                
                                return true;
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Error updating member role: " + e.getMessage());
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
                try {
                    String sql = "DELETE FROM guild_members WHERE player_uuid = ?";
                    try (Connection conn = databaseManager.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, playerUuid.toString());
                        int affectedRows = stmt.executeUpdate();
                        if (affectedRows > 0) {
                            try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                            getGuildByIdAsync(guildId).thenAccept(guild -> {
                                if (guild != null) {
                                    logGuildActionAsync(guildId, guild.getName(), playerUuid.toString(), member.getPlayerName(),
                                            GuildLog.LogType.MEMBER_KICKED, "成员被API移除", "玩家: " + member.getPlayerName());
                                    fireMemberLeave(guildId, guild.getName(), playerUuid, member.getPlayerName(), "kicked");
                                }
                            });
                            return true;
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("Error directly removing member: " + e.getMessage());
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
                try {
                    String sql = "UPDATE guild_members SET role = ? WHERE player_uuid = ?";
                    try (Connection conn = databaseManager.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, newRole.name());
                        stmt.setString(2, playerUuid.toString());
                        int affectedRows = stmt.executeUpdate();
                        if (affectedRows > 0) {
                            try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                            getGuildByIdAsync(guildId).thenAccept(guild -> {
                                if (guild != null) {
                                    fireMemberRoleChange(guildId, guild.getName(), playerUuid, member.getPlayerName(), oldRole, newRole.name());
                                }
                            });
                            return true;
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("Error directly modifying role: " + e.getMessage());
                }
                return false;
            });
        });
    }
    
    /**
     * 转移会长职位 (异步，供管理员操作)
     * 将 guild_id 的会长改为 newLeaderUuid，同时将原会长降为成员。
     */
    public CompletableFuture<Boolean> transferGuildLeadershipAsync(int guildId, UUID newLeaderUuid, String newLeaderName) {
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            UUID oldLeaderUuid = guild.getLeaderUuid();
            return CompletableFuture.supplyAsync(() -> {
                try (Connection conn = databaseManager.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        // 1. 更新 guilds 表新会长
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "UPDATE guilds SET leader_uuid = ?, leader_name = ? WHERE id = ?")) {
                            stmt.setString(1, newLeaderUuid.toString());
                            stmt.setString(2, newLeaderName);
                            stmt.setInt(3, guildId);
                            stmt.executeUpdate();
                        }
                        // 2. 原会长降级为成员
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "UPDATE guild_members SET role = 'MEMBER' WHERE guild_id = ? AND player_uuid = ? AND role = 'LEADER'")) {
                            stmt.setInt(1, guildId);
                            stmt.setString(2, oldLeaderUuid.toString());
                            stmt.executeUpdate();
                        }
                        // 3. 新会长升级
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "UPDATE guild_members SET role = 'LEADER' WHERE guild_id = ? AND player_uuid = ?")) {
                            stmt.setInt(1, guildId);
                            stmt.setString(2, newLeaderUuid.toString());
                            stmt.executeUpdate();
                        }
                        conn.commit();

                        // 刷新权限缓存
                        try { plugin.getPermissionManager().updatePlayerPermissions(oldLeaderUuid); } catch (Exception ignored) {}
                        try { plugin.getPermissionManager().updatePlayerPermissions(newLeaderUuid); } catch (Exception ignored) {}

                        // 记录日志
                        String desc = "会长由 " + guild.getLeaderName() + " 转移给 " + newLeaderName;
                        logGuildActionAsync(guildId, guild.getName(), newLeaderUuid.toString(), newLeaderName,
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
    }

    /**
     * 获取玩家工会 (异步)
     */
    public CompletableFuture<Guild> getPlayerGuildAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT g.* FROM guilds g " +
                            "INNER JOIN guild_members gm ON g.id = gm.guild_id " +
                            "WHERE gm.player_uuid = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching player guild: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * 获取玩家工会 (同步包装器)
     */
    public Guild getPlayerGuild(UUID playerUuid) {
        try {
            return getPlayerGuildAsync(playerUuid).get();
        } catch (Exception e) {
            logger.severe("Exception fetching player guild: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取工会成员 (异步)
     */
    public CompletableFuture<GuildMember> getGuildMemberAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guild_members WHERE player_uuid = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildMemberFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild members: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * 获取工会成员 (同步包装器)
     */
    public GuildMember getGuildMember(UUID playerUuid) {
        try {
            return getGuildMemberAsync(playerUuid).get();
        } catch (Exception e) {
            logger.severe("Exception fetching guild members: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取工会成员数量 (异步)
     */
    public CompletableFuture<Integer> getGuildMemberCountAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT COUNT(*) FROM guild_members WHERE guild_id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild member count: " + e.getMessage());
            }
            return 0;
        });
    }
    
    /**
     * 获取工会成员数量 (同步包装器)
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
     * 获取工会所有成员 (异步)
     */
    public CompletableFuture<List<GuildMember>> getGuildMembersAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildMember> members = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_members WHERE guild_id = ? ORDER BY role ASC, joined_at ASC";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            members.add(createGuildMemberFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild member list: " + e.getMessage());
            }
            return members;
        });
    }
    
    /**
     * 获取工会所有成员 (同步包装器)
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
     * 根据ID获取工会 (异步)
     */
    public CompletableFuture<Guild> getGuildByIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guilds WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild by ID: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * 根据ID获取工会 (同步包装器)
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
     * 根据名称获取工会 (异步)
     */
    public CompletableFuture<Guild> getGuildByNameAsync(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guilds WHERE name = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, name);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild by name: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * 根据名称获取工会 (同步包装器)
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
     * 根据标签获取工会 (异步)
     */
    public CompletableFuture<Guild> getGuildByTagAsync(String tag) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guilds WHERE tag = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, tag);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild by tag: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * 根据标签获取工会 (同步包装器)
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
     * 获取所有工会 (异步)
     */
    public CompletableFuture<List<Guild>> getAllGuildsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<Guild> guilds = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guilds ORDER BY created_at DESC";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        guilds.add(createGuildFromResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching all guilds: " + e.getMessage());
            }
            return guilds;
        });
    }
    
    /**
     * 获取所有工会 (同步包装器)
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
     * 检查是否为工会会长
     */
    public boolean isGuildLeader(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getRole() == GuildMember.Role.LEADER;
    }
    
    /**
     * 检查是否为指定工会的会长
     */
    public boolean isGuildLeader(UUID playerUuid, int guildId) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getGuildId() == guildId && member.getRole() == GuildMember.Role.LEADER;
    }
    
    /**
     * 检查是否为工会官员
     */
    public boolean isGuildOfficer(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getRole() == GuildMember.Role.OFFICER;
    }
    
    /**
     * 检查是否有工会权限
     */
    public boolean hasGuildPermission(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER);
    }
    
    /**
     * 从ResultSet创建Guild对象
     */
    private Guild createGuildFromResultSet(ResultSet rs) throws SQLException {
        Guild guild = new Guild();
        guild.setId(rs.getInt("id"));
        guild.setName(rs.getString("name"));
        guild.setTag(rs.getString("tag"));
        guild.setDescription(rs.getString("description"));
        guild.setLeaderUuid(UUID.fromString(rs.getString("leader_uuid")));
        guild.setLeaderName(rs.getString("leader_name"));
        
        // 家的位置信息（安全处理空值）
        String homeWorld = rs.getString("home_world");
        guild.setHomeWorld(homeWorld);
        
        if (homeWorld != null) {
            guild.setHomeX(rs.getDouble("home_x"));
            guild.setHomeY(rs.getDouble("home_y"));
            guild.setHomeZ(rs.getDouble("home_z"));
            guild.setHomeYaw(rs.getFloat("home_yaw"));
            guild.setHomePitch(rs.getFloat("home_pitch"));
        } else {
            // 如果home_world为null，设置默认值
            guild.setHomeX(0.0);
            guild.setHomeY(0.0);
            guild.setHomeZ(0.0);
            guild.setHomeYaw(0.0f);
            guild.setHomePitch(0.0f);
        }
        
        guild.setCreatedAt(parseTimestamp(rs, "created_at"));
        guild.setUpdatedAt(parseTimestamp(rs, "updated_at"));
        
        // 读取economy相关列（安全处理，如果列不存在则使用默认值）
        try {
            guild.setBalance(rs.getDouble("balance"));
        } catch (SQLException e) {
            guild.setBalance(0.0);
        }
        
        try {
            guild.setLevel(rs.getInt("level"));
        } catch (SQLException e) {
            guild.setLevel(1);
        }

        try {
            int peak = rs.getInt("peak_level");
            if (rs.wasNull() || peak < guild.getLevel()) {
                peak = guild.getLevel();
            }
            guild.setPeakLevel(peak);
        } catch (SQLException e) {
            guild.setPeakLevel(guild.getLevel());
        }
        
        try {
            guild.setMaxMembers(rs.getInt("max_members"));
        } catch (SQLException e) {
            guild.setMaxMembers(6);
        }
        
        try {
            guild.setFrozen(rs.getBoolean("frozen"));
        } catch (SQLException e) {
            guild.setFrozen(false);
        }
        
        return guild;
    }
    
    /**
     * 从ResultSet创建GuildMember对象
     */
    private GuildMember createGuildMemberFromResultSet(ResultSet rs) throws SQLException {
        GuildMember member = new GuildMember();
        member.setId(rs.getInt("id"));
        member.setGuildId(rs.getInt("guild_id"));
        member.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        member.setPlayerName(rs.getString("player_name"));
        member.setRole(GuildMember.Role.valueOf(rs.getString("role")));
        member.setJoinedAt(parseTimestamp(rs, "joined_at"));
        return member;
    }
    
    /**
     * 解析时间戳
     */
    private java.time.LocalDateTime parseTimestamp(ResultSet rs, String columnName) throws SQLException {
        // 优先以字符串按统一格式解析，避免驱动按时区转换导致偏差
        String s = rs.getString(columnName);
        if (s != null && !s.isEmpty()) {
            try {
                return LocalDateTime.parse(s, com.guild.core.time.TimeProvider.FULL_FORMATTER);
            } catch (Exception ignore) {
                try {
                    return LocalDateTime.parse(s.replace(" ", "T"));
                } catch (Exception ex) {
                    logger.warning("Failed to parse timestamp: " + s);
                }
            }
        }
        // 回退：使用驱动时间戳
        try {
            Timestamp ts = rs.getTimestamp(columnName);
            if (ts != null) return ts.toLocalDateTime();
        } catch (SQLException ignore) {}
        return LocalDateTime.now();
    }

    
    
    /**
     * 提交申请 (异步)
     */
    public CompletableFuture<Boolean> submitApplicationAsync(int guildId, UUID playerUuid, String playerName, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 检查是否已有待处理的申请
                if (hasPendingApplication(playerUuid, guildId)) {
                    return false;
                }
                
                String sql = "INSERT INTO guild_applications (guild_id, player_uuid, player_name, message, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    stmt.setString(2, playerUuid.toString());
                    stmt.setString(3, playerName);
                    stmt.setString(4, message);
                    stmt.setString(5, GuildApplication.ApplicationStatus.PENDING.name());
                    stmt.setString(6, nowString());
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Player " + playerName + " submitted a join application (guild ID: " + guildId + ")");
                        
                        // 记录申请提交日志
                        getGuildByIdAsync(guildId).thenAccept(guild -> {
                            if (guild != null) {
                                logGuildActionAsync(guildId, guild.getName(), playerUuid.toString(), playerName,
                                    GuildLog.LogType.APPLICATION_SUBMITTED, "申请提交", "申请消息: " + message);
                                
                                // 创建申请对象用于通知
                                GuildApplication application = new GuildApplication(guildId, playerUuid, playerName, message);
                                
                                // 实时通知工会会长
                                NotifyUtils.notifyLeaderNewApplication(plugin, guild, application);
                            }
                        });
                        
                        return true;
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error submitting application: " + e.getMessage());
            }
            return false;
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
     * 审批通过前先检查工会是否满员，避免申请状态已更新但成员无法加入的情况。
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
                
                // 审批通过前预检查工会人数上限
                if (status == GuildApplication.ApplicationStatus.APPROVED) {
                    return isGuildFullAsync(application.getGuildId()).thenCompose(isFull -> {
                        if (isFull) {
                            logger.info("Application approval rejected: guild " + application.getGuildId() 
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "UPDATE guild_applications SET status = ? WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, status.name());
                    stmt.setInt(2, applicationId);
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Application processed: " + application.getPlayerName() + " -> " + status.name());
                        
                        // 记录申请处理日志
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
                        
                        return true;
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error processing application: " + e.getMessage());
            }
            return false;
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT COUNT(*) FROM guild_applications WHERE player_uuid = ? AND guild_id = ? AND status = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, playerUuid.toString());
                    stmt.setInt(2, guildId);
                    stmt.setString(3, GuildApplication.ApplicationStatus.PENDING.name());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1) > 0;
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error checking pending applications: " + e.getMessage());
            }
            return false;
        });
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
     * 获取工会申请列表 (异步)
     */
    public CompletableFuture<List<GuildApplication>> getGuildApplicationsAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildApplication> applications = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_applications WHERE guild_id = ? ORDER BY created_at DESC";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            applications.add(createGuildApplicationFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild application list: " + e.getMessage());
            }
            return applications;
        });
    }
    
    /**
     * 获取工会申请列表 (同步包装器)
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
        return CompletableFuture.supplyAsync(() -> {
            List<GuildApplication> applications = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_applications WHERE player_uuid = ? ORDER BY created_at DESC";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            applications.add(createGuildApplicationFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching player application list: " + e.getMessage());
            }
            return applications;
        });
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guild_applications WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, applicationId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createGuildApplicationFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching application by ID: " + e.getMessage());
            }
            return null;
        });
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
     * 从ResultSet创建GuildApplication对象
     */
    private GuildApplication createGuildApplicationFromResultSet(ResultSet rs) throws SQLException {
        GuildApplication application = new GuildApplication();
        application.setId(rs.getInt("id"));
        application.setGuildId(rs.getInt("guild_id"));
        application.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        application.setPlayerName(rs.getString("player_name"));
        application.setMessage(rs.getString("message"));
                 application.setStatus(GuildApplication.ApplicationStatus.valueOf(rs.getString("status")));
         application.setCreatedAt(parseTimestamp(rs, "created_at"));
         
         return application;
     }
     
     /**
      * 设置工会家 (异步)
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
                     try {
                         String sql = "UPDATE guilds SET home_world = ?, home_x = ?, home_y = ?, home_z = ?, home_yaw = ?, home_pitch = ?, updated_at = ? WHERE id = ?";
                         
                         try (Connection conn = databaseManager.getConnection();
                              PreparedStatement stmt = conn.prepareStatement(sql)) {
                             
                             stmt.setString(1, location.getWorld().getName());
                             stmt.setDouble(2, location.getX());
                             stmt.setDouble(3, location.getY());
                             stmt.setDouble(4, location.getZ());
                             stmt.setFloat(5, location.getYaw());
                             stmt.setFloat(6, location.getPitch());
                             stmt.setString(7, nowString());
                             stmt.setInt(8, guildId);
                             
                             int affectedRows = stmt.executeUpdate();
                             if (affectedRows > 0) {
                                 logger.info("Guild home set successfully: " + guild.getName() + " (ID: " + guildId + ")");
                                 return true;
                             }
                         }
                     } catch (SQLException e) {
                         logger.severe("Error setting guild home: " + e.getMessage());
                     }
                     return false;
                 });
             });
         });
     }
     
     /**
      * 设置工会家 (同步包装器)
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
      * 获取工会家位置 (异步)
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
      * 获取工会家位置 (同步包装器)
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
         return getPlayerGuildAsync(targetUuid).thenCompose(existingGuild -> {
             if (existingGuild != null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return getPendingInvitationAsync(targetUuid, guildId).thenCompose(existingInvitation -> {
                 if (existingInvitation != null) {
                     return CompletableFuture.completedFuture(false);
                 }
                 
                 return CompletableFuture.supplyAsync(() -> {
                     try {
                         String sql = "INSERT INTO guild_invites (guild_id, player_uuid, player_name, inviter_uuid, inviter_name, status, expires_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                         
                         try (Connection conn = databaseManager.getConnection();
                              PreparedStatement stmt = conn.prepareStatement(sql)) {
                         
                             stmt.setInt(1, guildId);
                             stmt.setString(2, targetUuid.toString());
                             stmt.setString(3, targetName);
                             stmt.setString(4, inviterUuid.toString());
                             stmt.setString(5, inviterName);
                             stmt.setString(6, "PENDING");
                             stmt.setString(7, plusMinutesString(30));
                             stmt.setString(8, nowString());
                         
                             int affectedRows = stmt.executeUpdate();
                             if (affectedRows > 0) {
                                 logger.info("Invitation sent successfully: " + inviterName + " -> " + targetName + " (guild ID: " + guildId + ")");
                                 return true;
                             }
                         }
                     } catch (SQLException e) {
                         logger.severe("Error sending invitation: " + e.getMessage());
                     }
                     return false;
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
     * 接受邀请前先检查工会是否满员，避免邀请状态已更新但成员无法加入的情况。
     */
    public CompletableFuture<Boolean> processInvitationDirectAsync(GuildInvitation invitation, boolean accept) {
        logger.info("[Process-Debug] Starting invitation processing: id=" + invitation.getId() + ", guildId=" + invitation.getGuildId() + ", target=" + invitation.getTargetUuid() + ", accept=" + accept);
        
        // 接受邀请前预检查工会人数上限
        if (accept) {
            return isGuildFullAsync(invitation.getGuildId()).thenCompose(isFull -> {
                if (isFull) {
                    logger.info("[Process-Debug] Invitation accept rejected: guild " + invitation.getGuildId() 
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                String status = accept ? "ACCEPTED" : "DECLINED";
                String sql = "UPDATE guild_invites SET status = ? WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, status);
                    stmt.setInt(2, invitation.getId());
                    
                    logger.info("[Process-Debug] Executing update: status=" + status + ", id=" + invitation.getId());
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("[Process-Debug] Invitation status updated: " + invitation.getTargetUuid() + " -> " + status);
                        return true;
                    } else {
                        logger.warning("[Process-Debug] Invitation status update failed, no rows affected: id=" + invitation.getId());
                    }
                    return false;
                }
            } catch (SQLException e) {
                logger.severe("[Process-Debug] Error processing invitation: " + e.getMessage());
            }
            return false;
        }).thenCompose(success -> {
            if (success && accept) {
                logger.info("[Process-Debug] Preparing to add player to guild: guildId=" + invitation.getGuildId() + ", player=" + invitation.getTargetUuid());
                // 如果接受邀请，添加玩家到工会（addGuildMemberAsync 内部有二次容量校验）
                return addGuildMemberAsync(invitation.getGuildId(), invitation.getTargetUuid(), 
                    invitation.getTargetName(), GuildMember.Role.MEMBER)
                    .thenCompose(addSuccess -> {
                        if (addSuccess) {
                            logger.info("[Process-Debug] Player added, dispatching event");
                            // 分发成员加入事件给模块
                            return getGuildByIdAsync(invitation.getGuildId()).thenAccept(g -> {
                                if (g != null) fireMemberJoin(g.getId(), g.getName(), 
                                    invitation.getTargetUuid(), invitation.getTargetName());
                            }).thenApply(v -> true);
                        } else {
                            logger.warning("[Process-Debug] Failed to add player to guild");
                            return CompletableFuture.completedFuture(false);
                        }
                    });
            }
            return CompletableFuture.completedFuture(success);
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
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "SELECT * FROM guild_invites WHERE player_uuid = ? AND inviter_uuid = ? AND status = 'PENDING' AND expires_at > ? ORDER BY created_at DESC LIMIT 1";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setString(1, targetUuid.toString());
                     stmt.setString(2, inviterUuid.toString());
                     stmt.setString(3, nowString());
                 
                     try (ResultSet rs = stmt.executeQuery()) {
                         if (rs.next()) {
                             return createGuildInvitationFromResultSet(rs);
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Error fetching pending invitations: " + e.getMessage());
             }
             return null;
         });
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guild_invites WHERE player_uuid = ? AND guild_id = ? AND status = 'PENDING' AND expires_at > ? ORDER BY created_at DESC LIMIT 1";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                    stmt.setString(1, targetUuid.toString());
                    stmt.setInt(2, guildId);
                    stmt.setString(3, nowString());
                    
                    logger.info("[Invite-Debug] Querying invitation: player=" + targetUuid + ", guildId=" + guildId + ", now=" + nowString());
                
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            GuildInvitation invitation = createGuildInvitationFromResultSet(rs);
                            logger.info("[Invite-Debug] Found invitation: id=" + invitation.getId() + ", status=" + invitation.getStatus() + ", expires=" + invitation.getExpiresAt());
                            return invitation;
                        } else {
                            logger.info("[Invite-Debug] No invitation found, checking expiry: " + nowString());
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching invitation: " + e.getMessage());
            }
            return null;
        });
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
      * 从ResultSet创建GuildInvitation对象
      */
     private GuildInvitation createGuildInvitationFromResultSet(ResultSet rs) throws SQLException {
         GuildInvitation invitation = new GuildInvitation();
         invitation.setId(rs.getInt("id"));
         invitation.setGuildId(rs.getInt("guild_id"));
         invitation.setTargetUuid(UUID.fromString(rs.getString("player_uuid")));
         invitation.setTargetName(rs.getString("player_name"));
         invitation.setInviterUuid(UUID.fromString(rs.getString("inviter_uuid")));
         invitation.setInviterName(rs.getString("inviter_name"));
         invitation.setStatus(GuildInvitation.InvitationStatus.valueOf(rs.getString("status")));
         invitation.setInvitedAt(parseTimestamp(rs, "created_at"));
         invitation.setExpiresAt(parseTimestamp(rs, "expires_at"));
         return invitation;
     }
     
     /**
      * 获取待处理申请 (异步)
      */
     public CompletableFuture<List<GuildApplication>> getPendingApplicationsAsync(int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             List<GuildApplication> applications = new ArrayList<>();
             try {
                 String sql = "SELECT * FROM guild_applications WHERE guild_id = ? AND status = 'PENDING' ORDER BY created_at DESC";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         while (rs.next()) {
                             applications.add(createGuildApplicationFromResultSet(rs));
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Error fetching pending applications: " + e.getMessage());
             }
             return applications;
         });
     }
     
     /**
      * 获取申请历史 (异步)
      */
     public CompletableFuture<List<GuildApplication>> getApplicationHistoryAsync(int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             List<GuildApplication> applications = new ArrayList<>();
             try {
                 String sql = "SELECT * FROM guild_applications WHERE guild_id = ? AND status != 'PENDING' ORDER BY created_at DESC";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         while (rs.next()) {
                             applications.add(createGuildApplicationFromResultSet(rs));
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Error fetching application history: " + e.getMessage());
             }
            return applications;
        });
    }
    
    /**
     * 获取玩家所有待处理的邀请 (异步)
     */
    public CompletableFuture<List<GuildInvitation>> getPendingInvitationsAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildInvitation> invitations = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_invites WHERE player_uuid = ? AND status = 'PENDING' AND expires_at > ? ORDER BY created_at DESC";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, nowString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            invitations.add(createGuildInvitationFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching pending invitations: " + e.getMessage());
            }
            return invitations;
        });
    }
    
    /**
     * 清理过期的工会邀请 (异步) - 将过期邀请状态更新为EXPIRED
     * 建议定时调用，避免数据库中积累过多过期邀请
     */
    public CompletableFuture<Integer> cleanupExpiredInvitationsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "UPDATE guild_invites SET status = 'EXPIRED' WHERE status = 'PENDING' AND expires_at < ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, nowString());
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Cleaned up " + affectedRows + " expired guild invitations");
                    }
                    return affectedRows;
                }
            } catch (SQLException e) {
                logger.severe("Error cleaning up expired invitations: " + e.getMessage());
            }
            return 0;
        });
    }
    
    /**
     * 清理旧的已处理邀请 (异步) - 删除已过期超过指定天数的邀请记录
     * @param days 保留天数，超过此天数的已处理邀请（ACCEPTED/DECLINED/EXPIRED）将被删除
     */
    public CompletableFuture<Integer> cleanupOldProcessedInvitationsAsync(int days) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "DELETE FROM guild_invites WHERE status != 'PENDING' AND created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, days);
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Cleaned up " + affectedRows + " old processed invitation records");
                    }
                    return affectedRows;
                }
            } catch (SQLException e) {
                logger.severe("Error cleaning up old invitation records: " + e.getMessage());
            }
            return 0;
        });
    }
    
    /**
     * 获取工会成员 (异步) - 重载方法，接受guildId参数
     */
    public CompletableFuture<GuildMember> getGuildMemberAsync(int guildId, UUID playerUuid) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "SELECT * FROM guild_members WHERE guild_id = ? AND player_uuid = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     stmt.setString(2, playerUuid.toString());
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         if (rs.next()) {
                             return createGuildMemberFromResultSet(rs);
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Error fetching guild members: " + e.getMessage());
             }
             return null;
         });
     }
     
     /**
      * 更新工会描述 (异步)
      */
     public CompletableFuture<Boolean> updateGuildDescriptionAsync(int guildId, String description) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "UPDATE guilds SET description = ? WHERE id = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setString(1, description);
                     stmt.setInt(2, guildId);
                     
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Error updating guild description: " + e.getMessage());
                 return false;
             }
         });
     }
     
     // ==================== 工会关系系统 ====================
     
     /**
      * 创建工会关系 (异步)
      */
     public CompletableFuture<Boolean> createGuildRelationAsync(int guild1Id, int guild2Id, String guild1Name, String guild2Name,
                                                              GuildRelation.RelationType type, UUID initiatorUuid, String initiatorName) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "INSERT INTO guild_relations (guild1_id, guild2_id, guild1_name, guild2_name, relation_type, initiator_uuid, initiator_name, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setInt(1, guild1Id);
                     stmt.setInt(2, guild2Id);
                     stmt.setString(3, guild1Name);
                     stmt.setString(4, guild2Name);
                     stmt.setString(5, type.name());
                     stmt.setString(6, initiatorUuid.toString());
                     stmt.setString(7, initiatorName);
                     stmt.setString(8, plusDaysString(7));
                     
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Error creating guild relation: " + e.getMessage());
                 return false;
             }
         });
     }
     
     /**
      * 更新工会关系状态 (异步)
      */
     public CompletableFuture<Boolean> updateGuildRelationStatusAsync(int relationId, GuildRelation.RelationStatus status) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "UPDATE guild_relations SET status = ?, updated_at = ? WHERE id = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setString(1, status.name());
                     stmt.setString(2, nowString());
                     stmt.setInt(3, relationId);
                 
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Error updating guild relation status: " + e.getMessage());
                 return false;
             }
         });
     }
     
     /**
      * 获取工会关系 (异步)
      */
     public CompletableFuture<GuildRelation> getGuildRelationAsync(int guild1Id, int guild2Id) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "SELECT * FROM guild_relations WHERE (guild1_id = ? AND guild2_id = ?) OR (guild1_id = ? AND guild2_id = ?)";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guild1Id);
                     stmt.setInt(2, guild2Id);
                     stmt.setInt(3, guild2Id);
                     stmt.setInt(4, guild1Id);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         if (rs.next()) {
                             return createGuildRelationFromResultSet(rs);
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Error fetching guild relation: " + e.getMessage());
             }
             return null;
         });
     }
     
     /**
      * 获取工会的所有关系 (异步)
      */
     public CompletableFuture<List<GuildRelation>> getGuildRelationsAsync(int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             List<GuildRelation> relations = new ArrayList<>();
             try {
                 String sql = "SELECT * FROM guild_relations WHERE guild1_id = ? OR guild2_id = ? ORDER BY created_at DESC";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     stmt.setInt(2, guildId);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         while (rs.next()) {
                             relations.add(createGuildRelationFromResultSet(rs));
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Error fetching guild relation list: " + e.getMessage());
             }
             return relations;
         });
     }
     
     /**
      * 删除工会关系 (异步)
      */
     public CompletableFuture<Boolean> deleteGuildRelationAsync(int relationId) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "DELETE FROM guild_relations WHERE id = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, relationId);
                     
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Error deleting guild relation: " + e.getMessage());
                 return false;
             }
         });
     }
     
     // ==================== 工会经济系统 ====================
     
     /**
      * 初始化工会经济 (异步)
      */
     public CompletableFuture<Boolean> initializeGuildEconomyAsync(int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "INSERT INTO guild_economy (guild_id, balance, level, experience, max_experience, max_members) " +
                             "VALUES (?, 0.0, 1, 0.0, 5000.0, 6)";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Error initializing guild economy: " + e.getMessage());
                 return false;
             }
         });
     }
     
     /**
      * 获取工会经济信息 (异步)
      */
     public CompletableFuture<GuildEconomy> getGuildEconomyAsync(int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "SELECT * FROM guild_economy WHERE guild_id = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         if (rs.next()) {
                             return createGuildEconomyFromResultSet(rs);
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Error fetching guild economy info: " + e.getMessage());
             }
             return null;
         });
     }
     
     /**
      * 更新工会经济 (异步)
      */
     public CompletableFuture<Boolean> updateGuildEconomyAsync(int guildId, double balance, int level, double experience, double maxExperience, int maxMembers) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "UPDATE guild_economy SET balance = ?, level = ?, experience = ?, max_experience = ?, max_members = ?, last_updated = ? WHERE guild_id = ?";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setDouble(1, balance);
                     stmt.setInt(2, level);
                     stmt.setDouble(3, experience);
                     stmt.setDouble(4, maxExperience);
                     stmt.setInt(5, maxMembers);
                     stmt.setString(6, nowString());
                     stmt.setInt(7, guildId);
                 
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Error updating guild economy: " + e.getMessage());
                 return false;
             }
         });
     }
     
     /**
      * 添加工会贡献记录 (异步)
      */
     public CompletableFuture<Boolean> addGuildContributionAsync(int guildId, UUID playerUuid, String playerName,
                                                               double amount, GuildContribution.ContributionType type, String description) {
         return CompletableFuture.supplyAsync(() -> {
             try {
                 String sql = "INSERT INTO guild_contributions (guild_id, player_uuid, player_name, amount, contribution_type, description) " +
                             "VALUES (?, ?, ?, ?, ?, ?)";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     stmt.setString(2, playerUuid.toString());
                     stmt.setString(3, playerName);
                     stmt.setDouble(4, amount);
                     stmt.setString(5, type.name());
                     stmt.setString(6, description);
                     
                     int rowsAffected = stmt.executeUpdate();
                     return rowsAffected > 0;
                 }
             } catch (SQLException e) {
                 logger.severe("Error adding guild contribution record: " + e.getMessage());
                 return false;
             }
         });
     }
     
     /**
      * 获取工会贡献记录 (异步)
      */
     public CompletableFuture<List<GuildContribution>> getGuildContributionsAsync(int guildId) {
         return CompletableFuture.supplyAsync(() -> {
             List<GuildContribution> contributions = new ArrayList<>();
             try {
                 String sql = "SELECT * FROM guild_contributions WHERE guild_id = ? ORDER BY created_at DESC";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setInt(1, guildId);
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         while (rs.next()) {
                             contributions.add(createGuildContributionFromResultSet(rs));
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Error fetching guild contribution records: " + e.getMessage());
             }
             return contributions;
         });
     }
     
     /**
      * 获取玩家贡献记录 (异步)
      */
     public CompletableFuture<List<GuildContribution>> getPlayerContributionsAsync(UUID playerUuid) {
         return CompletableFuture.supplyAsync(() -> {
             List<GuildContribution> contributions = new ArrayList<>();
             try {
                 String sql = "SELECT * FROM guild_contributions WHERE player_uuid = ? ORDER BY created_at DESC";
                 
                 try (Connection conn = databaseManager.getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                     stmt.setString(1, playerUuid.toString());
                     
                     try (ResultSet rs = stmt.executeQuery()) {
                         while (rs.next()) {
                             contributions.add(createGuildContributionFromResultSet(rs));
                         }
                     }
                 }
             } catch (SQLException e) {
                 logger.severe("Error fetching player contribution records: " + e.getMessage());
             }
            return contributions;
        });
    }

    /**
     * 按玩家聚合工会净贡献（WITHDRAW 为负，其余为正）。
     * 返回 Map&lt;playerUuid, netAmount&gt;。
     */
    public CompletableFuture<Map<UUID, Double>> getGuildContributionNetByPlayerAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Double> nets = new HashMap<>();
            try {
                String sql = "SELECT player_uuid, contribution_type, amount FROM guild_contributions WHERE guild_id = ?";
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, guildId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                            String typeName = rs.getString("contribution_type");
                            double amount = rs.getDouble("amount");
                            GuildContribution.ContributionType type;
                            try {
                                type = GuildContribution.ContributionType.valueOf(typeName);
                            } catch (IllegalArgumentException ex) {
                                type = GuildContribution.ContributionType.ADMIN;
                            }
                            double net = (type == GuildContribution.ContributionType.WITHDRAW) ? -amount : amount;
                            nets.merge(uuid, net, Double::sum);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error aggregating guild net contributions: " + e.getMessage());
            }
            return nets;
        });
    }

    /**
     * 获取工会中各成员的存款总额（聚合查询，仅 DEPOSIT 类型）
     * 返回 List<GuildContribution>，每个玩家一条，amount 为累计存款总额。
     */
    public CompletableFuture<List<GuildContribution>> getGuildContributionTotalsAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildContribution> totals = new ArrayList<>();
            try {
                String sql = "SELECT player_uuid, player_name, SUM(amount) AS total_amount " +
                             "FROM guild_contributions " +
                             "WHERE guild_id = ? AND contribution_type = 'DEPOSIT' " +
                             "GROUP BY player_uuid, player_name " +
                             "ORDER BY total_amount DESC";

                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    stmt.setInt(1, guildId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            GuildContribution c = new GuildContribution();
                            c.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
                            c.setPlayerName(rs.getString("player_name"));
                            c.setAmount(rs.getDouble("total_amount"));
                            c.setType(GuildContribution.ContributionType.DEPOSIT);
                            totals.add(c);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild deposit summary: " + e.getMessage());
            }
            return totals;
        });
    }
    
    // ==================== 辅助方法 ====================
     
     private GuildRelation createGuildRelationFromResultSet(ResultSet rs) throws SQLException {
         GuildRelation relation = new GuildRelation();
         relation.setId(rs.getInt("id"));
         relation.setGuild1Id(rs.getInt("guild1_id"));
         relation.setGuild2Id(rs.getInt("guild2_id"));
         relation.setGuild1Name(rs.getString("guild1_name"));
         relation.setGuild2Name(rs.getString("guild2_name"));
         relation.setType(GuildRelation.RelationType.valueOf(rs.getString("relation_type")));
         relation.setStatus(GuildRelation.RelationStatus.valueOf(rs.getString("status")));
         relation.setInitiatorUuid(UUID.fromString(rs.getString("initiator_uuid")));
         relation.setInitiatorName(rs.getString("initiator_name"));
         relation.setCreatedAt(parseTimestamp(rs, "created_at"));
         relation.setUpdatedAt(parseTimestamp(rs, "updated_at"));
         
         String expiresAt = rs.getString("expires_at");
         if (expiresAt != null) {
             relation.setExpiresAt(parseTimestamp(rs, "expires_at"));
         }
         
         return relation;
     }
     
     private GuildEconomy createGuildEconomyFromResultSet(ResultSet rs) throws SQLException {
         GuildEconomy economy = new GuildEconomy();
         economy.setId(rs.getInt("id"));
         economy.setGuildId(rs.getInt("guild_id"));
         economy.setBalance(rs.getDouble("balance"));
         economy.setLevel(rs.getInt("level"));
         economy.setExperience(rs.getDouble("experience"));
         economy.setMaxExperience(rs.getDouble("max_experience"));
         economy.setMaxMembers(rs.getInt("max_members"));
         economy.setLastUpdated(parseTimestamp(rs, "last_updated"));
         return economy;
     }
     
     private GuildContribution createGuildContributionFromResultSet(ResultSet rs) throws SQLException {
         GuildContribution contribution = new GuildContribution();
         contribution.setId(rs.getInt("id"));
         contribution.setGuildId(rs.getInt("guild_id"));
         contribution.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
         contribution.setPlayerName(rs.getString("player_name"));
         contribution.setAmount(rs.getDouble("amount"));
         contribution.setType(GuildContribution.ContributionType.valueOf(rs.getString("contribution_type")));
         contribution.setDescription(rs.getString("description"));
         contribution.setCreatedAt(parseTimestamp(rs, "created_at"));
         return contribution;
     }
     
     // ==================== 工会经济管理方法 ====================
     
     /**
      * 更新工会余额 (异步)
      */
     public CompletableFuture<Boolean> updateGuildBalanceAsync(int guildId, double balance) {
        return updateGuildBalanceAsync(guildId, balance, null, null);
    }

    public CompletableFuture<Boolean> updateGuildBalanceAsync(int guildId, double balance,
                                                               String operatorUuid, String operatorName) {
         return getGuildByIdAsync(guildId).thenCompose(guild -> {
             if (guild == null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return CompletableFuture.supplyAsync(() -> {
                 try {
                     String sql = "UPDATE guilds SET balance = ?, updated_at = ? WHERE id = ?";
                     
                     try (Connection conn = databaseManager.getConnection();
                          PreparedStatement stmt = conn.prepareStatement(sql)) {
                     
                         stmt.setDouble(1, balance);
                         stmt.setString(2, nowString());
                         stmt.setInt(3, guildId);
                     
                         int affectedRows = stmt.executeUpdate();
                         if (affectedRows > 0) {
                             logger.info("Guild balance updated: " + guild.getName() + " (ID: " + guildId + ") new balance: " + balance);
                             
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
                     }
                 } catch (SQLException e) {
                     logger.severe("An error occurred while updating the guild balance: " + e.getMessage());
                 }
                 return false;
             });
         });
     }
    
    /**
     * 更新工会等级 (异步)
     */
    public CompletableFuture<Boolean> updateGuildLevelAsync(int guildId, int level) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // peak_level 只升不降：降级只改 level
                String sql = "UPDATE guilds SET level = ?, peak_level = CASE WHEN peak_level IS NULL OR peak_level < ? THEN ? ELSE peak_level END WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, level);
                    stmt.setInt(2, level);
                    stmt.setInt(3, level);
                    stmt.setInt(4, guildId);
                    
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            } catch (SQLException e) {
                logger.severe("Error updating guild level: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 更新工会最大成员数 (异步)
     */
    public CompletableFuture<Boolean> updateGuildMaxMembersAsync(int guildId, int maxMembers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "UPDATE guilds SET max_members = ? WHERE id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, maxMembers);
                    stmt.setInt(2, guildId);
                    
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            } catch (SQLException e) {
                logger.severe("Error updating guild max members: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 更新工会冻结状态 (异步)
     */
    public CompletableFuture<Boolean> updateGuildFrozenStatusAsync(int guildId, boolean frozen) {
        return getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String sql = "UPDATE guilds SET frozen = ? WHERE id = ?";
                    
                    try (Connection conn = databaseManager.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        
                        stmt.setBoolean(1, frozen);
                        stmt.setInt(2, guildId);
                        
                        int affectedRows = stmt.executeUpdate();
                        if (affectedRows > 0) {
                            // 记录冻结状态变更日志
                            GuildLog.LogType logType = frozen ? GuildLog.LogType.GUILD_FROZEN : GuildLog.LogType.GUILD_UNFROZEN;
                            String description = frozen ? "工会冻结" : "工会解冻";
                            
                            logGuildActionAsync(guildId, guild.getName(), "SYSTEM", "系统",
                                logType, description, "操作: " + (frozen ? "冻结" : "解冻"));
                            
                            return true;
                        }
                    }
                } catch (SQLException e) {
                    logger.severe("Error updating guild frozen status: " + e.getMessage());
                }
                return false;
            });
        });
    }

    /**
     * 直接插入工会成员（不做已有工会检查）。
     * 仅用于建会后插入会长，以避免额外读库造成的连接争用。
     */
    private CompletableFuture<Boolean> addGuildMemberDirectAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "INSERT INTO guild_members (guild_id, player_uuid, player_name, role, joined_at) VALUES (?, ?, ?, ?, ?)";
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, guildId);
                    stmt.setString(2, playerUuid.toString());
                    stmt.setString(3, playerName);
                    stmt.setString(4, role.name());
                    stmt.setString(5, nowString());
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        try { plugin.getPermissionManager().updatePlayerPermissions(playerUuid); } catch (Exception ignored) {}
                        return true;
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error directly adding guild member: " + e.getMessage());
            }
            return false;
        });
    }

    /**
     * 检查并自动升级工会等级
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
                    try {
                        String sql = "UPDATE guilds SET level = ?, max_members = ?, peak_level = CASE WHEN peak_level IS NULL OR peak_level < ? THEN ? ELSE peak_level END, updated_at = ? WHERE id = ?";
                        
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql)) {
                            
                            stmt.setInt(1, newLevel);
                            stmt.setInt(2, newMaxMembers);
                            stmt.setInt(3, newLevel);
                            stmt.setInt(4, newLevel);
                            stmt.setString(5, nowString());
                            stmt.setInt(6, guildId);
                            
                            int affectedRows = stmt.executeUpdate();
                            if (affectedRows > 0) {
                                logger.info("Guild auto-upgraded successfully: " + guild.getName() + " (ID: " + guildId + ") level: " + currentLevel + " -> " + newLevel);
                                
                                // 记录升级日志
                                logGuildActionAsync(guildId, guild.getName(), "SYSTEM", "系统",
                                    GuildLog.LogType.GUILD_LEVEL_UP, "工会升级", "新等级: " + newLevel + ", 新最大成员数: " + newMaxMembers);
                                
                                return true;
                            }
                        }
                    } catch (SQLException e) {
                        logger.severe("Error auto-upgrading guild: " + e.getMessage());
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
     * 作为所有工会的绝对上限，即使等级系统允许更多成员也不能超过此值。
     */
    private int getGlobalMaxMembers() {
        try {
            return plugin.getConfigManager().getMainConfig().getInt("guild.max-members", 100);
        } catch (Exception e) {
            return 100;
        }
    }
    
    /**
     * 获取工会的有效最大成员数。
     * 取工会自身存储的 max_members（由等级决定）与全局配置上限的较小值。
     * 对于已超限的存量工会，此方法仅影响新成员加入，不会踢出已有成员。
     */
    public int getEffectiveMaxMembers(Guild guild) {
        int guildMax = guild.getMaxMembers();
        int globalMax = getGlobalMaxMembers();
        return Math.min(guildMax, globalMax);
    }
    
    /**
     * 检查工会是否已满员 (异步)
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
     * 通知工会成员升级成功
     */
    private void notifyGuildMembersOfUpgrade(int guildId, int newLevel, int newMaxMembers) {
        getGuildMembersAsync(guildId).thenAccept(members -> {
            // 在每位成员所在区域线程中发送消息（Folia 实体调度）
            for (GuildMember member : members) {
                Player player = Bukkit.getPlayer(member.getPlayerUuid());
                if (player != null && player.isOnline()) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = plugin.getLanguageManager().getCoreMessage(player, "economy.level-up", "&a工会升级成功！当前等级：{level}", "{level}", String.valueOf(newLevel), "{max_members}", String.valueOf(newMaxMembers));
                        player.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                    });
                }
            }
        }).exceptionally(throwable -> {
            logger.warning("Error notifying guild members of upgrade: " + throwable.getMessage());
            return null;
        });
    }
    
    // ==================== 工会日志系统 ====================
    
    /**
     * 记录工会日志 (异步)
     */
    public CompletableFuture<Boolean> logGuildActionAsync(int guildId, String guildName, String playerUuid, 
                                                        String playerName, GuildLog.LogType logType, 
                                                        String description, String details) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "INSERT INTO guild_logs (guild_id, guild_name, player_uuid, player_name, log_type, description, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                 
                     stmt.setInt(1, guildId);
                     stmt.setString(2, guildName);
                     stmt.setString(3, playerUuid);
                     stmt.setString(4, playerName);
                     stmt.setString(5, logType.name());
                     stmt.setString(6, description);
                     stmt.setString(7, details);
                     stmt.setString(8, nowString());
                 
                     int affectedRows = stmt.executeUpdate();
                     return affectedRows > 0;
                 }
            } catch (SQLException e) {
                logger.severe("Error recording guild log: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 记录工会日志 (同步包装器)
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
     * 获取工会日志列表 (异步)
     */
    public CompletableFuture<List<GuildLog>> getGuildLogsAsync(int guildId, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildLog> logs = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_logs WHERE guild_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    stmt.setInt(2, limit);
                    stmt.setInt(3, offset);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            GuildLog log = createGuildLogFromResultSet(rs);
                            logs.add(log);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild logs: " + e.getMessage());
            }
            return logs;
        });
    }
    
    /**
     * 获取工会日志列表 (同步包装器)
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
     * 获取工会日志总数 (异步)
     */
    public CompletableFuture<Integer> getGuildLogsCountAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT COUNT(*) FROM guild_logs WHERE guild_id = ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, guildId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild log count: " + e.getMessage());
            }
            return 0;
        });
    }
    
    /**
     * 获取工会日志总数 (同步包装器)
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
     * 从ResultSet创建GuildLog对象
     */
    private GuildLog createGuildLogFromResultSet(ResultSet rs) throws SQLException {
        GuildLog log = new GuildLog();
        log.setId(rs.getInt("id"));
        log.setGuildId(rs.getInt("guild_id"));
        log.setGuildName(rs.getString("guild_name"));
        log.setPlayerUuid(rs.getString("player_uuid"));
        log.setPlayerName(rs.getString("player_name"));
        log.setLogType(GuildLog.LogType.valueOf(rs.getString("log_type")));
        log.setDescription(rs.getString("description"));
        log.setDetails(rs.getString("details"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            try {
                log.setCreatedAt(LocalDateTime.parse(createdAtStr, com.guild.core.time.TimeProvider.FULL_FORMATTER));
            } catch (Exception e1) {
                try {
                    log.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
                } catch (Exception e2) {
                    logger.warning("Failed to parse log creation time: " + createdAtStr);
                    log.setCreatedAt(com.guild.core.time.TimeProvider.nowLocalDateTime());
                }
            }
        } else {
            log.setCreatedAt(com.guild.core.time.TimeProvider.nowLocalDateTime());
        }
        
        return log;
    }
    
    /**
     * 清理旧日志 (异步)
     */
    public CompletableFuture<Integer> cleanOldLogsAsync(int daysToKeep) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 统一使用参数绑定的阈值时间（字符串），避免数据库侧时区差异
                String sql = "DELETE FROM guild_logs WHERE created_at < ?";
                
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    // 计算阈值字符串：当前系统时间减去 daysToKeep 天，格式 yyyy-MM-dd HH:mm:ss
                    String threshold = com.guild.core.time.TimeProvider.nowLocalDateTime().minusDays(daysToKeep)
                        .format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
                    stmt.setString(1, threshold);
                    int affectedRows = stmt.executeUpdate();
                    logger.info("Cleaned up " + affectedRows + " old log records");
                    return affectedRows;
                }
            } catch (SQLException e) {
                logger.severe("Error cleaning up old logs: " + e.getMessage());
                return 0;
            }
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
