package com.guild.services.repository;

import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.models.GuildLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class GuildLogRepository {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public GuildLogRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public CompletableFuture<Boolean> insertAsync(int guildId, String guildName, String playerUuid, String playerName,
                                                  GuildLog.LogType logType, String description, String details,
                                                  String createdAt) {
        return CompletableFuture.supplyAsync(() ->
                insert(guildId, guildName, playerUuid, playerName, logType, description, details, createdAt));
    }

    public boolean insert(int guildId, String guildName, String playerUuid, String playerName,
                          GuildLog.LogType logType, String description, String details, String createdAt) {
        try {
            String sql = "INSERT INTO guild_logs (guild_id, guild_name, player_uuid, player_name, log_type, "
                    + "description, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                stmt.setString(2, guildName);
                stmt.setString(3, playerUuid);
                stmt.setString(4, playerName);
                stmt.setString(5, logType.name());
                stmt.setString(6, description);
                stmt.setString(7, details);
                stmt.setString(8, createdAt);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error inserting guild log: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<List<GuildLog>> findByGuildIdAsync(int guildId, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> findByGuildId(guildId, limit, offset));
    }

    public List<GuildLog> findByGuildId(int guildId, int limit, int offset) {
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
                        logs.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild logs: " + e.getMessage());
        }
        return logs;
    }

    public CompletableFuture<Integer> countByGuildIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> countByGuildId(guildId));
    }

    public int countByGuildId(int guildId) {
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
    }

    public CompletableFuture<Integer> deleteOlderThanAsync(String threshold) {
        return CompletableFuture.supplyAsync(() -> deleteOlderThan(threshold));
    }

    public int deleteOlderThan(String threshold) {
        try {
            String sql = "DELETE FROM guild_logs WHERE created_at < ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, threshold);
                return stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.severe("Error cleaning up old guild logs: " + e.getMessage());
            return 0;
        }
    }

    public GuildLog mapRow(ResultSet rs) throws SQLException {
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
                log.setCreatedAt(LocalDateTime.parse(createdAtStr, TimeProvider.FULL_FORMATTER));
            } catch (Exception e1) {
                try {
                    log.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
                } catch (Exception e2) {
                    logger.warning("Failed to parse log creation time: " + createdAtStr);
                    log.setCreatedAt(TimeProvider.nowLocalDateTime());
                }
            }
        } else {
            log.setCreatedAt(TimeProvider.nowLocalDateTime());
        }
        return log;
    }
}
