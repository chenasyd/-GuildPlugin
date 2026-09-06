package com.guild.war;

import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WarCombatRulesTest {

    private static final UUID CHALLENGER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PLAYER_A1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAYER_A2 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab");
    private static final UUID PLAYER_B1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private WarMatchRegistry registry;
    private WarMatchLifecycle lifecycle;
    private WarBroadcastHelper broadcast;
    private WarCombatRules combat;
    private WarSettings settings;

    @BeforeEach
    void setUp() {
        registry = new WarMatchRegistry();
        lifecycle = mock(WarMatchLifecycle.class);
        broadcast = mock(WarBroadcastHelper.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("guild-war.friendly-fire", false);
        settings = new WarSettings(config);
        combat = new WarCombatRules(null, null, registry, broadcast, lifecycle, () -> settings);
    }

    @Test
    void shouldCancelDamage_blocksSameTeamWhenFriendlyFireDisabled() {
        WarMatch match = activeMatch(VictoryMode.FIRST_TO_SCORE, 10);
        Player allyA = addFighter(match, "AllyA", WarTeamSide.A, PLAYER_A1);
        Player allyB = addFighter(match, "AllyB", WarTeamSide.A, PLAYER_A2);

        assertTrue(combat.shouldCancelDamage(allyA, allyB));
    }

    @Test
    void shouldCancelDamage_allowsEnemyDamageInActivePhase() {
        WarMatch match = activeMatch(VictoryMode.FIRST_TO_SCORE, 10);
        Player attacker = addFighter(match, "Attacker", WarTeamSide.A, PLAYER_A1);
        Player victim = addFighter(match, "Victim", WarTeamSide.B, PLAYER_B1);

        assertFalse(combat.shouldCancelDamage(attacker, victim));
    }

    @Test
    void shouldCancelDamage_cancelsOutsideActivePhase() {
        WarMatch match = activeMatch(VictoryMode.FIRST_TO_SCORE, 10);
        match.setPhase(WarPhase.COUNTDOWN);
        Player attacker = addFighter(match, "Attacker", WarTeamSide.A, PLAYER_A1);
        Player victim = addFighter(match, "Victim", WarTeamSide.B, PLAYER_B1);

        assertTrue(combat.shouldCancelDamage(attacker, victim));
    }

    @Test
    void handleKill_firstToScoreEndsMatchWhenThresholdReached() {
        WarMatch match = activeMatch(VictoryMode.FIRST_TO_SCORE, 1);
        Player killer = addFighter(match, "Killer", WarTeamSide.A, PLAYER_A1);
        Player victim = addFighter(match, "Victim", WarTeamSide.B, PLAYER_B1);

        combat.handleKill(killer, victim);

        verify(lifecycle).endMatch(match, match.guildAId(), "war.reason.first-score");
    }

    @Test
    void checkSurviveWin_endsMatchWhenTeamWiped() {
        WarMatch match = activeMatch(VictoryMode.LAST_STANDING, 10);
        addFighter(match, "LastA", WarTeamSide.A, PLAYER_A1);
        addFighter(match, "B1", WarTeamSide.B, PLAYER_B1);
        match.get(PLAYER_A1).setEliminated(true);
        match.get(PLAYER_A1).setAlive(false);

        combat.checkSurviveWin(match);

        verify(lifecycle).endMatch(match, match.guildBId(), "war.reason.wipe");
    }

    @Test
    void handleKill_ignoresWhenNotInActivePhase() {
        WarMatch match = activeMatch(VictoryMode.FIRST_TO_SCORE, 1);
        match.setPhase(WarPhase.SIGNUP);
        Player killer = addFighter(match, "Killer", WarTeamSide.A, PLAYER_A1);
        Player victim = addFighter(match, "Victim", WarTeamSide.B, PLAYER_B1);

        combat.handleKill(killer, victim);

        verifyNoInteractions(lifecycle);
    }

    private WarMatch activeMatch(VictoryMode mode, int scoreToWin) {
        WarMatch match = new WarMatch(
                10, "GuildA", 20, "GuildB",
                CHALLENGER, "preset", mode, 5, scoreToWin, 600);
        match.setPhase(WarPhase.ACTIVE);
        registry.register(match);
        return match;
    }

    private Player addFighter(WarMatch match, String name, WarTeamSide side, UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn(name);
        match.participants().put(uuid, new WarParticipant(uuid, name, side));
        registry.linkPlayer(uuid, match.id());
        return player;
    }
}
