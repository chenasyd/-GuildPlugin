package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 公会成员投资记录服务 —— 跟踪每位成员在公会中的累计资金投入。
 * <p>
 * 使用独立表 guild_member_investments，不修改现有数据库结构，保证向后兼容。
 */
public class GuildInvestmentService {

    private final GuildPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Logger logger;

    // 内存缓存: "guildId_playerUuid" -> investedBalance
    private final ConcurrentHashMap<String, Double> balanceCache = new ConcurrentHashMap<>();

    public GuildInvestmentService(GuildPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getServiceContainer().get(DatabaseManager.class);
        this.logger = plugin.getLogger();
        initTable();
    }

    private void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS guild_member_investments (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id        INTEGER NOT NULL,
                player_uuid     TEXT    NOT NULL,
                player_name     TEXT    NOT NULL,
                total_invested  REAL    DEFAULT 0.0,
                last_deposit    REAL,
                last_deposit_at DATETIME,
                total_withdrawn REAL    DEFAULT 0.0,
                created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(guild_id, player_uuid)
            );
            """;
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("[Investment] 投资记录表初始化完成");
        } catch (SQLException e) {
            logger.severe("[Investment] 初始化投资记录表失败: " + e.getMessage());
        }
    }

    /** 记录存款并更新投资总额 */
    public void recordDeposit(int guildId, UUID playerUuid, String playerName, double amount) {
        if (amount <= 0) return;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO guild_member_investments (guild_id, player_uuid, player_name, total_invested, last_deposit, last_deposit_at) " +
                 "VALUES (?, ?, ?, ?, ?, datetime('now', 'localtime')) " +
                 "ON CONFLICT(guild_id, player_uuid) DO UPDATE SET " +
                 "total_invested = total_invested + ?, last_deposit = ?, last_deposit_at = datetime('now', 'localtime'), " +
                 "player_name = ?, updated_at = datetime('now', 'localtime')")) {
            stmt.setInt(1, guildId);
            stmt.setString(2, playerUuid.toString());
            stmt.setString(3, playerName);
            stmt.setDouble(4, amount);
            stmt.setDouble(5, amount);
            stmt.setDouble(6, amount);
            stmt.setString(7, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[Investment] 记录存款失败: " + e.getMessage());
        }
        // 更新缓存
        String key = guildId + "_" + playerUuid.toString();
        balanceCache.compute(key, (k, v) -> (v == null ? 0.0 : v) + amount);
    }

    /** 记录取款（不影响投资总额，仅跟踪取款统计） */
    public void recordWithdraw(int guildId, UUID playerUuid, double amount) {
        if (amount <= 0) return;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO guild_member_investments (guild_id, player_uuid, player_name, total_invested, total_withdrawn) " +
                 "VALUES (?, ?, '', 0, ?) " +
                 "ON CONFLICT(guild_id, player_uuid) DO UPDATE SET " +
                 "total_withdrawn = total_withdrawn + ?, updated_at = datetime('now', 'localtime')")) {
            stmt.setInt(1, guildId);
            stmt.setString(2, playerUuid.toString());
            stmt.setDouble(3, amount);
            stmt.setDouble(4, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[Investment] 记录取款失败: " + e.getMessage());
        }
    }

    /** 查询玩家在指定公会的累计投资额 */
    public double getInvestedBalance(int guildId, UUID playerUuid) {
        String key = guildId + "_" + playerUuid.toString();
        Double cached = balanceCache.get(key);
        if (cached != null) return cached;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT total_invested FROM guild_member_investments WHERE guild_id = ? AND player_uuid = ?")) {
            stmt.setInt(1, guildId);
            stmt.setString(2, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double balance = rs.getDouble("total_invested");
                    balanceCache.put(key, balance);
                    return balance;
                }
            }
        } catch (SQLException e) {
            logger.warning("[Investment] 查询投资额失败: " + e.getMessage());
        }
        return 0.0;
    }

    public void clearCache() {
        balanceCache.clear();
    }
}
