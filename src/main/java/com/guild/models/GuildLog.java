package com.guild.models;

import java.time.LocalDateTime;

/**
 * 工会日志模型
 * 用于记录工会的各种操作历史
 */
public class GuildLog {
    private int id;
    private int guildId;
    private String guildName;
    private String playerUuid;
    private String playerName;
    private LogType logType;
    private String description;
    private String details;
    private LocalDateTime createdAt;

    public GuildLog() {
    }

    public GuildLog(int guildId, String guildName, String playerUuid, String playerName, 
                   LogType logType, String description, String details) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.logType = logType;
        this.description = description;
        this.details = details;
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

    public String getGuildName() {
        return guildName;
    }

    public void setGuildName(String guildName) {
        this.guildName = guildName;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public LogType getLogType() {
        return logType;
    }

    public void setLogType(LogType logType) {
        this.logType = logType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 日志类型枚举
     */
    public enum LogType {
        GUILD_CREATED("工会创建"),
        GUILD_DISSOLVED("工会解散"),
        GUILD_RENAMED("工会改名"),
        MEMBER_JOINED("成员加入"),
        MEMBER_LEFT("成员退出"),
        MEMBER_KICKED("成员被踢出"),
        MEMBER_PROMOTED("成员升职"),
        MEMBER_DEMOTED("成员降职"),
        LEADER_TRANSFERRED("会长转让"),
        FUND_DEPOSITED("资金存入"),
        FUND_WITHDRAWN("资金取出"),
        FUND_TRANSFERRED("资金转账"),
        RELATION_CREATED("关系建立"),
        RELATION_DELETED("关系删除"),
        RELATION_ACCEPTED("关系接受"),
        RELATION_REJECTED("关系拒绝"),
        GUILD_FROZEN("工会冻结"),
        GUILD_UNFROZEN("工会解冻"),
        GUILD_LEVEL_UP("工会升级"),
        APPLICATION_SUBMITTED("申请提交"),
        APPLICATION_ACCEPTED("申请接受"),
        APPLICATION_REJECTED("申请拒绝"),
        INVITATION_SENT("邀请发送"),
        INVITATION_ACCEPTED("邀请接受"),
        INVITATION_REJECTED("邀请拒绝");

        private final String displayName;

        LogType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 获取格式化的时间字符串
     */
    public String getFormattedTime() {
        if (createdAt == null) return "未知";
        return createdAt.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }

    /**
     * 获取简化的时间字符串（用于显示）
     */
    public String getSimpleTime() {
        if (createdAt == null) return "未知";
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(createdAt, now);
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
            return days + "天前";
        } else if (hours > 0) {
            return hours + "小时前";
        } else if (minutes > 0) {
            return minutes + "分钟前";
        } else {
            return "刚刚";
        }
    }
}
