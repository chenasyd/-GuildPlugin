package com.guild.listeners;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * 轻量公会家保护：家点水平半径内禁止非成员破坏/放置。
 * 配置：{@code guild.home-protect.enabled} / {@code guild.home-protect.radius}
 */
public final class GuildHomeProtectListener implements Listener {

    private final GuildPlugin plugin;

    public GuildHomeProtectListener(GuildPlugin plugin) {
        this.plugin = plugin;
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
        try {
            for (Guild guild : plugin.getGuildService().getAllGuilds()) {
                if (guild.getHomeWorld() == null) {
                    continue;
                }
                if (!guild.getHomeWorld().equals(loc.getWorld().getName())) {
                    continue;
                }
                double dx = loc.getX() - guild.getHomeX();
                double dz = loc.getZ() - guild.getHomeZ();
                if (dx * dx + dz * dz > radiusSq()) {
                    continue;
                }
                GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
                if (member == null || member.getGuildId() != guild.getId()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }
}
