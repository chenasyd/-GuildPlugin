package com.guild.comm.api;

import com.guild.comm.bridge.ExtensionBridge;
import com.guild.comm.bridge.MessagePacket;

import java.util.logging.Logger;

/**
 * Client-side API for sub-servers to communicate with the BungeeCord proxy.
 *
 * <p>This API is used by Guild Plugin on each sub-server to send messages
 * to the BungeeCord proxy plugin ({@code guild-bungee}), which then routes
 * them to the appropriate target server or broadcasts across the network.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Guild Plugin calls {@link #initialize(Logger)} during {@code onEnable}.</li>
 *   <li>Outgoing messages are queued and sent via the Plugin Messaging Channel
 *       {@code guild:main}.</li>
 *   <li>Incoming messages from Bungee are dispatched through the
 *       {@link ExtensionBridge} for local processing.</li>
 *   <li>{@link #shutdown()} cleans up during {@code onDisable}.</li>
 * </ol>
 *
 * <h3>Message Types</h3>
 * <table>
 *   <tr><th>Type</th><th>Direction</th><th>Purpose</th></tr>
 *   <tr><td>{@code guild.sync.push}</td><td>Server → Bungee</td><td>Push guild data to Bungee</td></tr>
 *   <tr><td>{@code guild.sync.request}</td><td>Server → Bungee</td><td>Request guild data from another server</td></tr>
 *   <tr><td>{@code guild.sync.broadcast}</td><td>Bungee → All</td><td>Broadcast guild update to all servers</td></tr>
 *   <tr><td>{@code guild.chat.cross}</td><td>Server → Bungee → All</td><td>Cross-server guild chat</td></tr>
 *   <tr><td>{@code guild.event.*}</td><td>Server → Bungee → All</td><td>Guild event propagation</td></tr>
 * </table>
 */
public final class BungeeClientAPI {

    /** The BungeeCord Plugin Messaging Channel name used by this bridge. */
    public static final String CHANNEL_NAME = "guild:main";

    private static Logger logger;
    private static boolean initialized;

    private BungeeClientAPI() {}

    // ── Lifecycle ────────────────────────────────────────────────

    /**
     * Initialize the Bungee client API.
     * Must be called once during Guild Plugin onEnable.
     */
    public static void initialize(Logger logger) {
        if (initialized) return;
        BungeeClientAPI.logger = logger;
        initialized = true;
        logger.info("[BungeeClient] Initialized. Channel: " + CHANNEL_NAME);
    }

    /** Shut down the Bungee client API. */
    public static void shutdown() {
        if (!initialized) return;
        initialized = false;
        logger.info("[BungeeClient] Shut down.");
    }

    /** @return true if the Bungee client is initialized. */
    public static boolean isInitialized() {
        return initialized;
    }

    // ── Outgoing Messages ────────────────────────────────────────

    /**
     * Send a message to the BungeeCord proxy.
     * <p>
     * The message is serialized as JSON and transmitted via the Plugin
     * Messaging Channel. The Bungee proxy plugin receives it and dispatches
     * according to the message type.
     *
     * @param type    message type (e.g. "guild.sync.push")
     * @param payload JSON payload
     */
    public static void sendToBungee(String type, String payload) {
        if (!initialized) {
            if (logger != null) {
                logger.warning("[BungeeClient] Not initialized — message dropped: " + type);
            }
            return;
        }

        long seq = ExtensionBridge.getInstance().nextSequence();
        MessagePacket packet = MessagePacket.create(type, "guild-core")
                .target("guild-bungee")
                .payload(payload)
                .sequence(seq)
                .build();

        // TODO: Actual Plugin Messaging Channel transmission.
        //       Will be wired via guild-plugin's outgoing message channel.
        //       For now, the packet is logged and dispatched to local listeners.
        ExtensionBridge.getInstance().send(packet);
        logger.fine("[BungeeClient] Sent: " + packet);
    }

    /**
     * Push guild data snapshot to Bungee for cross-server sync.
     */
    public static void pushGuildData(String guildDataJson) {
        sendToBungee("guild.sync.push", guildDataJson);
    }

    /**
     * Request guild data from another server (via Bungee routing).
     */
    public static void requestGuildData(int guildId) {
        sendToBungee("guild.sync.request",
                "{\"guildId\":" + guildId + "}");
    }

    /**
     * Send a cross-server guild chat message.
     */
    public static void sendCrossChat(int guildId, String playerName, String message) {
        sendToBungee("guild.chat.cross",
                "{\"guildId\":" + guildId + ",\"player\":\""
                + escapeJson(playerName) + "\",\"message\":\""
                + escapeJson(message) + "\"}");
    }

    /**
     * Broadcast a guild event to all connected servers.
     */
    public static void broadcastEvent(String eventType, String eventDataJson) {
        sendToBungee("guild.event.broadcast",
                "{\"eventType\":\"" + escapeJson(eventType)
                + "\",\"data\":" + eventDataJson + "}");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static String escapeJson(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
