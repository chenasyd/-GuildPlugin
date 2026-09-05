package com.guild.services.repository;

import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.models.Guild;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * 公会表（guilds）数据访问层。
 */
public class GuildRepository {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public GuildRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public CompletableFuture<Guild> findByIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> findById(guildId));
    }

    public Guild findById(int guildId) {
        try {
            String sql = "SELECT * FROM guilds WHERE id = ?";
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
            logger.severe("Error fetching guild by ID: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<Guild> findByNameAsync(String name) {
        return CompletableFuture.supplyAsync(() -> findByName(name));
    }

    public Guild findByName(String name) {
        try {
            String sql = "SELECT * FROM guilds WHERE name = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild by name: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<Guild> findByTagAsync(String tag) {
        return CompletableFuture.supplyAsync(() -> findByTag(tag));
    }

    public Guild findByTag(String tag) {
        try {
            String sql = "SELECT * FROM guilds WHERE tag = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tag);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild by tag: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<Guild> findByPlayerUuidAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> findByPlayerUuid(playerUuid));
    }

    public Guild findByPlayerUuid(UUID playerUuid) {
        try {
            String sql = "SELECT g.* FROM guilds g "
                    + "INNER JOIN guild_members gm ON g.id = gm.guild_id "
                    + "WHERE gm.player_uuid = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching player guild: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<List<Guild>> findAllAsync() {
        return CompletableFuture.supplyAsync(this::findAll);
    }

    public List<Guild> findAll() {
        List<Guild> guilds = new ArrayList<>();
        try {
            String sql = "SELECT * FROM guilds ORDER BY created_at DESC";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    guilds.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching all guilds: " + e.getMessage());
        }
        return guilds;
    }

    public CompletableFuture<Integer> insertAsync(String name, String tag, String description,
                                                  UUID leaderUuid, String leaderName,
                                                  String createdAt, String updatedAt) {
        return CompletableFuture.supplyAsync(() ->
                insert(name, tag, description, leaderUuid, leaderName, createdAt, updatedAt));
    }

    /** @return 新公会 ID，失败返回 -1 */
    public int insert(String name, String tag, String description,
                      UUID leaderUuid, String leaderName,
                      String createdAt, String updatedAt) {
        try {
            String sql = "INSERT INTO guilds (name, tag, description, leader_uuid, leader_name, "
                    + "balance, level, peak_level, max_members, frozen, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, 0.0, 1, 1, 6, 0, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setString(2, tag);
                stmt.setString(3, description);
                stmt.setString(4, leaderUuid.toString());
                stmt.setString(5, leaderName);
                stmt.setString(6, createdAt);
                stmt.setString(7, updatedAt);
                if (stmt.executeUpdate() > 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error inserting guild: " + e.getMessage());
        }
        return -1;
    }

    public CompletableFuture<Boolean> deleteByIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> deleteById(guildId));
    }

    public boolean deleteById(int guildId) {
        try {
            String sql = "DELETE FROM guilds WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error deleting guild: " + e.getMessage());
            return false;
        }
    }

    public boolean updateInfo(int guildId, String name, String tag, String description, String updatedAt) {
        try {
            String sql = "UPDATE guilds SET name = COALESCE(?, name), tag = COALESCE(?, tag), "
                    + "description = COALESCE(?, description), updated_at = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, tag);
                stmt.setString(3, description);
                stmt.setString(4, updatedAt);
                stmt.setInt(5, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild info: " + e.getMessage());
            return false;
        }
    }

    public boolean updateBalance(int guildId, double balance, String updatedAt) {
        try {
            String sql = "UPDATE guilds SET balance = ?, updated_at = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, balance);
                stmt.setString(2, updatedAt);
                stmt.setInt(3, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild balance: " + e.getMessage());
            return false;
        }
    }

    public boolean updateLevel(int guildId, int level) {
        try {
            String sql = "UPDATE guilds SET level = ?, peak_level = CASE WHEN peak_level IS NULL "
                    + "OR peak_level < ? THEN ? ELSE peak_level END WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, level);
                stmt.setInt(2, level);
                stmt.setInt(3, level);
                stmt.setInt(4, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild level: " + e.getMessage());
            return false;
        }
    }

    public boolean updateMaxMembers(int guildId, int maxMembers) {
        try {
            String sql = "UPDATE guilds SET max_members = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, maxMembers);
                stmt.setInt(2, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild max members: " + e.getMessage());
            return false;
        }
    }

    public boolean updateFrozen(int guildId, boolean frozen) {
        try {
            String sql = "UPDATE guilds SET frozen = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, frozen);
                stmt.setInt(2, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild frozen status: " + e.getMessage());
            return false;
        }
    }

    public boolean updateLevelMaxMembersAndPeak(int guildId, int level, int maxMembers, String updatedAt) {
        try {
            String sql = "UPDATE guilds SET level = ?, max_members = ?, "
                    + "peak_level = CASE WHEN peak_level IS NULL OR peak_level < ? THEN ? ELSE peak_level END, "
                    + "updated_at = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, level);
                stmt.setInt(2, maxMembers);
                stmt.setInt(3, level);
                stmt.setInt(4, level);
                stmt.setString(5, updatedAt);
                stmt.setInt(6, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild level and max members: " + e.getMessage());
            return false;
        }
    }

    /** 事务内：更新会长，返回影响行数 */
    public int updateLeader(Connection conn, int guildId, UUID newLeaderUuid, String newLeaderName)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE guilds SET leader_uuid = ?, leader_name = ? WHERE id = ?")) {
            stmt.setString(1, newLeaderUuid.toString());
            stmt.setString(2, newLeaderName);
            stmt.setInt(3, guildId);
            return stmt.executeUpdate();
        }
    }

    public boolean updateHome(int guildId, String world, double x, double y, double z,
                              float yaw, float pitch, String updatedAt) {
        try {
            String sql = "UPDATE guilds SET home_world = ?, home_x = ?, home_y = ?, home_z = ?, "
                    + "home_yaw = ?, home_pitch = ?, updated_at = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, world);
                stmt.setDouble(2, x);
                stmt.setDouble(3, y);
                stmt.setDouble(4, z);
                stmt.setFloat(5, yaw);
                stmt.setFloat(6, pitch);
                stmt.setString(7, updatedAt);
                stmt.setInt(8, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild home: " + e.getMessage());
            return false;
        }
    }

    public boolean updateDescription(int guildId, String description) {
        try {
            String sql = "UPDATE guilds SET description = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, description);
                stmt.setInt(2, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild description: " + e.getMessage());
            return false;
        }
    }

    public Guild mapRow(ResultSet rs) throws SQLException {
        Guild guild = new Guild();
        guild.setId(rs.getInt("id"));
        guild.setName(rs.getString("name"));
        guild.setTag(rs.getString("tag"));
        guild.setDescription(rs.getString("description"));
        guild.setLeaderUuid(UUID.fromString(rs.getString("leader_uuid")));
        guild.setLeaderName(rs.getString("leader_name"));

        String homeWorld = rs.getString("home_world");
        guild.setHomeWorld(homeWorld);
        if (homeWorld != null) {
            guild.setHomeX(rs.getDouble("home_x"));
            guild.setHomeY(rs.getDouble("home_y"));
            guild.setHomeZ(rs.getDouble("home_z"));
            guild.setHomeYaw(rs.getFloat("home_yaw"));
            guild.setHomePitch(rs.getFloat("home_pitch"));
        } else {
            guild.setHomeX(0.0);
            guild.setHomeY(0.0);
            guild.setHomeZ(0.0);
            guild.setHomeYaw(0.0f);
            guild.setHomePitch(0.0f);
        }

        guild.setCreatedAt(parseTimestamp(rs, "created_at"));
        guild.setUpdatedAt(parseTimestamp(rs, "updated_at"));

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
