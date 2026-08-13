package com.guild.activity;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ScheduledTaskHandle;
import com.guild.models.Guild;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks online minutes and login days for guild members.
 */
public final class ActivityTracker implements Listener {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final GuildPlugin plugin;
    private final ActivityRepository repository;
    private final ActivitySettings settings;
    private final Map<UUID, Long> sessionStartMs = new ConcurrentHashMap<>();
    private ScheduledTaskHandle task;

    public ActivityTracker(GuildPlugin plugin, ActivityRepository repository, ActivitySettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.settings = settings;
    }

    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sessionStartMs.put(player.getUniqueId(), System.currentTimeMillis());
            CompatibleScheduler.runTaskAsync(plugin, () -> onLogin(player));
        }
        long period = settings.getTickIntervalSeconds() * 20L;
        task = CompatibleScheduler.runTaskTimer(plugin, this::tick, period, period);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[Activity] Tracker started (interval=" + settings.getTickIntervalSeconds() + "s)");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
        flushAllOnline();
        sessionStartMs.clear();
        plugin.getLogger().info("[Activity] Tracker stopped");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessionStartMs.put(player.getUniqueId(), System.currentTimeMillis());
        CompatibleScheduler.runTaskAsync(plugin, () -> onLogin(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            recordOnlineDelta(player.getUniqueId(), player.getName(), true);
            sessionStartMs.remove(player.getUniqueId());
        });
    }

    private void tick() {
        CompatibleScheduler.runTaskAsync(plugin, this::flushAllOnline);
    }

    private void flushAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            recordOnlineDelta(player.getUniqueId(), player.getName(), false);
            sessionStartMs.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    private void onLogin(Player player) {
        Guild guild = plugin.getGuildService().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        MemberActivityRecord record = loadOrCreate(guild.getId(), player.getUniqueId(), player.getName());
        normalizeCalendar(record);
        String today = today();
        record.setLastLoginDate(today);
        record.setLastSeen(System.currentTimeMillis());
        record.setPlayerName(player.getName());
        repository.upsert(record);
    }

    /**
     * @param endSession if true, consume remaining session without restarting the clock
     */
    private void recordOnlineDelta(UUID uuid, String name, boolean endSession) {
        Long start = sessionStartMs.get(uuid);
        if (start == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int minutes = (int) Math.max(0, (now - start) / 60_000L);
        if (!endSession) {
            // attribute at least the tick interval when clock skew yields 0
            if (minutes <= 0) {
                minutes = Math.max(1, settings.getTickIntervalSeconds() / 60);
            }
        }
        if (minutes <= 0 && endSession) {
            // still update lastSeen
            minutes = 0;
        }

        Guild guild = plugin.getGuildService().getPlayerGuild(uuid);
        if (guild == null) {
            return;
        }

        MemberActivityRecord record = loadOrCreate(guild.getId(), uuid, name);
        normalizeCalendar(record);
        if (minutes > 0) {
            record.setOnlineMinutesToday(record.getOnlineMinutesToday() + minutes);
            record.setOnlineMinutesTotal(record.getOnlineMinutesTotal() + minutes);
            maybeCountActiveDay(record);
        }
        record.setLastSeen(now);
        record.setPlayerName(name);
        repository.upsert(record);
    }

    private MemberActivityRecord loadOrCreate(int guildId, UUID uuid, String name) {
        return repository.find(guildId, uuid)
                .orElseGet(() -> {
                    MemberActivityRecord r = new MemberActivityRecord(guildId, uuid, name);
                    r.setTodayDate(today());
                    r.setWeekStartDate(weekStart());
                    return r;
                });
    }

    private void normalizeCalendar(MemberActivityRecord record) {
        String today = today();
        String week = weekStart();
        if (record.getWeekStartDate() == null || !week.equals(record.getWeekStartDate())) {
            record.setWeekStartDate(week);
            record.setActiveDaysWeek(0);
            record.setActiveDayDate(null);
        }
        if (record.getTodayDate() == null || !today.equals(record.getTodayDate())) {
            record.setTodayDate(today);
            record.setOnlineMinutesToday(0);
        }
    }

    private void maybeCountActiveDay(MemberActivityRecord record) {
        if (record.getOnlineMinutesToday() < settings.getDailyActiveMinutesThreshold()) {
            return;
        }
        String today = today();
        if (today.equals(record.getActiveDayDate())) {
            return;
        }
        record.setActiveDayDate(today);
        record.setActiveDaysWeek(record.getActiveDaysWeek() + 1);
    }

    static String today() {
        return LocalDate.now().format(DAY);
    }

    static String weekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(DAY);
    }

    /** Package-visible for score service day checks. */
    public static boolean loggedInToday(MemberActivityRecord record) {
        return record != null && today().equals(record.getLastLoginDate());
    }
}
