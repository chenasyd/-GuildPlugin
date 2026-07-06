package com.guild.comm.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes messages by topic to registered handlers.
 *
 * <p>Topics follow a dot-separated convention:
 * <ul>
 *   <li>{@code gui.image.*} — GUI image display operations</li>
 *   <li>{@code guild.sync.*} — Guild data synchronization</li>
 *   <li>{@code cross.server.*} — BungeeCord cross-server messages</li>
 * </ul>
 */
public class ChannelRouter {

    private final Map<String, java.util.List<TopicHandler>> handlers = new ConcurrentHashMap<>();
    private Logger logger;

    public ChannelRouter() {}

    void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Register a handler for a topic (supports wildcard "*" suffix).
     * <pre>{@code
     *   router.subscribe("gui.image.*", packet -> { ... });
     *   router.subscribe("guild.sync.request", packet -> { ... });
     * }</pre>
     */
    public void subscribe(String topic, TopicHandler handler) {
        handlers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    /** Remove a previously registered handler. */
    public void unsubscribe(String topic, TopicHandler handler) {
        java.util.List<TopicHandler> list = handlers.get(topic);
        if (list != null) {
            list.remove(handler);
            if (list.isEmpty()) {
                handlers.remove(topic);
            }
        }
    }

    /**
     * Route a packet to matching handlers.
     * Exact match takes priority; wildcard match is attempted if no exact match.
     */
    public void route(MessagePacket packet) {
        String type = packet.getType();

        // 1. Exact match
        java.util.List<TopicHandler> exact = handlers.get(type);
        if (exact != null && !exact.isEmpty()) {
            for (TopicHandler h : exact) {
                tryInvoke(h, packet);
            }
            return;
        }

        // 2. Wildcard match (e.g. "gui.image.bind" → "gui.image.*")
        for (Map.Entry<String, java.util.List<TopicHandler>> entry : handlers.entrySet()) {
            if (entry.getKey().endsWith(".*")) {
                String prefix = entry.getKey().substring(0, entry.getKey().length() - 2);
                if (type.startsWith(prefix)) {
                    for (TopicHandler h : entry.getValue()) {
                        tryInvoke(h, packet);
                    }
                }
            }
        }
    }

    /** @return number of registered topic handlers. */
    public int getTopicCount() {
        return handlers.size();
    }

    /** Remove all handlers. */
    public void clear() {
        handlers.clear();
    }

    private void tryInvoke(TopicHandler handler, MessagePacket packet) {
        try {
            handler.onMessage(packet);
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.WARNING,
                        "[ChannelRouter] Handler error for topic=" + packet.getType()
                        + ": " + e.getMessage(), e);
            }
        }
    }

    @FunctionalInterface
    public interface TopicHandler {
        void onMessage(MessagePacket packet);
    }
}
