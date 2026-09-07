package com.guild.war.report;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarReportSnapshot;
import com.guild.war.model.WarTeamSide;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarReportRepositoryTest {

    private static final UUID PLAYER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private HikariDataSource dataSource;
    private WarReportRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:war_report_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE war_matches (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        runtime_match_id INT NOT NULL,
                        guild_a_id INT NOT NULL,
                        guild_a_name VARCHAR(50) NOT NULL,
                        guild_b_id INT NOT NULL,
                        guild_b_name VARCHAR(50) NOT NULL,
                        winner_guild_id INT NULL,
                        mode VARCHAR(32) NOT NULL,
                        score_a INT NOT NULL,
                        score_b INT NOT NULL,
                        score_to_win INT NOT NULL,
                        preset_name VARCHAR(64),
                        end_reason VARCHAR(128),
                        started_at BIGINT NOT NULL,
                        ended_at BIGINT NOT NULL,
                        duration_ms BIGINT NOT NULL,
                        season_id VARCHAR(64) NOT NULL DEFAULT 'default'
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE war_match_players (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        match_report_id INT NOT NULL,
                        player_uuid VARCHAR(36) NOT NULL,
                        player_name VARCHAR(16) NOT NULL,
                        guild_id INT NOT NULL,
                        side VARCHAR(8) NOT NULL,
                        kills INT NOT NULL DEFAULT 0,
                        eliminated TINYINT NOT NULL DEFAULT 0
                    )
                    """);
        }

        GuildPlugin plugin = mock(GuildPlugin.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(plugin.getDatabaseManager()).thenReturn(databaseManager);
        when(databaseManager.getConnection()).thenAnswer(inv -> dataSource.getConnection());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("WarReportRepositoryTest"));

        repository = new WarReportRepository(plugin);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void saveAndLoad_roundTripsMatchAndPlayers() throws Exception {
        WarReportSnapshot snap = sampleSnapshot();
        WarReportSnapshot saved = repository.save(snap);

        assertNotNull(saved.reportId());
        WarReportSnapshot byId = repository.getByReportId(saved.reportId());
        assertNotNull(byId);
        assertEquals(saved.reportId(), byId.reportId());
        assertEquals(10, byId.scoreA());
        assertEquals(6, byId.scoreB());
        assertEquals(1, byId.participants().size());
        assertEquals(PLAYER, byId.participants().get(0).uuid());
        assertEquals(2, byId.participants().get(0).kills());
    }

    @Test
    void getRecent_returnsNewestFirst() throws Exception {
        repository.save(sampleSnapshot());
        Thread.sleep(5);
        WarReportSnapshot second = sampleSnapshot();
        WarMatch alt = new WarMatch(
                99, "X", 100, "Y",
                UUID.randomUUID(), "arena2", VictoryMode.LAST_STANDING,
                3, 3, 120);
        alt.setWinnerGuildId(100);
        alt.setEndReason("war.reason.test");
        second = WarReportSnapshot.fromMatch(alt, "s2");
        repository.save(second);

        List<WarReportSnapshot> recent = repository.getRecent(1);
        assertEquals(1, recent.size());
        assertEquals(100, recent.get(0).guildBId());
    }

    @Test
    void getLatestForPlayer_findsParticipation() throws Exception {
        WarReportSnapshot saved = repository.save(sampleSnapshot());
        WarReportSnapshot latest = repository.getLatestForPlayer(PLAYER);
        assertNotNull(latest);
        assertEquals(saved.reportId(), latest.reportId());
    }

    @Test
    void getByReportId_missingReturnsNull() {
        assertNull(repository.getByReportId(9999));
    }

    private static WarReportSnapshot sampleSnapshot() {
        WarMatch match = new WarMatch(
                42, "Alpha", 2, "Beta",
                UUID.randomUUID(), "arena1", VictoryMode.FIRST_TO_SCORE,
                5, 10, 600);
        match.setPhase(WarPhase.ENDED);
        match.setStartedAt(1_000L);
        match.setWinnerGuildId(1);
        match.setEndReason("war.reason.test");
        match.addScore(WarTeamSide.A, 10);
        match.addScore(WarTeamSide.B, 6);
        WarParticipant participant = new WarParticipant(PLAYER, "Hero", WarTeamSide.A);
        participant.addKill();
        participant.addKill();
        match.participants().put(PLAYER, participant);
        return WarReportSnapshot.fromMatch(match, "season-test");
    }
}
