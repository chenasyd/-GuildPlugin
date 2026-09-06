package com.guild.war.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarMatchTest {

    private static final UUID CHALLENGER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PLAYER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAYER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void newMatch_startsInPendingPhase() {
        WarMatch match = createMatch();
        assertEquals(WarPhase.PENDING, match.phase());
        assertEquals(0, match.scoreA());
        assertEquals(0, match.scoreB());
    }

    @Test
    void sideOfGuild_resolvesTeamByGuildId() {
        WarMatch match = createMatch();
        assertEquals(WarTeamSide.A, match.sideOfGuild(10));
        assertEquals(WarTeamSide.B, match.sideOfGuild(20));
        assertNull(match.sideOfGuild(99));
    }

    @Test
    void involvesGuild_trueForEitherTeam() {
        WarMatch match = createMatch();
        assertTrue(match.involvesGuild(10));
        assertTrue(match.involvesGuild(20));
        assertFalse(match.involvesGuild(30));
    }

    @Test
    void countSide_andBothTeamsHavePlayers() {
        WarMatch match = createMatch();
        match.participants().put(PLAYER_A, new WarParticipant(PLAYER_A, "Alpha", WarTeamSide.A));
        assertEquals(1, match.countSide(WarTeamSide.A));
        assertFalse(match.bothTeamsHavePlayers());

        match.participants().put(PLAYER_B, new WarParticipant(PLAYER_B, "Beta", WarTeamSide.B));
        assertEquals(1, match.countSide(WarTeamSide.B));
        assertTrue(match.bothTeamsHavePlayers());
    }

    @Test
    void aliveCount_excludesEliminatedAndSpectating() {
        WarMatch match = createMatch();
        WarParticipant fighting = new WarParticipant(PLAYER_A, "Fighter", WarTeamSide.A);
        WarParticipant eliminated = new WarParticipant(PLAYER_B, "Out", WarTeamSide.A);
        eliminated.setEliminated(true);
        WarParticipant spectator = new WarParticipant(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "Spec", WarTeamSide.A);
        spectator.setSpectating(true);

        match.participants().put(fighting.uuid(), fighting);
        match.participants().put(eliminated.uuid(), eliminated);
        match.participants().put(spectator.uuid(), spectator);

        assertEquals(1, match.aliveCount(WarTeamSide.A));
    }

    @Test
    void addScore_updatesTeamTotals() {
        WarMatch match = createMatch();
        match.addScore(WarTeamSide.A, 3);
        match.addScore(WarTeamSide.B, 1);
        match.addScore(WarTeamSide.A, 2);
        assertEquals(5, match.scoreA());
        assertEquals(1, match.scoreB());
    }

    @Test
    void guildIdAndNameOf_mapBySide() {
        WarMatch match = createMatch();
        assertEquals(10, match.guildIdOf(WarTeamSide.A));
        assertEquals(20, match.guildIdOf(WarTeamSide.B));
        assertEquals("GuildA", match.guildNameOf(WarTeamSide.A));
        assertEquals("GuildB", match.guildNameOf(WarTeamSide.B));
    }

    private static WarMatch createMatch() {
        return new WarMatch(
                10, "GuildA",
                20, "GuildB",
                CHALLENGER,
                "default",
                VictoryMode.FIRST_TO_SCORE,
                5, 10, 600);
    }
}
