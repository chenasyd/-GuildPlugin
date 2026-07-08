package com.guild.core.geyser;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Geyser API 静态工具类 —— 基于反射检测基岩版玩家，零编译依赖。
 * <p>
 * 运行时自动检测 Geyser-Spigot 是否存在：
 * <ul>
 *   <li>Geyser 在线 → 通过反射调用 {@code GeyserApi.api().connectionByUuid(uuid)}</li>
 *   <li>Geyser 不在 → 静默降级，所有玩家视为 Java 版</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>{@code
 *   GeyserAPI.initialize(plugin.getLogger());
 *   ...
 *   if (GeyserAPI.isBedrockPlayer(player)) { ... }
 *   ...
 *   GeyserAPI.shutdown();
 * }</pre>
 */
public final class GeyserAPI {

    private static boolean available = false;
    private static Logger logger;
    private static final Map<UUID, Boolean> bedrockCache = new ConcurrentHashMap<>();

    // 反射句柄（缓存，避免重复查找）
    private static Object geyserApiInstance;
    private static Method connectionByUuidMethod;

    private GeyserAPI() {}

    /**
     * 初始化 — 通过反射检测 Geyser 是否在线。
     */
    public static void initialize(Logger pluginLogger) {
        logger = pluginLogger;
        try {
            // 反射调用: org.geysermc.geyser.api.GeyserApi.api()
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Method apiMethod = geyserApiClass.getMethod("api");
            geyserApiInstance = apiMethod.invoke(null);

            if (geyserApiInstance != null) {
                // 缓存 connectionByUuid 方法句柄
                Class<?> apiBaseClass = Class.forName("org.geysermc.api.GeyserApiBase");
                connectionByUuidMethod = apiBaseClass.getMethod("connectionByUuid", UUID.class);
                available = true;
                logger.info("[GeyserAPI] Geyser detected via reflection, Bedrock support enabled.");
            } else {
                logger.info("[GeyserAPI] GeyserApi.api() returned null — Geyser not loaded.");
            }
        } catch (ClassNotFoundException e) {
            available = false;
            logger.info("[GeyserAPI] Geyser classes not found, Bedrock support disabled.");
        } catch (Exception e) {
            available = false;
            logger.info("[GeyserAPI] Geyser not available (" + e.getClass().getSimpleName() + "), Bedrock support disabled.");
        }
    }

    /**
     * 关闭 — 清空缓存与反射句柄。
     */
    public static void shutdown() {
        bedrockCache.clear();
        geyserApiInstance = null;
        connectionByUuidMethod = null;
        available = false;
    }

    /**
     * Geyser API 是否可用。
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * 判断玩家是否为基岩版玩家（缓存结果）。
     */
    public static boolean isBedrockPlayer(Player player) {
        return isBedrockPlayer(player.getUniqueId());
    }

    /**
     * 判断 UUID 对应玩家是否为基岩版玩家（缓存结果）。
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        if (!available || connectionByUuidMethod == null) return false;

        return bedrockCache.computeIfAbsent(uuid, id -> {
            try {
                // 反射调用: GeyserApi.api().connectionByUuid(uuid)
                Object conn = connectionByUuidMethod.invoke(geyserApiInstance, id);
                return conn != null;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * 玩家退出时清理缓存。
     */
    public static void onPlayerQuit(UUID uuid) {
        bedrockCache.remove(uuid);
    }
}
