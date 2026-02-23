package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;
import com.guild.core.language.LanguageManager;
import com.guild.GuildPlugin;

/**
 * 工会成员数据模型
 */
public class GuildMember {
    
    private int id;
    private int guildId;
    private UUID playerUuid;
    private String playerName;
    private Role role;
    private LocalDateTime joinedAt;
    
    public GuildMember() {}
    
    public GuildMember(int guildId, UUID playerUuid, String playerName, Role role) {
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
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
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    /**
     * 工会成员角色枚举
     */
    public enum Role {
        LEADER,
        OFFICER,
        MEMBER;

        /**
         * 获取角色显示名称（多语言支持）
         * @param lang 语言代码（如 "zh", "en", "pl"）
         * @return 本地化的显示名称
         */
        public String getDisplayName(String lang) {
            String key = "placeholder.role-" + name().toLowerCase();
            LanguageManager languageManager = GuildPlugin.getInstance().getLanguageManager();

            switch (this) {
                case LEADER:
                    return languageManager.getMessage(lang, key, "Leader");
                case OFFICER:
                    return languageManager.getMessage(lang, key, "Officer");
                case MEMBER:
                    return languageManager.getMessage(lang, key, "Member");
                default:
                    return name();
            }
        }

        /**
         * 获取角色显示名称（使用默认语言）
         * @return 本地化的显示名称
         */
        public String getDisplayName() {
            return getDisplayName("en");
        }

        public boolean canInvite() {
            return this == LEADER || this == OFFICER;
        }

        public boolean canKick() {
            return this == LEADER || this == OFFICER;
        }

        public boolean canPromote() {
            return this == LEADER;
        }

        public boolean canDemote() {
            return this == LEADER;
        }

        public boolean canDeleteGuild() {
            return this == LEADER;
        }
    }
    
    @Override
    public String toString() {
        return "GuildMember{" +
                "id=" + id +
                ", guildId=" + guildId +
                ", playerUuid=" + playerUuid +
                ", playerName='" + playerName + '\'' +
                ", role=" + role +
                ", joinedAt=" + joinedAt +
                '}';
    }


}
