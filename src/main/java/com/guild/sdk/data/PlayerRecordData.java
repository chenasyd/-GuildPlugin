package com.guild.sdk.data;

import java.util.UUID;

/**
 * 玩家记录数据（通用 DTO，可用于外部 API 数据对接）
 * <p>
 * 例如：从 NDPR 联合封禁系统获取的玩家黑历史记录。
 */
public class PlayerRecordData {

    private final UUID playerUuid;
    private final String playerName;
    private final String recordType;     // 如: BAN, KICK, WARN
    private final String reason;
    private final String sourceServer;   // 来源服务器
    private final String operatorName;   // 操作者
    private final long timestamp;
    private final long expiryTime;       // -1 表示永久

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

    @Override
    public String toString() {
        return "PlayerRecordData{player=" + playerName +
                ", type=" + recordType + ", reason='" + reason + "'}";
    }
}
