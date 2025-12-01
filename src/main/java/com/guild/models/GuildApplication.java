package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model danych aplikacji do gildii
 */
public class GuildApplication {
    
    private int id;
    private int guildId;
    private UUID playerUuid;
    private String playerName;
    private String message;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
    
    public GuildApplication() {}
    
    public GuildApplication(int guildId, UUID playerUuid, String playerName, String message) {
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.message = message;
        this.status = ApplicationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getGuildId() {
        return guildId;
    }
    
    public void setGuildId(int guildId) {
        this.guildId = guildId;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public ApplicationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 申请状态枚举
     */
    public enum ApplicationStatus {
        PENDING("待处理"),
        APPROVED("已通过"),
        REJECTED("已拒绝");
        
        private final String displayName;
        
        ApplicationStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    @Override
    public String toString() {
        return "GuildApplication{" +
                "id=" + id +
                ", guildId=" + guildId +
                ", playerUuid=" + playerUuid +
                ", playerName='" + playerName + '\'' +
                ", message='" + message + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
