package com.guild.bungee.data;

import com.google.gson.annotations.SerializedName;

/**
 * Data payload for cross-server guild synchronization.
 *
 * <p>Used within {@link BungeeMessage#payload} for sync-related operations.
 * Serialized as JSON and transmitted via the Plugin Messaging Channel.
 *
 * <h3>Fields</h3>
 * <table>
 *   <tr><th>Field</th><th>Description</th></tr>
 *   <tr><td>guildId</td><td>The guild ID being synchronized</td></tr>
 *   <tr><td>sourceServer</td><td>Name of the server that originated the sync</td></tr>
 *   <tr><td>targetServer</td><td>Name of the target server (for directed requests)</td></tr>
 *   <tr><td>data</td><td>Guild data as a JSON string</td></tr>
 *   <tr><td>timestamp</td><td>Epoch millis of the sync event</td></tr>
 * </table>
 */
public class SyncPayload {

    @SerializedName("guildId")
    private int guildId;

    @SerializedName("sourceServer")
    private String sourceServer;

    @SerializedName("targetServer")
    private String targetServer;

    @SerializedName("data")
    private String data;

    @SerializedName("timestamp")
    private long timestamp;

    // ── Constructors ──────────────────────────────────────────────

    public SyncPayload() {
        this.timestamp = System.currentTimeMillis();
    }

    public SyncPayload(int guildId, String sourceServer, String data) {
        this.guildId = guildId;
        this.sourceServer = sourceServer;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ── Accessors ─────────────────────────────────────────────────

    public int    getGuildId()              { return guildId; }
    public void   setGuildId(int id)        { this.guildId = id; }

    public String getSourceServer()         { return sourceServer; }
    public void   setSourceServer(String s) { this.sourceServer = s; }

    public String getTargetServer()         { return targetServer; }
    public void   setTargetServer(String s) { this.targetServer = s; }

    public String getData()                 { return data; }
    public void   setData(String d)         { this.data = d; }

    public long   getTimestamp()            { return timestamp; }
    public void   setTimestamp(long t)      { this.timestamp = t; }

    // ── Object ────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("SyncPayload[guild=%d %s → %s]",
                guildId, sourceServer,
                targetServer != null ? targetServer : "broadcast");
    }
}
