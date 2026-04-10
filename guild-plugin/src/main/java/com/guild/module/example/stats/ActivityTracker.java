package com.guild.module.example.stats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class ActivityTracker implements Listener {

    private final GuildStatsModule module;
    private final ActivityDataPersistence persistence;
    private BukkitTask task;

    private final Map<UUID, Long> loginTimeMap = new java.util.concurrent.ConcurrentHashMap<>();

    public ActivityTracker(GuildStatsModule module, ActivityDataPersistence persistence) {
        this.module = module;
        this.persistence = persistence;
    }

    public void start() {
        tickCount = 0;

        Bukkit.getPluginManager().registerEvents(this, module.getContext().getPlugin());

        // 加载持久化数据
        persistence.load();

        // 初始化当前在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            loginTimeMap.put(player.getUniqueId(), System.currentTimeMillis());
        }

        // 每60秒执行一次：统计在线时长 + 检测日期变更
        long periodTicks = 60L * 20L; // 1分钟 = 20 ticks, 60分钟 = 1200 ticks
        this.task = Bukkit.getScheduler().runTaskTimer(
            module.getContext().getPlugin(),
            this::tick,
            periodTicks,
            periodTicks
        );

        module.getContext().getLogger().info(
            "[Stats-追踪器] 活动度追踪器已启动 (周期: " + (periodTicks / 20) + "秒)");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        // 停止前保存所有数据
        flushAllOnline();
        persistence.save();

        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);

        loginTimeMap.clear();

        module.getContext().getLogger().info("[Stats-追踪器] 活动度追踪器已停止");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        loginTimeMap.put(uuid, System.currentTimeMillis());

        // 如果是新的一天，先切换日期
        checkAndSwitchDay();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        recordOnlineTime(uuid);
        loginTimeMap.remove(uuid);
    }

    private void tick() {
        // 1. 检查是否需要切换到新的一天
        checkAndSwitchDay();

        // 2. 统计所有在线玩家的时长并保存
        flushAllOnline();

        // 3. 定期保存（每10次tick保存一次文件，避免频繁IO）
        if (shouldSavePeriodically()) {
            persistence.save();
        }
    }

    private int tickCount = 0;
    private static final int SAVE_INTERVAL = 10; // 每10次tick（即10分钟）保存一次

    private boolean shouldSavePeriodically() {
        tickCount++;
        if (tickCount >= SAVE_INTERVAL) {
            tickCount = 0;
            return true;
        }
        return false;
    }

    private void checkAndSwitchDay() {
        if (persistence.isNewDay()) {
            persistence.switchToNewDay();

            module.getContext().getLogger().info(String.format(
                "[Stats-追踪器] 已切换到新日期: %s",
                LocalDate.now()
            ));
        }
    }

    private void flushAllOnline() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Long loginTime = loginTimeMap.get(uuid);
            if (loginTime == null) {
                loginTime = now; // 防御性处理
                loginTimeMap.put(uuid, loginTime);
                continue;
            }

            long onlineMillis = now - loginTime;
            long onlineMinutes = onlineMillis / 60000L;

            if (onlineMinutes > 0) {
                // 记录到持久化层
                persistence.recordOnlineMinutes(uuid, onlineMinutes);

                // 更新登录时间（避免重复计算）
                loginTimeMap.put(uuid, now);
            }
        }
    }

    private void recordOnlineTime(UUID uuid) {
        Long loginTime = loginTimeMap.get(uuid);
        if (loginTime == null) return;

        long onlineMillis = System.currentTimeMillis() - loginTime;
        long minutesToAdd = onlineMillis / 60000L;

        if (minutesToAdd > 0) {
            persistence.recordOnlineMinutes(uuid, minutesToAdd);
        }
    }

    public long getOnlineMinutesToday(UUID uuid) {
        // 先刷新一下当前数据（确保最新）
        if (loginTimeMap.containsKey(uuid)) {
            Long loginTime = loginTimeMap.get(uuid);
            if (loginTime != null) {
                long onlineMillis = System.currentTimeMillis() - loginTime;
                long unrecordedMinutes = onlineMillis / 60000L;
                if (unrecordedMinutes > 0) {
                    persistence.recordOnlineMinutes(uuid, unrecordedMinutes);
                    loginTimeMap.put(uuid, System.currentTimeMillis());
                }
            }
        }

        return persistence.getOnlineMinutesToday(uuid);
    }

    public int getActiveDaysThisWeek(UUID uuid) {
        return persistence.getActiveDaysThisWeek(uuid);
    }

    public boolean isPlayerOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline();
    }

    public long getLastSeen(UUID uuid) {
        if (isPlayerOnline(uuid)) {
            return System.currentTimeMillis();
        }
        Long loginTime = loginTimeMap.get(uuid);
        if (loginTime != null) {
            return loginTime; // 最后在线时间
        }
        return 0;
    }

    /**
     * 强制立即保存（用于模块禁用时）
     */
    public void forceSave() {
        flushAllOnline();
        persistence.save();
    }
}
