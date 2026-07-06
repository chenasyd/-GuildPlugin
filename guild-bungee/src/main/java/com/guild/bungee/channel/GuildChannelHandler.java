package com.guild.bungee.channel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.guild.bungee.GuildBungeePlugin;
import com.guild.bungee.bridge.CrossServerBridge;
import com.guild.bungee.data.BungeeMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for Plugin Messages on the {@code guild:main} channel
 * and dispatches them to the {@link CrossServerBridge}.
 *
 * <p>Registered with {@code ProxyServer.registerChannel("guild:main")}
 * during {@link GuildBungeePlugin#onEnable()}.
 *
 * <h3>Inbound Messages</h3>
 * Messages arrive as raw byte arrays from sub-servers.
 * The first bytes contain the channel name, followed by the JSON payload.
 * Expected JSON structure:
 * <pre>
 * {
 *   "type": "guild.sync.push",
 *   "source": "guild-core",
 *   "target": "guild-bungee",
 *   "payload": "...",
 *   "sequence": 1,
 *   "timestamp": 1712345678000
 * }
 * </pre>
 *
 * <h3>Outbound Messages</h3>
 * The handler also provides {@link #sendToServer(ServerInfo, BungeeMessage)}
 * to forward messages from Bungee back to specific sub-servers.
 */
public final class GuildChannelHandler implements Listener {

    /** Plugin Messaging Channel name — must match {@code BungeeClientAPI.CHANNEL_NAME}. */
    public static final String CHANNEL_NAME = "guild:main";

    private final GuildBungeePlugin plugin;
    private final CrossServerBridge bridge;
    private final Logger logger;
    private final Gson gson;

    public GuildChannelHandler(GuildBungeePlugin plugin, CrossServerBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.logger = plugin.getLogger();
        this.gson = new Gson();
    }

    // ── Inbound: Receive Plugin Message from sub-server ───────────

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(CHANNEL_NAME)) {
            return;
        }

        // Only process messages originating from sub-servers
        if (!(event.getSender() instanceof Server)) {
            return;
        }

        event.setCancelled(true); // Prevent further processing

        Server sender = (Server) event.getSender();
        ServerInfo sourceServer = sender.getInfo();

        try {
            byte[] data = event.getData();
            String json = new String(data, StandardCharsets.UTF_8);
            logger.fine("[Channel] Received from '" + sourceServer.getName()
                    + "': " + truncate(json, 200));

            BungeeMessage message = gson.fromJson(json, BungeeMessage.class);
            if (message == null) {
                logger.warning("[Channel] Failed to parse message from '"
                        + sourceServer.getName() + "'");
                return;
            }

            // Attach metadata
            message.setSourceServer(sourceServer.getName());

            // Delegate to bridge for routing
            bridge.handleIncoming(message, sourceServer);

        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[Channel] Error processing message from '"
                            + sourceServer.getName() + "': " + e.getMessage(), e);
        }
    }

    // ── Outbound: Send message from Bungee to a sub-server ───────

    /**
     * Forward a message from Bungee to a specific sub-server.
     *
     * @param target  the destination server
     * @param message the message to forward
     */
    public void sendToServer(ServerInfo target, BungeeMessage message) {
        if (target == null || message == null) return;

        try {
            String json = gson.toJson(message);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            if (!target.sendData(CHANNEL_NAME, data, false)) {
                logger.warning("[Channel] Failed to send data to server '"
                        + target.getName() + "'");
            } else {
                logger.fine("[Channel] Forwarded " + message.getType()
                        + " → " + target.getName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "[Channel] Error sending to '" + target.getName()
                            + "': " + e.getMessage(), e);
        }
    }

    /**
     * Broadcast a message to all connected sub-servers.
     */
    public void broadcastToAll(BungeeMessage message) {
        String json = gson.toJson(message);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            try {
                server.sendData(CHANNEL_NAME, data, false);
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "[Channel] Broadcast error to '" + server.getName()
                                + "': " + e.getMessage());
            }
        }

        logger.fine("[Channel] Broadcast " + message.getType()
                + " to all servers");
    }

    /**
     * Broadcast a message to all connected sub-servers except the specified one.
     */
    public void broadcastToAllExcept(ServerInfo exclude, BungeeMessage message) {
        String json = gson.toJson(message);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            if (server.equals(exclude)) continue;
            try {
                server.sendData(CHANNEL_NAME, data, false);
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        "[Channel] Broadcast error to '" + server.getName()
                                + "': " + e.getMessage());
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
