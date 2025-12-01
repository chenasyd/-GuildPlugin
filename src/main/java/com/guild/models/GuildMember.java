package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model danych członka gildii
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
        LEADER("会长"),
        OFFICER("官员"),
        MEMBER("成员");
        
        private final String displayName;
        
        Role(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
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
