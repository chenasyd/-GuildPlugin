package com.guild.core.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * 启动后异步列迁移（legacy guilds 表补列）。
 */
public final class DatabaseMigrationService {

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection getConnection() throws SQLException;
    }

    private final Logger logger;

    public DatabaseMigrationService(Logger logger) {
        this.logger = logger;
    }

    public void checkAndAddMissingColumns(DatabaseManager.DatabaseType type, ConnectionSupplier connectionSupplier) {
        try {
            if (type == DatabaseManager.DatabaseType.SQLITE) {
                checkAndAddSQLiteColumns(connectionSupplier);
            } else {
                checkAndAddMySQLColumns(connectionSupplier);
            }
            logger.info("Database column check completed");
        } catch (Exception e) {
            logger.warning("Error checking database columns: " + e.getMessage());
        }
    }

    private void checkAndAddSQLiteColumns(ConnectionSupplier connectionSupplier) {
        try (Connection conn = connectionSupplier.getConnection()) {
            conn.setAutoCommit(false);

            try (var rs = conn.getMetaData().getColumns(null, null, "guilds", "home_world")) {
                if (!rs.next()) {
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_world TEXT")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_x REAL")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_y REAL")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_z REAL")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_yaw REAL")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_pitch REAL")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added home columns to guilds table");
                }
            }

            try (var rs = conn.getMetaData().getColumns(null, null, "guilds", "balance")) {
                if (!rs.next()) {
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN balance REAL DEFAULT 0.0")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN level INTEGER DEFAULT 1")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN max_members INTEGER DEFAULT 6")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN frozen INTEGER DEFAULT 0")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added economy columns to guilds table");
                }
            }

            try (var rs = conn.getMetaData().getColumns(null, null, "guilds", "peak_level")) {
                if (!rs.next()) {
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN peak_level INTEGER DEFAULT 1")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement(
                            "UPDATE guilds SET peak_level = level WHERE peak_level IS NULL OR peak_level < level")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added peak_level column to guilds table");
                }
            }

            conn.commit();
        } catch (SQLException e) {
            logger.warning("Error checking SQLite columns: " + e.getMessage());
        }
    }

    private void checkAndAddMySQLColumns(ConnectionSupplier connectionSupplier) {
        try (Connection conn = connectionSupplier.getConnection()) {
            conn.setAutoCommit(false);

            try (var rs = conn.getMetaData().getColumns(null, null, "guilds", "home_world")) {
                if (!rs.next()) {
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_world VARCHAR(100)")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_x DOUBLE")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_y DOUBLE")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_z DOUBLE")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_yaw FLOAT")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_pitch FLOAT")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added home columns to guilds table");
                }
            }

            try (var rs = conn.getMetaData().getColumns(null, null, "guilds", "balance")) {
                if (!rs.next()) {
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN balance DOUBLE DEFAULT 0.0")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN level INT DEFAULT 1")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN max_members INT DEFAULT 6")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN frozen BOOLEAN DEFAULT FALSE")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added economy columns to guilds table");
                }
            }

            try (var rs = conn.getMetaData().getColumns(null, null, "guilds", "peak_level")) {
                if (!rs.next()) {
                    try (var stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN peak_level INT DEFAULT 1")) {
                        stmt.executeUpdate();
                    }
                    try (var stmt = conn.prepareStatement(
                            "UPDATE guilds SET peak_level = level WHERE peak_level IS NULL OR peak_level < level")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added peak_level column to guilds table");
                }
            }

            conn.commit();
        } catch (SQLException e) {
            logger.warning("Error checking MySQL columns: " + e.getMessage());
        }
    }
}
