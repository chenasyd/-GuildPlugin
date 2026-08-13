package com.guild.module.example.quest.tree;

import com.guild.core.database.DatabaseManager;
import com.guild.core.module.ModuleContext;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Guild shared virtual XP pool (quest tree): deposit from quests, withdraw vanilla EXP, upgrade.
 */
public class GuildTreeService {

    public enum WithdrawResult {
        SUCCESS,
        NO_PERMISSION,
        INSUFFICIENT_BALANCE,
        DAILY_CAP,
        INVALID_AMOUNT,
        ERROR
    }

    public enum UpgradeResult {
        SUCCESS,
        NO_PERMISSION,
        GUILD_LEVEL_TOO_LOW,
        INSUFFICIENT_EXP,
        ERROR
    }

    private final ModuleContext context;
    private final GuildTreeRepository repository;
    private final Logger logger;
    private final ConcurrentHashMap<Integer, GuildTreeState> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Object> locks = new ConcurrentHashMap<>();

    public GuildTreeService(ModuleContext context) {
        this.context = context;
        this.logger = context.getLogger();
        DatabaseManager db = context.getPlugin().getServiceContainer().get(DatabaseManager.class);
        this.repository = new GuildTreeRepository(db, logger);
    }

    public GuildTreeState getOrCreate(int guildId) {
        if (guildId <= 0) {
            return new GuildTreeState(0, 1, 0);
        }
        return cache.computeIfAbsent(guildId, id -> {
            return repository.find(id).orElseGet(() -> {
                GuildTreeState created = new GuildTreeState(id, 1, 0);
                repository.insert(created);
                return created;
            });
        });
    }

    public boolean deposit(int guildId, Player player, long amount, String reason) {
        if (guildId <= 0 || amount <= 0) return false;
        synchronized (lockFor(guildId)) {
            GuildTreeState state = getOrCreate(guildId);
            state.addVirtualExp(amount);
            if (!repository.update(state)) {
                repository.insert(state);
            }
            UUID uuid = player != null ? player.getUniqueId() : new UUID(0, 0);
            String name = player != null ? player.getName() : "system";
            repository.insertLedger(guildId, uuid, name, "DEPOSIT", amount, 0,
                state.getTreeLevel(), reason);
            context.logDetail("[GuildTree] Guild #" + guildId + " +" + amount + " virtual EXP (" + reason + ")");
            return true;
        }
    }

    public WithdrawResult withdraw(Player player, int guildId, int vanillaExpRequested) {
        if (player == null || !player.isOnline()) return WithdrawResult.ERROR;
        if (vanillaExpRequested <= 0) return WithdrawResult.INVALID_AMOUNT;

        synchronized (lockFor(guildId)) {
            GuildTreeState state = getOrCreate(guildId);
            int treeLevel = state.getTreeLevel();
            int maxSingle = getMaxSingleWithdraw(treeLevel);
            int dailyCap = getDailyWithdrawCap(treeLevel);
            String dayKey = LocalDate.now().toString();
            int already = repository.getDailyWithdrawn(guildId, player.getUniqueId(), dayKey);
            int remainingDaily = Math.max(0, dailyCap - already);
            if (remainingDaily <= 0) return WithdrawResult.DAILY_CAP;

            int vanilla = Math.min(vanillaExpRequested, Math.min(maxSingle, remainingDaily));
            if (vanilla <= 0) return WithdrawResult.DAILY_CAP;

            double rate = getConversionRate(treeLevel);
            if (rate <= 0) rate = 1.0;

            long cost = (long) Math.ceil(vanilla / rate);
            if (cost <= 0) cost = vanilla;
            if (state.getVirtualExp() < cost) {
                long balance = state.getVirtualExp();
                if (balance <= 0) return WithdrawResult.INSUFFICIENT_BALANCE;
                vanilla = (int) Math.floor(balance * rate);
                if (vanilla <= 0) return WithdrawResult.INSUFFICIENT_BALANCE;
                vanilla = Math.min(vanilla, Math.min(maxSingle, remainingDaily));
                cost = (long) Math.ceil(vanilla / rate);
                if (cost > balance || !state.consumeVirtualExp(cost)) {
                    return WithdrawResult.INSUFFICIENT_BALANCE;
                }
            } else if (!state.consumeVirtualExp(cost)) {
                return WithdrawResult.INSUFFICIENT_BALANCE;
            }

            if (!repository.update(state)) {
                return WithdrawResult.ERROR;
            }

            player.giveExp(vanilla);
            repository.addDailyWithdrawn(guildId, player.getUniqueId(), dayKey, vanilla);
            repository.insertLedger(guildId, player.getUniqueId(), player.getName(),
                "WITHDRAW", cost, vanilla, state.getTreeLevel(), "player_withdraw");
            context.logDetail("[GuildTree] " + player.getName() + " withdrew " + vanilla
                + " EXP from guild #" + guildId + " (cost " + cost + ")");
            return WithdrawResult.SUCCESS;
        }
    }

    public UpgradeResult tryUpgrade(Player player, int guildId) {
        if (player == null) return UpgradeResult.ERROR;
        if (!player.isOp()
            && !player.hasPermission("guild.quest.tree.upgrade")
            && !isGuildOfficerOrLeader(guildId, player.getUniqueId())) {
            return UpgradeResult.NO_PERMISSION;
        }

        synchronized (lockFor(guildId)) {
            GuildTreeState state = getOrCreate(guildId);
            int treeLevel = state.getTreeLevel();
            int guildLevel = 0;
            try {
                var guild = context.getPlugin().getGuildService().getGuildById(guildId);
                if (guild != null) guildLevel = guild.getLevel();
            } catch (Exception e) {
                return UpgradeResult.ERROR;
            }

            // May upgrade when guild level >= current tree level
            if (guildLevel < treeLevel) {
                return UpgradeResult.GUILD_LEVEL_TOO_LOW;
            }

            long cost = getUpgradeCost(treeLevel);
            if (!state.consumeVirtualExp(cost)) {
                return UpgradeResult.INSUFFICIENT_EXP;
            }

            state.setTreeLevel(treeLevel + 1);
            if (!repository.update(state)) {
                return UpgradeResult.ERROR;
            }
            repository.insertLedger(guildId, player.getUniqueId(), player.getName(),
                "UPGRADE", cost, 0, state.getTreeLevel(), "tree_upgrade");
            context.logDetail("[GuildTree] Guild #" + guildId + " tree upgraded to " + state.getTreeLevel()
                + " by " + player.getName());
            return UpgradeResult.SUCCESS;
        }
    }

    public double getConversionRate(int treeLevel) {
        double base = context.getConfig().getDouble("tree.base-rate", 1.0);
        double perLevel = context.getConfig().getDouble("tree.rate-per-level", 0.05);
        return base + Math.max(0, treeLevel - 1) * perLevel;
    }

    public int getDailyWithdrawCap(int treeLevel) {
        int perLevel = context.getConfig().getInt("tree.daily-withdraw-cap-per-level", 1000);
        return Math.max(0, perLevel * Math.max(1, treeLevel));
    }

    public int getMaxSingleWithdraw(int treeLevel) {
        int perLevel = context.getConfig().getInt("tree.max-single-withdraw-per-level", 500);
        return Math.max(0, perLevel * Math.max(1, treeLevel));
    }

    public long getUpgradeCost(int currentTreeLevel) {
        long base = context.getConfig().getLong("tree.upgrade-base-cost", 500L);
        return base * Math.max(1, currentTreeLevel);
    }

    public int getDailyWithdrawn(int guildId, UUID playerUuid) {
        return repository.getDailyWithdrawn(guildId, playerUuid, LocalDate.now().toString());
    }

    public void invalidate(int guildId) {
        cache.remove(guildId);
    }

    private boolean isGuildOfficerOrLeader(int guildId, UUID playerUuid) {
        try {
            var member = context.getPlugin().getGuildService().getGuildMember(playerUuid);
            if (member == null || member.getGuildId() != guildId) return false;
            String role = member.getRole() != null ? member.getRole().name() : "";
            return "LEADER".equalsIgnoreCase(role)
                || "OWNER".equalsIgnoreCase(role)
                || "OFFICER".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role);
        } catch (Exception e) {
            return false;
        }
    }

    private Object lockFor(int guildId) {
        return locks.computeIfAbsent(guildId, id -> new Object());
    }
}
