package com.guildplugin.util;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ServerUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Folia 兼容传送工具。
 *
 * <p>Folia 下跨区域/跨世界必须使用 {@code teleportAsync}；
 * 同步 {@code teleport} 仅允许在玩家当前区域线程调用，否则会报错或静默失败。
 */
public final class FoliaTeleportUtils {

    private FoliaTeleportUtils() {
    }

    /**
     * 在 Spigot / Paper / Folia 下安全传送玩家。
     *
     * @return 传送是否成功（Folia 为 teleportAsync 结果；Spigot 为同步结果）
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> safeTeleport(JavaPlugin plugin, Player player, Location location) {
        if (plugin == null || player == null || location == null || location.getWorld() == null) {
            return CompletableFuture.completedFuture(false);
        }
        if (!player.isOnline()) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            Method teleportAsync = Player.class.getMethod("teleportAsync", Location.class);
            Object result = teleportAsync.invoke(player, location);
            if (result instanceof CompletableFuture<?> future) {
                return ((CompletableFuture<Boolean>) future).exceptionally(ex -> false);
            }
        } catch (NoSuchMethodException ignored) {
            // Spigot：无 teleportAsync
        } catch (Exception e) {
            // 反射失败时走下方回退
        }

        // Spigot / 回退：必须在玩家实体线程执行同步 teleport
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompatibleScheduler.runTask(plugin, player, () -> {
            try {
                future.complete(player.teleport(location));
            } catch (Throwable t) {
                future.complete(false);
            }
        });
        return future;
    }

    /**
     * 是否应优先使用异步传送（Paper/Folia）。
     */
    public static boolean prefersAsyncTeleport() {
        try {
            Player.class.getMethod("teleportAsync", Location.class);
            return true;
        } catch (NoSuchMethodException e) {
            return ServerUtils.isFolia();
        }
    }
}
