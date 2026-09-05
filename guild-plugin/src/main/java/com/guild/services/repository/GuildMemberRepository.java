package com.guild.services.repository;

import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.models.GuildMember;

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

/**
 * 公会成员表（guild_members）数据访问层。
 */
public class GuildMemberRepository {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public GuildMemberRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public CompletableFuture<GuildMember> findByPlayerUuidAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guild_members WHERE player_uuid = ?";
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
                logger.severe("Error fetching guild member by uuid: " + e.getMessage());
            }
            return null;
        });
    }

    public CompletableFuture<GuildMember> findByGuildAndPlayerUuidAsync(int guildId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM guild_members WHERE guild_id = ? AND player_uuid = ?";
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, guildId);
                    stmt.setString(2, playerUuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return mapRow(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild member by guild and uuid: " + e.getMessage());
            }
            return null;
        });
    }

    public CompletableFuture<Integer> countByGuildIdAsync(int guildId) {
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

    public CompletableFuture<List<GuildMember>> findAllByGuildIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildMember> members = new ArrayList<>();
            try {
                String sql = "SELECT * FROM guild_members WHERE guild_id = ? ORDER BY role ASC, joined_at ASC";
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, guildId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            members.add(mapRow(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error fetching guild member list: " + e.getMessage());
            }
            return members;
        });
    }

    public CompletableFuture<Boolean> insertAsync(int guildId, UUID playerUuid, String playerName,
                                                  GuildMember.Role role, String joinedAt) {
        return CompletableFuture.supplyAsync(() -> insert(guildId, playerUuid, playerName, role, joinedAt));
    }

    public boolean insert(int guildId, UUID playerUuid, String playerName,
                          GuildMember.Role role, String joinedAt) {
        try {
            String sql = "INSERT INTO guild_members (guild_id, player_uuid, player_name, role, joined_at) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                stmt.setString(2, playerUuid.toString());
                stmt.setString(3, playerName);
                stmt.setString(4, role.name());
                stmt.setString(5, joinedAt);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error inserting guild member: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> deleteByPlayerUuidAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> deleteByPlayerUuid(playerUuid));
    }

    public boolean deleteByPlayerUuid(UUID playerUuid) {
        try {
            String sql = "DELETE FROM guild_members WHERE player_uuid = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error deleting guild member: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> updateRoleAsync(UUID playerUuid, int guildId, GuildMember.Role newRole) {
        return CompletableFuture.supplyAsync(() -> updateRole(playerUuid, guildId, newRole));
    }

    public boolean updateRole(UUID playerUuid, int guildId, GuildMember.Role newRole) {
        try {
            String sql = "UPDATE guild_members SET role = ? WHERE player_uuid = ? AND guild_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newRole.name());
                stmt.setString(2, playerUuid.toString());
                stmt.setInt(3, guildId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild member role: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> updateRoleByPlayerUuidAsync(UUID playerUuid, GuildMember.Role newRole) {
        return CompletableFuture.supplyAsync(() -> updateRoleByPlayerUuid(playerUuid, newRole));
    }

    public boolean updateRoleByPlayerUuid(UUID playerUuid, GuildMember.Role newRole) {
        try {
            String sql = "UPDATE guild_members SET role = ? WHERE player_uuid = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newRole.name());
                stmt.setString(2, playerUuid.toString());
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error directly updating guild member role: " + e.getMessage());
            return false;
        }
    }

    /** 事务内：原会长 LEADER → MEMBER */
    public void demoteLeaderToMember(Connection conn, int guildId, UUID oldLeaderUuid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE guild_members SET role = 'MEMBER' WHERE guild_id = ? AND player_uuid = ? AND role = 'LEADER'")) {
            stmt.setInt(1, guildId);
            stmt.setString(2, oldLeaderUuid.toString());
            stmt.executeUpdate();
        }
    }

    /** 事务内：目标成员 → LEADER，返回影响行数 */
    public int promoteMemberToLeader(Connection conn, int guildId, UUID newLeaderUuid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE guild_members SET role = 'LEADER' WHERE guild_id = ? AND player_uuid = ?")) {
            stmt.setInt(1, guildId);
            stmt.setString(2, newLeaderUuid.toString());
            return stmt.executeUpdate();
        }
    }

    /** 删除公会全部成员（删公会/强制删公会时使用） */
    public int deleteAllByGuildId(Connection conn, int guildId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM guild_members WHERE guild_id = ?")) {
            stmt.setInt(1, guildId);
            return stmt.executeUpdate();
        }
    }

    public GuildMember mapRow(ResultSet rs) throws SQLException {
        GuildMember member = new GuildMember();
        member.setId(rs.getInt("id"));
        member.setGuildId(rs.getInt("guild_id"));
        member.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        member.setPlayerName(rs.getString("player_name"));
        member.setRole(GuildMember.Role.valueOf(rs.getString("role")));
        member.setJoinedAt(parseJoinedAt(rs, "joined_at"));
        return member;
    }

    private LocalDateTime parseJoinedAt(ResultSet rs, String columnName) throws SQLException {
        String s = rs.getString(columnName);
        if (s != null && !s.isEmpty()) {
            try {
                return LocalDateTime.parse(s, TimeProvider.FULL_FORMATTER);
            } catch (Exception ignore) {
                try {
                    return LocalDateTime.parse(s.replace(" ", "T"));
                } catch (Exception ex) {
                    logger.warning("Failed to parse member joined_at: " + s);
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
