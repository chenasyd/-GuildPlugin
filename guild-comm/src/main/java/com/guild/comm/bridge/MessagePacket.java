package com.guild.comm.bridge;

/**
 * A serializable message packet exchanged between Guild Plugin and
 * registered external extensions via the {@link ExtensionBridge}.
 *
 * <p>Packets are JSON-serialized for transport over Bukkit Plugin
 * Messaging Channels or in-process direct calls.
 */
public class MessagePacket {

    /** Message type identifier (e.g. "gui.image.bind", "guild.sync.request"). */
    private final String type;

    /** Source extension ID. */
    private final String source;

    /** Target extension ID (or "*" for broadcast). */
    private final String target;

    /** JSON payload — structured by each message type's schema. */
    private final String payload;

    /** Monotonic sequence number for tracing. */
    private final long sequence;

    /** Timestamp (System.currentTimeMillis) when the packet was created. */
    private final long timestamp;

    public MessagePacket(String type, String source, String target,
                         String payload, long sequence) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.payload = payload;
        this.sequence = sequence;
        this.timestamp = System.currentTimeMillis();
    }

    // ── Accessors ────────────────────────────────────────────────

    public String getType()    { return type; }
    public String getSource()  { return source; }
    public String getTarget()  { return target; }
    public String getPayload() { return payload; }
    public long   getSequence(){ return sequence; }
    public long   getTimestamp(){ return timestamp; }

    /** Returns true if this packet targets all extensions. */
    public boolean isBroadcast() {
        return "*".equals(target);
    }

    // ── Builder ──────────────────────────────────────────────────

    public static Builder create(String type, String source) {
        return new Builder(type, source);
    }

    public static class Builder {
        private final String type;
        private final String source;
        private String target = "*";
        private String payload = "{}";
        private long sequence;

        private Builder(String type, String source) {
            this.type = type;
            this.source = source;
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder sequence(long seq) {
            this.sequence = seq;
            return this;
        }

        public MessagePacket build() {
            return new MessagePacket(type, source, target, payload, sequence);
        }
    }

    @Override
    public String toString() {
        return String.format("MessagePacket[#%d %s → %s type=%s]",
                sequence, source, target, type);
    }
}
