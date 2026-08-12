package com.guild.war.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 对局结算不可变快照（事件 / 落库 / API 共用）。 */
public final class WarReportSnapshot {

    private final int runtimeMatchId;
    private final Integer reportId;
    private final int guildAId;
    private final String guildAName;
    private final int guildBId;
    private final String guildBName;
    private final Integer winnerGuildId;
    private final VictoryMode mode;
    private final int scoreA;
    private final int scoreB;
    private final int scoreToWin;
    private final String presetName;
    private final String endReason;
    private final long createdAt;
    private final long startedAt;
    private final long endedAt;
    private final String seasonId;
    private final List<WarParticipantSnapshot> participants;

    public WarReportSnapshot(int runtimeMatchId, Integer reportId,
                             int guildAId, String guildAName,
                             int guildBId, String guildBName,
                             Integer winnerGuildId, VictoryMode mode,
                             int scoreA, int scoreB, int scoreToWin,
                             String presetName, String endReason,
                             long createdAt, long startedAt, long endedAt,
                             String seasonId,
                             List<WarParticipantSnapshot> participants) {
        this.runtimeMatchId = runtimeMatchId;
        this.reportId = reportId;
        this.guildAId = guildAId;
        this.guildAName = guildAName;
        this.guildBId = guildBId;
        this.guildBName = guildBName;
        this.winnerGuildId = winnerGuildId;
        this.mode = mode;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.scoreToWin = scoreToWin;
        this.presetName = presetName;
        this.endReason = endReason;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.seasonId = seasonId;
        this.participants = Collections.unmodifiableList(participants);
    }

    public static WarReportSnapshot fromMatch(WarMatch match, String seasonId) {
        long ended = System.currentTimeMillis();
        long started = match.startedAt() > 0 ? match.startedAt() : match.createdAt();
        List<WarParticipantSnapshot> list = match.participantList().stream()
                .map(p -> new WarParticipantSnapshot(
                        p.uuid(), p.name(), p.side(),
                        match.guildIdOf(p.side()),
                        p.getKills(), p.isEliminated()))
                .toList();
        return new WarReportSnapshot(
                match.id(), null,
                match.guildAId(), match.guildAName(),
                match.guildBId(), match.guildBName(),
                match.winnerGuildId(), match.mode(),
                match.scoreA(), match.scoreB(), match.scoreToWin(),
                match.presetName(), match.endReason(),
                match.createdAt(), started, ended,
                seasonId != null ? seasonId : "",
                list);
    }

    public WarReportSnapshot withReportId(int id) {
        return new WarReportSnapshot(
                runtimeMatchId, id,
                guildAId, guildAName, guildBId, guildBName,
                winnerGuildId, mode, scoreA, scoreB, scoreToWin,
                presetName, endReason, createdAt, startedAt, endedAt,
                seasonId, List.copyOf(participants));
    }

    public int runtimeMatchId() {
        return runtimeMatchId;
    }

    public Integer reportId() {
        return reportId;
    }

    public int guildAId() {
        return guildAId;
    }

    public String guildAName() {
        return guildAName;
    }

    public int guildBId() {
        return guildBId;
    }

    public String guildBName() {
        return guildBName;
    }

    public Integer winnerGuildId() {
        return winnerGuildId;
    }

    public VictoryMode mode() {
        return mode;
    }

    public int scoreA() {
        return scoreA;
    }

    public int scoreB() {
        return scoreB;
    }

    public int scoreToWin() {
        return scoreToWin;
    }

    public String presetName() {
        return presetName;
    }

    public String endReason() {
        return endReason;
    }

    public long createdAt() {
        return createdAt;
    }

    public long startedAt() {
        return startedAt;
    }

    public long endedAt() {
        return endedAt;
    }

    public long durationMs() {
        return Math.max(0L, endedAt - startedAt);
    }

    public String seasonId() {
        return seasonId;
    }

    public List<WarParticipantSnapshot> participants() {
        return participants;
    }

    public boolean involvesPlayer(UUID uuid) {
        for (WarParticipantSnapshot p : participants) {
            if (p.uuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public String winnerName() {
        if (winnerGuildId == null) {
            return "DRAW";
        }
        if (winnerGuildId == guildAId) {
            return guildAName;
        }
        if (winnerGuildId == guildBId) {
            return guildBName;
        }
        return String.valueOf(winnerGuildId);
    }
}
