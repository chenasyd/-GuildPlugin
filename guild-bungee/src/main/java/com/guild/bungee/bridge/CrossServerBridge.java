package com.guild.bungee.bridge;

import com.guild.bungee.channel.GuildChannelHandler;
import com.guild.bungee.data.BungeeMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes guild messages between sub-servers via the BungeeCord proxy.
 *
 * <p>This is the central routing engine for cross-server guild communication.
 * It receives messages from sub-servers (via {@code GuildChannelHandler}) and
 * forwards them to the appropriate target server(s) based on the message type.
 *
 * <h3>Message Routing Table</h3>
 * <table>
 *   <tr><th>Message Type</th><th>Direction</th><th>Action</th></tr>
 *   <tr><td>{@code guild.sync.push}</td><td>Server → Bungee</td>
 *       <td>Forward to all servers (data propagation)</td></tr>
 *   <tr><td>{@code guild.sync.request}</td><td>Server → Bungee → Target</td>
 *       <td>Forward to the server specified in payload</td></tr>
 *   <tr><td>{@code guild.chat.cross}</td><td>Server → Bungee → All</td>
 *       <td>Broadcast cross-server guild chat to all servers</td></tr>
 *   <tr><td>{@code guild.event.*}</td><td>Server → Bungee → All</td>
 *       <td>Broadcast guild events to all servers (except source)</td></tr>
 * </table>
 */
public final class CrossServerBridge {

    /**
     * Functional interface for sending messages out to sub-servers.
     * Decouples the bridge from the specific channel implementation.
     */
    @FunctionalInterface
    public interface MessageSender {
        void send(ServerInfo target, BungeeMessage message);
    }

    private final Logger logger;
    private GuildChannelHandler channelHandler;
    private boolean initialized;

    public CrossServerBridge(Logger logger) {
        this.logger = logger;
    }

    /**
     * Set the channel handler after both objects are constructed.
     * Called by {@code GuildBungeePlugin} during onEnable wiring.
     */
    public void setChannelHandler(GuildChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
    }

    // ── Lifecycle ────────────────────────────────────────────────

    /** Initialize the bridge. Called during GuildBungeePlugin#onEnable. */
    public void initialize() {
        if (initialized) return;
        this.initialized = true;
        logger.info("[Bridge] CrossServerBridge initialized.");
    }

    /** Shut down the bridge. */
    public void shutdown() {
        if (!initialized) return;
        this.initialized = false;
        logger.info("[Bridge] CrossServerBridge shut down.");
    }

    /** @return true if the bridge is initialized. */
    public boolean isInitialized() {
        return initialized;
    }

    // ── Inbound Handling ─────────────────────────────────────────

    /**
     * Process an incoming message from a sub-server.
     * Routes based on the message type prefix.
     *
     * @param message      the deserialized message
     * @param sourceServer the server that sent the message
     */
    public void handleIncoming(BungeeMessage message, ServerInfo sourceServer) {
        if (!initialized) {
            logger.warning("[Bridge] Not initialized — dropping message: "
                    + message.getType());
            return;
        }

        String type = message.getType();
        if (type == null) return;

        try {
            if (type.startsWith("guild.sync.push")) {
                handleSyncPush(message, sourceServer);
            } else if (type.startsWith("guild.sync.request")) {
                handleSyncRequest(message, sourceServer);
            } else if (type.startsWith("guild.chat.cross")) {
                handleCrossChat(message, sourceServer);
            } else if (type.startsWith("guild.event.")) {
                handleEventBroadcast(message, sourceServer);
            } else {
                logger.fine("[Bridge] Unrecognized message type: " + type
                        + " from " + sourceServer.getName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[Bridge] Error handling message type '" + type
                            + "': " + e.getMessage(), e);
        }
    }

    // ── Message Type Handlers ────────────────────────────────────

    /**
     * Handle guild data push: forward to all connected servers
     * so each instance stays in sync.
     */
    private void handleSyncPush(BungeeMessage message, ServerInfo sourceServer) {
        logger.fine("[Bridge] Sync push from '" + sourceServer.getName()
                + "': guildId=" + extractGuildId(message));

        BungeeMessage forward = BungeeMessage.create("guild.sync.broadcast", "guild-bungee")
                .payload(message.getPayload())
                .build();

        // Forward to all servers except the source
        broadcastToAllExcept(sourceServer, forward);
    }

    /**
     * Handle guild data request: forward to the target server
     * specified in the payload.
     */
    private void handleSyncRequest(BungeeMessage message, ServerInfo sourceServer) {
        String targetServerName = extractTargetServer(message);
        if (targetServerName == null) {
            logger.warning("[Bridge] Sync request missing target server from '"
                    + sourceServer.getName() + "'");
            return;
        }

        ServerInfo targetServer = ProxyServer.getInstance()
                .getServerInfo(targetServerName);
        if (targetServer == null) {
            logger.warning("[Bridge] Unknown target server '"
                    + targetServerName + "' for sync request");
            return;
        }

        logger.fine("[Bridge] Forwarding sync request: "
                + sourceServer.getName() + " → " + targetServerName);

        BungeeMessage forward = BungeeMessage.create("guild.sync.request", "guild-bungee")
                .payload(message.getPayload())
                .build();

        forwardToServer(targetServer, forward);
    }

    /**
     * Handle cross-server guild chat: broadcast to all connected servers.
     */
    private void handleCrossChat(BungeeMessage message, ServerInfo sourceServer) {
        logger.fine("[Bridge] Cross-chat from '" + sourceServer.getName() + "'");

        BungeeMessage forward = BungeeMessage.create("guild.chat.cross", "guild-bungee")
                .payload(message.getPayload())
                .build();

        // Broadcast to all servers including source (for chat display)
        broadcastToAll(forward);
    }

    /**
     * Handle guild event broadcast: forward to all servers except source.
     */
    private void handleEventBroadcast(BungeeMessage message, ServerInfo sourceServer) {
        logger.fine("[Bridge] Event broadcast from '" + sourceServer.getName()
                + "': " + message.getType());

        BungeeMessage forward = BungeeMessage.create(message.getType(), "guild-bungee")
                .payload(message.getPayload())
                .build();

        broadcastToAllExcept(sourceServer, forward);
    }

    // ── Forwarding Helpers ───────────────────────────────────────

    /**
     * Forward a message to a specific server via the Plugin Messaging Channel.
     */
    private void forwardToServer(ServerInfo target, BungeeMessage message) {
        if (channelHandler != null) {
            channelHandler.sendToServer(target, message);
        } else {
            logger.warning("[Bridge] Channel handler not wired — cannot forward "
                    + message.getType() + " → " + target.getName());
        }
    }

    /**
     * Broadcast a message to all connected servers.
     */
    private void broadcastToAll(BungeeMessage message) {
        if (channelHandler != null) {
            channelHandler.broadcastToAll(message);
        } else {
            logger.warning("[Bridge] Channel handler not wired — cannot broadcast "
                    + message.getType());
        }
    }

    /**
     * Broadcast a message to all servers except the specified one.
     */
    private void broadcastToAllExcept(ServerInfo exclude, BungeeMessage message) {
        if (channelHandler != null) {
            channelHandler.broadcastToAllExcept(exclude, message);
        } else {
            logger.warning("[Bridge] Channel handler not wired — cannot broadcast "
                    + message.getType());
        }
    }

    // ── Payload Helpers ──────────────────────────────────────────

    /**
     * Extract guild ID from the message payload (JSON).
     * Returns -1 if not found.
     */
    private int extractGuildId(BungeeMessage message) {
        // TODO: Parse payload JSON to extract guildId
        return -1;
    }

    /**
     * Extract the target server name from a sync request payload.
     */
    private String extractTargetServer(BungeeMessage message) {
        // TODO: Parse payload JSON to extract targetServer field
        return null;
    }

    // ── Misc ─────────────────────────────────────────────────────

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
