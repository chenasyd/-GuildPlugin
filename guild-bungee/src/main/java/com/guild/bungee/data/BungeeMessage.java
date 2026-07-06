package com.guild.bungee.data;

import com.google.gson.annotations.SerializedName;

/**
 * Lightweight message envelope used for BungeeCord ↔ sub-server communication.
 *
 * <p>This is a simplified equivalent of {@code com.guild.comm.bridge.MessagePacket},
 * designed to be self-contained within the guild-bungee module without depending
 * on Spigot API or guild-comm. Messages are JSON-serialized and transmitted
 * via the BungeeCord Plugin Messaging Channel {@code guild:main}.
 *
 * <h3>Fields</h3>
 * <table>
 *   <tr><th>Field</th><th>Type</th><th>Description</th></tr>
 *   <tr><td>type</td><td>String</td><td>Message type (e.g. "guild.sync.push")</td></tr>
 *   <tr><td>source</td><td>String</td><td>Source identifier (extension ID or server name)</td></tr>
 *   <tr><td>target</td><td>String</td><td>Target identifier ("*" = broadcast)</td></tr>
 *   <tr><td>payload</td><td>String</td><td>JSON payload, structured per message type</td></tr>
 *   <tr><td>sequence</td><td>long</td><td>Monotonic sequence number for tracing</td></tr>
 *   <tr><td>timestamp</td><td>long</td><td>Epoch millis when the message was created</td></tr>
 * </table>
 */
public class BungeeMessage {

    @SerializedName("type")
    private String type;

    @SerializedName("source")
    private String source;

    @SerializedName("target")
    private String target;

    @SerializedName("payload")
    private String payload;

    @SerializedName("sequence")
    private long sequence;

    @SerializedName("timestamp")
    private long timestamp;

    /** Transient — not serialized. Set by the channel handler on receipt. */
    private transient String sourceServer;

    // ── Constructors ──────────────────────────────────────────────

    public BungeeMessage() {}

    public BungeeMessage(String type, String source, String target,
                         String payload, long sequence) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.payload = payload;
        this.sequence = sequence;
        this.timestamp = System.currentTimeMillis();
    }

    // ── Accessors ─────────────────────────────────────────────────

    public String getType()           { return type; }
    public void   setType(String t)   { this.type = t; }

    public String getSource()         { return source; }
    public void   setSource(String s) { this.source = s; }

    public String getTarget()         { return target; }
    public void   setTarget(String t) { this.target = t; }

    public String getPayload()        { return payload; }
    public void   setPayload(String p) { this.payload = p; }

    public long   getSequence()       { return sequence; }
    public void   setSequence(long s) { this.sequence = s; }

    public long   getTimestamp()      { return timestamp; }
    public void   setTimestamp(long t) { this.timestamp = t; }

    /** @return the source server name (transient, set by handler). */
    public String getSourceServer()          { return sourceServer; }
    public void   setSourceServer(String s)  { this.sourceServer = s; }

    /** Returns true if this message is targeted at all servers. */
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

        public BungeeMessage build() {
            return new BungeeMessage(type, source, target, payload, sequence);
        }
    }

    // ── Object ────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("BungeeMessage[#%d %s → %s type=%s]",
                sequence, source, target, type);
    }
}
