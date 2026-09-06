package com.guild.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationServiceTest {

    private static final Logger LOGGER = Logger.getLogger("DatabaseMigrationServiceTest");

    private HikariDataSource dataSource;
    private DatabaseMigrationService migrationService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        migrationService = new DatabaseMigrationService(LOGGER);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void checkAndAddMissingColumns_mysql_addsLegacyGuildColumns() throws Exception {
        openH2Mysql();
        createLegacyGuildsTable("""
                CREATE TABLE guilds (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) NOT NULL,
                    leader_uuid VARCHAR(36) NOT NULL,
                    leader_name VARCHAR(16) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

        assertFalse(hasColumn("home_world"));
        assertFalse(hasColumn("balance"));
        assertFalse(hasColumn("peak_level"));

        migrationService.checkAndAddMissingColumns(
                DatabaseManager.DatabaseType.MYSQL, dataSource::getConnection);

        assertTrue(hasColumn("home_world"));
        assertTrue(hasColumn("home_x"));
        assertTrue(hasColumn("balance"));
        assertTrue(hasColumn("level"));
        assertTrue(hasColumn("peak_level"));
    }

    @Test
    void checkAndAddMissingColumns_sqlite_addsLegacyGuildColumns() throws Exception {
        openSqlite(tempDir.resolve("legacy.db").toString());
        createLegacyGuildsTable("""
                CREATE TABLE guilds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    leader_uuid TEXT NOT NULL,
                    leader_name TEXT NOT NULL,
                    created_at TEXT,
                    updated_at TEXT
                )
                """);

        assertFalse(hasColumn("home_world"));
        assertFalse(hasColumn("balance"));
        assertFalse(hasColumn("peak_level"));

        migrationService.checkAndAddMissingColumns(
                DatabaseManager.DatabaseType.SQLITE, dataSource::getConnection);

        assertTrue(hasColumn("home_world"));
        assertTrue(hasColumn("home_x"));
        assertTrue(hasColumn("balance"));
        assertTrue(hasColumn("level"));
        assertTrue(hasColumn("peak_level"));
    }

    private void openH2Mysql() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:migration_mysql_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(config);
    }

    private void openSqlite(String dbPath) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setMaximumPoolSize(1);
        dataSource = new HikariDataSource(config);
    }

    private void createLegacyGuildsTable(String ddl) throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        }
    }

    private boolean hasColumn(String column) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                                 + "WHERE UPPER(TABLE_NAME) = 'GUILDS' AND UPPER(COLUMN_NAME) = '"
                                 + column.toUpperCase() + "'")) {
                rs.next();
                return rs.getInt(1) > 0;
            } catch (SQLException ignored) {
                try (var stmt = conn.createStatement();
                     var rs = stmt.executeQuery("PRAGMA table_info(guilds)")) {
                    while (rs.next()) {
                        if (column.equalsIgnoreCase(rs.getString("name"))) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
    }
}
