package com.guild.services.repository;

import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.models.GuildEconomy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class GuildEconomyRepository {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public GuildEconomyRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public CompletableFuture<Boolean> insertAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> insert(guildId));
    }

    public boolean insert(int guildId) {
        try {
            String sql = "INSERT INTO guild_economy (guild_id, balance, level, experience, max_experience, max_members) "
                    + "VALUES (?, 0.0, 1, 0.0, 5000.0, 6)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error inserting guild economy: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<GuildEconomy> findByGuildIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> findByGuildId(guildId));
    }

    public GuildEconomy findByGuildId(int guildId) {
        try {
            String sql = "SELECT * FROM guild_economy WHERE guild_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild economy info: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<Boolean> updateAsync(int guildId, double balance, int level, double experience,
                                                  double maxExperience, int maxMembers, String lastUpdated) {
        return CompletableFuture.supplyAsync(() ->
                update(guildId, balance, level, experience, maxExperience, maxMembers, lastUpdated));
    }

    public boolean update(int guildId, double balance, int level, double experience, double maxExperience,
                          int maxMembers, String lastUpdated) {
        try {
            String sql = "UPDATE guild_economy SET balance = ?, level = ?, experience = ?, max_experience = ?, "
                    + "max_members = ?, last_updated = ? WHERE guild_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, balance);
                stmt.setInt(2, level);
                stmt.setDouble(3, experience);
                stmt.setDouble(4, maxExperience);
                stmt.setInt(5, maxMembers);
                stmt.setString(6, lastUpdated);
                stmt.setInt(7, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild economy: " + e.getMessage());
            return false;
        }
    }

    public GuildEconomy mapRow(ResultSet rs) throws SQLException {
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
