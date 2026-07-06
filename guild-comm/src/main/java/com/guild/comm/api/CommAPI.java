package com.guild.comm.api;

import com.guild.comm.bridge.ChannelRouter;
import com.guild.comm.bridge.ExtensionBridge;
import com.guild.comm.bridge.ExtensionHandle;
import com.guild.comm.bridge.MessagePacket;
import com.guild.comm.debug.TraceContext;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Public API facade for the Guild Communication Bridge.
 *
 * <h3>Usage — Guild Plugin (host)</h3>
 * <pre>{@code
 *   CommAPI.initialize(logger);
 *   CommAPI.getBridge().register(new ExtensionHandle("my-ext", "My Extension", "1.0"));
 *   CommAPI.send(MessagePacket.create("gui.image.bind", "guild-core")
 *       .target("my-ext").payload("{\"guiId\":\"main\"}").build());
 * }</pre>
 *
 * <h3>Usage — External Plugin (extension)</h3>
 * <pre>{@code
 *   CommAPI.connect("imago-core", "ImagoCore", "1.0", "gui.image.*", "render.*");
 *   CommAPI.on("gui.image.*", packet -> { ... });
 *   CommAPI.send(MessagePacket.create("gui.image.ready", "imago-core")
 *       .target("guild-core").payload("{}").build());
 * }</pre>
 */
public final class CommAPI {

    private CommAPI() {}

    // ── Bridge access ────────────────────────────────────────────

    /** Get the singleton bridge instance. */
    public static ExtensionBridge getBridge() {
        return ExtensionBridge.getInstance();
    }

    /** Initialize the bridge (call once from Guild Plugin onEnable). */
    public static void initialize(Logger logger) {
        ExtensionBridge.getInstance().initialize(logger);
    }

    /** Shut down the bridge (call once from Guild Plugin onDisable). */
    public static void shutdown() {
        ExtensionBridge.getInstance().shutdown();
    }

    /** @return true if the bridge has been {@link #initialize}d. */
    public static boolean isInitialized() {
        return ExtensionBridge.getInstance().isInitialized();
    }

    // ── Convenience: Registration ────────────────────────────────

    /**
     * Build an ExtensionHandle and register it in one call.
     * Shortcut used by external plugins during their onEnable.
     */
    public static ExtensionHandle connect(String extensionId, String displayName,
                                          String version, String... supportedTypes) {
        ExtensionHandle handle = new ExtensionHandle(
                extensionId, displayName, version, supportedTypes);
        ExtensionBridge.getInstance().register(handle);
        return handle;
    }

    /** Deregister an extension by ID. */
    public static boolean disconnect(String extensionId) {
        return ExtensionBridge.getInstance().deregister(extensionId);
    }

    /** Check if an extension is connected and active. */
    public static boolean isConnected(String extensionId) {
        return ExtensionBridge.getInstance().isConnected(extensionId);
    }

    // ── Convenience: Messaging ───────────────────────────────────

    /** Send a message packet through the bridge. */
    public static void send(MessagePacket packet) {
        ExtensionBridge.getInstance().send(packet);
    }

    /**
     * Build and send a message in one call.
     * @param type    message type (e.g. "gui.image.bind")
     * @param source  sender extension ID
     * @param target  recipient extension ID ("*" for broadcast)
     * @param payload JSON payload string
     */
    public static void send(String type, String source, String target, String payload) {
        long seq = ExtensionBridge.getInstance().nextSequence();
        MessagePacket packet = MessagePacket.create(type, source)
                .target(target)
                .payload(payload)
                .sequence(seq)
                .build();
        ExtensionBridge.getInstance().send(packet);
    }

    // ── Convenience: Listeners ───────────────────────────────────

    /**
     * Register a global message listener.
     * @see ExtensionBridge#addMessageListener(ExtensionBridge.MessageListener)
     */
    public static void addListener(ExtensionBridge.MessageListener listener) {
        ExtensionBridge.getInstance().addMessageListener(listener);
    }

    /** Register a topic-specific handler via the ChannelRouter. */
    public static void on(String topic, ChannelRouter.TopicHandler handler) {
        ExtensionBridge.getInstance().getRouter().subscribe(topic, handler);
    }

    /** Unregister a topic handler. */
    public static void off(String topic, ChannelRouter.TopicHandler handler) {
        ExtensionBridge.getInstance().getRouter().unsubscribe(topic, handler);
    }

    // ── Queries ──────────────────────────────────────────────────

    public static Set<String> getExtensions() {
        return ExtensionBridge.getInstance().getExtensionIds();
    }

    public static ExtensionHandle getHandle(String id) {
        return ExtensionBridge.getInstance().getExtension(id);
    }

    public static int getCount() {
        return ExtensionBridge.getInstance().getExtensionCount();
    }

    // ── Debug ────────────────────────────────────────────────────

    /** Create a trace context for debugging a request chain. */
    public static TraceContext trace(String operation) {
        return new TraceContext(operation);
    }
}
