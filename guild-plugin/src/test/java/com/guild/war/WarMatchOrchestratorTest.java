package com.guild.war;

import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WarMatchOrchestratorTest {

    private static final UUID CHALLENGER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FIGHTER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID FIGHTER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private WarMatchLifecycle lifecycle;
    private WarMatchOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        lifecycle = mock(WarMatchLifecycle.class);
        orchestrator = new WarMatchOrchestrator(
                null, new WarMatchScheduler(null), null, lifecycle, () -> new WarSettings(
                        new org.bukkit.configuration.file.YamlConfiguration()));
    }

    @Test
    void resolveTimedScore_teamAWins() {
        WarMatch match = activeMatch(VictoryMode.TIMED_SCORE);
        match.addScore(WarTeamSide.A, 5);
        match.addScore(WarTeamSide.B, 2);

        orchestrator.resolveTimedScore(match);

        verify(lifecycle).endMatch(match, match.guildAId(), "war.reason.timed-win");
    }

    @Test
    void resolveTimedScore_draw() {
        WarMatch match = activeMatch(VictoryMode.TIMED_SCORE);
        match.addScore(WarTeamSide.A, 3);
        match.addScore(WarTeamSide.B, 3);

        orchestrator.resolveTimedScore(match);

        verify(lifecycle).endMatch(match, null, "war.reason.timed-draw");
    }

    @Test
    void resolveSurviveTimeout_winnerByAliveCount() {
        WarMatch match = activeMatch(VictoryMode.LAST_STANDING);
        match.participants().put(FIGHTER_A, new WarParticipant(FIGHTER_A, "A", WarTeamSide.A));
        match.participants().put(FIGHTER_B, new WarParticipant(FIGHTER_B, "B", WarTeamSide.B));
        match.get(FIGHTER_B).setEliminated(true);
        match.get(FIGHTER_B).setAlive(false);

        orchestrator.resolveSurviveTimeout(match);

        verify(lifecycle).endMatch(match, match.guildAId(), "war.reason.survive-alive");
    }

    @Test
    void resolveSurviveTimeout_tieBreakByKills() {
        WarMatch match = activeMatch(VictoryMode.LAST_STANDING);
        match.participants().put(FIGHTER_A, new WarParticipant(FIGHTER_A, "A", WarTeamSide.A));
        match.participants().put(FIGHTER_B, new WarParticipant(FIGHTER_B, "B", WarTeamSide.B));
        match.get(FIGHTER_A).setEliminated(true);
        match.get(FIGHTER_A).setAlive(false);
        match.get(FIGHTER_B).setEliminated(true);
        match.get(FIGHTER_B).setAlive(false);
        match.addScore(WarTeamSide.B, 4);
        match.addScore(WarTeamSide.A, 1);

        orchestrator.resolveSurviveTimeout(match);

        verify(lifecycle).endMatch(match, match.guildBId(), "war.reason.survive-kills");
    }

    private static WarMatch activeMatch(VictoryMode mode) {
        WarMatch match = new WarMatch(
                10, "GuildA", 20, "GuildB",
                CHALLENGER, "preset", mode, 5, 10, 600);
        match.setPhase(WarPhase.ACTIVE);
        return match;
    }
}
