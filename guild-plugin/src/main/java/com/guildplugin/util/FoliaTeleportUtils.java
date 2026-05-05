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
	 * 注意：Folia 传送功能已禁用，始终回退到直接传送
	 */
	public static void safeTeleport(JavaPlugin plugin, Player player, Location location) {
		if (plugin == null || player == null || location == null) return;

		// Folia 传送功能已禁用，直接传送
		plugin.getLogger().info("[Teleport-Debug] 传送功能已禁用，直接传送玩家 " + player.getName() + " 到 " + location);
		player.teleport(location);
	}
}
