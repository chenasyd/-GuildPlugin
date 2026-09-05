package com.guild.services;

import com.guild.core.utils.QuietLog;
import com.guild.models.GuildLog;
import com.guild.models.GuildMember;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class GuildLifecycleService extends GuildServiceSupport {

    GuildLifecycleService(GuildServiceContext ctx) {
        super(ctx);
    }

    /**
     * 创建公会 (异步)
     */
    public CompletableFuture<Boolean> createGuildAsync(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        return guardServiceFutureBoolean("createGuild", ctx.serviceRef.getGuildByNameAsync(name).thenCompose(existingGuildByName -> {
            if (existingGuildByName != null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return ctx.serviceRef.getGuildByTagAsync(tag).thenCompose(existingGuildByTag -> {
                if (existingGuildByTag != null) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return ctx.repos.guilds().insertAsync(name, tag, description, leaderUuid, leaderName,
                        nowString(), nowString()).thenApply(guildId -> {
                    if (guildId > 0) {
                        QuietLog.system("Guild created successfully: " + name + " (ID: " + guildId + ")");
                    }
                    return guildId;
                }).thenCompose(guildId -> {
                    if ((Integer) guildId > 0) {
                        // 添加会长为公会成员（避免重复查询）
                        return ctx.members.addGuildMemberDirectAsync((Integer) guildId, leaderUuid, leaderName, GuildMember.Role.LEADER)
                            .thenCompose(success -> {
                                if (success) {
                                    fireGuildCreate((Integer) guildId, name, leaderName);
                                    // 记录公会创建日志
                                    return ctx.serviceRef.logGuildActionAsync((Integer) guildId, name, leaderUuid.toString(), leaderName,
                                        GuildLog.LogType.GUILD_CREATED, "创建公会", "公会名称: " + name + ", 标签: " + tag)
                                        .thenApply(logSuccess -> success);
                                }
                                return CompletableFuture.completedFuture(success);
                            });
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
        }));
    }
    
    /**
     * 创建公会 (同步包装器)
     */
    public boolean createGuild(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        try {
            return createGuildAsync(name, tag, description, leaderUuid, leaderName).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception creating guild: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 删除公会 (异步)
     */
    public CompletableFuture<Boolean> deleteGuildAsync(int guildId, UUID requesterUuid) {
        return guardServiceFutureBoolean("deleteGuild", ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return ctx.serviceRef.getGuildMemberAsync(requesterUuid).thenCompose(member -> {
                // 检查权限
                if (member == null || member.getGuildId() != guildId || member.getRole() != GuildMember.Role.LEADER) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        // 获取公会余额用于退款
                        double guildBalance = guild.getBalance();
                        
                        // 删除公会成员
                        try (Connection conn = ctx.databaseManager.getConnection()) {
                            ctx.repos.members().deleteAllByGuildId(conn, guildId);
                        }

                        deleteWarehouseData(guildId);

                        if (ctx.repos.guilds().deleteById(guildId)) {
                            QuietLog.system("Guild deleted successfully: " + guild.getName() + " (ID: " + guildId + ")");

                            // 退款给会长（如果经济系统可用）
                            if (guildBalance > 0 && ctx.plugin.getEconomyManager().isVaultAvailable()) {
                                try {
                                    org.bukkit.entity.Player leaderPlayer = org.bukkit.Bukkit.getPlayer(guild.getLeaderUuid());
                                    if (leaderPlayer != null && leaderPlayer.isOnline()) {
                                        ctx.plugin.getEconomyManager().deposit(leaderPlayer, guildBalance);
                                        String message = ctx.plugin.getLanguageManager().getCoreMessage(leaderPlayer, "economy.disband-compensation", "&a公会解散，您获得了 {amount} 金币补偿！", "{amount}", ctx.plugin.getEconomyManager().format(guildBalance));
                                        leaderPlayer.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                                    }
                                } catch (Exception e) {
                                    ctx.logger.warning("Error refunding leader: " + e.getMessage());
                                }
                            }

                            // 记录公会解散日志
                            ctx.serviceRef.logGuildActionAsync(guildId, guild.getName(), guild.getLeaderUuid().toString(), guild.getLeaderName(),
                                GuildLog.LogType.GUILD_DISSOLVED, "公会解散", "公会余额: " + guildBalance + " 金币");

                            // 分发公会解散事件给模块
                            fireGuildDelete(guildId, guild.getName(), guild.getLeaderName());

                            return true;
                        }
                    } catch (SQLException e) {
                        ctx.logger.severe("Error deleting guild: " + e.getMessage());
                    }
                    return false;
                });
            });
        }));
    }
    
    /**
     * 删除公会 (同步包装器)
     */
    public boolean deleteGuild(int guildId, UUID requesterUuid) {
        try {
            return deleteGuildAsync(guildId, requesterUuid).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception deleting guild: " + e.getMessage());
            return false;
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
        return guardServiceFutureBoolean("forceDeleteGuild", ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // 获取公会余额用于退款（退款给会长，而非管理员）
                    double guildBalance = guild.getBalance();
                    
                    // 删除所有公会成员
                    try (Connection conn = ctx.databaseManager.getConnection()) {
                        ctx.repos.members().deleteAllByGuildId(conn, guildId);
                    }

                    deleteWarehouseData(guildId);

                    if (ctx.repos.guilds().deleteById(guildId)) {
                        QuietLog.system("Admin force-deleted guild: " + guild.getName() + " (ID: " + guildId + ", by: " + adminUuid + ")");

                        // 退款给会长（如果经济系统可用）— 注意：退款给会长而非管理员
                        if (guildBalance > 0 && ctx.plugin.getEconomyManager().isVaultAvailable()) {
                            try {
                                org.bukkit.entity.Player leaderPlayer = org.bukkit.Bukkit.getPlayer(guild.getLeaderUuid());
                                if (leaderPlayer != null && leaderPlayer.isOnline()) {
                                    ctx.plugin.getEconomyManager().deposit(leaderPlayer, guildBalance);
                                    String message = ctx.plugin.getLanguageManager().getCoreMessage(leaderPlayer, "economy.disband-compensation", "&a公会解散，您获得了 {amount} 金币补偿！", "{amount}", ctx.plugin.getEconomyManager().format(guildBalance));
                                    leaderPlayer.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                                }
                            } catch (Exception e) {
                                ctx.logger.warning("Error refunding to guild leader: " + e.getMessage());
                            }
                        }

                        // 记录公会强制解散日志
                        ctx.serviceRef.logGuildActionAsync(guildId, guild.getName(), guild.getLeaderUuid().toString(), guild.getLeaderName(),
                            GuildLog.LogType.GUILD_DISSOLVED, "Admin force deleted",
                            "By: " + adminUuid + ", balance: " + guildBalance + " coins");

                        // 分发公会解散事件给模块
                        fireGuildDelete(guildId, guild.getName(), guild.getLeaderName());

                        return true;
                    }
                } catch (SQLException e) {
                    ctx.logger.severe("Error during admin force-delete: " + e.getMessage());
                }
                return false;
            });
        }));
    }

    /**
     * 更新公会信息 (异步)
     */
    public CompletableFuture<Boolean> updateGuildAsync(int guildId, String name, String tag, String description, UUID requesterUuid) {
        return guardServiceFutureBoolean("updateGuild", ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(guild -> {
            if (guild == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            return ctx.serviceRef.getGuildMemberAsync(requesterUuid).thenCompose(member -> {
                // 检查权限
                if (member == null || member.getGuildId() != guildId || 
                    (member.getRole() != GuildMember.Role.LEADER && member.getRole() != GuildMember.Role.OFFICER)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                // 检查名称和标签是否与其他公会冲突
                CompletableFuture<Boolean> nameCheck = CompletableFuture.completedFuture(true);
                if (name != null && !name.equals(guild.getName())) {
                    nameCheck = ctx.serviceRef.getGuildByNameAsync(name).thenApply(existingGuild -> existingGuild == null);
                }
                
                CompletableFuture<Boolean> tagCheck = CompletableFuture.completedFuture(true);
                if (tag != null && !tag.equals(guild.getTag())) {
                    tagCheck = ctx.serviceRef.getGuildByTagAsync(tag).thenApply(existingGuild -> existingGuild == null);
                }
                
                return nameCheck.thenCombine(tagCheck, (nameValid, tagValid) -> nameValid && tagValid)
                    .thenCompose(valid -> {
                        if (!valid) {
                            return CompletableFuture.completedFuture(false);
                        }
                        
                        return CompletableFuture.supplyAsync(() -> {
                            if (ctx.repos.guilds().updateInfo(guildId, name, tag, description, nowString())) {
                                QuietLog.system("Guild info updated successfully: " + guild.getName() + " (ID: " + guildId + ")");
                                return true;
                            }
                            return false;
                        });
                    });
            });
        }));
    }
    
    /**
     * 更新公会信息 (同步包装器)
     */
    public boolean updateGuild(int guildId, String name, String tag, String description, UUID requesterUuid) {
        try {
            return updateGuildAsync(guildId, name, tag, description, requesterUuid).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception updating guild info: " + e.getMessage());
            return false;
        }
    }

     public CompletableFuture<Boolean> updateGuildDescriptionAsync(int guildId, String description) {
         return guardServiceFutureBoolean("updateGuildDescription", CompletableFuture.supplyAsync(() -> ctx.repos.guilds().updateDescription(guildId, description)));
     }

}
