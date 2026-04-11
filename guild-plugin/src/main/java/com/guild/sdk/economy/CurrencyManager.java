package com.guild.sdk.economy;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.core.utils.ColorUtils;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 货币管理API
 * <p>
 * 提供多种类型货币的管理，支持：
 * - A币（成员排名模块）
 * - B币（公会统计模块）
 * - C币（任务模块）
 * <p>
 * 所有货币操作都是线程安全的
 */
public class CurrencyManager {

    public enum CurrencyType {
        A_COIN("A币", "member_rank", "a_coin"),
        B_COIN("B币", "guild_stats", "b_coin"),
        C_COIN("C币", "guild_quest", "c_coin");

        private final String displayName;
        private final String moduleName;
        private final String dbColumn;

        CurrencyType(String displayName, String moduleName, String dbColumn) {
            this.displayName = displayName;
            this.moduleName = moduleName;
            this.dbColumn = dbColumn;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getDbColumn() {
            return dbColumn;
        }
    }

    private final GuildPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    
    // 内存缓存: (guildId, playerUuid, currencyType) -> amount
    private final ConcurrentHashMap<String, Double> currencyCache = new ConcurrentHashMap<>();

    public CurrencyManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getServiceContainer().get(DatabaseManager.class);
        this.logger = plugin.getLogger();
        initDatabase();
    }

    /**
     * 初始化数据库表
     */
    private void initDatabase() {
        String createTableSql = """
        CREATE TABLE IF NOT EXISTS guild_currencies (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            guild_id INTEGER NOT NULL,
            player_uuid TEXT NOT NULL,
            player_name TEXT NOT NULL,
            a_coin REAL DEFAULT 0,
            b_coin REAL DEFAULT 0,
            c_coin REAL DEFAULT 0,
            last_updated DATETIME DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(guild_id, player_uuid)
        );
        """;

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSql);
            logger.info("[Currency] 货币表初始化完成");
        } catch (SQLException e) {
            logger.severe("[Currency] 初始化货币表失败: " + e.getMessage());
        }
    }

    /**
     * 获取玩家的货币余额
     *
     * @param guildId     公会ID
     * @param playerUuid  玩家UUID
     * @param currencyType 货币类型
     * @return 货币余额
     */
    public double getBalance(int guildId, UUID playerUuid, CurrencyType currencyType) {
        String key = buildCacheKey(guildId, playerUuid, currencyType);
        Double cached = currencyCache.get(key);
        if (cached != null) {
            return cached;
        }

        double balance = loadBalanceFromDatabase(guildId, playerUuid, currencyType);
        currencyCache.put(key, balance);
        return balance;
    }

    /**
     * 增加玩家的货币
     *
     * @param guildId     公会ID
     * @param playerUuid  玩家UUID
     * @param playerName  玩家名称
     * @param currencyType 货币类型
     * @param amount      增加金额
     * @return 操作是否成功
     */
    public boolean deposit(int guildId, UUID playerUuid, String playerName, CurrencyType currencyType, double amount) {
        if (amount <= 0) {
            return false;
        }

        try {
            // 先检查记录是否存在
            String checkSql = "SELECT 1 FROM guild_currencies WHERE guild_id = ? AND player_uuid = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, guildId);
                checkStmt.setString(2, playerUuid.toString());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // 记录存在，执行更新
                        String updateSql = String.format(
                            "UPDATE guild_currencies SET player_name = ?, %s = %s + ?, last_updated = CURRENT_TIMESTAMP WHERE guild_id = ? AND player_uuid = ?",
                            currencyType.getDbColumn(),
                            currencyType.getDbColumn()
                        );
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, playerName);
                            updateStmt.setDouble(2, amount);
                            updateStmt.setInt(3, guildId);
                            updateStmt.setString(4, playerUuid.toString());
                            int affected = updateStmt.executeUpdate();
                            if (affected > 0) {
                                // 更新缓存
                                String key = buildCacheKey(guildId, playerUuid, currencyType);
                                double newBalance = getBalance(guildId, playerUuid, currencyType) + amount;
                                currencyCache.put(key, newBalance);
                                return true;
                            }
                        }
                    } else {
                        // 记录不存在，执行插入
                        String insertSql = String.format(
                            "INSERT INTO guild_currencies (guild_id, player_uuid, player_name, %s, last_updated) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                            currencyType.getDbColumn()
                        );
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, guildId);
                            insertStmt.setString(2, playerUuid.toString());
                            insertStmt.setString(3, playerName);
                            insertStmt.setDouble(4, amount);
                            int affected = insertStmt.executeUpdate();
                            if (affected > 0) {
                                // 更新缓存
                                String key = buildCacheKey(guildId, playerUuid, currencyType);
                                currencyCache.put(key, amount);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("[Currency] 增加货币失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 减少玩家的货币
     *
     * @param guildId     公会ID
     * @param playerUuid  玩家UUID
     * @param currencyType 货币类型
     * @param amount      减少金额
     * @return 操作是否成功
     */
    public boolean withdraw(int guildId, UUID playerUuid, CurrencyType currencyType, double amount) {
        if (amount <= 0) {
            return false;
        }

        double currentBalance = getBalance(guildId, playerUuid, currencyType);
        if (currentBalance < amount) {
            return false;
        }

        try {
            String updateSql = String.format(
                "UPDATE guild_currencies " +
                "SET %s = %s - ?, last_updated = CURRENT_TIMESTAMP " +
                "WHERE guild_id = ? AND player_uuid = ? AND %s >= ?",
                currencyType.getDbColumn(),
                currencyType.getDbColumn(),
                currencyType.getDbColumn()
            );

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setDouble(1, amount);
                stmt.setInt(2, guildId);
                stmt.setString(3, playerUuid.toString());
                stmt.setDouble(4, amount);
                int affected = stmt.executeUpdate();
                
                if (affected > 0) {
                    // 更新缓存
                    String key = buildCacheKey(guildId, playerUuid, currencyType);
                    double newBalance = currentBalance - amount;
                    currencyCache.put(key, newBalance);
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.severe("[Currency] 减少货币失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 从数据库加载余额
     */
    private double loadBalanceFromDatabase(int guildId, UUID playerUuid, CurrencyType currencyType) {
        try {
            String querySql = String.format(
                "SELECT %s FROM guild_currencies " +
                "WHERE guild_id = ? AND player_uuid = ?",
                currencyType.getDbColumn()
            );

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(querySql)) {
                stmt.setInt(1, guildId);
                stmt.setString(2, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble(currencyType.getDbColumn());
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("[Currency] 加载余额失败: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(int guildId, UUID playerUuid, CurrencyType currencyType) {
        return guildId + "_" + playerUuid.toString() + "_" + currencyType.name();
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        currencyCache.clear();
        logger.info("[Currency] 货币缓存已清除");
    }

    /**
     * 为玩家发送货币变动消息
     */
    public void sendCurrencyMessage(Player player, CurrencyType currencyType, double amount, boolean isDeposit) {
        String prefix = isDeposit ? "&a获得" : "&c消耗";
        String amountStr = String.format("%.0f", amount);
        String message = ColorUtils.colorize(
            String.format("&6[货币] %s %s %s", prefix, amountStr, currencyType.getDisplayName())
        );
        player.sendMessage(message);
    }
}