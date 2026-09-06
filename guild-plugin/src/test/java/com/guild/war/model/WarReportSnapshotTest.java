package com.guild.war.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarReportSnapshotTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void fromMatch_capturesScoresParticipantsAndTiming() {
        WarMatch match = new WarMatch(
                1, "Alpha", 2, "Beta",
                UUID.randomUUID(), "arena1", VictoryMode.TIMED_SCORE,
                5, 15, 300);
        match.setPhase(WarPhase.ACTIVE);
        match.setStartedAt(1_000L);
        match.setWinnerGuildId(1);
        match.setEndReason("war.reason.timed-win");
        match.addScore(WarTeamSide.A, 12);
        match.addScore(WarTeamSide.B, 8);

        WarParticipant participant = new WarParticipant(PLAYER, "Hero", WarTeamSide.A);
        participant.addKill();
        participant.setEliminated(false);
        match.participants().put(PLAYER, participant);

        WarReportSnapshot snap = WarReportSnapshot.fromMatch(match, "season-1");

        assertEquals(match.id(), snap.runtimeMatchId());
        assertEquals(1, snap.guildAId());
        assertEquals(2, snap.guildBId());
        assertEquals(12, snap.scoreA());
        assertEquals(8, snap.scoreB());
        assertEquals(15, snap.scoreToWin());
        assertEquals(VictoryMode.TIMED_SCORE, snap.mode());
        assertEquals("war.reason.timed-win", snap.endReason());
        assertEquals("season-1", snap.seasonId());
        assertEquals(1, snap.participants().size());
        assertEquals(PLAYER, snap.participants().get(0).uuid());
        assertEquals(1, snap.participants().get(0).kills());
        assertTrue(snap.involvesPlayer(PLAYER));
        assertFalse(snap.involvesPlayer(UUID.randomUUID()));
        assertTrue(snap.endedAt() >= snap.startedAt());
    }

    @Test
    void winnerName_returnsDrawOrGuildName() {
        WarMatch match = createMinimalMatch();
        match.setWinnerGuildId(null);
        assertEquals("DRAW", WarReportSnapshot.fromMatch(match, "default").winnerName());

        match.setWinnerGuildId(1);
        assertEquals("Alpha", WarReportSnapshot.fromMatch(match, "default").winnerName());

        match.setWinnerGuildId(2);
        assertEquals("Beta", WarReportSnapshot.fromMatch(match, "default").winnerName());
    }

    @Test
    void withReportId_preservesFields() {
        WarReportSnapshot base = WarReportSnapshot.fromMatch(createMinimalMatch(), "default");
        WarReportSnapshot saved = base.withReportId(42);
        assertEquals(42, saved.reportId());
        assertEquals(base.runtimeMatchId(), saved.runtimeMatchId());
        assertEquals(base.guildAName(), saved.guildAName());
    }

    @Test
    void durationMs_nonNegative() {
        WarMatch match = createMinimalMatch();
        match.setStartedAt(5_000L);
        WarReportSnapshot snap = WarReportSnapshot.fromMatch(match, "default");
        assertTrue(snap.durationMs() >= 0);
    }

    private static WarMatch createMinimalMatch() {
        return new WarMatch(
                1, "Alpha", 2, "Beta",
                UUID.randomUUID(), "arena1", VictoryMode.FIRST_TO_SCORE,
                5, 10, 600);
    }
}
