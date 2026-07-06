package com.guild.comm.api;

/**
 * SDK stub for {@link BungeeClientAPI}.
 * External modules compile against this stub.
 */
public class BungeeClientAPI {

    public static final String CHANNEL_NAME = "guild:main";

    private BungeeClientAPI() {}

    public static void initialize(java.util.logging.Logger logger) {}
    public static void shutdown() {}
    public static boolean isInitialized() { return false; }

    public static void sendToBungee(String type, String payload) {}
    public static void pushGuildData(String guildDataJson) {}
    public static void requestGuildData(int guildId) {}
    public static void sendCrossChat(int guildId, String playerName, String message) {}
    public static void broadcastEvent(String eventType, String eventDataJson) {}
}
