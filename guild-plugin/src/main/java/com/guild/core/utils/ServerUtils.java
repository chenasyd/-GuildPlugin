package com.guild.core.utils;

import org.bukkit.Bukkit;

import java.util.Set;

/**
 * 服务器类型检测工具
 */
public class ServerUtils {

    /**
     * 官方兼容目标版本（插件核心与 Folia gworld 共用）。
     *
     * @see MinecraftVersionSupport#OFFICIAL_VERSIONS
     * @deprecated 使用 {@link MinecraftVersionSupport#OFFICIAL_VERSIONS}
     */
    @Deprecated
    public static final Set<String> FOLIA_SUPPORTED_VERSIONS = MinecraftVersionSupport.OFFICIAL_VERSIONS;

    public enum ServerType {
        SPIGOT,
        FOLIA,
        UNKNOWN
    }

    private static ServerType serverType = null;
    private static String minecraftVersion = null;

    /**
     * 检测服务器类型
     */
    public static ServerType getServerType() {
        if (serverType == null) {
            serverType = detectServerType();
        }
        return serverType;
    }

    /**
     * 检测是否为Folia服务器
     */
    public static boolean isFolia() {
        return getServerType() == ServerType.FOLIA;
    }

    /**
     * 检测是否为Spigot服务器
     */
    public static boolean isSpigot() {
        return getServerType() == ServerType.SPIGOT;
    }

    /**
     * 获取服务器版本
     */
    public static String getServerVersion() {
        return Bukkit.getServer().getBukkitVersion();
    }

    /**
     * 解析 Minecraft 版本号（去掉 Bukkit 修订后缀与 Folia build 元数据）。
     * 例：{@code 1.20.4-R0.1-SNAPSHOT} → {@code 1.20.4}；
     * {@code 26.1.2.build.8-R0.1-SNAPSHOT} → {@code 26.1.2}
     */
    public static String getMinecraftVersion() {
        if (minecraftVersion == null) {
            String bukkit = getServerVersion();
            int dash = bukkit.indexOf('-');
            String ver = dash > 0 ? bukkit.substring(0, dash) : bukkit;
            int buildMeta = ver.indexOf(".build.");
            if (buildMeta > 0) {
                ver = ver.substring(0, buildMeta);
            }
            minecraftVersion = ver;
        }
        return minecraftVersion;
    }

    /**
     * 当前 Folia 版本是否可启用 gworld（官方或 best-effort）。
     * 非 Folia 环境无意义，调用方应先判断 {@link #isFolia()}。
     */
    public static boolean isFoliaVersionSupported() {
        return MinecraftVersionSupport.isFoliaGworldCompatible(getMinecraftVersion());
    }

    /** 当前版本是否为官方兼容目标。 */
    public static boolean isOfficialMinecraftVersion() {
        return MinecraftVersionSupport.isOfficialVersion(getMinecraftVersion());
    }

    /** 当前版本是否为 best-effort（如 26.1.4、25.1）。 */
    public static boolean isBestEffortMinecraftVersion() {
        return MinecraftVersionSupport.resolve(getMinecraftVersion())
                == MinecraftVersionSupport.CompatibilityLevel.BEST_EFFORT;
    }

    /**
     * 只读视图（便于日志/诊断）。
     */
    public static Set<String> getFoliaSupportedVersions() {
        return MinecraftVersionSupport.OFFICIAL_VERSIONS;
    }

    /**
     * 检测服务器类型的具体实现
     */
    private static ServerType detectServerType() {
        try {
            // 尝试加载Folia特有的类
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return ServerType.FOLIA;
        } catch (ClassNotFoundException e) {
            // 检查是否为Spigot
            try {
                Class.forName("org.spigotmc.SpigotConfig");
                return ServerType.SPIGOT;
            } catch (ClassNotFoundException e2) {
                return ServerType.UNKNOWN;
            }
        }
    }

    /**
     * 检查是否支持指定的API版本
     */
    public static boolean supportsApiVersion(String requiredVersion) {
        String serverVersion = getServerVersion();
        return compareVersions(serverVersion, requiredVersion) >= 0;
    }

    /**
     * 版本比较工具
     */
    private static int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("-")[0].split("\\.");
        String[] v2Parts = version2.split("-")[0].split("\\.");

        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1Part != v2Part) {
                return Integer.compare(v1Part, v2Part);
            }
        }

        return 0;
    }
}
