package com.guild.war.model;

import java.util.UUID;

/** 单场参赛者状态。 */
public final class WarParticipant {
    private final UUID uuid;
    private final String name;
    private final WarTeamSide side;
    private volatile boolean alive = true;
    private volatile boolean eliminated = false;
    private volatile boolean spectating = false;
    private int kills;

    public WarParticipant(UUID uuid, String name, WarTeamSide side) {
        this.uuid = uuid;
        this.name = name;
        this.side = side;
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

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public boolean isSpectating() {
        return spectating;
    }

    public void setSpectating(boolean spectating) {
        this.spectating = spectating;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }

    public boolean isFighting() {
        return !eliminated && !spectating;
    }
}
