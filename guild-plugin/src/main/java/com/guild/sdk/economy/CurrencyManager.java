package com.guild.sdk.economy;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.core.database.DatabaseManager.DatabaseType;
import com.guild.core.utils.ColorUtils;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Module currency API (A/B/C coins).
 * <p>
 * Cache-first reads; prefer {@code *Async} methods off the server main thread.
 * Sync methods remain for compatibility but may hit JDBC on cache miss / writes.
 */
public class CurrencyManager {

    public enum CurrencyType {
        A_COIN("ACoin", "member_rank", "a_coin"),
        B_COIN("BCoin", "guild_stats", "b_coin"),
        C_COIN("CCoin", "guild_quest", "c_coin");

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

    /** (guildId, playerUuid, currencyType) -> amount */
    private final ConcurrentHashMap<String, Double> currencyCache = new ConcurrentHashMap<>();

    public CurrencyManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getServiceContainer().get(DatabaseManager.class);
        this.logger = plugin.getLogger();
        initDatabase();
    }

    private void initDatabase() {
        DatabaseType dbType = databaseManager.getDatabaseType();
        String createTableSql;
        if (dbType == DatabaseType.MYSQL) {
            createTableSql = """
            CREATE TABLE IF NOT EXISTS guild_currencies (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                a_coin DOUBLE DEFAULT 0,
                b_coin DOUBLE DEFAULT 0,
                c_coin DOUBLE DEFAULT 0,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY unique_guild_currency (guild_id, player_uuid)
            )
            """;
        } else {
            createTableSql = """
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
            )
            """;
        }

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSql);
            logger.info("[Currency] Currency table initialized");
        } catch (SQLException e) {
            logger.severe("[Currency] Failed to initialize currency table: " + e.getMessage());
        }
    }

    /**
     * Cached balance only; never hits the database. Returns null on miss.
     */
    public Double getCachedBalance(int guildId, UUID playerUuid, CurrencyType currencyType) {
        return currencyCache.get(buildCacheKey(guildId, playerUuid, currencyType));
    }

    /**
     * Get balance (cache-first; JDBC on miss). Prefer {@link #getBalanceAsync}.
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

    public CompletableFuture<Double> getBalanceAsync(int guildId, UUID playerUuid, CurrencyType currencyType) {
        String key = buildCacheKey(guildId, playerUuid, currencyType);
        Double cached = currencyCache.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> getBalance(guildId, playerUuid, currencyType));
    }

    /**
     * Deposit currency. Prefer {@link #depositAsync} off the main thread.
     */
    public boolean deposit(int guildId, UUID playerUuid, String playerName, CurrencyType currencyType, double amount) {
        if (amount <= 0) {
            return false;
        }

        String key = buildCacheKey(guildId, playerUuid, currencyType);
        try {
            String checkSql = "SELECT 1 FROM guild_currencies WHERE guild_id = ? AND player_uuid = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, guildId);
                checkStmt.setString(2, playerUuid.toString());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
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
                                // Avoid double-count: only bump known cache; otherwise reload from DB.
                                Double old = currencyCache.get(key);
                                if (old != null) {
                                    currencyCache.put(key, old + amount);
                                } else {
                                    currencyCache.put(key, loadBalanceFromDatabase(guildId, playerUuid, currencyType));
                                }
                                return true;
                            }
                        }
                    } else {
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
                                currencyCache.put(key, amount);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("[Currency] Failed to deposit currency: " + e.getMessage());
        }
        return false;
    }

    public CompletableFuture<Boolean> depositAsync(int guildId, UUID playerUuid, String playerName,
                                                   CurrencyType currencyType, double amount) {
        return CompletableFuture.supplyAsync(
                () -> deposit(guildId, playerUuid, playerName, currencyType, amount));
    }

    /**
     * Withdraw currency. Prefer {@link #withdrawAsync} off the main thread.
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
                    String key = buildCacheKey(guildId, playerUuid, currencyType);
                    currencyCache.put(key, currentBalance - amount);
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.severe("[Currency] Failed to withdraw currency: " + e.getMessage());
        }
        return false;
    }

    public CompletableFuture<Boolean> withdrawAsync(int guildId, UUID playerUuid,
                                                    CurrencyType currencyType, double amount) {
        return CompletableFuture.supplyAsync(() -> withdraw(guildId, playerUuid, currencyType, amount));
    }

    private double loadBalanceFromDatabase(int guildId, UUID playerUuid, CurrencyType currencyType) {
        try {
            String querySql = String.format(
                "SELECT %s FROM guild_currencies WHERE guild_id = ? AND player_uuid = ?",
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
            logger.severe("[Currency] Failed to load balance: " + e.getMessage());
        }
        return 0.0;
    }

    private String buildCacheKey(int guildId, UUID playerUuid, CurrencyType currencyType) {
        return guildId + "_" + playerUuid + "_" + currencyType.name();
    }

    public void clearCache() {
        currencyCache.clear();
        logger.info("[Currency] Currency cache cleared");
    }

    public void invalidate(int guildId, UUID playerUuid, CurrencyType currencyType) {
        currencyCache.remove(buildCacheKey(guildId, playerUuid, currencyType));
    }

    public void sendCurrencyMessage(Player player, CurrencyType currencyType, double amount, boolean isDeposit) {
        String key = isDeposit ? "currency.notify.gain" : "currency.notify.spend";
        String fallback = isDeposit
                ? "&6[Currency] &a+{0} {1}"
                : "&6[Currency] &c-{0} {1}";
        String amountStr = String.format("%.0f", amount);
        String msg;
        try {
            msg = plugin.getLanguageManager().getCoreIndexedMessage(
                    key, fallback, amountStr, currencyType.getDisplayName());
        } catch (Exception e) {
            msg = fallback.replace("{0}", amountStr).replace("{1}", currencyType.getDisplayName());
        }
        player.sendMessage(ColorUtils.colorize(msg));
    }
}
