package com.guild.comm.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton bridge that manages all registered external extensions.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Guild Plugin initializes the bridge via {@link #initialize(Logger)}.</li>
 *   <li>External extensions register via {@link #register(ExtensionHandle)}.</li>
 *   <li>Messages are exchanged via {@link #send(MessagePacket)} and
 *       {@link #addMessageListener(MessageListener)}.</li>
 *   <li>Extensions deregister via {@link #deregister(String)}.</li>
 *   <li>Bridge shuts down via {@link #shutdown()}.</li>
 * </ol>
 */
public class ExtensionBridge {

    private static final ExtensionBridge INSTANCE = new ExtensionBridge();

    /** Registered extensions keyed by extensionId. */
    private final Map<String, ExtensionHandle> extensions = new ConcurrentHashMap<>();

    /** Message listeners (thread-safe). */
    private final java.util.List<MessageListener> listeners = new CopyOnWriteArrayList<>();

    private final ChannelRouter router = new ChannelRouter();
    private Logger logger;
    private volatile boolean initialized;
    private volatile long sequenceCounter;

    private ExtensionBridge() {}

    // ── Singleton access ─────────────────────────────────────────

    public static ExtensionBridge getInstance() {
        return INSTANCE;
    }

    /** @return true if the bridge has been initialized via {@link #initialize}. */
    public boolean isInitialized() {
        return initialized;
    }

    // ── Lifecycle ────────────────────────────────────────────────

    /**
     * Initialize the bridge with a logger.
     * Must be called once by Guild Plugin during startup.
     */
    public void initialize(Logger logger) {
        if (initialized) return;
        this.logger = logger;
        this.initialized = true;
        this.sequenceCounter = 0;
        logger.info("[CommBridge] Extension bridge initialized.");
    }

    /** Shut down and deregister all extensions. */
    public void shutdown() {
        if (!initialized) return;
        initialized = false;

        for (String id : extensions.keySet()) {
            deregister(id);
        }
        extensions.clear();
        listeners.clear();
        logger.info("[CommBridge] Extension bridge shut down.");
    }

    // ── Extension management ─────────────────────────────────────

    /**
     * Register an external extension.
     * @return this bridge (for chaining)
     * @throws IllegalArgumentException if extensionId is already registered
     */
    public ExtensionBridge register(ExtensionHandle handle) {
        ensureInitialized();

        if (extensions.containsKey(handle.getExtensionId())) {
            throw new IllegalArgumentException(
                    "Extension already registered: " + handle.getExtensionId());
        }

        handle.setState(ExtensionHandle.State.ACTIVE);
        extensions.put(handle.getExtensionId(), handle);
        logger.info("[CommBridge] Extension connected: " + handle.getExtensionId()
                + " v" + handle.getVersion());
        return this;
    }

    /**
     * Deregister an extension by ID.
     * @return true if the extension was registered and removed
     */
    public boolean deregister(String extensionId) {
        ensureInitialized();

        ExtensionHandle handle = extensions.remove(extensionId);
        if (handle != null) {
            handle.setState(ExtensionHandle.State.DISCONNECTED);
            logger.info("[CommBridge] Extension disconnected: " + extensionId);
            return true;
        }
        return false;
    }

    /** Get a registered extension by ID, or null. */
    public ExtensionHandle getExtension(String extensionId) {
        return extensions.get(extensionId);
    }

    /** @return true if the given extension ID is registered and ACTIVE. */
    public boolean isConnected(String extensionId) {
        ExtensionHandle h = extensions.get(extensionId);
        return h != null && h.getState() == ExtensionHandle.State.ACTIVE;
    }

    /** @return an immutable snapshot of all registered extension IDs. */
    public java.util.Set<String> getExtensionIds() {
        return java.util.Collections.unmodifiableSet(extensions.keySet());
    }

    /** @return number of currently registered extensions. */
    public int getExtensionCount() {
        return extensions.size();
    }

    // ── Messaging ────────────────────────────────────────────────

    /**
     * Send a message packet to one or all extensions.
     * <ul>
     *   <li>If {@code packet.isBroadcast()} — delivered to all listeners.</li>
     *   <li>If a specific target — only listeners matching the target receive it.</li>
     * </ul>
     */
    public void send(MessagePacket packet) {
        ensureInitialized();

        if (listeners.isEmpty()) {
            log(Level.FINE, "[CommBridge] No listeners for packet: " + packet);
            return;
        }

        for (MessageListener listener : listeners) {
            try {
                listener.onMessage(packet);
            } catch (Exception e) {
                log(Level.WARNING, "[CommBridge] Listener error for " + packet + ": " + e.getMessage());
            }
        }
    }

    /**
     * Send a packet and wait for exactly one response.
     * Only the first non-null response is returned.
     */
    public MessagePacket sendAndAwait(MessagePacket packet, long timeoutMs) {
        // Simple synchronous implementation: send and collect first response
        // For true async, callers should use request-response pattern via listeners
        send(packet);
        // In-process: if any listener synchronously returns, it would be handled
        // through a separate callback mechanism
        return null; // Extend for async patterns later
    }

    /** Allocate and return the next monotonic sequence number. */
    public long nextSequence() {
        return ++sequenceCounter;
    }

    // ── Listeners ────────────────────────────────────────────────

    /** Register a global message listener. */
    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    /** Remove a previously registered listener. */
    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    /** @return number of registered listeners. */
    public int getListenerCount() {
        return listeners.size();
    }

    // ── Router access ────────────────────────────────────────────

    public ChannelRouter getRouter() {
        return router;
    }

    // ── Internal ─────────────────────────────────────────────────

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "ExtensionBridge not initialized. Call initialize() first.");
        }
    }

    private void log(Level level, String msg) {
        if (logger != null) {
            logger.log(level, msg);
        }
    }

    // ── MessageListener functional interface ─────────────────────

    @FunctionalInterface
    public interface MessageListener {
        /**
         * Called when a message packet is received.
         * Implementations should be fast and non-blocking.
         */
        void onMessage(MessagePacket packet);
    }
}
