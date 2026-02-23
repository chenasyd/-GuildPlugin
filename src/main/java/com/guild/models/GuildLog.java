package com.guild.models;

import java.time.LocalDateTime;
import com.guild.core.language.LanguageManager;
import com.guild.GuildPlugin;

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
        GUILD_CREATED,
        GUILD_DISSOLVED,
        GUILD_RENAMED,
        MEMBER_JOINED,
        MEMBER_LEFT,
        MEMBER_KICKED,
        MEMBER_PROMOTED,
        MEMBER_DEMOTED,
        LEADER_TRANSFERRED,
        FUND_DEPOSITED,
        FUND_WITHDRAWN,
        FUND_TRANSFERRED,
        RELATION_CREATED,
        RELATION_DELETED,
        RELATION_ACCEPTED,
        RELATION_REJECTED,
        GUILD_FROZEN,
        GUILD_UNFROZEN,
        GUILD_LEVEL_UP,
        APPLICATION_SUBMITTED,
        APPLICATION_ACCEPTED,
        APPLICATION_REJECTED,
        INVITATION_SENT,
        INVITATION_ACCEPTED,
        INVITATION_REJECTED;

        /**
         * 获取日志类型显示名称（多语言支持）
         * @param lang 语言代码（如 "zh", "en", "pl"）
         * @return 本地化的显示名称
         */
        public String getDisplayName(String lang) {
            String key = "log.type." + name().toLowerCase();
            LanguageManager languageManager = GuildPlugin.getInstance().getLanguageManager();

            switch (this) {
                case GUILD_CREATED:
                    return languageManager.getMessage(lang, key, "Guild Created");
                case GUILD_DISSOLVED:
                    return languageManager.getMessage(lang, key, "Guild Dissolved");
                case GUILD_RENAMED:
                    return languageManager.getMessage(lang, key, "Guild Renamed");
                case MEMBER_JOINED:
                    return languageManager.getMessage(lang, key, "Member Joined");
                case MEMBER_LEFT:
                    return languageManager.getMessage(lang, key, "Member Left");
                case MEMBER_KICKED:
                    return languageManager.getMessage(lang, key, "Member Kicked");
                case MEMBER_PROMOTED:
                    return languageManager.getMessage(lang, key, "Member Promoted");
                case MEMBER_DEMOTED:
                    return languageManager.getMessage(lang, key, "Member Demoted");
                case LEADER_TRANSFERRED:
                    return languageManager.getMessage(lang, key, "Leader Transferred");
                case FUND_DEPOSITED:
                    return languageManager.getMessage(lang, key, "Fund Deposited");
                case FUND_WITHDRAWN:
                    return languageManager.getMessage(lang, key, "Fund Withdrawn");
                case FUND_TRANSFERRED:
                    return languageManager.getMessage(lang, key, "Fund Transferred");
                case RELATION_CREATED:
                    return languageManager.getMessage(lang, key, "Relation Created");
                case RELATION_DELETED:
                    return languageManager.getMessage(lang, key, "Relation Deleted");
                case RELATION_ACCEPTED:
                    return languageManager.getMessage(lang, key, "Relation Accepted");
                case RELATION_REJECTED:
                    return languageManager.getMessage(lang, key, "Relation Rejected");
                case GUILD_FROZEN:
                    return languageManager.getMessage(lang, key, "Guild Frozen");
                case GUILD_UNFROZEN:
                    return languageManager.getMessage(lang, key, "Guild Unfrozen");
                case GUILD_LEVEL_UP:
                    return languageManager.getMessage(lang, key, "Guild Level Up");
                case APPLICATION_SUBMITTED:
                    return languageManager.getMessage(lang, key, "Application Submitted");
                case APPLICATION_ACCEPTED:
                    return languageManager.getMessage(lang, key, "Application Accepted");
                case APPLICATION_REJECTED:
                    return languageManager.getMessage(lang, key, "Application Rejected");
                case INVITATION_SENT:
                    return languageManager.getMessage(lang, key, "Invitation Sent");
                case INVITATION_ACCEPTED:
                    return languageManager.getMessage(lang, key, "Invitation Accepted");
                case INVITATION_REJECTED:
                    return languageManager.getMessage(lang, key, "Invitation Rejected");
                default:
                    return name();
            }
        }

        /**
         * 获取日志类型显示名称（使用默认语言）
         * @return 本地化的显示名称
         */
        public String getDisplayName() {
            return getDisplayName("en");
        }
    }

    /**
     * 获取格式化的时间字符串
     */
    public String getFormattedTime() {
        if (createdAt == null) return "Unknown";
        return createdAt.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }

    /**
     * 获取简化的时间字符串（用于显示，多语言支持）
     * @param lang 语言代码（如 "zh", "en", "pl"）
     * @return 本地化的相对时间
     */
    public String getSimpleTime(String lang) {
        if (createdAt == null) {
            return getLogTimeMessage(lang, "unknown", "Unknown");
        }
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(createdAt, now);

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return getLogTimeMessage(lang, "days-ago", "{0} days ago", String.valueOf(days));
        } else if (hours > 0) {
            return getLogTimeMessage(lang, "hours-ago", "{0} hours ago", String.valueOf(hours));
        } else if (minutes > 0) {
            return getLogTimeMessage(lang, "minutes-ago", "{0} minutes ago", String.valueOf(minutes));
        } else {
            return getLogTimeMessage(lang, "just-now", "Just now");
        }
    }

    /**
     * 获取简化的时间字符串（用于显示，使用默认语言）
     * @return 本地化的相对时间
     */
    public String getSimpleTime() {
        return getSimpleTime("en");
    }

    /**
     * 获取时间相关消息
     */
    private String getLogTimeMessage(String lang, String key, String defaultValue, String... args) {
        LanguageManager languageManager = GuildPlugin.getInstance().getLanguageManager();
        return languageManager.getMessage(lang, "log.time." + key, defaultValue, args);
    }
}
