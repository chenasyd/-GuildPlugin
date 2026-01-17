package com.guild.core.utils;
package com.guild.core.utils;

import com.guild.GuildPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 提供安全的传送封装：避免调用 teleport().join()，并在 Folia 且被禁用时直接跳过传送。
 */
public final class TeleportUtils {
	private TeleportUtils() {}

	/**
	 * 安全传送：在 Folia 且配置禁用时不执行传送并提示玩家/记录日志；
	 * 否则在主线程同步执行 player.teleport(...)，避免使用 teleport().join() 导致 Folia 死锁。
	 */
	public static void safeTeleport(GuildPlugin plugin, Player player, Location target) {
		if (ServerUtils.isFolia() && plugin.isFoliaTeleportDisabled()) {
			plugin.getLogger().warning("已在 Folia 上禁用自动传送，跳过对 " + player.getName() + " 的传送。");
			try {
				player.sendMessage("§c自动传送在 Folia 服务器上被禁用。");
			} catch (Exception ignored) {}
			return;
		}

		Runnable task = () -> {
			try {
				player.teleport(target);
			} catch (Exception ex) {
				plugin.getLogger().severe("传送失败: " + ex.getMessage());
				ex.printStackTrace();
			}
		};

		if (Bukkit.isPrimaryThread()) {
			task.run();
		} else {
			plugin.getServer().getScheduler().runTask(plugin, task);
		}
	}
}

public class TeleportUtils {
    
}
