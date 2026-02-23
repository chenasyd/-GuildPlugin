package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;
import com.guild.core.language.LanguageManager;
import com.guild.GuildPlugin;

/**
 * 工会邀请数据模型
 */
public class GuildInvitation {
    
    private int id;
    private int guildId;
    private UUID inviterUuid;
    private String inviterName;
    private UUID targetUuid;
    private String targetName;
    private LocalDateTime invitedAt;
    private LocalDateTime expiresAt;
    private InvitationStatus status;
    
    public GuildInvitation() {}
    
    public GuildInvitation(int guildId, UUID inviterUuid, String inviterName, UUID targetUuid, String targetName) {
        this.guildId = guildId;
        this.inviterUuid = inviterUuid;
        this.inviterName = inviterName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.invitedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(30); // 30分钟过期
        this.status = InvitationStatus.PENDING;
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
    
    public UUID getInviterUuid() {
        return inviterUuid;
    }
    
    public void setInviterUuid(UUID inviterUuid) {
        this.inviterUuid = inviterUuid;
    }
    
    public String getInviterName() {
        return inviterName;
    }
    
    public void setInviterName(String inviterName) {
        this.inviterName = inviterName;
    }
    
    public UUID getTargetUuid() {
        return targetUuid;
    }
    
    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }
    
    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }
    
    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public InvitationStatus getStatus() {
        return status;
    }
    
    public void setStatus(InvitationStatus status) {
        this.status = status;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 邀请状态枚举
     */
    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED;

        /**
         * 获取邀请状态显示名称（多语言支持）
         * @param lang 语言代码（如 "zh", "en", "pl"）
         * @return 本地化的显示名称
         */
        public String getDisplayName(String lang) {
            String key = "invitation.status." + name().toLowerCase();
            LanguageManager languageManager = GuildPlugin.getInstance().getLanguageManager();

            switch (this) {
                case PENDING:
                    return languageManager.getMessage(lang, key, "Pending");
                case ACCEPTED:
                    return languageManager.getMessage(lang, key, "Accepted");
                case DECLINED:
                    return languageManager.getMessage(lang, key, "Declined");
                case EXPIRED:
                    return languageManager.getMessage(lang, key, "Expired");
                default:
                    return name();
            }
        }

        /**
         * 获取邀请状态显示名称（使用默认语言）
         * @return 本地化的显示名称
         */
        public String getDisplayName() {
            return getDisplayName("en");
        }
    }
    
    @Override
    public String toString() {
        return "GuildInvitation{" +
                "id=" + id +
                ", guildId=" + guildId +
                ", inviterUuid=" + inviterUuid +
                ", inviterName='" + inviterName + '\'' +
                ", targetUuid=" + targetUuid +
                ", targetName='" + targetName + '\'' +
                ", invitedAt=" + invitedAt +
                ", expiresAt=" + expiresAt +
                ", status=" + status +
                '}';
    }
}
