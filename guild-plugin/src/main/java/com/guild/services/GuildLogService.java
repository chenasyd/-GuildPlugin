package com.guild.services;

import com.guild.core.time.TimeProvider;
import com.guild.core.utils.QuietLog;
import com.guild.models.GuildLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class GuildLogService extends GuildServiceSupport {

    GuildLogService(GuildServiceContext ctx) {
        super(ctx);
    }

    // ==================== 公会日志系统 ====================
    
    /**
     * 记录公会日志 (异步)
     */
    public CompletableFuture<Boolean> logGuildActionAsync(int guildId, String guildName, String playerUuid, 
                                                        String playerName, GuildLog.LogType logType, 
                                                        String description, String details) {
        return guardServiceFutureBoolean("logGuildAction", ctx.repos.logs().insertAsync(guildId, guildName, playerUuid, playerName, logType, description, details, nowString()));
    }
    
    /**
     * 记录公会日志 (同步包装器)
     */
    public boolean logGuildAction(int guildId, String guildName, String playerUuid, String playerName, 
                                GuildLog.LogType logType, String description, String details) {
        try {
            return logGuildActionAsync(guildId, guildName, playerUuid, playerName, logType, description, details).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception recording guild log: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取公会日志列表 (异步)
     */
    public CompletableFuture<List<GuildLog>> getGuildLogsAsync(int guildId, int limit, int offset) {
        return guardServiceFutureList("getGuildLogs", ctx.repos.logs().findByGuildIdAsync(guildId, limit, offset));
    }
    
    /**
     * 获取公会日志列表 (同步包装器)
     */
    public List<GuildLog> getGuildLogs(int guildId, int limit, int offset) {
        try {
            return getGuildLogsAsync(guildId, limit, offset).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching guild logs: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取公会日志总数 (异步)
     */
    public CompletableFuture<Integer> getGuildLogsCountAsync(int guildId) {
        return guardServiceFutureInt("getGuildLogsCount", ctx.repos.logs().countByGuildIdAsync(guildId));
    }
    
    /**
     * 获取公会日志总数 (同步包装器)
     */
    public int getGuildLogsCount(int guildId) {
        try {
            return getGuildLogsCountAsync(guildId).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching guild log count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 清理旧日志 (异步)
     */
    public CompletableFuture<Integer> cleanOldLogsAsync(int daysToKeep) {
        String threshold = TimeProvider.nowLocalDateTime().minusDays(daysToKeep)
                .format(TimeProvider.FULL_FORMATTER);
        return guardServiceFutureInt("cleanOldLogs", ctx.repos.logs().deleteOlderThanAsync(threshold).thenApply(affectedRows -> {
            QuietLog.system("Cleaned up " + affectedRows + " old log records");
            return affectedRows;
        }));
    }
    
    /**
     * 清理旧日志 (同步包装器)
     */
    public int cleanOldLogs(int daysToKeep) {
        try {
            return cleanOldLogsAsync(daysToKeep).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception cleaning up old logs: " + e.getMessage());
            return 0;
        }
    }

}
