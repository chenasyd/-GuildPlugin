package com.guild.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * Narzędzie do wykrywania typu serwera
 */
public class ServerUtils {

    public enum ServerType {
        SPIGOT,
        FOLIA,
        UNKNOWN
    }

    private static ServerType serverType = null;

    /**
     * Wykryj typ serwera
     */
    public static ServerType getServerType() {
        if (serverType == null) {
            serverType = detectServerType();
        }
        return serverType;
    }

    /**
     * Wykryj, czy jest to serwer Folia
     */
    public static boolean isFolia() {
        return getServerType() == ServerType.FOLIA;
    }

    /**
     * Wykryj, czy jest to serwer Spigot
     */
    public static boolean isSpigot() {
        return getServerType() == ServerType.SPIGOT;
    }

    /**
     * Pobierz wersję serwera
     */
    public static String getServerVersion() {
        return Bukkit.getServer().getBukkitVersion();
    }

    /**
     * Konkretna implementacja wykrywania typu serwera
     */
    private static ServerType detectServerType() {
        try {
            // Spróbuj załadować klasy specyficzne dla Folia
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return ServerType.FOLIA;
        } catch (ClassNotFoundException e) {
            // Sprawdź, czy to Spigot
            try {
                Class.forName("org.spigotmc.SpigotConfig");
                return ServerType.SPIGOT;
            } catch (ClassNotFoundException e2) {
                return ServerType.UNKNOWN;
            }
        }
    }

    /**
     * Sprawdź, czy obsługuje określoną wersję API
     */
    public static boolean supportsApiVersion(String requiredVersion) {
        String serverVersion = getServerVersion();
        return compareVersions(serverVersion, requiredVersion) >= 0;
    }

    /**
     * Narzędzie do porównywania wersji
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
