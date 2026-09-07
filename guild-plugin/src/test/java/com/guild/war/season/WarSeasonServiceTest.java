package com.guild.war.season;

import com.guild.GuildPlugin;
import com.guild.core.config.ConfigManager;
import com.guild.core.database.DatabaseManager;
import com.guild.war.GuildWarService;
import com.guild.war.WarSettings;
import com.guild.war.event.WarMatchEndEvent;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarReportSnapshot;
import com.guild.war.model.WarTeamSide;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarSeasonServiceTest {

    private HikariDataSource dataSource;
    private GuildPlugin plugin;
    private WarSeasonService service;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:war_season_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE war_season_stats (
                        guild_id INT NOT NULL,
                        guild_name VARCHAR(50) NOT NULL,
                        season_id VARCHAR(64) NOT NULL,
                        wins INT NOT NULL DEFAULT 0,
                        losses INT NOT NULL DEFAULT 0,
                        draws INT NOT NULL DEFAULT 0,
                        kills INT NOT NULL DEFAULT 0,
                        matches INT NOT NULL DEFAULT 0,
                        PRIMARY KEY (guild_id, season_id)
                    )
                    """);
        }

        plugin = mock(GuildPlugin.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        ConfigManager configManager = mock(ConfigManager.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("database.type", "mysql");
        cfg.set("guild-war.season.id", "season-2026");

        GuildWarService warService = mock(GuildWarService.class);
        when(warService.settings()).thenReturn(new WarSettings(cfg));

        when(plugin.getDatabaseManager()).thenReturn(databaseManager);
        when(databaseManager.getConnection()).thenAnswer(inv -> dataSource.getConnection());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("WarSeasonServiceTest"));
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);
        when(plugin.getGuildWarService()).thenReturn(warService);

        service = new WarSeasonService(plugin);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void currentSeasonId_readsFromWarSettings() {
        assertEquals("season-2026", service.currentSeasonId());
    }

    @Test
    void onWarEnd_recordsWinLossAndKills() throws Exception {
        service.onWarEnd(new WarMatchEndEvent(matchSnapshot(1, "season-1")));

        awaitMatches(1, "season-1", 1);
        awaitMatches(2, "season-1", 1);

        WarSeasonService.SeasonRow alpha = service.getGuildStats(1, "season-1");
        assertEquals(1, alpha.wins());
        assertEquals(0, alpha.losses());
        assertEquals(0, alpha.draws());
        assertEquals(3, alpha.kills());
        assertEquals(1, alpha.matches());

        WarSeasonService.SeasonRow beta = service.getGuildStats(2, "season-1");
        assertEquals(0, beta.wins());
        assertEquals(1, beta.losses());
        assertEquals(1, beta.kills());
    }

    @Test
    void onWarEnd_recordsDraw() throws Exception {
        service.onWarEnd(new WarMatchEndEvent(matchSnapshot(null, "draw-season")));

        awaitMatches(1, "draw-season", 1);

        WarSeasonService.SeasonRow alpha = service.getGuildStats(1, "draw-season");
        assertEquals(0, alpha.wins());
        assertEquals(0, alpha.losses());
        assertEquals(1, alpha.draws());
    }

    @Test
    void onWarEnd_accumulatesAcrossMatches() throws Exception {
        service.onWarEnd(new WarMatchEndEvent(matchSnapshot(1, "accum")));
        awaitMatches(1, "accum", 1);
        service.onWarEnd(new WarMatchEndEvent(matchSnapshot(2, "accum")));
        awaitMatches(1, "accum", 2);

        WarSeasonService.SeasonRow alpha = service.getGuildStats(1, "accum");
        assertEquals(1, alpha.wins());
        assertEquals(1, alpha.losses());
        assertEquals(2, alpha.matches());
    }

    @Test
    void getLeaderboard_ordersByWinsThenKills() throws Exception {
        insertRow(1, "First", "lb", 3, 0, 0, 10, 3);
        insertRow(2, "Second", "lb", 3, 1, 0, 20, 4);
        insertRow(3, "Third", "lb", 1, 0, 0, 5, 1);

        List<WarSeasonService.SeasonRow> rows = service.getLeaderboard("lb", 10);

        assertEquals(3, rows.size());
        assertEquals("Second", rows.get(0).guildName());
        assertEquals("First", rows.get(1).guildName());
        assertEquals("Third", rows.get(2).guildName());
    }

    @Test
    void getGuildStats_returnsEmptyDefaultsWhenMissing() {
        WarSeasonService.SeasonRow row = service.getGuildStats(99, "missing");

        assertEquals(99, row.guildId());
        assertEquals("missing", row.seasonId());
        assertEquals(0, row.wins());
        assertEquals(0, row.matches());
    }

    private void insertRow(int guildId, String name, String season,
                           int wins, int losses, int draws, int kills, int matches) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO war_season_stats (guild_id, guild_name, season_id, wins, losses, draws, kills, matches) "
                    + "VALUES (" + guildId + ", '" + name + "', '" + season + "', "
                    + wins + ", " + losses + ", " + draws + ", " + kills + ", " + matches + ")");
        }
    }

    private void awaitMatches(int guildId, String season, int expectedMatches) throws Exception {
        for (int i = 0; i < 50; i++) {
            if (service.getGuildStats(guildId, season).matches() >= expectedMatches) {
                return;
            }
            Thread.sleep(20);
        }
        assertEquals(expectedMatches, service.getGuildStats(guildId, season).matches());
    }

    private static WarReportSnapshot matchSnapshot(Integer winnerGuildId, String seasonId) {
        List<WarParticipantSnapshot> participants = List.of(
                new WarParticipantSnapshot(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "AlphaP", WarTeamSide.A, 1, 3, false),
                new WarParticipantSnapshot(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "BetaP", WarTeamSide.B, 2, 1, false));
        return new WarReportSnapshot(
                1, 10,
                1, "Alpha", 2, "Beta",
                winnerGuildId,
                VictoryMode.FIRST_TO_SCORE,
                10, 5, 20,
                "arena", "war.reason.test",
                0L, 1_000L, 2_000L,
                seasonId,
                participants);
    }
}
