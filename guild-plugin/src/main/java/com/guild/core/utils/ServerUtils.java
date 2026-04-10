package com.guild.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * 服务器类型检测工具
 */
public class ServerUtils {
    
    public enum ServerType {
        SPIGOT,
        FOLIA,
        UNKNOWN
    }
    
    private static ServerType serverType = null;
    
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
