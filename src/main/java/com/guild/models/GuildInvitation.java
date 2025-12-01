package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model danych zaproszenia do gildii
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
        this.expiresAt = LocalDateTime.now().plusMinutes(30); // wygasa za 30 minut
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
     * Enumeracja statusu zaproszenia
     */
    public enum InvitationStatus {
        PENDING("Oczekujące"),
        ACCEPTED("Zaakceptowane"),
        DECLINED("Odrzucone"),
        EXPIRED("Wygasłe");

        private final String displayName;

        InvitationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
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
