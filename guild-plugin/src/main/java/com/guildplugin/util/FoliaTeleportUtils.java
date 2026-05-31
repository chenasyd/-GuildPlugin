package com.guildplugin.util;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Folia 兼容传送工具。
 * <p>
 * Folia 环境下必须通过 teleportAsync 传送，
 * 因为同步 teleport 只能在玩家所在区域线程调用。
 * </p>
 */
public final class FoliaTeleportUtils {
    private FoliaTeleportUtils() {}

    /**
     * 在 Spigot / Folia 环境下安全传送玩家。
     * Paper/Folia 使用 teleportAsync（反射），Spigot 使用同步 teleport 回退。
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> safeTeleport(JavaPlugin plugin, Player player, Location location) {
        if (plugin == null || player == null || location == null) {
            return CompletableFuture.completedFuture(false);
        }

        // 尝试通过反射调用 Paper/Folia 的 teleportAsync
        try {
            Method teleportAsync = Player.class.getMethod("teleportAsync", Location.class);
            return (CompletableFuture<Boolean>) teleportAsync.invoke(player, location);
        } catch (Exception e) {
            // Spigot 无 teleportAsync，回退到同步传送
            player.teleport(location);
            return CompletableFuture.completedFuture(true);
        }
    }
}
