package com.guild.module.example.member.rank;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ScheduledTaskHandle;
import com.guild.models.GuildMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Online activity value tracker.
 * <p>
 * Rules:
 * - Scan online members every minute
 * - A player is considered active only if they recently acted (move/interact/command)
 * - Award contribution points every N cumulative active minutes
 * - Daily cap on awarded contribution to prevent AFK farming
 */
public class OnlineActivityTracker implements Listener {

    private final MemberRankModule module;

    private ScheduledTaskHandle task;

    private final Map<UUID, Long> lastActionMillis = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> onlineActiveMinutes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> dailyAwarded = new ConcurrentHashMap<>();
    private LocalDate dailyBucket = LocalDate.now();

    // Default parameters (overridable via module config)
    private final int checkIntervalMinutes;
    private final int awardEveryMinutes;
    private final long activeWindowSeconds;
    private final int awardPoints;
    private final int dailyCap;

    public OnlineActivityTracker(MemberRankModule module) {
        this.module = module;
        var config = module.getContext().getConfig();
        this.checkIntervalMinutes = Math.max(1, config.getInt("activity.online.check-interval-minutes", 1));
        this.awardEveryMinutes = Math.max(1, config.getInt("activity.online.award-every-minutes", 5));
        this.activeWindowSeconds = Math.max(30L, config.getLong("activity.online.active-window-seconds", 120L));
        this.awardPoints = Math.max(1, config.getInt("activity.online.award-points", 2));
        this.dailyCap = Math.max(1, config.getInt("activity.online.daily-cap", 60));
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, module.getContext().getPlugin());
        long period = checkIntervalMinutes * 60L * 20L;
        this.task = CompatibleScheduler.runTaskTimer(
                module.getContext().getPlugin(),
                this::tick,
                period,
                period
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            markActive(player.getUniqueId());
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        PlayerMoveEvent.getHandlerList().unregister(this);
        PlayerInteractEvent.getHandlerList().unregister(this);
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this);

        lastActionMillis.clear();
        onlineActiveMinutes.clear();
        dailyAwarded.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        markActive(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastActionMillis.remove(uuid);
        onlineActiveMinutes.remove(uuid);
        dailyAwarded.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        markActive(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL) {
            return;
        }
        markActive(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        markActive(event.getPlayer().getUniqueId());
    }

    private void tick() {
        rolloverDailyIfNeeded();
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            long last = lastActionMillis.getOrDefault(uuid, 0L);
            boolean recentlyActive = now - last <= activeWindowSeconds * 1000L;
            if (!recentlyActive) {
                continue;
            }

            module.getContext().getPlugin().getGuildService().getGuildMemberAsync(uuid).thenAccept(member -> {
                if (member == null) {
                    return;
                }
                awardIfReached(member, player.getName(), uuid);
            });
        }
    }

    private void awardIfReached(GuildMember member, String playerName, UUID uuid) {
        if (module.getState() != com.guild.core.module.ModuleState.ACTIVE) {
            return;
        }
        int guildId = member.getGuildId();
        module.getRankManager().touchActive(guildId, uuid, playerName);

        int newMinutes = onlineActiveMinutes.merge(uuid, checkIntervalMinutes, Integer::sum);
        if (newMinutes < awardEveryMinutes) {
            return;
        }

        int daily = dailyAwarded.getOrDefault(uuid, 0);
        if (daily >= dailyCap) {
            return;
        }

        // Adjust reward based on player level and activity
        int baseAward = awardPoints;
        int adjustedAward = calculateAdjustedAward(baseAward, member);
        int award = Math.min(adjustedAward, dailyCap - daily);
        if (award <= 0) {
            return;
        }

        module.getRankManager().addACoin(guildId, uuid, playerName, award);
        dailyAwarded.put(uuid, daily + award);

        // Reset per-window to avoid drift from tick rate variance
        onlineActiveMinutes.put(uuid, Math.max(0, newMinutes - awardEveryMinutes));
    }

    private int calculateAdjustedAward(int baseAward, GuildMember member) {
        // Adjust bonus based on player role
        double roleMultiplier = 1.0;
        switch (member.getRole()) {
            case LEADER:
                roleMultiplier = 2.0;
                break;
            case OFFICER:
                roleMultiplier = 1.5;
                break;
            case MEMBER:
            default:
                roleMultiplier = 1.0;
                break;
        }
        
        // Can also factor in other criteria: guild level, join time, etc.
        return (int) Math.round(baseAward * roleMultiplier);
    }

    private void markActive(UUID uuid) {
        lastActionMillis.put(uuid, System.currentTimeMillis());
    }

    private void rolloverDailyIfNeeded() {
        LocalDate nowDate = LocalDate.now();
        if (!nowDate.equals(dailyBucket)) {
            dailyBucket = nowDate;
            dailyAwarded.clear();
            onlineActiveMinutes.clear();
        }
    }
}
