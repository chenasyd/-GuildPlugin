package com.guild.activity;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Persistence for {@code guild_member_activity}.
 */
public final class ActivityRepository {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public ActivityRepository(GuildPlugin plugin) {
        this.databaseManager = plugin.getDatabaseManager();
        this.logger = plugin.getLogger();
    }

    public CompletableFuture<Optional<MemberActivityRecord>> findAsync(int guildId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> find(guildId, playerUuid));
    }

    public Optional<MemberActivityRecord> find(int guildId, UUID playerUuid) {
        String sql = "SELECT * FROM guild_member_activity WHERE guild_id = ? AND player_uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.setString(2, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(fromRs(rs));
                }
            }
        } catch (SQLException e) {
            logger.severe("[Activity] find failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    public CompletableFuture<List<MemberActivityRecord>> findByGuildAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<MemberActivityRecord> list = new ArrayList<>();
            String sql = "SELECT * FROM guild_member_activity WHERE guild_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(fromRs(rs));
                    }
                }
            } catch (SQLException e) {
                logger.severe("[Activity] findByGuild failed: " + e.getMessage());
            }
            return list;
        });
    }

    public void upsert(MemberActivityRecord record) {
        String sql;
        if (databaseManager.getDatabaseType() == DatabaseManager.DatabaseType.MYSQL) {
            sql = """
                INSERT INTO guild_member_activity (
                    guild_id, player_uuid, player_name, online_minutes_today, online_minutes_total,
                    active_days_week, active_day_date, week_start_date, last_login_date, last_seen, today_date
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    player_name = VALUES(player_name),
                    online_minutes_today = VALUES(online_minutes_today),
                    online_minutes_total = VALUES(online_minutes_total),
                    active_days_week = VALUES(active_days_week),
                    active_day_date = VALUES(active_day_date),
                    week_start_date = VALUES(week_start_date),
                    last_login_date = VALUES(last_login_date),
                    last_seen = VALUES(last_seen),
                    today_date = VALUES(today_date)
                """;
        } else {
            sql = """
                INSERT INTO guild_member_activity (
                    guild_id, player_uuid, player_name, online_minutes_today, online_minutes_total,
                    active_days_week, active_day_date, week_start_date, last_login_date, last_seen, today_date
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(guild_id, player_uuid) DO UPDATE SET
                    player_name = excluded.player_name,
                    online_minutes_today = excluded.online_minutes_today,
                    online_minutes_total = excluded.online_minutes_total,
                    active_days_week = excluded.active_days_week,
                    active_day_date = excluded.active_day_date,
                    week_start_date = excluded.week_start_date,
                    last_login_date = excluded.last_login_date,
                    last_seen = excluded.last_seen,
                    today_date = excluded.today_date
                """;
        }
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, record);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[Activity] upsert failed: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> upsertAsync(MemberActivityRecord record) {
        return CompletableFuture.runAsync(() -> upsert(record));
    }

    private static void bind(PreparedStatement stmt, MemberActivityRecord r) throws SQLException {
        stmt.setInt(1, r.getGuildId());
        stmt.setString(2, r.getPlayerUuid().toString());
        stmt.setString(3, r.getPlayerName());
        stmt.setInt(4, r.getOnlineMinutesToday());
        stmt.setInt(5, r.getOnlineMinutesTotal());
        stmt.setInt(6, r.getActiveDaysWeek());
        stmt.setString(7, r.getActiveDayDate());
        stmt.setString(8, r.getWeekStartDate());
        stmt.setString(9, r.getLastLoginDate());
        stmt.setLong(10, r.getLastSeen());
        stmt.setString(11, r.getTodayDate());
    }

    private static MemberActivityRecord fromRs(ResultSet rs) throws SQLException {
        MemberActivityRecord r = new MemberActivityRecord(
                rs.getInt("guild_id"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"));
        r.setOnlineMinutesToday(rs.getInt("online_minutes_today"));
        r.setOnlineMinutesTotal(rs.getInt("online_minutes_total"));
        r.setActiveDaysWeek(rs.getInt("active_days_week"));
        r.setActiveDayDate(rs.getString("active_day_date"));
        r.setWeekStartDate(rs.getString("week_start_date"));
        r.setLastLoginDate(rs.getString("last_login_date"));
        r.setLastSeen(rs.getLong("last_seen"));
        r.setTodayDate(rs.getString("today_date"));
        return r;
    }
}
