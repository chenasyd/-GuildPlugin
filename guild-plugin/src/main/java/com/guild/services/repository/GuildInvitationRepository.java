package com.guild.services.repository;

import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.models.GuildInvitation;

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

public class GuildInvitationRepository {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public GuildInvitationRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public CompletableFuture<Boolean> insertAsync(int guildId, UUID targetUuid, String targetName,
                                                  UUID inviterUuid, String inviterName,
                                                  String status, String expiresAt, String createdAt) {
        return CompletableFuture.supplyAsync(() ->
                insert(guildId, targetUuid, targetName, inviterUuid, inviterName, status, expiresAt, createdAt));
    }

    public boolean insert(int guildId, UUID targetUuid, String targetName, UUID inviterUuid, String inviterName,
                          String status, String expiresAt, String createdAt) {
        try {
            String sql = "INSERT INTO guild_invites (guild_id, player_uuid, player_name, inviter_uuid, inviter_name, "
                    + "status, expires_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                stmt.setString(2, targetUuid.toString());
                stmt.setString(3, targetName);
                stmt.setString(4, inviterUuid.toString());
                stmt.setString(5, inviterName);
                stmt.setString(6, status);
                stmt.setString(7, expiresAt);
                stmt.setString(8, createdAt);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error inserting guild invitation: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> updateStatusAsync(int invitationId, String status) {
        return CompletableFuture.supplyAsync(() -> updateStatus(invitationId, status));
    }

    public boolean updateStatus(int invitationId, String status) {
        try {
            String sql = "UPDATE guild_invites SET status = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setInt(2, invitationId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error updating guild invitation status: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<GuildInvitation> findPendingByTargetAndInviterAsync(UUID targetUuid, UUID inviterUuid,
                                                                                 String now) {
        return CompletableFuture.supplyAsync(() -> findPendingByTargetAndInviter(targetUuid, inviterUuid, now));
    }

    public GuildInvitation findPendingByTargetAndInviter(UUID targetUuid, UUID inviterUuid, String now) {
        try {
            String sql = "SELECT * FROM guild_invites WHERE player_uuid = ? AND inviter_uuid = ? "
                    + "AND status = 'PENDING' AND expires_at > ? ORDER BY created_at DESC LIMIT 1";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, targetUuid.toString());
                stmt.setString(2, inviterUuid.toString());
                stmt.setString(3, now);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching pending invitations: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<GuildInvitation> findPendingByTargetAndGuildAsync(UUID targetUuid, int guildId, String now) {
        return CompletableFuture.supplyAsync(() -> findPendingByTargetAndGuild(targetUuid, guildId, now));
    }

    public GuildInvitation findPendingByTargetAndGuild(UUID targetUuid, int guildId, String now) {
        try {
            String sql = "SELECT * FROM guild_invites WHERE player_uuid = ? AND guild_id = ? "
                    + "AND status = 'PENDING' AND expires_at > ? ORDER BY created_at DESC LIMIT 1";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, targetUuid.toString());
                stmt.setInt(2, guildId);
                stmt.setString(3, now);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching invitation: " + e.getMessage());
        }
        return null;
    }

    public CompletableFuture<List<GuildInvitation>> findAllPendingByPlayerAsync(UUID playerUuid, String now) {
        return CompletableFuture.supplyAsync(() -> findAllPendingByPlayer(playerUuid, now));
    }

    public List<GuildInvitation> findAllPendingByPlayer(UUID playerUuid, String now) {
        List<GuildInvitation> invitations = new ArrayList<>();
        try {
            String sql = "SELECT * FROM guild_invites WHERE player_uuid = ? AND status = 'PENDING' "
                    + "AND expires_at > ? ORDER BY created_at DESC";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, now);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        invitations.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching pending invitations: " + e.getMessage());
        }
        return invitations;
    }

    public CompletableFuture<Integer> markExpiredBeforeAsync(String now) {
        return CompletableFuture.supplyAsync(() -> markExpiredBefore(now));
    }

    public int markExpiredBefore(String now) {
        try {
            String sql = "UPDATE guild_invites SET status = 'EXPIRED' WHERE status = 'PENDING' AND expires_at < ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, now);
                return stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.severe("Error cleaning up expired invitations: " + e.getMessage());
            return 0;
        }
    }

    public CompletableFuture<Integer> deleteOldProcessedAsync(int days) {
        return CompletableFuture.supplyAsync(() -> deleteOldProcessed(days));
    }

    public int deleteOldProcessed(int days) {
        try {
            String sql = "DELETE FROM guild_invites WHERE status != 'PENDING' AND created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, days);
                return stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.severe("Error cleaning up old invitation records: " + e.getMessage());
            return 0;
        }
    }

    public GuildInvitation mapRow(ResultSet rs) throws SQLException {
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
