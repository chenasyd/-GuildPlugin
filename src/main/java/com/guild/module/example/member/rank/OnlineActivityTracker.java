package com.guild.module.example.member.rank;

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
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线活跃值追踪器
 * <p>
 * 规则：
 * - 每分钟扫描一次在线成员
 * - 玩家最近有行为（移动/交互/命令）才记为活跃
 * - 每累计 N 分钟发放一次贡献值
 * - 每日最多发放固定贡献，防止挂机刷分
 */
public class OnlineActivityTracker implements Listener {

    private final MemberRankModule module;

    private BukkitTask task;

    private final Map<UUID, Long> lastActionMillis = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> onlineActiveMinutes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> dailyAwarded = new ConcurrentHashMap<>();
    private LocalDate dailyBucket = LocalDate.now();

    // 默认参数（可通过 module config 覆盖）
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
        this.task = Bukkit.getScheduler().runTaskTimer(
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

        int award = Math.min(awardPoints, dailyCap - daily);
        if (award <= 0) {
            return;
        }

        module.getRankManager().addContribution(guildId, uuid, playerName, award);
        dailyAwarded.put(uuid, daily + award);

        // 按窗口累计，避免因为刷新频率漂移导致结算抖动
        onlineActiveMinutes.put(uuid, Math.max(0, newMinutes - awardEveryMinutes));
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
