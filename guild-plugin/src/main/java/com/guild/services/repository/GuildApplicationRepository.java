package com.guild.services.repository;

import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.models.GuildApplication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class GuildApplicationRepository {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public GuildApplicationRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public CompletableFuture<Boolean> insertAsync(int guildId, UUID playerUuid, String playerName,
                                                  String message, GuildApplication.ApplicationStatus status,
                                                  String createdAt) {
        return CompletableFuture.supplyAsync(() ->
                insert(guildId, playerUuid, playerName, message, status, createdAt));
    }

    public boolean insert(int guildId, UUID playerUuid, String playerName, String message,
                          GuildApplication.ApplicationStatus status, String createdAt) {
        try {
            String sql = "INSERT INTO guild_applications (guild_id, player_uuid, player_name, message, status, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                stmt.setString(2, playerUuid.toString());
                stmt.setString(3, playerName);
                stmt.setString(4, message);
                stmt.setString(5, status.name());
                stmt.setString(6, createdAt);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error inserting guild application: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> updateStatusAsync(int applicationId, GuildApplication.ApplicationStatus status) {
        return CompletableFuture.supplyAsync(() -> updateStatus(applicationId, status));
    }

    public boolean updateStatus(int applicationId, GuildApplication.ApplicationStatus status) {
        try {
            String sql = "UPDATE guild_applications SET status = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status.name());
                stmt.setInt(2, applicationId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild application status: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> hasPendingAsync(UUID playerUuid, int guildId) {
        return CompletableFuture.supplyAsync(() -> hasPending(playerUuid, guildId));
    }

    public boolean hasPending(UUID playerUuid, int guildId) {
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
    }

    public CompletableFuture<GuildApplication> findByIdAsync(int applicationId) {
        return CompletableFuture.supplyAsync(() -> findById(applicationId));
    }

    public GuildApplication findById(int applicationId) {
        try {
            String sql = "SELECT * FROM guild_applications WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, applicationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching application by ID: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<List<GuildApplication>> findAllByGuildIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> findAllByGuildId(guildId));
    }

    public List<GuildApplication> findAllByGuildId(int guildId) {
        return queryList("SELECT * FROM guild_applications WHERE guild_id = ? ORDER BY created_at DESC", guildId);
    }

    public CompletableFuture<List<GuildApplication>> findAllByPlayerUuidAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> findAllByPlayerUuid(playerUuid));
    }

    public List<GuildApplication> findAllByPlayerUuid(UUID playerUuid) {
        List<GuildApplication> applications = new ArrayList<>();
        try {
            String sql = "SELECT * FROM guild_applications WHERE player_uuid = ? ORDER BY created_at DESC";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        applications.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching player application list: " + e.getMessage());
        }
        return applications;
    }

    public CompletableFuture<List<GuildApplication>> findPendingByGuildIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> findPendingByGuildId(guildId));
    }

    public List<GuildApplication> findPendingByGuildId(int guildId) {
        return queryList("SELECT * FROM guild_applications WHERE guild_id = ? AND status = 'PENDING' ORDER BY created_at DESC",
                guildId);
    }

    public CompletableFuture<List<GuildApplication>> findHistoryByGuildIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> findHistoryByGuildId(guildId));
    }

    public List<GuildApplication> findHistoryByGuildId(int guildId) {
        return queryList("SELECT * FROM guild_applications WHERE guild_id = ? AND status != 'PENDING' ORDER BY created_at DESC",
                guildId);
    }

    private List<GuildApplication> queryList(String sql, int guildId) {
        List<GuildApplication> applications = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    applications.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild application list: " + e.getMessage());
        }
        return applications;
    }

    public GuildApplication mapRow(ResultSet rs) throws SQLException {
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

    private LocalDateTime parseTimestamp(ResultSet rs, String columnName) throws SQLException {
        String s = rs.getString(columnName);
        if (s != null && !s.isEmpty()) {
            try {
                return LocalDateTime.parse(s, TimeProvider.FULL_FORMATTER);
            } catch (Exception ignore) {
                try {
                    return LocalDateTime.parse(s.replace(" ", "T"));
                } catch (Exception ex) {
                    logger.warning("Failed to parse timestamp: " + s);
                }
            }
        }
        try {
            Timestamp ts = rs.getTimestamp(columnName);
            if (ts != null) {
                return ts.toLocalDateTime();
            }
        } catch (SQLException ignore) {
            // fall through
        }
        return LocalDateTime.now();
    }
}
