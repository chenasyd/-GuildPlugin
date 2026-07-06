package com.guild.bungee;

import com.guild.bungee.bridge.CrossServerBridge;
import com.guild.bungee.channel.GuildChannelHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.logging.Logger;

/**
 * BungeeCord proxy plugin that bridges guild data across sub-servers.
 *
 * <h3>Architecture</h3>
 * <pre>
 *   [Server A: GuildPlugin + BungeeClientAPI]
 *         |  Plugin Messaging Channel "guild:main"
 *         v
 *   [BungeeCord: GuildBungeePlugin]
 *         |  GuildChannelHandler → CrossServerBridge
 *         v
 *   [Server B: GuildPlugin + BungeeClientAPI]
 * </pre>
 *
 * <h3>Message Flow</h3>
 * <ol>
 *   <li>Sub-server GuildPlugin sends a message via {@code BungeeClientAPI.sendToBungee()}.</li>
 *   <li>This plugin's {@link GuildChannelHandler} receives the raw Plugin Message.</li>
 *   <li>{@link CrossServerBridge} deserializes and routes the message
 *       to the target server(s) based on the message type.</li>
 *   <li>Target server processes the message and optionally responds.</li>
 * </ol>
 */
public final class GuildBungeePlugin extends Plugin {

    private static GuildBungeePlugin instance;

    private CrossServerBridge bridge;
    private GuildChannelHandler channelHandler;

    // ── Singleton ─────────────────────────────────────────────────

    /** @return the singleton plugin instance, or null before onEnable. */
    public static GuildBungeePlugin getInstance() {
        return instance;
    }

    // ── Lifecycle ────────────────────────────────────────────────

    @Override
    public void onEnable() {
        instance = this;
        Logger log = getLogger();

        log.info("╔══════════════════════════════════════════╗");
        log.info("║       Guild BungeeCord Bridge v" + getDescription().getVersion()
                + "      ║");
        log.info("╚══════════════════════════════════════════╝");

        // 1. Initialize the cross-server bridge
        bridge = new CrossServerBridge(log);
        bridge.initialize();
        log.info("[GuildBungee] CrossServerBridge initialized.");

        // 2. Register Plugin Messaging Channel handler and wire bridge ↔ handler
        channelHandler = new GuildChannelHandler(this, bridge);
        bridge.setChannelHandler(channelHandler);
        ProxyServer.getInstance().registerChannel(GuildChannelHandler.CHANNEL_NAME);
        ProxyServer.getInstance().getPluginManager().registerListener(this, channelHandler);
        log.info("[GuildBungee] Channel '" + GuildChannelHandler.CHANNEL_NAME
                + "' registered.");

        // 3. Log active servers
        int serverCount = ProxyServer.getInstance().getServers().size();
        log.info("[GuildBungee] Monitoring " + serverCount + " connected server(s).");
    }

    @Override
    public void onDisable() {
        Logger log = getLogger();

        // 1. Unregister channel
        ProxyServer.getInstance().unregisterChannel(GuildChannelHandler.CHANNEL_NAME);
        ProxyServer.getInstance().getPluginManager().unregisterListener(channelHandler);

        // 2. Shutdown bridge
        if (bridge != null) {
            bridge.shutdown();
        }

        log.info("[GuildBungee] Guild BungeeCord Bridge disabled.");
        instance = null;
    }

    // ── Accessors ─────────────────────────────────────────────────

    /** @return the cross-server message bridge. */
    public CrossServerBridge getBridge() {
        return bridge;
    }

    /** @return the Plugin Messaging Channel handler. */
    public GuildChannelHandler getChannelHandler() {
        return channelHandler;
    }
}
