package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.core.ServiceContainer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildInvestmentServiceTest {

    private HikariDataSource dataSource;
    private GuildInvestmentService service;
    private final UUID playerUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:invest_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(config);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getConnection()).thenAnswer(invocation -> dataSource.getConnection());
        when(databaseManager.getDatabaseType()).thenReturn(DatabaseManager.DatabaseType.MYSQL);

        ServiceContainer serviceContainer = mock(ServiceContainer.class);
        when(serviceContainer.get(DatabaseManager.class)).thenReturn(databaseManager);

        GuildPlugin plugin = mock(GuildPlugin.class);
        when(plugin.getServiceContainer()).thenReturn(serviceContainer);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        service = new GuildInvestmentService(plugin);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void recordDepositTwiceUpsertsWithoutNullPlayerName() throws Exception {
        service.recordDeposit(1, playerUuid, "BrawnyHalo", 11.0);
        service.recordDeposit(1, playerUuid, "BrawnyHalo", 12.0);

        assertEquals(23.0, service.getInvestedBalance(1, playerUuid), 0.001);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT player_name, total_invested, last_deposit FROM guild_member_investments "
                             + "WHERE guild_id = ? AND player_uuid = ?")) {
            stmt.setInt(1, 1);
            stmt.setString(2, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                assert rs.next();
                assertNotNull(rs.getString("player_name"));
                assertEquals("BrawnyHalo", rs.getString("player_name"));
                assertEquals(23.0, rs.getDouble("total_invested"), 0.001);
                assertEquals(12.0, rs.getDouble("last_deposit"), 0.001);
            }
        }
    }
}
