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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects whether a player connected via Java Edition or Bedrock Edition (Geyser)
 * and notifies backend servers of the connection type.
 *
 * <h3>Detection Flow</h3>
 * <ol>
 *   <li>{@link PostLoginEvent} — player authenticates with the proxy.
 *       Best-effort early detection. <b>May return JAVA for Bedrock players</b>
 *       because Geyser registers sessions in its UUID-keyed map only after
 *       {@code ClientboundLoginFinishedPacket} is processed, which happens
 *       <em>after</em> PostLoginEvent fires.</li>
 *   <li>{@link ServerConnectedEvent} — player finishes connecting to a backend server.
 *       <b>Authoritative detection point.</b> By this time Geyser's session map
 *       is populated, so {@code isBedrockPlayer(uuid)} returns the correct result.
 *       We re-detect here (not relying on the PostLoginEvent cache) and send
 *       the connection type info to the backend server via Plugin Messaging.</li>
 *   <li>Delayed retry (1.5s) — safety net re-detection and re-send in case
 *       even ServerConnectedEvent was slightly too early.</li>
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

    /**
     * Whether Geyser API is available on this proxy.
     * <p>
     * NOT final — uses lazy re-detection to handle plugin load order issues.
     * If guild-bungee loads before Geyser-BungeeCord (e.g. softDepends mismatch),
     * the initial check returns false. This field is re-checked on each player
     * detection until Geyser becomes available.
     */
    private volatile boolean geyserAvailable;

    /** Whether the initial Geyser check result has been logged. */
    private volatile boolean geyserCheckLogged;

    public PlayerTypeDetector(GuildBungeePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.geyserAvailable = detectGeyser();
        this.geyserCheckLogged = true;

        if (geyserAvailable) {
            logger.info("[PlayerType] Geyser API detected — Bedrock player detection enabled.");
        } else {
            logger.info("[PlayerType] Geyser not found at startup — will re-check on player login."
                    + " (If Geyser loads after GuildBungee, detection activates automatically.)");
        }
    }

    // ── Event Handlers ───────────────────────────────────────────

    /**
     * Called when a player authenticates with the proxy.
     * <p>
     * Best-effort early detection. Geyser's session map may not yet contain
     * this player's UUID at this point (session is registered only after
     * {@code ClientboundLoginFinishedPacket}), so Bedrock players may be
     * misdetected as JAVA here. The authoritative re-detection happens in
     * {@link #onServerConnected(ServerConnectedEvent)}.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Best-effort: may return JAVA for Bedrock players (Geyser session not yet registered)
        PlayerConnectionType type = detectConnectionType(uuid);
        connectionTypeCache.put(uuid, type);

        if (type == PlayerConnectionType.BEDROCK) {
            extractBedrockInfo(uuid);
            logger.info("[PlayerType] " + player.getName()
                    + " connected via Bedrock Edition (early detection)"
                    + (platformCache.containsKey(uuid)
                        ? " (" + platformCache.get(uuid) + ")" : ""));
        } else {
            logger.info("[PlayerType] " + player.getName()
                    + " detected as Java Edition at PostLogin (will re-check at ServerConnected)");
        }
    }

    /**
     * Called when a player successfully connects to a backend server.
     * <p>
     * <b>Authoritative detection point.</b> By this time Geyser has processed
     * {@code ClientboundLoginFinishedPacket} and registered the session in its
     * UUID-keyed map, so {@code isBedrockPlayer(uuid)} returns the correct result.
     * <p>
     * We re-detect here instead of relying on the PostLoginEvent cache, which
     * may contain a stale JAVA result for Bedrock players.
     * <p>
     * Uses {@code queue=true} so the message is buffered if the target server
     * has no players yet. Also schedules a delayed retry with fresh re-detection
     * as a safety net.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        String serverName = event.getServer().getInfo().getName();

        // ── Authoritative re-detection (Geyser session map is populated by now) ──
        PlayerConnectionType type = detectConnectionType(uuid);
        PlayerConnectionType previousType = connectionTypeCache.put(uuid, type);

        if (type == PlayerConnectionType.BEDROCK) {
            extractBedrockInfo(uuid);
            if (previousType != PlayerConnectionType.BEDROCK) {
                logger.info("[PlayerType] " + playerName
                        + " re-detected as Bedrock Edition at ServerConnected"
                        + " (PostLogin had: " + (previousType != null ? previousType.toWire() : "none") + ")"
                        + (platformCache.containsKey(uuid)
                            ? " (" + platformCache.get(uuid) + ")" : ""));
            }
        }

        // Send the connection type to the backend server
        sendConnectionType(player, uuid, playerName, serverName, type, 0);

        // Delayed retry: re-detect + re-send after 1.5s as a safety net.
        // Covers edge cases where even ServerConnectedEvent is slightly too early,
        // or the backend plugin was still initializing its plugin channel.
        // The backend deduplicates by UUID (ConcurrentHashMap.put), so duplicate is harmless.
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            if (!player.isConnected()) return;
            if (player.getServer() == null) return;
            if (!player.getServer().getInfo().getName().equals(serverName)) return;

            // Fresh re-detection for the retry
            PlayerConnectionType retryType = detectConnectionType(uuid);
            connectionTypeCache.put(uuid, retryType);
            if (retryType == PlayerConnectionType.BEDROCK) {
                extractBedrockInfo(uuid);
            }

            sendConnectionType(player, uuid, playerName, serverName, retryType, 1);
            logger.info("[PlayerType] Retry sent connection type for "
                    + playerName + " → " + serverName
                    + " (type=" + retryType.toWire() + ")");
        }, 1500, TimeUnit.MILLISECONDS);
    }

    /**
     * Build and send a {@code guild.player.connect} message to the specified backend server.
     *
     * @param player     the proxied player (used for sendData carrier)
     * @param uuid       player UUID
     * @param playerName player name
     * @param serverName target backend server name
     * @param type       detected connection type
     * @param sequence   message sequence number (0 = initial, 1 = retry)
     */
    private void sendConnectionType(ProxiedPlayer player, UUID uuid, String playerName,
                                     String serverName, PlayerConnectionType type, int sequence) {
        // Build the payload
        JsonObject payload = new JsonObject();
        payload.addProperty("uuid", uuid.toString());
        payload.addProperty("name", playerName);
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
        message.addProperty("target", serverName);
        message.addProperty("payload", payload.toString());
        message.addProperty("sequence", sequence);
        message.addProperty("timestamp", System.currentTimeMillis());

        byte[] data = message.toString().getBytes(StandardCharsets.UTF_8);
        boolean sent = player.getServer().getInfo()
                .sendData(GuildChannelHandler.CHANNEL_NAME, data, true);

        if (sent && sequence == 0) {
            logger.info("[PlayerType] Sent connection type for " + playerName
                    + " → " + serverName + " (type=" + type.toWire() + ")");
        } else if (!sent && sequence == 0) {
            logger.warning("[PlayerType] Failed to send connection type for "
                    + playerName + " → " + serverName + " (queued for retry)");
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

    /** Geyser-BungeeCord plugin name in BungeeCord's PluginManager. */
    private static final String GEYSER_PLUGIN_NAME = "Geyser-BungeeCord";

    /**
     * Ensure Geyser availability has been checked (lazy re-detection).
     * <p>
     * If the initial check at construction time returned false (Geyser not yet loaded),
     * this method re-checks on every call until Geyser becomes available.
     * Once Geyser is confirmed available, no further re-checks are performed.
     *
     * @return true if Geyser API is available
     */
    private boolean ensureGeyserChecked() {
        if (geyserAvailable) return true;

        // Re-check: Geyser might have loaded after GuildBungee
        boolean available = detectGeyser();
        if (available) {
            geyserAvailable = true;
            logger.info("[PlayerType] Geyser API detected (late initialization) — "
                    + "Bedrock player detection now enabled.");
        }
        return available;
    }

    /**
     * Get the ClassLoader of the Geyser-BungeeCord plugin.
     * <p>
     * BungeeCord isolates each plugin in its own ClassLoader. Using
     * {@code Class.forName("org.geysermc.api.Geyser")} from GuildBungee's
     * ClassLoader will throw ClassNotFoundException because Geyser's classes
     * live in Geyser-BungeeCord's ClassLoader. We must use Geyser's own
     * ClassLoader to load its API classes.
     *
     * @return Geyser's ClassLoader, or null if Geyser plugin is not found
     */
    private ClassLoader getGeyserClassLoader() {
        net.md_5.bungee.api.plugin.Plugin geyserPlugin =
                ProxyServer.getInstance().getPluginManager().getPlugin(GEYSER_PLUGIN_NAME);
        if (geyserPlugin == null) return null;
        return geyserPlugin.getClass().getClassLoader();
    }

    /**
     * Detect whether the Geyser API is available on this proxy.
     * <p>
     * Uses Geyser plugin's ClassLoader to bypass BungeeCord's ClassLoader isolation.
     */
    private boolean detectGeyser() {
        try {
            ClassLoader geyserLoader = getGeyserClassLoader();
            if (geyserLoader == null) {
                return false;
            }

            Class<?> geyserClass = Class.forName("org.geysermc.api.Geyser", true, geyserLoader);
            return (boolean) geyserClass.getMethod("isRegistered").invoke(null);
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "[PlayerType] Error checking Geyser API: " + e.getMessage());
            return false;
        }
    }

    /**
     * Detect the connection type for a given player UUID.
     * <p>
     * Uses Geyser plugin's ClassLoader for all reflection calls.
     */
    private PlayerConnectionType detectConnectionType(UUID uuid) {
        if (!ensureGeyserChecked()) {
            return PlayerConnectionType.JAVA;
        }

        try {
            ClassLoader geyserLoader = getGeyserClassLoader();
            if (geyserLoader == null) return PlayerConnectionType.JAVA;

            Class<?> geyserClass = Class.forName("org.geysermc.api.Geyser", true, geyserLoader);
            Object api = geyserClass.getMethod("api").invoke(null);

            // Use GeyserApiBase interface (declares isBedrockPlayer) for reliable method lookup
            Class<?> apiBaseClass = Class.forName("org.geysermc.api.GeyserApiBase", true, geyserLoader);
            boolean isBedrock = (boolean) apiBaseClass
                    .getMethod("isBedrockPlayer", UUID.class)
                    .invoke(api, uuid);

            return isBedrock ? PlayerConnectionType.BEDROCK : PlayerConnectionType.JAVA;
        } catch (Exception e) {
            logger.log(Level.WARNING, "[PlayerType] Error detecting connection type for "
                    + uuid + ": " + e.getMessage());
            return PlayerConnectionType.JAVA;
        }
    }

    /**
     * Extract additional Bedrock connection info (platform, input mode).
     * <p>
     * Uses Geyser plugin's ClassLoader for all reflection calls.
     */
    private void extractBedrockInfo(UUID uuid) {
        try {
            ClassLoader geyserLoader = getGeyserClassLoader();
            if (geyserLoader == null) return;

            Class<?> geyserClass = Class.forName("org.geysermc.api.Geyser", true, geyserLoader);
            Object api = geyserClass.getMethod("api").invoke(null);

            Class<?> apiBaseClass = Class.forName("org.geysermc.api.GeyserApiBase", true, geyserLoader);
            Object connection = apiBaseClass
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
