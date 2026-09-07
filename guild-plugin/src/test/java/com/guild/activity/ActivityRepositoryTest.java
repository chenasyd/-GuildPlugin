package com.guild.activity;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityRepositoryTest {

    private static final UUID PLAYER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private HikariDataSource dataSource;
    private ActivityRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:activity_repo_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE guild_member_activity (
                        guild_id INT NOT NULL,
                        player_uuid VARCHAR(36) NOT NULL,
                        player_name VARCHAR(16) NOT NULL,
                        online_minutes_today INT NOT NULL DEFAULT 0,
                        online_minutes_total INT NOT NULL DEFAULT 0,
                        active_days_week INT NOT NULL DEFAULT 0,
                        active_day_date VARCHAR(16),
                        week_start_date VARCHAR(16),
                        last_login_date VARCHAR(16),
                        last_seen BIGINT NOT NULL DEFAULT 0,
                        today_date VARCHAR(16),
                        PRIMARY KEY (guild_id, player_uuid)
                    )
                    """);
        }

        GuildPlugin plugin = mock(GuildPlugin.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(plugin.getDatabaseManager()).thenReturn(databaseManager);
        when(databaseManager.getConnection()).thenAnswer(inv -> dataSource.getConnection());
        when(databaseManager.getDatabaseType()).thenReturn(DatabaseManager.DatabaseType.MYSQL);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ActivityRepositoryTest"));

        repository = new ActivityRepository(plugin);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void upsertAndFind_roundTripsRecord() {
        MemberActivityRecord record = new MemberActivityRecord(7, PLAYER, "Alice");
        record.setOnlineMinutesToday(12);
        record.setOnlineMinutesTotal(100);
        record.setActiveDaysWeek(3);
        record.setTodayDate("2026-09-07");
        record.setWeekStartDate("2026-09-01");
        record.setLastLoginDate("2026-09-07");
        record.setLastSeen(1_700_000_000_000L);

        repository.upsert(record);

        Optional<MemberActivityRecord> loaded = repository.find(7, PLAYER);
        assertTrue(loaded.isPresent());
        assertEquals("Alice", loaded.get().getPlayerName());
        assertEquals(12, loaded.get().getOnlineMinutesToday());
        assertEquals(100, loaded.get().getOnlineMinutesTotal());
        assertEquals(3, loaded.get().getActiveDaysWeek());
    }

    @Test
    void upsert_updatesExistingRow() {
        MemberActivityRecord first = new MemberActivityRecord(7, PLAYER, "Alice");
        first.setOnlineMinutesToday(5);
        repository.upsert(first);

        MemberActivityRecord updated = new MemberActivityRecord(7, PLAYER, "Alice2");
        updated.setOnlineMinutesToday(20);
        repository.upsert(updated);

        Optional<MemberActivityRecord> loaded = repository.find(7, PLAYER);
        assertTrue(loaded.isPresent());
        assertEquals("Alice2", loaded.get().getPlayerName());
        assertEquals(20, loaded.get().getOnlineMinutesToday());
    }

    @Test
    void findByGuild_returnsAllMembers() {
        UUID other = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        repository.upsert(new MemberActivityRecord(7, PLAYER, "A"));
        repository.upsert(new MemberActivityRecord(7, other, "B"));
        repository.upsert(new MemberActivityRecord(8, other, "C"));

        List<MemberActivityRecord> guild7 = repository.findByGuildAsync(7).join();

        assertEquals(2, guild7.size());
    }
}
