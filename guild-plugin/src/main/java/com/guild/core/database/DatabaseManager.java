package com.guild.core.database;

import com.guild.GuildPlugin;
import com.zaxxer.hikari.HikariConfig;
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
    
    public DatabaseManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 初始化数据库连接
     */
    public void initialize() {
        FileConfiguration config = plugin.getConfigManager().getDatabaseConfig();
        // 兼容两种结构：root 与 database. 前缀
        String type = config.getString("type", config.getString("database.type", "sqlite")).toLowerCase();
        
        try {
            if ("mysql".equals(type)) {
                initializeMySQL(config);
            } else {
                initializeSQLite(config);
            }
            
            // 创建数据表
            createTables();
            
            logger.info("数据库连接初始化成功: " + databaseType);
            
        } catch (Exception e) {
            logger.severe("数据库连接初始化失败: " + e.getMessage());
            throw new RuntimeException("数据库连接失败", e);
        }
    }
    
    /**
     * 初始化MySQL连接
     */
    private void initializeMySQL(FileConfiguration config) {
        databaseType = DatabaseType.MYSQL;
        
        HikariConfig hikariConfig = new HikariConfig();
        String host = config.getString("mysql.host", config.getString("database.mysql.host", "localhost"));
        int port = config.getInt("mysql.port", config.getInt("database.mysql.port", 3306));
        String database = config.getString("mysql.database", config.getString("database.mysql.database", "guild"));
        String params = "?useSSL=" + (config.getBoolean("mysql.use-ssl", config.getBoolean("database.mysql.use-ssl", false)) ? "true" : "false") +
                "&serverTimezone=" + config.getString("mysql.timezone", config.getString("database.mysql.timezone", "UTC")) +
                "&characterEncoding=" + config.getString("mysql.character-encoding", config.getString("database.mysql.character-encoding", "UTF-8"));
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + params);
        
        hikariConfig.setUsername(config.getString("mysql.username", config.getString("database.mysql.username", "root")));
        hikariConfig.setPassword(config.getString("mysql.password", config.getString("database.mysql.password", "")));
        hikariConfig.setMaximumPoolSize(config.getInt("mysql.pool-size", config.getInt("database.mysql.pool-size", 20)));
        hikariConfig.setMinimumIdle(config.getInt("mysql.min-idle", config.getInt("database.mysql.min-idle", 10)));
        hikariConfig.setConnectionTimeout(config.getLong("mysql.connection-timeout", config.getLong("database.mysql.connection-timeout", 60000)));
        hikariConfig.setIdleTimeout(config.getLong("mysql.idle-timeout", config.getLong("database.mysql.idle-timeout", 600000)));
        hikariConfig.setMaxLifetime(config.getLong("mysql.max-lifetime", config.getLong("database.mysql.max-lifetime", 1800000)));
        
        dataSource = new HikariDataSource(hikariConfig);
    }
    
    /**
     * 初始化SQLite连接
     */
    private void initializeSQLite(FileConfiguration config) {
        databaseType = DatabaseType.SQLITE;
        
        HikariConfig hikariConfig = new HikariConfig();
        String fileName = config.getString("sqlite.file", config.getString("database.sqlite.file", "guild.db"));
        String dbPath = plugin.getDataFolder() + "/" + fileName;
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbPath);
        int maxPool = config.getInt("connection-pool.maximum-pool-size", 2);
        if (maxPool < 1) { maxPool = 1; }
        hikariConfig.setMaximumPoolSize(maxPool);
        long connTimeout = config.getLong("connection-pool.connection-timeout", 10000);
        hikariConfig.setConnectionTimeout(connTimeout);
        long idleTimeout = config.getLong("connection-pool.idle-timeout", 600000);
        hikariConfig.setIdleTimeout(idleTimeout);
        long maxLifetime = config.getLong("connection-pool.max-lifetime", 1800000);
        hikariConfig.setMaxLifetime(maxLifetime);

        // SQLite优化：根据配置设置PRAGMA，减少锁等待与提升并发读
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
        
        dataSource = new HikariDataSource(hikariConfig);
    }
    
    /**
     * 创建数据表
     */
    private void createTables() {
        if (databaseType == DatabaseType.SQLITE) {
            createSQLiteTables();
        } else {
            createMySQLTables();
        }
        
        // 异步检查并添加缺失的列，避免阻塞启动
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // 等待1秒确保数据库连接稳定
                checkAndAddMissingColumns();
            } catch (Exception e) {
                logger.warning("异步检查数据库列时发生错误: " + e.getMessage());
            }
        });
        
        logger.info("数据表创建完成");
    }
    
    /**
     * 创建SQLite数据表
     */
    private void createSQLiteTables() {
        // 工会表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guilds (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                tag TEXT UNIQUE,
                description TEXT,
                leader_uuid TEXT NOT NULL,
                leader_name TEXT NOT NULL,
                home_world TEXT,
                home_x REAL,
                home_y REAL,
                home_z REAL,
                home_yaw REAL,
                home_pitch REAL,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                updated_at TEXT DEFAULT (datetime('now','localtime'))
            )
        """);
        
        // 工会成员表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                role TEXT DEFAULT 'MEMBER',
                joined_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE(guild_id, player_uuid)
            )
        """);
        
        // 工会申请表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_applications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                message TEXT,
                status TEXT DEFAULT 'PENDING',
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        // 工会邀请表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_invites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                inviter_uuid TEXT NOT NULL,
                inviter_name TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING',
                expires_at TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        // 工会关系表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_relations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild1_id INTEGER NOT NULL,
                guild2_id INTEGER NOT NULL,
                guild1_name TEXT NOT NULL,
                guild2_name TEXT NOT NULL,
                relation_type TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING',
                initiator_uuid TEXT NOT NULL,
                initiator_name TEXT NOT NULL,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                updated_at TEXT DEFAULT (datetime('now','localtime')),
                expires_at TEXT,
                FOREIGN KEY (guild1_id) REFERENCES guilds(id) ON DELETE CASCADE,
                FOREIGN KEY (guild2_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE(guild1_id, guild2_id)
            )
        """);
        
        // 工会经济表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_economy (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL UNIQUE,
                balance REAL DEFAULT 0.0,
                level INTEGER DEFAULT 1,
                experience REAL DEFAULT 0.0,
                max_experience REAL DEFAULT 5000.0,
                max_members INTEGER DEFAULT 6,
                last_updated TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        // 工会贡献记录表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_contributions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                amount REAL NOT NULL,
                contribution_type TEXT NOT NULL,
                description TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        // 工会日志表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                guild_name TEXT NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                log_type TEXT NOT NULL,
                description TEXT NOT NULL,
                details TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }
    
    /**
     * 创建MySQL数据表
     */
    private void createMySQLTables() {
        // 工会表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guilds (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) UNIQUE NOT NULL,
                tag VARCHAR(10) UNIQUE,
                description TEXT,
                leader_uuid VARCHAR(36) NOT NULL,
                leader_name VARCHAR(16) NOT NULL,
                home_world VARCHAR(100),
                home_x DOUBLE,
                home_y DOUBLE,
                home_z DOUBLE,
                home_yaw FLOAT,
                home_pitch FLOAT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
        """);
        
        // 工会成员表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_members (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                role VARCHAR(20) DEFAULT 'MEMBER',
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE KEY unique_guild_player (guild_id, player_uuid)
            )
        """);
        
        // 工会申请表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_applications (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                message TEXT,
                status VARCHAR(20) DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        // 工会邀请表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_invites (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                inviter_uuid VARCHAR(36) NOT NULL,
                inviter_name VARCHAR(16) NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                expires_at TIMESTAMP NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        // 工会关系表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_relations (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild1_id INT NOT NULL,
                guild2_id INT NOT NULL,
                guild1_name VARCHAR(50) NOT NULL,
                guild2_name VARCHAR(50) NOT NULL,
                relation_type VARCHAR(20) NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                initiator_uuid VARCHAR(36) NOT NULL,
                initiator_name VARCHAR(16) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NULL,
                FOREIGN KEY (guild1_id) REFERENCES guilds(id) ON DELETE CASCADE,
                FOREIGN KEY (guild2_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE KEY unique_guild_relation (guild1_id, guild2_id)
            )
        """);
        
        // 工会经济表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_economy (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL UNIQUE,
                balance DOUBLE DEFAULT 0.0,
                level INT DEFAULT 1,
                experience DOUBLE DEFAULT 0.0,
                max_experience DOUBLE DEFAULT 5000.0,
                max_members INT DEFAULT 6,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        // 工会贡献记录表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_contributions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                amount DOUBLE NOT NULL,
                contribution_type VARCHAR(20) NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        
        // 工会日志表
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_logs (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                guild_name VARCHAR(50) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                log_type VARCHAR(50) NOT NULL,
                description TEXT NOT NULL,
                details TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
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
            logger.severe("执行更新操作失败: " + e.getMessage());
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
            logger.severe("执行查询操作失败: " + e.getMessage());
            throw new RuntimeException("数据库操作失败", e);
        }
    }
    
    /**
     * 关闭数据库连接
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("数据库连接已关闭");
        }
    }
    
    /**
     * 获取数据库类型
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
    
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
            logger.info("数据库列检查完成");
        } catch (Exception e) {
            logger.warning("检查数据库列时发生错误: " + e.getMessage());
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
                    logger.info("已为guilds表添加home相关列");
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
                    logger.info("已为guilds表添加economy相关列");
                }
            }
            
            conn.commit(); // 提交事务
        } catch (SQLException e) {
            logger.warning("检查SQLite列时发生错误: " + e.getMessage());
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
                    logger.info("已为guilds表添加home相关列");
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
                    logger.info("已为guilds表添加economy相关列");
                }
            }
            
            conn.commit(); // 提交事务
        } catch (SQLException e) {
            logger.warning("检查MySQL列时发生错误: " + e.getMessage());
        }
    }
    
    public enum DatabaseType {
        MYSQL, SQLITE
    }
}
