package com.guildplugin.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoliaTeleportUtils {
	private FoliaTeleportUtils() {}

	/**
	 * 在 Folia（Paper 线程化区域）上安全传送玩家：优先通过 RegionScheduler 调度，
	 * 若找不到或调用失败则回退到直接 teleport。
	 */
	public static void safeTeleport(JavaPlugin plugin, Player player, Location location) {
		if (plugin == null || player == null || location == null) return;

		if (isFoliaEnvironment()) {
			try {
				Object server = plugin.getServer();
				Method getRegionScheduler = server.getClass().getMethod("getRegionScheduler");
				Object regionScheduler = getRegionScheduler.invoke(server);

				if (regionScheduler == null) {
					plugin.getLogger().warning("RegionScheduler 为 null，回退到直接传送");
					player.teleport(location);
					return;
				}

				// 尝试多种常见签名： (Plugin, Location, Runnable), (Plugin, Runnable, Location), (Plugin, Runnable)
				try {
					Method run = regionScheduler.getClass().getMethod("run", Plugin.class, Location.class, Runnable.class);
					run.invoke(regionScheduler, plugin, location, (Runnable) () -> player.teleport(location));
					return;
				} catch (NoSuchMethodException ignored) {}

				try {
					Method run = regionScheduler.getClass().getMethod("run", Plugin.class, Runnable.class, Location.class);
					run.invoke(regionScheduler, plugin, (Runnable) () -> player.teleport(location), location);
					return;
				} catch (NoSuchMethodException ignored) {}

				try {
					Method run = regionScheduler.getClass().getMethod("run", Plugin.class, Runnable.class);
					run.invoke(regionScheduler, plugin, (Runnable) () -> player.teleport(location));
					return;
				} catch (NoSuchMethodException ignored) {}

				plugin.getLogger().warning("找不到兼容的 RegionScheduler.run 签名，回退到直接传送");
				player.teleport(location);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				plugin.getLogger().log(Level.WARNING, "Folia 调度失败，回退到直接传送", e);
				player.teleport(location);
			}
		} else {
			player.teleport(location);
		}
	}

	private static boolean isFoliaEnvironment() {
		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
