package com.guild.module.example.territory;

import com.guild.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** {@code guild_territories} 表读写（跨服权威元数据）。 */
final class TerritoryDatabaseStore {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    TerritoryDatabaseStore(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    Map<String, TerritoryRecord> loadAll() {
        Map<String, TerritoryRecord> loaded = new HashMap<>();
        String sql = """
                SELECT guild_id, guild_name, server_id, world_name, region_id,
                       min_x, min_y, min_z, max_x, max_y, max_z,
                       claimed_at, sync_state, updated_at
                FROM guild_territories
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                TerritoryRecord record = mapRow(rs);
                loaded.put(TerritoryRepository.storageKey(record), record);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load guild_territories: " + e.getMessage(), e);
        }
        return loaded;
    }

    boolean upsert(TerritoryRecord record) {
        String sql = """
                INSERT INTO guild_territories (
                    guild_id, guild_name, server_id, world_name, region_id,
                    min_x, min_y, min_z, max_x, max_y, max_z,
                    claimed_at, sync_state, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(guild_id, server_id, world_name) DO UPDATE SET
                    guild_name = excluded.guild_name,
                    region_id = excluded.region_id,
                    min_x = excluded.min_x,
                    min_y = excluded.min_y,
                    min_z = excluded.min_z,
                    max_x = excluded.max_x,
                    max_y = excluded.max_y,
                    max_z = excluded.max_z,
                    claimed_at = excluded.claimed_at,
                    sync_state = excluded.sync_state,
                    updated_at = excluded.updated_at
                """;
        if (isMysql()) {
            sql = """
                    INSERT INTO guild_territories (
                        guild_id, guild_name, server_id, world_name, region_id,
                        min_x, min_y, min_z, max_x, max_y, max_z,
                        claimed_at, sync_state, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        guild_name = VALUES(guild_name),
                        region_id = VALUES(region_id),
                        min_x = VALUES(min_x),
                        min_y = VALUES(min_y),
                        min_z = VALUES(min_z),
                        max_x = VALUES(max_x),
                        max_y = VALUES(max_y),
                        max_z = VALUES(max_z),
                        claimed_at = VALUES(claimed_at),
                        sync_state = VALUES(sync_state),
                        updated_at = VALUES(updated_at)
                    """;
        }
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindRecord(stmt, record);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to upsert guild territory: " + e.getMessage(), e);
            return false;
        }
    }

    boolean delete(int guildId, String serverId, String worldName) {
        String sql = "DELETE FROM guild_territories WHERE guild_id = ? AND server_id = ? AND world_name = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.setString(2, serverId);
            stmt.setString(3, worldName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete guild territory: " + e.getMessage(), e);
            return false;
        }
    }

    boolean deleteAllForGuild(int guildId) {
        String sql = "DELETE FROM guild_territories WHERE guild_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete guild territories for guild " + guildId + ": " + e.getMessage(), e);
            return false;
        }
    }

    Optional<TerritoryRecord> findOne(int guildId, String serverId, String worldName) {
        String sql = """
                SELECT guild_id, guild_name, server_id, world_name, region_id,
                       min_x, min_y, min_z, max_x, max_y, max_z,
                       claimed_at, sync_state, updated_at
                FROM guild_territories
                WHERE guild_id = ? AND server_id = ? AND world_name = ?
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.setString(2, serverId);
            stmt.setString(3, worldName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to query guild territory: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    List<TerritoryRecord> findByGuildId(int guildId) {
        List<TerritoryRecord> rows = new ArrayList<>();
        String sql = """
                SELECT guild_id, guild_name, server_id, world_name, region_id,
                       min_x, min_y, min_z, max_x, max_y, max_z,
                       claimed_at, sync_state, updated_at
                FROM guild_territories
                WHERE guild_id = ?
                ORDER BY server_id, world_name
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to list guild territories: " + e.getMessage(), e);
        }
        return rows;
    }

    private void bindRecord(PreparedStatement stmt, TerritoryRecord record) throws SQLException {
        stmt.setInt(1, record.getGuildId());
        stmt.setString(2, record.getGuildName());
        stmt.setString(3, record.getServerId());
        stmt.setString(4, record.getWorldName());
        stmt.setString(5, record.getRegionId());
        stmt.setInt(6, record.getMinX());
        stmt.setInt(7, record.getMinY());
        stmt.setInt(8, record.getMinZ());
        stmt.setInt(9, record.getMaxX());
        stmt.setInt(10, record.getMaxY());
        stmt.setInt(11, record.getMaxZ());
        stmt.setLong(12, record.getClaimedAtEpochMs());
        stmt.setString(13, record.getSyncState().name());
        stmt.setLong(14, record.getUpdatedAtEpochMs());
    }

    private static TerritoryRecord mapRow(ResultSet rs) throws SQLException {
        return new TerritoryRecord(
                rs.getInt("guild_id"),
                rs.getString("guild_name"),
                rs.getString("region_id"),
                rs.getString("server_id"),
                rs.getString("world_name"),
                rs.getInt("min_x"),
                rs.getInt("min_y"),
                rs.getInt("min_z"),
                rs.getInt("max_x"),
                rs.getInt("max_y"),
                rs.getInt("max_z"),
                rs.getLong("claimed_at"),
                TerritorySyncState.fromString(rs.getString("sync_state")),
                rs.getLong("updated_at")
        );
    }

    private boolean isMysql() {
        try {
            return databaseManager.getDatabaseType() == DatabaseManager.DatabaseType.MYSQL;
        } catch (Exception e) {
            return false;
        }
    }
}
