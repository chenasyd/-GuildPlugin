package com.guild.world.bridge;

import com.guild.GuildPlugin;
import com.guild.core.utils.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.logging.Logger;

/**
 * Folia/Paper 版本探测与 NMS ClassLoader 解析。
 */
final class FoliaPlatformProbe {

    private static volatile ClassLoader nmsClassLoader;

    private FoliaPlatformProbe() {
    }

    static Logger logger() {
        GuildPlugin plugin = GuildPlugin.getInstance();
        return plugin != null ? plugin.getLogger() : Bukkit.getLogger();
    }

    static boolean isPaper26Family(String mc) {
        return mc != null && (mc.equals("26") || mc.startsWith("26."));
    }

    static String minecraftVersion() {
        return ServerUtils.getMinecraftVersion();
    }

    /**
     * 从主世界 ServerLevel 取 NMS ClassLoader（CraftServer 的 URLClassLoader 看不到 net.minecraft）。
     */
    static void ensureNmsClassLoader() throws Exception {
        if (nmsClassLoader != null) {
            return;
        }
        if (Bukkit.getWorlds().isEmpty()) {
            throw new IllegalStateException("No worlds loaded — cannot resolve NMS ClassLoader");
        }
        World world = Bukkit.getWorlds().get(0);
        Object handle = world.getClass().getMethod("getHandle").invoke(world);
        nmsClassLoader = handle.getClass().getClassLoader();
        logger().info("[World] [Folia] NMS ClassLoader=" + nmsClassLoader.getClass().getName());
    }

    static ClassLoader nmsClassLoader() {
        return nmsClassLoader;
    }
}
