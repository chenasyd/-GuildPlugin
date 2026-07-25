package com.guild.comm.api;

import org.bukkit.plugin.Plugin;
import java.util.UUID;

/**
 * SDK stub for BungeeClientAPI.
 * External modules compile against this stub.
 * Runtime implementation lives in guild-comm.
 */
public class BungeeClientAPI {

    public static final String CHANNEL_NAME = "guild:main";

    private BungeeClientAPI() {}

    public static void initialize(Plugin plugin) {}
    public static void shutdown() {}
    public static boolean isInitialized() { return false; }

    public static void sendToBungee(String type, String payload) {}
    public static void pushGuildData(String guildDataJson) {}
    public static void requestGuildData(int guildId, String targetServer) {}
    public static void sendCrossChat(int guildId, String playerName, String message) {}
    public static void broadcastEvent(String eventType, String eventDataJson) {}

    public static boolean isBedrockPlayer(UUID uuid) { return false; }
    public static PlayerConnectionInfo getConnectionInfo(UUID uuid) { return null; }
    public static void removeConnectionInfo(UUID uuid) {}

    /**
     * SDK stub for player connection info.
     */
    public static class PlayerConnectionInfo {
        public UUID getUuid() { return null; }
        public String getName() { return null; }
        public String getConnectionType() { return null; }
        public String getPlatform() { return null; }
        public String getInputMode() { return null; }
        public boolean isBedrock() { return false; }
    }
}
