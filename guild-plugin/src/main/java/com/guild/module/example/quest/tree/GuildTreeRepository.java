package com.guild.module.example.quest.tree;

import com.guild.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * JDBC access for guild quest tree tables.
 */
public class GuildTreeRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public GuildTreeRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public Optional<GuildTreeState> find(int guildId) {
        String sql = "SELECT tree_level, virtual_exp FROM guild_quest_tree WHERE guild_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new GuildTreeState(
                        guildId,
                        rs.getInt("tree_level"),
                        rs.getLong("virtual_exp")));
                }
            }
        } catch (SQLException e) {
            logger.warning("[GuildTree] find failed for guild " + guildId + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean insert(GuildTreeState state) {
        String sql = "INSERT INTO guild_quest_tree (guild_id, tree_level, virtual_exp) VALUES (?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, state.getGuildId());
            stmt.setInt(2, state.getTreeLevel());
            stmt.setLong(3, state.getVirtualExp());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.warning("[GuildTree] insert failed for guild " + state.getGuildId() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean update(GuildTreeState state) {
        String sql = "UPDATE guild_quest_tree SET tree_level = ?, virtual_exp = ?, updated_at = CURRENT_TIMESTAMP WHERE guild_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, state.getTreeLevel());
            stmt.setLong(2, state.getVirtualExp());
            stmt.setInt(3, state.getGuildId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.warning("[GuildTree] update failed for guild " + state.getGuildId() + ": " + e.getMessage());
            return false;
        }
    }

    public void insertLedger(int guildId, UUID playerUuid, String playerName,
                             String action, long amount, int vanillaExp,
                             int treeLevelAfter, String reason) {
        String sql = """
            INSERT INTO guild_quest_tree_ledger
            (guild_id, player_uuid, player_name, action, amount, vanilla_exp, tree_level_after, reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.setString(2, playerUuid != null ? playerUuid.toString() : "");
            stmt.setString(3, playerName != null ? playerName : "");
            stmt.setString(4, action);
            stmt.setLong(5, amount);
            stmt.setInt(6, vanillaExp);
            stmt.setInt(7, treeLevelAfter);
            stmt.setString(8, reason);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[GuildTree] ledger insert failed: " + e.getMessage());
        }
    }

    public int getDailyWithdrawn(int guildId, UUID playerUuid, String dayKey) {
        String sql = """
            SELECT withdrawn_vanilla FROM guild_quest_tree_daily
            WHERE guild_id = ? AND player_uuid = ? AND day_key = ?
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.setString(2, playerUuid.toString());
            stmt.setString(3, dayKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("withdrawn_vanilla");
                }
            }
        } catch (SQLException e) {
            logger.warning("[GuildTree] getDailyWithdrawn failed: " + e.getMessage());
        }
        return 0;
    }

    public void addDailyWithdrawn(int guildId, UUID playerUuid, String dayKey, int vanillaAmount) {
        if (vanillaAmount <= 0) return;
        int current = getDailyWithdrawn(guildId, playerUuid, dayKey);
        if (current <= 0) {
            String insert = """
                INSERT INTO guild_quest_tree_daily (guild_id, player_uuid, day_key, withdrawn_vanilla)
                VALUES (?, ?, ?, ?)
                """;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insert)) {
                stmt.setInt(1, guildId);
                stmt.setString(2, playerUuid.toString());
                stmt.setString(3, dayKey);
                stmt.setInt(4, vanillaAmount);
                stmt.executeUpdate();
                return;
            } catch (SQLException e) {
                // fall through to update path for race / unique conflict
            }
        }
        String update = """
            UPDATE guild_quest_tree_daily SET withdrawn_vanilla = withdrawn_vanilla + ?
            WHERE guild_id = ? AND player_uuid = ? AND day_key = ?
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(update)) {
            stmt.setInt(1, vanillaAmount);
            stmt.setInt(2, guildId);
            stmt.setString(3, playerUuid.toString());
            stmt.setString(4, dayKey);
            if (stmt.executeUpdate() == 0) {
                String insert = """
                    INSERT INTO guild_quest_tree_daily (guild_id, player_uuid, day_key, withdrawn_vanilla)
                    VALUES (?, ?, ?, ?)
                    """;
                try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                    insertStmt.setInt(1, guildId);
                    insertStmt.setString(2, playerUuid.toString());
                    insertStmt.setString(3, dayKey);
                    insertStmt.setInt(4, vanillaAmount);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.warning("[GuildTree] addDailyWithdrawn failed: " + e.getMessage());
        }
    }
}
