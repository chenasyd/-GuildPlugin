package com.guild.utils;

import com.guild.core.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public final class TeleportManager {
	private TeleportManager() {}

	/**
	 * 尝试传送玩家，如果检测到 Folia 则阻止并通知玩家/记录日志。
	 * 返回 true 表示进行了传送（或已调度到主线程），false 表示被阻止或失败。
	 */
	public static boolean teleport(Plugin plugin, Player player, Location target) {
		if (FoliaUtils.isFolia()) {
			try {
				player.sendMessage(ColorUtils.colorize("&c检测到 Folia 服务端，已禁用传送以避免触发看门狗。"));
			} catch (Throwable ignored) {}
			plugin.getLogger().warning("Blocked teleport for player " + player.getName() + " because server is Folia.");
			return false;
		}

		try {
			if (Bukkit.isPrimaryThread()) {
				player.teleport(target);
			} else {
				Bukkit.getScheduler().runTask(plugin, () -> player.teleport(target));
			}
			return true;
		} catch (Throwable t) {
			plugin.getLogger().log(Level.SEVERE, "Teleport failed", t);
			try {
				player.sendMessage(ColorUtils.colorize("&c传送失败: " + t.getMessage()));
			} catch (Throwable ignored) {}
			return false;
		}
	}
}
