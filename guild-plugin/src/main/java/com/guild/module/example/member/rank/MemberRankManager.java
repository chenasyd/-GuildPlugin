package com.guild.module.example.member.rank;

import com.guild.sdk.economy.CurrencyManager;
import com.guild.GuildPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 成员贡献排名管理器
 * <p>
 * 存储结构：guildId -> List&lt;MemberRank&gt;
 * 使用 ConcurrentHashMap 保证线程安全，货币数据存储在数据库中。
 */
public class MemberRankManager {

    /** 工会ID -> 该工会的成员排名列表 */
    private final Map<Integer, List<MemberRank>> ranks = new ConcurrentHashMap<>();
    private final CurrencyManager currencyManager;
    private final Logger logger;

    public MemberRankManager(GuildPlugin plugin) {
        this.currencyManager = plugin.getServiceContainer().get(CurrencyManager.class);
        this.logger = plugin.getLogger();
    }

    // ==================== CRUD ====================

    /**
     * 获取工会的成员排名（按A币降序排列）
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
     * 获取指定成员的排名数据
     */
    public MemberRank getMemberRank(int guildId, UUID playerUuid) {
        List<MemberRank> list = ranks.get(guildId);
        if (list == null) return null;
        for (MemberRank r : list) {
            if (r.getPlayerUuid().equals(playerUuid)) {
                // 从数据库更新A币余额
                double balance = currencyManager.getBalance(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
                r.setACoin((long) balance);
                return r;
            }
        }
        return null;
    }

    /**
     * 确保成员在排名列表中存在（若不存在则创建）
     */
    public MemberRank getOrCreate(int guildId, UUID playerUuid, String playerName) {
        List<MemberRank> list = ranks.computeIfAbsent(guildId, k -> new CopyOnWriteArrayList<>());
        for (MemberRank r : list) {
            if (r.getPlayerUuid().equals(playerUuid)) {
                // 更新名称（玩家可能改名）
                r.setPlayerName(playerName);
                // 从数据库更新A币余额
                double balance = currencyManager.getBalance(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
                r.setACoin((long) balance);
                return r;
            }
        }
        // 从数据库获取A币余额
        double balance = currencyManager.getBalance(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
        MemberRank rank = new MemberRank(playerUuid, playerName, guildId, (long) balance);
        list.add(rank);
        return rank;
    }

    /**
     * 增加成员A币
     *
     * @return 更新后的 MemberRank，若工会不存在返回 null
     */
    public MemberRank addACoin(int guildId, UUID playerUuid, String playerName, long amount) {
        // 使用货币管理器增加A币
        currencyManager.deposit(guildId, playerUuid, playerName, CurrencyManager.CurrencyType.A_COIN, amount);
        
        // 更新内存中的数据
        MemberRank rank = getOrCreate(guildId, playerUuid, playerName);
        if (rank == null) return null;
        double balance = currencyManager.getBalance(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
        rank.setACoin((long) balance);
        return rank;
    }

    /**
     * 减少成员A币（不低于0）
     *
     * @return 更新后的 MemberRank，若工会不存在返回 null
     */
    public MemberRank reduceACoin(int guildId, UUID playerUuid, String playerName, long amount) {
        // 使用货币管理器减少A币
        boolean success = currencyManager.withdraw(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN, amount);
        if (!success) return null;
        
        // 更新内存中的数据
        MemberRank rank = getOrCreate(guildId, playerUuid, playerName);
        if (rank == null) return null;
        double balance = currencyManager.getBalance(guildId, playerUuid, CurrencyManager.CurrencyType.A_COIN);
        rank.setACoin((long) balance);
        rank.touchActive();
        return rank;
    }

    /**
     * 记录成员活跃
     */
    public void touchActive(int guildId, UUID playerUuid, String playerName) {
        MemberRank rank = getOrCreate(guildId, playerUuid, playerName);
        if (rank != null) {
            rank.touchActive();
        }
    }

    /**
     * 移除某工会中某成员的排名数据（成员离开时调用）
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
     * 清除某工会的所有排名数据（工会删除时调用）
     */
    public void clearByGuild(int guildId) {
        ranks.remove(guildId);
    }

    /** 清除所有数据 */
    public void clearAll() {
        ranks.clear();
    }

    /**
     * 从内存加载所有排名数据
     */
    public void loadAll() {
        // 数据从数据库加载，此处仅初始化内存结构
        logger.info("Member rank manager initialized");
    }

    /**
     * 保存所有排名数据
     */
    public void saveAll() {
        // 数据自动保存到数据库，此处无需操作
        logger.info("Member rank data saved to database");
    }
}
