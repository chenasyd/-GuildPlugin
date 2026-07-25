package com.guild.bungee.listener;

import com.google.gson.JsonObject;
import com.guild.bungee.GuildBungeePlugin;
import com.guild.bungee.channel.GuildChannelHandler;
import com.guild.bungee.data.PlayerConnectionType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects whether a player connected via Java Edition or Bedrock Edition (Geyser)
 * and notifies backend servers of the connection type.
 *
 * <h3>Detection Flow</h3>
 * <ol>
 *   <li>{@link PostLoginEvent} — player authenticates with the proxy.
 *       We check the Geyser API (if present) to determine connection type.</li>
 *   <li>{@link ServerConnectedEvent} — player finishes connecting to a backend server.
 *       We send the connection type info to that server via Plugin Messaging.</li>
 *   <li>{@link PlayerDisconnectEvent} — player leaves. Clean up the cache.</li>
 * </ol>
 *
 * <h3>Geyser Detection Strategy</h3>
 * <p>
 * Geyser registers a {@code GeyserApiBase} instance when running on the proxy.
 * We use reflection to avoid a hard compile-time dependency:
 * <ul>
 *   <li>Check if {@code org.geysermc.api.Geyser} class exists</li>
 *   <li>Call {@code Geyser.isRegistered()} to verify API availability</li>
 *   <li>Call {@code Geyser.api().isBedrockPlayer(uuid)} for per-player check</li>
 *   <li>Optionally extract platform/inputMode from the Connection object</li>
 * </ul>
 * This approach allows guild-bungee to work with or without Geyser installed.
 */
public final class PlayerTypeDetector implements Listener {

    private final GuildBungeePlugin plugin;
    private final Logger logger;

    /** Cached connection types, keyed by player UUID. */
    private final Map<UUID, PlayerConnectionType> connectionTypeCache = new ConcurrentHashMap<>();

    /** Cached extra info for Bedrock players (platform string). */
    private final Map<UUID, String> platformCache = new ConcurrentHashMap<>();

    /** Cached input mode for Bedrock players. */
    private final Map<UUID, String> inputModeCache = new ConcurrentHashMap<>();

    /** Whether Geyser API is available on this proxy. */
    private final boolean geyserAvailable;

    public PlayerTypeDetector(GuildBungeePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.geyserAvailable = detectGeyser();

        if (geyserAvailable) {
            logger.info("[PlayerType] Geyser API detected — Bedrock player detection enabled.");
        } else {
            logger.info("[PlayerType] Geyser not found — all players treated as Java Edition.");
        }
    }

    // ── Event Handlers ───────────────────────────────────────────

    /**
     * Called when a player authenticates with the proxy.
     * Detects connection type and caches the result.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerConnectionType type = detectConnectionType(uuid);
        connectionTypeCache.put(uuid, type);

        if (type == PlayerConnectionType.BEDROCK) {
            // Extract extra Bedrock info
            extractBedrockInfo(uuid);
            logger.info("[PlayerType] " + player.getName()
                    + " connected via Bedrock Edition"
                    + (platformCache.containsKey(uuid)
                        ? " (" + platformCache.get(uuid) + ")" : ""));
        } else {
            logger.fine("[PlayerType] " + player.getName() + " connected via Java Edition");
        }
    }

    /**
     * Called when a player successfully connects to a backend server.
     * Sends the connection type info to that server.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerConnectionType type = connectionTypeCache.getOrDefault(uuid, PlayerConnectionType.JAVA);

        // Build the payload
        JsonObject payload = new JsonObject();
        payload.addProperty("uuid", uuid.toString());
        payload.addProperty("name", player.getName());
        payload.addProperty("connectionType", type.toWire());

        if (type == PlayerConnectionType.BEDROCK) {
            String platform = platformCache.get(uuid);
            String inputMode = inputModeCache.get(uuid);
            if (platform != null) payload.addProperty("platform", platform);
            if (inputMode != null) payload.addProperty("inputMode", inputMode);
        }

        // Build the full message envelope
        JsonObject message = new JsonObject();
        message.addProperty("type", "guild.player.connect");
        message.addProperty("source", "guild-bungee");
        message.addProperty("target", event.getServer().getInfo().getName());
        message.addProperty("payload", payload.toString());
        message.addProperty("sequence", 0);
        message.addProperty("timestamp", System.currentTimeMillis());

        // Send to the backend server the player just connected to
        byte[] data = message.toString().getBytes(StandardCharsets.UTF_8);
        boolean sent = event.getServer().getInfo().sendData(GuildChannelHandler.CHANNEL_NAME, data, false);

        if (sent) {
            logger.fine("[PlayerType] Sent connection type for " + player.getName()
                    + " → " + event.getServer().getInfo().getName());
        } else {
            logger.warning("[PlayerType] Failed to send connection type for "
                    + player.getName() + " → " + event.getServer().getInfo().getName()
                    + " (no player on target server to carry the message?)");
        }
    }

    /**
     * Called when a player disconnects from the proxy.
     * Cleans up cached data.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onDisconnect(PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        connectionTypeCache.remove(uuid);
        platformCache.remove(uuid);
        inputModeCache.remove(uuid);
    }

    // ── Detection Logic ──────────────────────────────────────────

    /**
     * Detect whether the Geyser API class is available on the classpath.
     */
    private boolean detectGeyser() {
        try {
            Class.forName("org.geysermc.api.Geyser");
            // Verify the API is actually registered (Geyser plugin is enabled)
            return (boolean) Class.forName("org.geysermc.api.Geyser")
                    .getMethod("isRegistered")
                    .invoke(null);
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "[PlayerType] Error checking Geyser API: " + e.getMessage());
            return false;
        }
    }

    /**
     * Detect the connection type for a given player UUID.
     */
    private PlayerConnectionType detectConnectionType(UUID uuid) {
        if (!geyserAvailable) {
            return PlayerConnectionType.JAVA;
        }

        try {
            Object api = Class.forName("org.geysermc.api.Geyser")
                    .getMethod("api")
                    .invoke(null);

            boolean isBedrock = (boolean) api.getClass()
                    .getMethod("isBedrockPlayer", UUID.class)
                    .invoke(api, uuid);

            return isBedrock ? PlayerConnectionType.BEDROCK : PlayerConnectionType.JAVA;
        } catch (Exception e) {
            logger.log(Level.FINE, "[PlayerType] Error detecting connection type for "
                    + uuid + ": " + e.getMessage());
            return PlayerConnectionType.JAVA;
        }
    }

    /**
     * Extract additional Bedrock connection info (platform, input mode).
     */
    private void extractBedrockInfo(UUID uuid) {
        try {
            Object api = Class.forName("org.geysermc.api.Geyser")
                    .getMethod("api")
                    .invoke(null);

            Object connection = api.getClass()
                    .getMethod("connectionByUuid", UUID.class)
                    .invoke(api, uuid);

            if (connection == null) return;

            // Get platform
            Object platform = connection.getClass()
                    .getMethod("platform")
                    .invoke(connection);
            if (platform != null) {
                platformCache.put(uuid, platform.toString());
            }

            // Get input mode
            Object inputMode = connection.getClass()
                    .getMethod("inputMode")
                    .invoke(connection);
            if (inputMode != null) {
                inputModeCache.put(uuid, inputMode.toString());
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "[PlayerType] Error extracting Bedrock info for "
                    + uuid + ": " + e.getMessage());
        }
    }

    // ── Public API ───────────────────────────────────────────────

    /**
     * Get the cached connection type for a player.
     *
     * @param uuid the player's UUID
     * @return the connection type, or JAVA if unknown
     */
    public PlayerConnectionType getConnectionType(UUID uuid) {
        return connectionTypeCache.getOrDefault(uuid, PlayerConnectionType.JAVA);
    }

    /**
     * Check if a player is a Bedrock player.
     */
    public boolean isBedrockPlayer(UUID uuid) {
        return getConnectionType(uuid) == PlayerConnectionType.BEDROCK;
    }

    /**
     * @return true if Geyser API is available on this proxy.
     */
    public boolean isGeyserAvailable() {
        return geyserAvailable;
    }
}
