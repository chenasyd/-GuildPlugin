package com.guild.core.database.schema;

import com.guild.core.database.DatabaseManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaInitializerTest {

    private HikariDataSource dataSource;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        dataSource = null;
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void createAll_mysql_createsExpectedTablesOnEmptyDatabase() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:schema_mysql_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(config);

        runInitializer(DatabaseManager.DatabaseType.MYSQL);
        assertAllExpectedTablesPresent();
    }

    @Test
    void createAll_sqlite_createsExpectedTablesOnEmptyDatabase() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + tempDir.resolve("schema.db"));
        config.setMaximumPoolSize(1);
        dataSource = new HikariDataSource(config);

        runInitializer(DatabaseManager.DatabaseType.SQLITE);
        assertAllExpectedTablesPresent();
    }

    private void runInitializer(DatabaseManager.DatabaseType type) throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            SchemaInitializer.createAll(type, sql -> {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void assertAllExpectedTablesPresent() throws SQLException {
        Set<String> tables = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (var rs = meta.getTables(null, null, null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        }
        for (String expected : SchemaInitializer.expectedTableNames()) {
            assertTrue(tables.contains(expected.toLowerCase()),
                    "Missing table: " + expected + ", found: " + tables);
        }
    }
}
