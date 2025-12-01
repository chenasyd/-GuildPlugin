package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model danych relacji gildii
 */
public class GuildRelation {

    private int id;
    private int guild1Id;
    private int guild2Id;
    private String guild1Name;
    private String guild2Name;
    private RelationType type;
    private RelationStatus status;
    private UUID initiatorUuid;
    private String initiatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    public enum RelationType {
        ALLY("Sojusznik", "&a"),
        ENEMY("Wróg", "&c"),
        WAR("Wojna", "&4"),
        TRUCE("Rozejm", "&e"),
        NEUTRAL("Neutralny", "&7");

        private final String displayName;
        private final String color;

        RelationType(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }

    public enum RelationStatus {
        PENDING("Oczekujące"),
        ACTIVE("Aktywne"),
        EXPIRED("Wygasłe"),
        CANCELLED("Anulowane");

        private final String displayName;

        RelationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public GuildRelation() {}

    public GuildRelation(int guild1Id, int guild2Id, String guild1Name, String guild2Name,
                        RelationType type, UUID initiatorUuid, String initiatorName) {
        this.guild1Id = guild1Id;
        this.guild2Id = guild2Id;
        this.guild1Name = guild1Name;
        this.guild2Name = guild2Name;
        this.type = type;
        this.status = RelationStatus.PENDING;
        this.initiatorUuid = initiatorUuid;
        this.initiatorName = initiatorName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Ustaw czas wygaśnięcia (7 dni później)
        this.expiresAt = LocalDateTime.now().plusDays(7);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGuild1Id() {
        return guild1Id;
    }

    public void setGuild1Id(int guild1Id) {
        this.guild1Id = guild1Id;
    }

    public int getGuild2Id() {
        return guild2Id;
    }

    public void setGuild2Id(int guild2Id) {
        this.guild2Id = guild2Id;
    }

    public String getGuild1Name() {
        return guild1Name;
    }

    public void setGuild1Name(String guild1Name) {
        this.guild1Name = guild1Name;
    }

    public String getGuild2Name() {
        return guild2Name;
    }

    public void setGuild2Name(String guild2Name) {
        this.guild2Name = guild2Name;
    }

    public RelationType getType() {
        return type;
    }

    public void setType(RelationType type) {
        this.type = type;
    }

    public RelationStatus getStatus() {
        return status;
    }

    public void setStatus(RelationStatus status) {
        this.status = status;
    }

    public UUID getInitiatorUuid() {
        return initiatorUuid;
    }

    public void setInitiatorUuid(UUID initiatorUuid) {
        this.initiatorUuid = initiatorUuid;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public void setInitiatorName(String initiatorName) {
        this.initiatorName = initiatorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Sprawdź, czy relacja wygasła
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Sprawdź, czy jest w stanie wojny
     */
    public boolean isWar() {
        return type == RelationType.WAR && status == RelationStatus.ACTIVE;
    }

    /**
     * Uzyskaj ID drugiej gildii
     */
    public int getOtherGuildId(int currentGuildId) {
        return guild1Id == currentGuildId ? guild2Id : guild1Id;
    }

    /**
     * Uzyskaj nazwę drugiej gildii
     */
    public String getOtherGuildName(int currentGuildId) {
        return guild1Id == currentGuildId ? guild2Name : guild1Name;
    }

    @Override
    public String toString() {
        return "GuildRelation{" +
                "id=" + id +
                ", guild1Id=" + guild1Id +
                ", guild2Id=" + guild2Id +
                ", type=" + type +
                ", status=" + status +
                '}';
    }
}
