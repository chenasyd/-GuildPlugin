package com.guild.war.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarParticipantTest {

    private static final UUID UUID_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void isFighting_trueWhenNotEliminatedAndNotSpectating() {
        WarParticipant p = new WarParticipant(UUID_A, "Player", WarTeamSide.A);
        assertTrue(p.isFighting());
    }

    @Test
    void isFighting_falseWhenEliminatedOrSpectating() {
        WarParticipant eliminated = new WarParticipant(UUID_A, "Player", WarTeamSide.A);
        eliminated.setEliminated(true);
        assertFalse(eliminated.isFighting());

        WarParticipant spectator = new WarParticipant(UUID_A, "Player", WarTeamSide.B);
        spectator.setSpectating(true);
        assertFalse(spectator.isFighting());
    }

    @Test
    void addKill_incrementsKillCount() {
        WarParticipant p = new WarParticipant(UUID_A, "Player", WarTeamSide.A);
        assertEquals(0, p.getKills());
        p.addKill();
        p.addKill();
        assertEquals(2, p.getKills());
    }
}
