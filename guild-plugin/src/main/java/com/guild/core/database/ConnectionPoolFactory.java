package com.guild.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

/**
 * 根据 database 配置创建 Hikari 连接池（SQLite / MySQL）。
 */
public final class ConnectionPoolFactory {

    private static final String DEFAULT_SQLITE_FILE = "guild.db";

    /**
     * 连接池及备份/诊断所需的连接元数据。
     */
    public record PoolConfig(
            HikariDataSource dataSource,
            DatabaseManager.DatabaseType databaseType,
            String sqliteFileName,
            String mysqlHost,
            int mysqlPort,
            String mysqlDatabase,
            String mysqlUsername,
            String mysqlPassword
    ) {
    }

    private ConnectionPoolFactory() {
    }

    public static PoolConfig create(File dataFolder, FileConfiguration config) {
        String type = resolveDatabaseType(config);
        if ("mysql".equals(type)) {
            return createMySql(config);
        }
        return createSqlite(dataFolder, config);
    }

    static String resolveDatabaseType(FileConfiguration config) {
        return config.getString("type", config.getString("database.type", "sqlite")).toLowerCase();
    }

    /** 供单测验证 JDBC 配置，不创建连接池。 */
    static HikariConfig buildMySqlHikariConfig(FileConfiguration config) {
        return configureMySql(config, new MySqlMeta());
    }

    /** 供单测验证 JDBC 配置，不创建连接池。 */
    static HikariConfig buildSqliteHikariConfig(File dataFolder, FileConfiguration config) {
        return configureSqlite(dataFolder, config, new SqliteMeta());
    }

    private static final class MySqlMeta {
        String host = "localhost";
        int port = 3306;
        String database = "guild";
        String username = "root";
        String password = "";
    }

    private static final class SqliteMeta {
        String sqliteFileName = DEFAULT_SQLITE_FILE;
    }

    private static PoolConfig createMySql(FileConfiguration config) {
        MySqlMeta meta = new MySqlMeta();
        HikariConfig hikariConfig = configureMySql(config, meta);
        return new PoolConfig(
                new HikariDataSource(hikariConfig),
                DatabaseManager.DatabaseType.MYSQL,
                DEFAULT_SQLITE_FILE,
                meta.host,
                meta.port,
                meta.database,
                meta.username,
                meta.password
        );
    }

    private static HikariConfig configureMySql(FileConfiguration config, MySqlMeta meta) {
        String host = config.getString("mysql.host", config.getString("database.mysql.host", "localhost"));
        int port = config.getInt("mysql.port", config.getInt("database.mysql.port", 3306));
        String database = config.getString("mysql.database", config.getString("database.mysql.database", "guild"));
        String params = "?useSSL=" + (config.getBoolean("mysql.use-ssl", config.getBoolean("database.mysql.use-ssl", false)) ? "true" : "false")
                + "&serverTimezone=" + config.getString("mysql.timezone", config.getString("database.mysql.timezone", "UTC"))
                + "&characterEncoding=" + config.getString("mysql.character-encoding", config.getString("database.mysql.character-encoding", "UTF-8"));

        meta.host = host;
        meta.port = port;
        meta.database = database;
        meta.username = config.getString("mysql.username", config.getString("database.mysql.username", "root"));
        meta.password = config.getString("mysql.password", config.getString("database.mysql.password", ""));

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + params);
        hikariConfig.setUsername(meta.username);
        hikariConfig.setPassword(meta.password);
        hikariConfig.setMaximumPoolSize(config.getInt("mysql.pool-size", config.getInt("database.mysql.pool-size", 20)));
        hikariConfig.setMinimumIdle(config.getInt("mysql.min-idle", config.getInt("database.mysql.min-idle", 10)));
        hikariConfig.setConnectionTimeout(config.getLong("mysql.connection-timeout", config.getLong("database.mysql.connection-timeout", 60000)));
        hikariConfig.setIdleTimeout(config.getLong("mysql.idle-timeout", config.getLong("database.mysql.idle-timeout", 600000)));
        hikariConfig.setMaxLifetime(config.getLong("mysql.max-lifetime", config.getLong("database.mysql.max-lifetime", 1800000)));
        return hikariConfig;
    }

    private static PoolConfig createSqlite(File dataFolder, FileConfiguration config) {
        SqliteMeta meta = new SqliteMeta();
        HikariConfig hikariConfig = configureSqlite(dataFolder, config, meta);
        return new PoolConfig(
                new HikariDataSource(hikariConfig),
                DatabaseManager.DatabaseType.SQLITE,
                meta.sqliteFileName,
                "localhost",
                3306,
                "guild",
                "root",
                ""
        );
    }

    private static HikariConfig configureSqlite(File dataFolder, FileConfiguration config, SqliteMeta meta) {
        HikariConfig hikariConfig = new HikariConfig();
        meta.sqliteFileName = config.getString("sqlite.file", config.getString("database.sqlite.file", DEFAULT_SQLITE_FILE));
        String dbPath = dataFolder + "/" + meta.sqliteFileName;
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbPath);

        int maxPool = config.getInt("connection-pool.maximum-pool-size", 2);
        if (maxPool < 1) {
            maxPool = 1;
        }
        hikariConfig.setMaximumPoolSize(maxPool);
        hikariConfig.setConnectionTimeout(config.getLong("connection-pool.connection-timeout", 10000));
        hikariConfig.setIdleTimeout(config.getLong("connection-pool.idle-timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("connection-pool.max-lifetime", 1800000));

        boolean walMode = config.getBoolean("sqlite.wal-mode", true);
        String synchronous = config.getString("sqlite.synchronous", "NORMAL");
        boolean foreignKeys = config.getBoolean("sqlite.foreign-keys", true);
        int cacheSize = config.getInt("sqlite.cache-size", 2000);
        int busyTimeoutMs = (int) config.getLong("sqlite.busy-timeout", 5000);
        StringBuilder initSql = new StringBuilder();
        if (walMode) {
            initSql.append("PRAGMA journal_mode=WAL;");
        }
        if (synchronous != null) {
            initSql.append("PRAGMA synchronous=").append(synchronous).append(";");
        }
        initSql.append("PRAGMA foreign_keys=").append(foreignKeys ? "ON" : "OFF").append(";");
        initSql.append("PRAGMA cache_size=").append(cacheSize).append(";");
        initSql.append("PRAGMA busy_timeout=").append(busyTimeoutMs).append(";");
        hikariConfig.setConnectionInitSql(initSql.toString());
        return hikariConfig;
    }
}
