package com.guild.module.example.member.rank;

import com.guild.sdk.economy.CurrencyManager;
import com.guild.GuildPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Member contribution ranking manager.
 * <p>
 * Storage structure: guildId -> List&lt;MemberRank&gt;
 * Uses ConcurrentHashMap for thread safety; currency data is stored in the database.
 */
public class MemberRankManager {

    /** Guild ID -> member ranking list for that guild */
    private final Map<Integer, List<MemberRank>> ranks = new ConcurrentHashMap<>();
    private final CurrencyManager currencyManager;
    private final Logger logger;

    public MemberRankManager(GuildPlugin plugin) {
        this.currencyManager = plugin.getServiceContainer().get(CurrencyManager.class);
        this.logger = plugin.getLogger();
    }

    // ==================== CRUD ====================

    /**
     * Get member ranking for a guild (sorted by A-Coins descending)
     */
    public List<MemberRank> getRanks(int guildId) {
        List<MemberRank> list = ranks.get(guildId);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<MemberRank> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> Long.compare(b.getACoin(), a.getACoin()));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Get ranking data for a specific member
     */
    public MemberRank getMemberRank(int guildId, UUID playerUuid) {
        List<MemberRank> list = ranks.get(guildId);
        if (list == null) return null;
        for (MemberRank r : list) {
            if (r.getPlayerUuid().equals(playerUuid)) {
                Double cached = currencyManager.getCachedBalance(
                        guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
                if (cached != null) {
                    r.setACoin(cached.longValue());
                } else {
                    // Prefetch off-thread; keep current in-memory value for GUI responsiveness.
                    currencyManager.getBalanceAsync(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN)
                            .thenAccept(bal -> r.setACoin(bal.longValue()));
                }
                return r;
            }
        }
        return null;
    }

    /**
     * Ensure a member exists in the ranking list (create if absent)
     */
    public MemberRank getOrCreate(int guildId, UUID playerUuid, String playerName) {
        List<MemberRank> list = ranks.computeIfAbsent(guildId, k -> new CopyOnWriteArrayList<>());
        for (MemberRank r : list) {
            if (r.getPlayerUuid().equals(playerUuid)) {
                r.setPlayerName(playerName);
                Double cached = currencyManager.getCachedBalance(
                        guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
                if (cached != null) {
                    r.setACoin(cached.longValue());
                }
                return r;
            }
        }
        Double cached = currencyManager.getCachedBalance(
                guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
        long bal = cached != null ? cached.longValue() : 0L;
        MemberRank rank = new MemberRank(playerUuid, playerName, guildId, bal);
        list.add(rank);
        if (cached == null) {
            currencyManager.getBalanceAsync(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN)
                    .thenAccept(v -> rank.setACoin(v.longValue()));
        }
        return rank;
    }

    /**
     * Add A-Coins for a member (async JDBC). Updates in-memory rank when done.
     */
    public CompletableFuture<MemberRank> addACoinAsync(int guildId, UUID playerUuid, String playerName, long amount) {
        return currencyManager.depositAsync(guildId, playerUuid, playerName, CurrencyManager.CurrencyType.A_COIN, amount)
                .thenApply(ok -> {
                    MemberRank rank = getOrCreateCached(guildId, playerUuid, playerName);
                    if (rank == null) {
                        return null;
                    }
                    Double bal = currencyManager.getCachedBalance(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
                    if (bal != null) {
                        rank.setACoin(bal.longValue());
                    } else {
                        rank.setACoin(rank.getACoin() + amount);
                    }
                    return rank;
                });
    }

    /**
     * Add A-Coins (sync). Prefer {@link #addACoinAsync} off the main thread.
     */
    public MemberRank addACoin(int guildId, UUID playerUuid, String playerName, long amount) {
        currencyManager.deposit(guildId, playerUuid, playerName, CurrencyManager.CurrencyType.A_COIN, amount);
        MemberRank rank = getOrCreate(guildId, playerUuid, playerName);
        if (rank == null) return null;
        Double bal = currencyManager.getCachedBalance(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
        if (bal != null) {
            rank.setACoin(bal.longValue());
        }
        return rank;
    }

    /**
     * Subtract A-Coins from a member (floor at 0)
     *
     * @return Updated MemberRank, or null if the guild doesn't exist / insufficient balance
     */
    public MemberRank reduceACoin(int guildId, UUID playerUuid, String playerName, long amount) {
        boolean success = currencyManager.withdraw(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN, amount);
        if (!success) return null;

        MemberRank rank = getOrCreate(guildId, playerUuid, playerName);
        if (rank == null) return null;
        Double bal = currencyManager.getCachedBalance(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
        if (bal != null) {
            rank.setACoin(bal.longValue());
        }
        rank.touchActive();
        return rank;
    }

    /**
     * In-memory only create/lookup — does not hit JDBC.
     */
    private MemberRank getOrCreateCached(int guildId, UUID playerUuid, String playerName) {
        List<MemberRank> list = ranks.computeIfAbsent(guildId, k -> new CopyOnWriteArrayList<>());
        for (MemberRank r : list) {
            if (r.getPlayerUuid().equals(playerUuid)) {
                r.setPlayerName(playerName);
                return r;
            }
        }
        MemberRank rank = new MemberRank(playerUuid, playerName, guildId, 0L);
        list.add(rank);
        return rank;
    }

    /**
     * Record member activity
     */
    public void touchActive(int guildId, UUID playerUuid, String playerName) {
        MemberRank rank = getOrCreate(guildId, playerUuid, playerName);
        if (rank != null) {
            rank.touchActive();
        }
    }

    /**
     * Remove a member's ranking data (called when member leaves guild)
     */
    public void removeMember(int guildId, UUID playerUuid) {
        List<MemberRank> list = ranks.get(guildId);
        if (list == null) return;
        list.removeIf(r -> r.getPlayerUuid().equals(playerUuid));
        if (list.isEmpty()) {
            ranks.remove(guildId);
        }
    }

    /**
     * Clear all ranking data for a guild (called when guild is deleted)
     */
    public void clearByGuild(int guildId) {
        ranks.remove(guildId);
    }

    /** Clear all data */
    public void clearAll() {
        ranks.clear();
    }

    /**
     * Load all ranking data into memory
     */
    public void loadAll() {
        // Data is loaded from the database; here we only init the in-memory structure
        logger.info("Member rank manager initialized");
    }

    /**
     * Save all ranking data
     */
    public void saveAll() {
        // Data is automatically persisted to the database; no-op here
        logger.info("Member rank data saved to database");
    }
}
