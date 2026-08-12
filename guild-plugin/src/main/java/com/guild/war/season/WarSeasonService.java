package com.guild.war.season;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.war.WarSettings;
import com.guild.war.event.WarMatchEndEvent;
import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarReportSnapshot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** 赛季战绩统计（监听结算事件）。 */
public final class WarSeasonService implements Listener {

    public record SeasonRow(int guildId, String guildName, String seasonId,
                            int wins, int losses, int draws, int kills, int matches) {
    }

    private final GuildPlugin plugin;
    private final DatabaseManager db;

    public WarSeasonService(GuildPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public String currentSeasonId() {
        WarSettings s = plugin.getGuildWarService() != null
                ? plugin.getGuildWarService().settings() : null;
        return s != null ? s.seasonId : "default";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWarEnd(WarMatchEndEvent event) {
        WarReportSnapshot snap = event.getSnapshot();
        String season = snap.seasonId() != null && !snap.seasonId().isEmpty()
                ? snap.seasonId() : currentSeasonId();
        CompletableFuture.runAsync(() -> {
            try {
                applyResult(snap, season);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[GuildWar] Season stats update failed", e);
            }
        });
    }

    private void applyResult(WarReportSnapshot snap, String season) throws Exception {
        int killsA = 0;
        int killsB = 0;
        for (WarParticipantSnapshot p : snap.participants()) {
            if (p.guildId() == snap.guildAId()) {
                killsA += p.kills();
            } else if (p.guildId() == snap.guildBId()) {
                killsB += p.kills();
            }
        }
        upsert(snap.guildAId(), snap.guildAName(), season,
                snap.winnerGuildId(), snap.guildAId(), killsA);
        upsert(snap.guildBId(), snap.guildBName(), season,
                snap.winnerGuildId(), snap.guildBId(), killsB);
    }

    private void upsert(int guildId, String guildName, String season,
                        Integer winnerId, int selfId, int kills) throws Exception {
        int win = 0;
        int loss = 0;
        int draw = 0;
        if (winnerId == null) {
            draw = 1;
        } else if (winnerId == selfId) {
            win = 1;
        } else {
            loss = 1;
        }
        String sql = """
            INSERT INTO war_season_stats (guild_id, guild_name, season_id, wins, losses, draws, kills, matches)
            VALUES (?, ?, ?, ?, ?, ?, ?, 1)
            ON CONFLICT(guild_id, season_id) DO UPDATE SET
              guild_name = excluded.guild_name,
              wins = wins + excluded.wins,
              losses = losses + excluded.losses,
              draws = draws + excluded.draws,
              kills = kills + excluded.kills,
              matches = matches + 1
            """;
        // MySQL uses different upsert; detect via config
        boolean mysql = isMySql();
        if (mysql) {
            sql = """
                INSERT INTO war_season_stats (guild_id, guild_name, season_id, wins, losses, draws, kills, matches)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE
                  guild_name = VALUES(guild_name),
                  wins = wins + VALUES(wins),
                  losses = losses + VALUES(losses),
                  draws = draws + VALUES(draws),
                  kills = kills + VALUES(kills),
                  matches = matches + 1
                """;
        }
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.setString(2, guildName);
            stmt.setString(3, season);
            stmt.setInt(4, win);
            stmt.setInt(5, loss);
            stmt.setInt(6, draw);
            stmt.setInt(7, kills);
            stmt.executeUpdate();
        }
    }

    private boolean isMySql() {
        try {
            String type = plugin.getConfigManager().getMainConfig().getString("database.type", "sqlite");
            return "mysql".equalsIgnoreCase(type);
        } catch (Exception e) {
            return false;
        }
    }

    public CompletableFuture<List<SeasonRow>> getLeaderboardAsync(String seasonId, int limit) {
        return CompletableFuture.supplyAsync(() -> getLeaderboard(seasonId, limit));
    }

    public List<SeasonRow> getLeaderboard(String seasonId, int limit) {
        String sql = """
            SELECT * FROM war_season_stats WHERE season_id = ?
            ORDER BY wins DESC, kills DESC, matches ASC LIMIT ?
            """;
        List<SeasonRow> out = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, seasonId);
            stmt.setInt(2, Math.max(1, Math.min(limit, 50)));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    out.add(new SeasonRow(
                            rs.getInt("guild_id"),
                            rs.getString("guild_name"),
                            rs.getString("season_id"),
                            rs.getInt("wins"),
                            rs.getInt("losses"),
                            rs.getInt("draws"),
                            rs.getInt("kills"),
                            rs.getInt("matches")));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GuildWar] Failed to load season leaderboard", e);
        }
        return out;
    }

    public CompletableFuture<SeasonRow> getGuildStatsAsync(int guildId, String seasonId) {
        return CompletableFuture.supplyAsync(() -> getGuildStats(guildId, seasonId));
    }

    public SeasonRow getGuildStats(int guildId, String seasonId) {
        String sql = "SELECT * FROM war_season_stats WHERE guild_id = ? AND season_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guildId);
            stmt.setString(2, seasonId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new SeasonRow(
                            rs.getInt("guild_id"),
                            rs.getString("guild_name"),
                            rs.getString("season_id"),
                            rs.getInt("wins"),
                            rs.getInt("losses"),
                            rs.getInt("draws"),
                            rs.getInt("kills"),
                            rs.getInt("matches"));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GuildWar] Failed to load guild season stats", e);
        }
        return new SeasonRow(guildId, "", seasonId, 0, 0, 0, 0, 0);
    }
}
