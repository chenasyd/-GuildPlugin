package com.guild.sdk.data;

import java.util.UUID;

public class PlayerRecordData {
    private final UUID playerUuid;
    private final String playerName;
    private final String recordType;
    private final String reason;
    private final String sourceServer;
    private final String operatorName;
    private final long timestamp;
    private final long expiryTime;

    public PlayerRecordData(UUID playerUuid, String playerName,
                            String recordType, String reason,
                            String sourceServer, String operatorName,
                            long timestamp, long expiryTime) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.recordType = recordType;
        this.reason = reason;
        this.sourceServer = sourceServer;
        this.operatorName = operatorName;
        this.timestamp = timestamp;
        this.expiryTime = expiryTime;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getRecordType() { return recordType; }
    public String getReason() { return reason; }
    public String getSourceServer() { return sourceServer; }
    public String getOperatorName() { return operatorName; }
    public long getTimestamp() { return timestamp; }
    public long getExpiryTime() { return expiryTime; }
    public boolean isPermanent() { return expiryTime == -1; }
}
