package com.guild.core.database;

import com.guild.GuildPlugin;
import com.guild.core.database.schema.SchemaInitializer;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * 数据库管理器 - 管理数据库连接和操作
 */
public class DatabaseManager {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;
    private DatabaseType databaseType;
    /** Relative or absolute sqlite file name from config (under data folder when relative). */
    private String sqliteFileName = "guild.db";
    private String mysqlHost = "localhost";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "guild";
    private String mysqlUsername = "root";
    private String mysqlPassword = "";
    
    private final DatabaseMigrationService migrationService;

    public DatabaseManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.migrationService = new DatabaseMigrationService(logger);
    }
    
    /**
     * 初始化数据库连接
     */
    public void initialize() {
        FileConfiguration config = plugin.getConfigManager().getDatabaseConfig();

        try {
            ConnectionPoolFactory.PoolConfig pool = ConnectionPoolFactory.create(plugin.getDataFolder(), config);
            applyPoolConfig(pool);

            // 创建数据表
            createTables();

            logger.info("Database connection initialized successfully: " + databaseType);

        } catch (Exception e) {
            logger.severe("Database connection initialization failed: " + e.getMessage());
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    private void applyPoolConfig(ConnectionPoolFactory.PoolConfig pool) {
        this.dataSource = pool.dataSource();
        this.databaseType = pool.databaseType();
        this.sqliteFileName = pool.sqliteFileName();
        this.mysqlHost = pool.mysqlHost();
        this.mysqlPort = pool.mysqlPort();
        this.mysqlDatabase = pool.mysqlDatabase();
        this.mysqlUsername = pool.mysqlUsername();
        this.mysqlPassword = pool.mysqlPassword();
    }

    /**
     * 创建数据表
     */
    private void createTables() {
        SchemaInitializer.createAll(databaseType, this::executeUpdate);

        // 异步检查并添加缺失的列，避免阻塞启动
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // 等待1秒确保数据库连接稳定
                migrationService.checkAndAddMissingColumns(databaseType, this::getConnection);
            } catch (Exception e) {
                logger.warning("Error during async database column check: " + e.getMessage());
            }
        });

        logger.info("Database tables created successfully");
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("数据库连接未初始化");
        }
        return dataSource.getConnection();
    }
    
    /**
     * 执行更新操作
     */
    public int executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.severe("Update operation failed: " + e.getMessage());
            throw new RuntimeException("数据库操作失败", e);
        }
    }
    
    /**
     * 异步执行更新操作
     */
    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> executeUpdate(sql, params));
    }
    
    /**
     * 执行查询操作
     */
    public ResultSet executeQuery(String sql, Object... params) {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            return stmt.executeQuery();
            
        } catch (SQLException e) {
            logger.severe("Query operation failed: " + e.getMessage());
            throw new RuntimeException("数据库操作失败", e);
        }
    }
    
    /**
     * 关闭数据库连接
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection closed");
        }
    }
    
    /**
     * 获取数据库类型
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    /** Absolute path to the primary SQLite database file. */
    public java.io.File getSqliteDatabaseFile() {
        java.io.File f = new java.io.File(sqliteFileName);
        if (f.isAbsolute()) {
            return f;
        }
        return new java.io.File(plugin.getDataFolder(), sqliteFileName);
    }

    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }

    public enum DatabaseType {
        MYSQL, SQLITE
    }
}
