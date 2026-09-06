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
    
    public DatabaseManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
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
                checkAndAddMissingColumns();
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
    
    /**
     * 数据库类型枚举
     */
    /**
     * 检查并添加缺失的列
     */
    private void checkAndAddMissingColumns() {
        try {
            if (databaseType == DatabaseType.SQLITE) {
                checkAndAddSQLiteColumns();
            } else {
                checkAndAddMySQLColumns();
            }
            logger.info("Database column check completed");
        } catch (Exception e) {
            logger.warning("Error checking database columns: " + e.getMessage());
        }
    }
    
    /**
     * 检查并添加SQLite缺失的列
     */
    private void checkAndAddSQLiteColumns() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // 开启事务以提高性能
            
            // 检查guilds表是否有home相关列
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "guilds", "home_world")) {
                if (!rs.next()) {
                    // 添加home相关列
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_world TEXT")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_x REAL")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_y REAL")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_z REAL")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_yaw REAL")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_pitch REAL")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added home columns to guilds table");
                }
            }
            
            // 检查guilds表是否有economy相关列
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "guilds", "balance")) {
                if (!rs.next()) {
                    // 添加economy相关列
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN balance REAL DEFAULT 0.0")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN level INTEGER DEFAULT 1")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN max_members INTEGER DEFAULT 6")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN frozen INTEGER DEFAULT 0")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added economy columns to guilds table");
                }
            }

            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "guilds", "peak_level")) {
                if (!rs.next()) {
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN peak_level INTEGER DEFAULT 1")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE guilds SET peak_level = level WHERE peak_level IS NULL OR peak_level < level")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added peak_level column to guilds table");
                }
            }
            
            conn.commit(); // 提交事务
        } catch (SQLException e) {
            logger.warning("Error checking SQLite columns: " + e.getMessage());
        }
    }
    
    /**
     * 检查并添加MySQL缺失的列
     */
    private void checkAndAddMySQLColumns() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // 开启事务以提高性能
            
            // 检查guilds表是否有home相关列
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "guilds", "home_world")) {
                if (!rs.next()) {
                    // 添加home相关列
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_world VARCHAR(100)")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_x DOUBLE")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_y DOUBLE")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_z DOUBLE")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_yaw FLOAT")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN home_pitch FLOAT")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added home columns to guilds table");
                }
            }
            
            // 检查guilds表是否有economy相关列
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "guilds", "balance")) {
                if (!rs.next()) {
                    // 添加economy相关列
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN balance DOUBLE DEFAULT 0.0")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN level INT DEFAULT 1")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN max_members INT DEFAULT 6")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN frozen BOOLEAN DEFAULT FALSE")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added economy columns to guilds table");
                }
            }

            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "guilds", "peak_level")) {
                if (!rs.next()) {
                    try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE guilds ADD COLUMN peak_level INT DEFAULT 1")) {
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE guilds SET peak_level = level WHERE peak_level IS NULL OR peak_level < level")) {
                        stmt.executeUpdate();
                    }
                    logger.info("Added peak_level column to guilds table");
                }
            }
            
            conn.commit(); // 提交事务
        } catch (SQLException e) {
            logger.warning("Error checking MySQL columns: " + e.getMessage());
        }
    }
    
    public enum DatabaseType {
        MYSQL, SQLITE
    }
}
