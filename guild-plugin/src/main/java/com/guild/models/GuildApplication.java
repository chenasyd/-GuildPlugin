package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;
import com.guild.core.language.LanguageManager;
import com.guild.GuildPlugin;

/**
 * 工会申请数据模型
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
        PENDING,
        APPROVED,
        REJECTED;

        /**
         * 获取状态显示名称（多语言支持）
         * @param lang 语言代码（如 "zh", "en", "pl"）
         * @return 本地化的显示名称
         */
        public String getDisplayName(String lang) {
            String key = "application.status." + name().toLowerCase();
            LanguageManager languageManager = GuildPlugin.getInstance().getLanguageManager();

            switch (this) {
                case PENDING:
                    return languageManager.getMessage(lang, key, "Pending");
                case APPROVED:
                    return languageManager.getMessage(lang, key, "Approved");
                case REJECTED:
                    return languageManager.getMessage(lang, key, "Rejected");
                default:
                    return name();
            }
        }

        /**
         * 获取状态显示名称（使用默认语言）
         * @return 本地化的显示名称
         */
        public String getDisplayName() {
            return getDisplayName("en");
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
