package com.guild.war.model;

import java.util.UUID;

/** 结算用参赛者不可变快照。 */
public final class WarParticipantSnapshot {

    private final UUID uuid;
    private final String name;
    private final WarTeamSide side;
    private final int guildId;
    private final int kills;
    private final boolean eliminated;

    public WarParticipantSnapshot(UUID uuid, String name, WarTeamSide side,
                                  int guildId, int kills, boolean eliminated) {
        this.uuid = uuid;
        this.name = name;
        this.side = side;
        this.guildId = guildId;
        this.kills = kills;
        this.eliminated = eliminated;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public WarTeamSide side() {
        return side;
    }

    public int guildId() {
        return guildId;
    }

    public int kills() {
        return kills;
    }

    public boolean eliminated() {
        return eliminated;
    }
}
