package com.guild.comm.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.guild.comm.bridge.ChannelRouter;
import com.guild.comm.bridge.ExtensionBridge;
import com.guild.comm.bridge.MessagePacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
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
 *   <li>Guild Plugin calls {@link #initialize(Plugin)} during {@code onEnable}.</li>
 *   <li>Outgoing messages are sent via the Plugin Messaging Channel {@code guild:main}
 *       using any available online player as the carrier.</li>
 *   <li>Incoming messages from Bungee are received via {@link PluginMessageListener}
 *       and dispatched through the {@link ChannelRouter} for local processing.</li>
 *   <li>{@link #shutdown()} cleans up during {@code onDisable}.</li>
 * </ol>
 *
 * <h3>Message Types (Inbound from Bungee)</h3>
 * <table>
 *   <tr><th>Type</th><th>Purpose</th></tr>
 *   <tr><td>{@code guild.player.connect}</td><td>Player connection type info (Java/Bedrock)</td></tr>
 *   <tr><td>{@code guild.sync.broadcast}</td><td>Guild data sync broadcast</td></tr>
 *   <tr><td>{@code guild.chat.cross}</td><td>Cross-server guild chat</td></tr>
 *   <tr><td>{@code guild.event.*}</td><td>Guild event propagation</td></tr>
 * </table>
 */
public final class BungeeClientAPI implements PluginMessageListener {

    /** The BungeeCord Plugin Messaging Channel name used by this bridge. */
    public static final String CHANNEL_NAME = "guild:main";

    private static BungeeClientAPI instance;
    private static Logger logger;
    private static Plugin plugin;
    private static volatile boolean initialized;
    private static String initializationError;
    private static final Gson gson = new Gson();

    /** Cache of player connection types, keyed by UUID. Populated by Bungee messages. */
    private static final ConcurrentHashMap<UUID, PlayerConnectionInfo> playerConnectionCache =
            new ConcurrentHashMap<>();

    /**
     * Optional callback invoked after a player's connection info is cached
     * from a Bungee proxy message. Allows guild-plugin to react to late-arriving
     * Bedrock detection (e.g., refresh an already-open GUI).
     * <p>
     * The callback is invoked on the thread that processes the plugin message
     * (main thread on Spigot, potentially a network thread on Folia). Implementations
     * must schedule entity-thread work themselves if needed.
     */
    private static volatile Consumer<PlayerConnectionInfo> connectionInfoCallback;

    /**
     * Optional callback invoked when a form response is received from the proxy.
     * Parameters: (formId, responseData). responseData is null if the form was closed.
     * <p>
     * Used by guild-plugin's BedrockFormSender to invoke the original form's
     * response handlers when a Bedrock player interacts with a forwarded form.
     */
    private static volatile BiConsumer<String, String> formResponseCallback;

    private BungeeClientAPI() {}

    // ── Lifecycle ────────────────────────────────────────────────

    /**
     * Initialize the Bungee client API.
     * Must be called once during Guild Plugin onEnable.
     *
     * @param bukkitPlugin the owning Bukkit plugin (for channel registration)
     */
    public static void initialize(Plugin bukkitPlugin) {
        if (initialized) return;
        try {
            plugin = bukkitPlugin;
            logger = bukkitPlugin.getLogger();
            instance = new BungeeClientAPI();

            // Register Plugin Messaging Channels
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_NAME);
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_NAME, instance);

            initialized = true;
            initializationError = null;
            logger.info("[BungeeClient] Initialized. Channel: " + CHANNEL_NAME);
        } catch (Throwable e) {
            initializationError = e.getClass().getSimpleName() + ": " + e.getMessage();
            if (logger != null) {
                logger.warning("[BungeeClient] Initialization failed: " + initializationError);
            }
        }
    }

    /**
     * Ensure the Bungee client API is initialized (lazy retry).
     * <p>
     * Called by PlayerConnectionService on first player detection if the
     * initial onEnable call failed. Safe to call multiple times.
     *
     * @param bukkitPlugin the owning Bukkit plugin
     * @return true if initialized (either already or just now)
     */
    public static boolean ensureInitialized(Plugin bukkitPlugin) {
        if (initialized) return true;
        initialize(bukkitPlugin);
        return initialized;
    }

    /**
     * @return the last initialization error message, or null if initialization succeeded.
     */
    public static String getInitializationError() {
        return initializationError;
    }

    /** Shut down the Bungee client API. */
    public static void shutdown() {
        if (!initialized) return;

        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_NAME);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_NAME, instance);

        playerConnectionCache.clear();
        initialized = false;
        instance = null;
        logger.info("[BungeeClient] Shut down.");
    }

    /** @return true if the Bungee client is initialized. */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Register a callback to be invoked whenever a player's connection info
     * is received and cached from the Bungee proxy.
     * <p>
     * Used by guild-plugin to refresh GUIs when a late-arriving Bedrock
     * detection updates a player's connection type after they already
     * opened a GUI in Java mode.
     *
     * @param callback the callback, or null to unregister
     */
    public static void setConnectionInfoCallback(Consumer<PlayerConnectionInfo> callback) {
        connectionInfoCallback = callback;
    }

    /**
     * Register a callback for form responses forwarded by the BungeeCord proxy.
     *
     * @param callback receives (formId, responseData), or null to unregister
     */
    public static void setFormResponseCallback(BiConsumer<String, String> callback) {
        formResponseCallback = callback;
    }

    // ── Incoming: PluginMessageListener ──────────────────────────

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL_NAME.equals(channel)) return;

        try {
            String json = new String(message, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            String type = root.has("type") ? root.get("type").getAsString() : null;
            if (type == null) {
                logger.warning("[BungeeClient] Received message without type field");
                return;
            }

            String payload = root.has("payload") ? root.get("payload").getAsString() : "{}";

            logger.info("[BungeeClient] Received: type=" + type);

            // Handle player connection type messages internally
            if ("guild.player.connect".equals(type)) {
                handlePlayerConnect(payload);
            }

            // Handle form responses forwarded by the proxy
            if ("guild.form.response".equals(type)) {
                handleFormResponse(payload);
            }

            // Dispatch to ChannelRouter for guild-plugin listeners
            long seq = root.has("sequence") ? root.get("sequence").getAsLong() : 0;
            String source = root.has("source") ? root.get("source").getAsString() : "guild-bungee";

            MessagePacket packet = MessagePacket.create(type, source)
                    .target("guild-core")
                    .payload(payload)
                    .sequence(seq)
                    .build();

            ExtensionBridge.getInstance().getRouter().route(packet);

        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[BungeeClient] Error processing incoming message: " + e.getMessage(), e);
        }
    }

    /**
     * Handle player connection type info from Bungee.
     * Stores the connection type in the local cache.
     */
    private void handlePlayerConnect(String payload) {
        try {
            JsonObject data = JsonParser.parseString(payload).getAsJsonObject();
            String uuidStr = data.get("uuid").getAsString();
            String name = data.has("name") ? data.get("name").getAsString() : "unknown";
            String connectionType = data.get("connectionType").getAsString();

            UUID uuid = UUID.fromString(uuidStr);
            PlayerConnectionInfo info = new PlayerConnectionInfo(
                    uuid, name, connectionType,
                    data.has("platform") ? data.get("platform").getAsString() : null,
                    data.has("inputMode") ? data.get("inputMode").getAsString() : null
            );

            playerConnectionCache.put(uuid, info);
            logger.info("[BungeeClient] Cached connection type for " + name
                    + ": " + connectionType);

            // Notify registered callback (e.g., guild-plugin GUI refresh for late Bedrock detection)
            Consumer<PlayerConnectionInfo> cb = connectionInfoCallback;
            if (cb != null) {
                try {
                    cb.accept(info);
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                            "[BungeeClient] Connection info callback error: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[BungeeClient] Error parsing player connect payload: " + e.getMessage(), e);
        }
    }

    /**
     * Handle form response forwarded by the BungeeCord proxy.
     * Invokes the registered callback with (formId, responseData).
     */
    private void handleFormResponse(String payload) {
        try {
            JsonObject data = JsonParser.parseString(payload).getAsJsonObject();
            String formId = data.get("formId").getAsString();
            String responseData = data.has("responseData")
                    ? data.get("responseData").getAsString() : null;

            logger.fine("[BungeeClient] Form response received: formId=" + formId
                    + " closed=" + (responseData == null));

            BiConsumer<String, String> cb = formResponseCallback;
            if (cb != null) {
                try {
                    cb.accept(formId, responseData);
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                            "[BungeeClient] Form response callback error: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[BungeeClient] Error parsing form response payload: " + e.getMessage(), e);
        }
    }

    // ── Player Connection Cache ──────────────────────────────────

    /**
     * Get the connection info for a player.
     *
     * @param uuid the player's UUID
     * @return the connection info, or null if not yet received from Bungee
     */
    public static PlayerConnectionInfo getConnectionInfo(UUID uuid) {
        return playerConnectionCache.get(uuid);
    }

    /**
     * Check if a player is a Bedrock player (connected via Geyser).
     *
     * @param uuid the player's UUID
     * @return true if Bedrock, false if Java or unknown
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        PlayerConnectionInfo info = playerConnectionCache.get(uuid);
        return info != null && "BEDROCK".equals(info.getConnectionType());
    }

    /**
     * Remove a player from the connection cache (e.g. on disconnect).
     */
    public static void removeConnectionInfo(UUID uuid) {
        playerConnectionCache.remove(uuid);
    }

    // ── Outgoing Messages ────────────────────────────────────────

    /**
     * Send a message to the BungeeCord proxy.
     * <p>
     * The message is serialized as JSON and transmitted via the Plugin
     * Messaging Channel. Requires at least one player online to carry the message.
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

        // Serialize to JSON
        JsonObject json = new JsonObject();
        json.addProperty("type", packet.getType());
        json.addProperty("source", packet.getSource());
        json.addProperty("target", packet.getTarget());
        json.addProperty("payload", packet.getPayload());
        json.addProperty("sequence", packet.getSequence());
        json.addProperty("timestamp", packet.getTimestamp());

        byte[] data = gson.toJson(json).getBytes(StandardCharsets.UTF_8);

        // Send via any online player (BungeeCord Plugin Messaging requires a player carrier)
        Player carrier = getAnyOnlinePlayer();
        if (carrier == null) {
            logger.warning("[BungeeClient] No online players to carry message: " + type);
            return;
        }

        carrier.sendPluginMessage(plugin, CHANNEL_NAME, data);
        logger.fine("[BungeeClient] Sent: " + packet + " via " + carrier.getName());
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
    public static void requestGuildData(int guildId, String targetServer) {
        sendToBungee("guild.sync.request",
                "{\"guildId\":" + guildId + ",\"targetServer\":\""
                + escapeJson(targetServer) + "\"}");
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

    /**
     * Get any online player to use as a Plugin Message carrier.
     * BungeeCord requires a player connection to transmit plugin messages.
     */
    private static Player getAnyOnlinePlayer() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            return p;
        }
        return null;
    }

    private static String escapeJson(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ── Player Connection Info ───────────────────────────────────

    /**
     * Immutable data class holding a player's connection type information.
     */
    public static class PlayerConnectionInfo {
        private final UUID uuid;
        private final String name;
        private final String connectionType; // "JAVA" or "BEDROCK"
        private final String platform;      // Bedrock platform (null for Java)
        private final String inputMode;     // Bedrock input mode (null for Java)

        public PlayerConnectionInfo(UUID uuid, String name, String connectionType,
                                    String platform, String inputMode) {
            this.uuid = uuid;
            this.name = name;
            this.connectionType = connectionType;
            this.platform = platform;
            this.inputMode = inputMode;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public String getConnectionType() { return connectionType; }
        public String getPlatform() { return platform; }
        public String getInputMode() { return inputMode; }
        public boolean isBedrock() { return "BEDROCK".equals(connectionType); }

        @Override
        public String toString() {
            return String.format("PlayerConnectionInfo[%s/%s type=%s platform=%s input=%s]",
                    name, uuid, connectionType, platform, inputMode);
        }
    }
}
