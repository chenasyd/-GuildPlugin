package com.guild.war.model;

import org.bukkit.Location;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 一场工会战对局。 */
public final class WarMatch {

    private static final AtomicInteger ID_SEQ = new AtomicInteger(1);

    private final int id;
    private final int guildAId;
    private final int guildBId;
    private final String guildAName;
    private final String guildBName;
    private final UUID challengerUuid;
    private final String presetName;
    private final VictoryMode mode;
    private final int maxPerTeam;
    private final int scoreToWin;
    private final int durationSeconds;

    private volatile WarPhase phase = WarPhase.PENDING;
    private volatile String worldName;
    private volatile Location spawnA;
    private volatile Location spawnB;
    private volatile Location spectatorSpawn;
    private volatile int scoreA;
    private volatile int scoreB;
    private volatile Integer winnerGuildId;
    private volatile String endReason;
    private volatile boolean teamAReady;
    private volatile boolean teamBReady;

    private final Map<UUID, WarParticipant> participants = new ConcurrentHashMap<>();
    private final long createdAt = System.currentTimeMillis();

    public WarMatch(int guildAId, String guildAName, int guildBId, String guildBName,
                    UUID challengerUuid, String presetName, VictoryMode mode,
                    int maxPerTeam, int scoreToWin, int durationSeconds) {
        this.id = ID_SEQ.getAndIncrement();
        this.guildAId = guildAId;
        this.guildAName = guildAName;
        this.guildBId = guildBId;
        this.guildBName = guildBName;
        this.challengerUuid = challengerUuid;
        this.presetName = presetName;
        this.mode = mode;
        this.maxPerTeam = maxPerTeam;
        this.scoreToWin = scoreToWin;
        this.durationSeconds = durationSeconds;
    }

    public int id() {
        return id;
    }

    public int guildAId() {
        return guildAId;
    }

    public int guildBId() {
        return guildBId;
    }

    public String guildAName() {
        return guildAName;
    }

    public String guildBName() {
        return guildBName;
    }

    public UUID challengerUuid() {
        return challengerUuid;
    }

    public String presetName() {
        return presetName;
    }

    public VictoryMode mode() {
        return mode;
    }

    public int maxPerTeam() {
        return maxPerTeam;
    }

    public int scoreToWin() {
        return scoreToWin;
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public WarPhase phase() {
        return phase;
    }

    public void setPhase(WarPhase phase) {
        this.phase = phase;
    }

    public String worldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Location spawnA() {
        return spawnA;
    }

    public void setSpawnA(Location spawnA) {
        this.spawnA = spawnA;
    }

    public Location spawnB() {
        return spawnB;
    }

    public void setSpawnB(Location spawnB) {
        this.spawnB = spawnB;
    }

    public Location spectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public int scoreA() {
        return scoreA;
    }

    public int scoreB() {
        return scoreB;
    }

    public synchronized void addScore(WarTeamSide side, int amount) {
        if (side == WarTeamSide.A) {
            scoreA += amount;
        } else {
            scoreB += amount;
        }
    }

    public Integer winnerGuildId() {
        return winnerGuildId;
    }

    public void setWinnerGuildId(Integer winnerGuildId) {
        this.winnerGuildId = winnerGuildId;
    }

    public String endReason() {
        return endReason;
    }

    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }

    public boolean isTeamAReady() {
        return teamAReady;
    }

    public void setTeamAReady(boolean teamAReady) {
        this.teamAReady = teamAReady;
    }

    public boolean isTeamBReady() {
        return teamBReady;
    }

    public void setTeamBReady(boolean teamBReady) {
        this.teamBReady = teamBReady;
    }

    public long createdAt() {
        return createdAt;
    }

    public Map<UUID, WarParticipant> participants() {
        return participants;
    }

    public Collection<WarParticipant> participantList() {
        return participants.values();
    }

    public WarParticipant get(UUID uuid) {
        return participants.get(uuid);
    }

    public boolean involvesGuild(int guildId) {
        return guildAId == guildId || guildBId == guildId;
    }

    public WarTeamSide sideOfGuild(int guildId) {
        if (guildId == guildAId) {
            return WarTeamSide.A;
        }
        if (guildId == guildBId) {
            return WarTeamSide.B;
        }
        return null;
    }

    public int guildIdOf(WarTeamSide side) {
        return side == WarTeamSide.A ? guildAId : guildBId;
    }

    public String guildNameOf(WarTeamSide side) {
        return side == WarTeamSide.A ? guildAName : guildBName;
    }

    public int countSide(WarTeamSide side) {
        int n = 0;
        for (WarParticipant p : participants.values()) {
            if (p.side() == side) {
                n++;
            }
        }
        return n;
    }

    public int aliveCount(WarTeamSide side) {
        int n = 0;
        for (WarParticipant p : participants.values()) {
            if (p.side() == side && p.isFighting()) {
                n++;
            }
        }
        return n;
    }

    public boolean bothTeamsHavePlayers() {
        return countSide(WarTeamSide.A) > 0 && countSide(WarTeamSide.B) > 0;
    }
}
