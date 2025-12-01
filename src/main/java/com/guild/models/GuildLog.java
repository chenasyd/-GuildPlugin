package com.guild.models;

import java.time.LocalDateTime;

/**
 * Model dziennika gildii
 * Służy do rejestrowania historii różnych operacji gildii
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
     * Enumeracja typu dziennika
     */
    public enum LogType {
        GUILD_CREATED("Utworzenie gildii"),
        GUILD_DISSOLVED("Rozwiązanie gildii"),
        GUILD_RENAMED("Zmiana nazwy gildii"),
        MEMBER_JOINED("Dołączenie członka"),
        MEMBER_LEFT("Opuszczenie członka"),
        MEMBER_KICKED("Wyrzucenie członka"),
        MEMBER_PROMOTED("Awans członka"),
        MEMBER_DEMOTED("Degradacja członka"),
        LEADER_TRANSFERRED("Przekazanie lidera"),
        FUND_DEPOSITED("Wpłata funduszy"),
        FUND_WITHDRAWN("Wypłata funduszy"),
        FUND_TRANSFERRED("Przelew funduszy"),
        RELATION_CREATED("Utworzenie relacji"),
        RELATION_DELETED("Usunięcie relacji"),
        RELATION_ACCEPTED("Akceptacja relacji"),
        RELATION_REJECTED("Odrzucenie relacji"),
        GUILD_FROZEN("Zamrożenie gildii"),
        GUILD_UNFROZEN("Odmrożenie gildii"),
        GUILD_LEVEL_UP("Awans gildii"),
        APPLICATION_SUBMITTED("Złożenie aplikacji"),
        APPLICATION_ACCEPTED("Akceptacja aplikacji"),
        APPLICATION_REJECTED("Odrzucenie aplikacji"),
        INVITATION_SENT("Wysłanie zaproszenia"),
        INVITATION_ACCEPTED("Akceptacja zaproszenia"),
        INVITATION_REJECTED("Odrzucenie zaproszenia");

        private final String displayName;

        LogType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Pobierz sformatowany ciąg czasu
     */
    public String getFormattedTime() {
        if (createdAt == null) return "Nieznany";
        return createdAt.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }

    /**
     * Pobierz uproszczony ciąg czasu (do wyświetlania)
     */
    public String getSimpleTime() {
        if (createdAt == null) return "Nieznany";
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(createdAt, now);

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return days + " dni temu";
        } else if (hours > 0) {
            return hours + " godz. temu";
        } else if (minutes > 0) {
            return minutes + " min. temu";
        } else {
            return "Przed chwilą";
        }
    }
}
