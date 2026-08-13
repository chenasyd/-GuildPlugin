package com.guild.listeners;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ScheduledTaskHandle;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight guild-home protection: non-members cannot break/place within home radius.
 * Config: {@code guild.home-protect.enabled} / {@code guild.home-protect.radius}
 * <p>
 * Home locations are refreshed on a timer (not on every block event) to avoid
 * {@code getAllGuilds()} sync JDBC on the main thread.
 */
public final class GuildHomeProtectListener implements Listener {

    private static final class HomePoint {
        final int guildId;
        final String world;
        final double x;
        final double z;

        HomePoint(int guildId, String world, double x, double z) {
            this.guildId = guildId;
            this.world = world;
            this.x = x;
            this.z = z;
        }
    }

    private final GuildPlugin plugin;
    private final List<HomePoint> homes = new CopyOnWriteArrayList<>();
    private ScheduledTaskHandle refreshTask;

    public GuildHomeProtectListener(GuildPlugin plugin) {
        this.plugin = plugin;
        startRefreshTask();
    }

    private void startRefreshTask() {
        CompatibleScheduler.runTaskAsync(plugin, this::refreshHomes);
        refreshTask = CompatibleScheduler.runTaskTimer(plugin,
                () -> CompatibleScheduler.runTaskAsync(plugin, this::refreshHomes),
                20L * 30L,
                20L * 30L);
    }

    /** Optional: call after /guild sethome so protection updates sooner than the timer. */
    public void refreshHomesAsync() {
        CompatibleScheduler.runTaskAsync(plugin, this::refreshHomes);
    }

    private void refreshHomes() {
        try {
            List<Guild> guilds = plugin.getGuildService().getAllGuildsAsync().join();
            List<HomePoint> next = new ArrayList<>();
            if (guilds != null) {
                for (Guild guild : guilds) {
                    if (guild.getHomeWorld() == null || guild.getHomeWorld().isEmpty()) {
                        continue;
                    }
                    next.add(new HomePoint(guild.getId(), guild.getHomeWorld(),
                            guild.getHomeX(), guild.getHomeZ()));
                }
            }
            homes.clear();
            homes.addAll(next);
        } catch (Exception ignored) {
            // keep previous snapshot
        }
    }

    private boolean enabled() {
        return plugin.getConfigManager().getMainConfig()
                .getBoolean("guild.home-protect.enabled", false);
    }

    private double radiusSq() {
        double r = plugin.getConfigManager().getMainConfig()
                .getDouble("guild.home-protect.radius", 16);
        return r * r;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (shouldCancel(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancel(Player player, Location loc) {
        if (!enabled() || player == null || loc == null || loc.getWorld() == null) {
            return false;
        }
        if (player.hasPermission("guild.admin")) {
            return false;
        }
        if (homes.isEmpty()) {
            return false;
        }
        String world = loc.getWorld().getName();
        double x = loc.getX();
        double z = loc.getZ();
        double r2 = radiusSq();

        GuildMember member = null;
        boolean memberLoaded = false;

        for (HomePoint home : homes) {
            if (!world.equals(home.world)) {
                continue;
            }
            double dx = x - home.x;
            double dz = z - home.z;
            if (dx * dx + dz * dz > r2) {
                continue;
            }
            if (!memberLoaded) {
                member = plugin.getGuildService().getGuildMember(player.getUniqueId());
                memberLoaded = true;
            }
            if (member == null || member.getGuildId() != home.guildId) {
                return true;
            }
        }
        return false;
    }
}
