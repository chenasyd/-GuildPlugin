package com.guild.war.report;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarReportSnapshot;
import com.guild.war.model.WarTeamSide;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** 工会战战报持久化。 */
public final class WarReportRepository {

    private final GuildPlugin plugin;
    private final DatabaseManager db;

    public WarReportRepository(GuildPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public CompletableFuture<WarReportSnapshot> saveAsync(WarReportSnapshot snap) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return save(snap);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[GuildWar] Failed to persist war report", e);
                return snap;
            }
        });
    }

    public WarReportSnapshot save(WarReportSnapshot snap) throws Exception {
        String insertMatch = """
            INSERT INTO war_matches (
              runtime_match_id, guild_a_id, guild_a_name, guild_b_id, guild_b_name,
              winner_guild_id, mode, score_a, score_b, score_to_win, preset_name,
              end_reason, started_at, ended_at, duration_ms, season_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertMatch, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, snap.runtimeMatchId());
            stmt.setInt(2, snap.guildAId());
            stmt.setString(3, snap.guildAName());
            stmt.setInt(4, snap.guildBId());
            stmt.setString(5, snap.guildBName());
            if (snap.winnerGuildId() == null) {
                stmt.setObject(6, null);
            } else {
                stmt.setInt(6, snap.winnerGuildId());
            }
            stmt.setString(7, snap.mode().name());
            stmt.setInt(8, snap.scoreA());
            stmt.setInt(9, snap.scoreB());
            stmt.setInt(10, snap.scoreToWin());
            stmt.setString(11, snap.presetName());
            stmt.setString(12, snap.endReason());
            stmt.setLong(13, snap.startedAt());
            stmt.setLong(14, snap.endedAt());
            stmt.setLong(15, snap.durationMs());
            stmt.setString(16, snap.seasonId());
            stmt.executeUpdate();

            int reportId;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("No generated key for war_matches");
                }
                reportId = keys.getInt(1);
            }

            String insertPlayer = """
                INSERT INTO war_match_players (
                  match_report_id, player_uuid, player_name, guild_id, side, kills, eliminated
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            try (PreparedStatement ps = conn.prepareStatement(insertPlayer)) {
                for (WarParticipantSnapshot p : snap.participants()) {
                    ps.setInt(1, reportId);
                    ps.setString(2, p.uuid().toString());
                    ps.setString(3, p.name());
                    ps.setInt(4, p.guildId());
                    ps.setString(5, p.side().name());
                    ps.setInt(6, p.kills());
                    ps.setInt(7, p.eliminated() ? 1 : 0);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return snap.withReportId(reportId);
        }
    }

    public CompletableFuture<List<WarReportSnapshot>> getRecentAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> getRecent(Math.max(1, Math.min(limit, 50))));
    }

    public List<WarReportSnapshot> getRecent(int limit) {
        String sql = "SELECT * FROM war_matches ORDER BY id DESC LIMIT ?";
        List<WarReportSnapshot> out = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    out.add(loadFull(conn, rs));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GuildWar] Failed to load recent reports", e);
        }
        return out;
    }

    public CompletableFuture<WarReportSnapshot> getByReportIdAsync(int reportId) {
        return CompletableFuture.supplyAsync(() -> getByReportId(reportId));
    }

    public WarReportSnapshot getByReportId(int reportId) {
        String sql = "SELECT * FROM war_matches WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reportId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return loadFull(conn, rs);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GuildWar] Failed to load report #" + reportId, e);
        }
        return null;
    }

    public CompletableFuture<WarReportSnapshot> getLatestForPlayerAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getLatestForPlayer(uuid));
    }

    public WarReportSnapshot getLatestForPlayer(UUID uuid) {
        String sql = """
            SELECT m.* FROM war_matches m
            INNER JOIN war_match_players p ON p.match_report_id = m.id
            WHERE p.player_uuid = ?
            ORDER BY m.id DESC LIMIT 1
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return loadFull(conn, rs);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GuildWar] Failed to load latest report for " + uuid, e);
        }
        return null;
    }

    private WarReportSnapshot loadFull(Connection conn, ResultSet rs) throws Exception {
        int reportId = rs.getInt("id");
        List<WarParticipantSnapshot> players = new ArrayList<>();
        String psSql = "SELECT * FROM war_match_players WHERE match_report_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(psSql)) {
            ps.setInt(1, reportId);
            try (ResultSet prs = ps.executeQuery()) {
                while (prs.next()) {
                    players.add(new WarParticipantSnapshot(
                            UUID.fromString(prs.getString("player_uuid")),
                            prs.getString("player_name"),
                            WarTeamSide.valueOf(prs.getString("side")),
                            prs.getInt("guild_id"),
                            prs.getInt("kills"),
                            prs.getInt("eliminated") != 0));
                }
            }
        }
        Integer winner = (Integer) rs.getObject("winner_guild_id");
        return new WarReportSnapshot(
                rs.getInt("runtime_match_id"),
                reportId,
                rs.getInt("guild_a_id"),
                rs.getString("guild_a_name"),
                rs.getInt("guild_b_id"),
                rs.getString("guild_b_name"),
                winner,
                VictoryMode.valueOf(rs.getString("mode")),
                rs.getInt("score_a"),
                rs.getInt("score_b"),
                rs.getInt("score_to_win"),
                rs.getString("preset_name"),
                rs.getString("end_reason"),
                rs.getLong("started_at"),
                rs.getLong("started_at"),
                rs.getLong("ended_at"),
                rs.getString("season_id"),
                players);
    }
}
