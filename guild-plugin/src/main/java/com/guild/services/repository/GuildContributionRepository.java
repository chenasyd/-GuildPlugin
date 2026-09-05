package com.guild.services.repository;

import com.guild.core.database.DatabaseManager;
import com.guild.core.time.TimeProvider;
import com.guild.models.GuildContribution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class GuildContributionRepository {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public GuildContributionRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public CompletableFuture<Boolean> insertAsync(int guildId, UUID playerUuid, String playerName, double amount,
                                                GuildContribution.ContributionType type, String description) {
        return CompletableFuture.supplyAsync(() -> insert(guildId, playerUuid, playerName, amount, type, description));
    }

    public boolean insert(int guildId, UUID playerUuid, String playerName, double amount,
                          GuildContribution.ContributionType type, String description) {
        try {
            String sql = "INSERT INTO guild_contributions (guild_id, player_uuid, player_name, amount, "
                    + "contribution_type, description) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                stmt.setString(2, playerUuid.toString());
                stmt.setString(3, playerName);
                stmt.setDouble(4, amount);
                stmt.setString(5, type.name());
                stmt.setString(6, description);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error inserting guild contribution: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<List<GuildContribution>> findAllByGuildIdAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> findAllByGuildId(guildId));
    }

    public List<GuildContribution> findAllByGuildId(int guildId) {
        List<GuildContribution> contributions = new ArrayList<>();
        try {
            String sql = "SELECT * FROM guild_contributions WHERE guild_id = ? ORDER BY created_at DESC";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        contributions.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild contribution records: " + e.getMessage());
        }
        return contributions;
    }

    public CompletableFuture<List<GuildContribution>> findAllByPlayerUuidAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> findAllByPlayerUuid(playerUuid));
    }

    public List<GuildContribution> findAllByPlayerUuid(UUID playerUuid) {
        List<GuildContribution> contributions = new ArrayList<>();
        try {
            String sql = "SELECT * FROM guild_contributions WHERE player_uuid = ? ORDER BY created_at DESC";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        contributions.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching player contribution records: " + e.getMessage());
        }
        return contributions;
    }

    public CompletableFuture<Map<UUID, Double>> computeNetByPlayerAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> computeNetByPlayer(guildId));
    }

    public Map<UUID, Double> computeNetByPlayer(int guildId) {
        Map<UUID, Double> nets = new HashMap<>();
        try {
            String sql = "SELECT player_uuid, contribution_type, amount FROM guild_contributions WHERE guild_id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                        String typeName = rs.getString("contribution_type");
                        double amount = rs.getDouble("amount");
                        GuildContribution.ContributionType type;
                        try {
                            type = GuildContribution.ContributionType.valueOf(typeName);
                        } catch (IllegalArgumentException ex) {
                            type = GuildContribution.ContributionType.ADMIN;
                        }
                        double net = (type == GuildContribution.ContributionType.WITHDRAW) ? -amount : amount;
                        nets.merge(uuid, net, Double::sum);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error aggregating guild net contributions: " + e.getMessage());
        }
        return nets;
    }

    public CompletableFuture<List<GuildContribution>> computeDepositTotalsAsync(int guildId) {
        return CompletableFuture.supplyAsync(() -> computeDepositTotals(guildId));
    }

    public List<GuildContribution> computeDepositTotals(int guildId) {
        List<GuildContribution> totals = new ArrayList<>();
        try {
            String sql = "SELECT player_uuid, player_name, SUM(amount) AS total_amount "
                    + "FROM guild_contributions "
                    + "WHERE guild_id = ? AND contribution_type = 'DEPOSIT' "
                    + "GROUP BY player_uuid, player_name "
                    + "ORDER BY total_amount DESC";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, guildId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        GuildContribution c = new GuildContribution();
                        c.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
                        c.setPlayerName(rs.getString("player_name"));
                        c.setAmount(rs.getDouble("total_amount"));
                        c.setType(GuildContribution.ContributionType.DEPOSIT);
                        totals.add(c);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Error fetching guild deposit summary: " + e.getMessage());
        }
        return totals;
    }

    public GuildContribution mapRow(ResultSet rs) throws SQLException {
        GuildContribution contribution = new GuildContribution();
        contribution.setId(rs.getInt("id"));
        contribution.setGuildId(rs.getInt("guild_id"));
        contribution.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        contribution.setPlayerName(rs.getString("player_name"));
        contribution.setAmount(rs.getDouble("amount"));
        contribution.setType(GuildContribution.ContributionType.valueOf(rs.getString("contribution_type")));
        contribution.setDescription(rs.getString("description"));
        contribution.setCreatedAt(parseTimestamp(rs, "created_at"));
        return contribution;
    }

    private LocalDateTime parseTimestamp(ResultSet rs, String columnName) throws SQLException {
        String s = rs.getString(columnName);
        if (s != null && !s.isEmpty()) {
            try {
                return LocalDateTime.parse(s, TimeProvider.FULL_FORMATTER);
            } catch (Exception ignore) {
                try {
                    return LocalDateTime.parse(s.replace(" ", "T"));
                } catch (Exception ex) {
                    logger.warning("Failed to parse timestamp: " + s);
                }
            }
        }
        try {
            Timestamp ts = rs.getTimestamp(columnName);
            if (ts != null) {
                return ts.toLocalDateTime();
            }
        } catch (SQLException ignore) {
            // fall through
        }
        return LocalDateTime.now();
    }
}
