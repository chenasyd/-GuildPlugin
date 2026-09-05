package com.guild.services.repository;

import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.models.GuildRelation;

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
 * 公会关系表（guild_relations）数据访问层。
 */
public class GuildRelationRepository {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public GuildRelationRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public CompletableFuture<GuildRelation> findByIdAsync(int relationId) {
        return CompletableFuture.supplyAsync(() -> findById(relationId));
    }

    public GuildRelation findById(int relationId) {
        try {
            String sql = "SELECT * FROM guild_relations WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, relationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild relation by id: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<GuildRelation> findByGuildPairAsync(int guild1Id, int guild2Id) {
        return CompletableFuture.supplyAsync(() -> findByGuildPair(guild1Id, guild2Id));
    }

    public GuildRelation findByGuildPair(int guild1Id, int guild2Id) {
        try {
            String sql = "SELECT * FROM guild_relations WHERE (guild1_id = ? AND guild2_id = ?) "
                    + "OR (guild1_id = ? AND guild2_id = ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guild1Id);
                stmt.setInt(2, guild2Id);
                stmt.setInt(3, guild2Id);
                stmt.setInt(4, guild1Id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild relation: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<List<GuildRelation>> findAllByGuildIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> findAllByGuildId(guildId));
    }

    public List<GuildRelation> findAllByGuildId(int guildId) {
        List<GuildRelation> relations = new ArrayList<>();
        try {
            String sql = "SELECT * FROM guild_relations WHERE guild1_id = ? OR guild2_id = ? "
                    + "ORDER BY created_at DESC";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                stmt.setInt(2, guildId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        relations.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild relation list: " + e.getMessage());
        }
        return relations;
    }

    public CompletableFuture<Boolean> insertAsync(int guild1Id, int guild2Id, String guild1Name, String guild2Name,
                                                  GuildRelation.RelationType type, UUID initiatorUuid,
                                                  String initiatorName, String expiresAt) {
        return CompletableFuture.supplyAsync(() ->
                insert(guild1Id, guild2Id, guild1Name, guild2Name, type, initiatorUuid, initiatorName, expiresAt));
    }

    public boolean insert(int guild1Id, int guild2Id, String guild1Name, String guild2Name,
                          GuildRelation.RelationType type, UUID initiatorUuid,
                          String initiatorName, String expiresAt) {
        try {
            String now = TimeProvider.nowString();
            String sql = "INSERT INTO guild_relations (guild1_id, guild2_id, guild1_name, guild2_name, "
                    + "relation_type, initiator_uuid, initiator_name, created_at, updated_at, expires_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guild1Id);
                stmt.setInt(2, guild2Id);
                stmt.setString(3, guild1Name);
                stmt.setString(4, guild2Name);
                stmt.setString(5, type.name());
                stmt.setString(6, initiatorUuid.toString());
                stmt.setString(7, initiatorName);
                stmt.setString(8, now);
                stmt.setString(9, now);
                stmt.setString(10, expiresAt);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error inserting guild relation: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> updateStatusAsync(int relationId, GuildRelation.RelationStatus status,
                                                        String updatedAt) {
        return CompletableFuture.supplyAsync(() -> updateStatus(relationId, status, updatedAt));
    }

    public boolean updateStatus(int relationId, GuildRelation.RelationStatus status, String updatedAt) {
        try {
            String sql = "UPDATE guild_relations SET status = ?, updated_at = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status.name());
                stmt.setString(2, updatedAt);
                stmt.setInt(3, relationId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild relation status: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> deleteByIdAsync(int relationId) {
        return CompletableFuture.supplyAsync(() -> deleteById(relationId));
    }

    public boolean deleteById(int relationId) {
        try {
            String sql = "DELETE FROM guild_relations WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, relationId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error deleting guild relation: " + e.getMessage());
            return false;
        }
    }

    public GuildRelation mapRow(ResultSet rs) throws SQLException {
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
