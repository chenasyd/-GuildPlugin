package com.guild.sdk.event;

import java.util.UUID;

public class EconomyEventData {
    private final int guildId;
    private final String guildName;
    private final UUID playerUuid;
    private final String playerName;
    private final double amount;
    private final String eventType; // DEPOSIT, WITHDRAW

    public EconomyEventData(int guildId, String guildName, UUID playerUuid, String playerName, double amount, String eventType) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.amount = amount;
        this.eventType = eventType;
    }

    public int getGuildId() {
        return guildId;
    }

    public String getGuildName() {
        return guildName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public double getAmount() {
        return amount;
    }

    public String getEventType() {
        return eventType;
    }
}