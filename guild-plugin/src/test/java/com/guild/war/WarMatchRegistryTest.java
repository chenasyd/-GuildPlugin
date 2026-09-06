package com.guild.war;

import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarMatchRegistryTest {

    private static final UUID CHALLENGER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PLAYER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAYER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private WarMatchRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WarMatchRegistry();
    }

    @Test
    void register_indexesMatchAndBothGuilds() {
        WarMatch match = createMatch(10, 20);
        registry.register(match);

        assertSame(match, registry.getMatch(match.id()));
        assertSame(match, registry.getByGuild(10));
        assertSame(match, registry.getByGuild(20));
        assertEquals(1, registry.allMatches().size());
    }

    @Test
    void linkPlayer_resolvesByPlayerUuid() {
        WarMatch match = createMatch(10, 20);
        registry.register(match);
        registry.linkPlayer(PLAYER_A, match.id());

        assertSame(match, registry.getByPlayer(PLAYER_A));
        assertTrue(registry.isPlayerLinked(PLAYER_A));
    }

    @Test
    void unregister_clearsMatchAndGuildIndexes() {
        WarMatch match = createMatch(10, 20);
        registry.register(match);
        registry.linkPlayer(PLAYER_A, match.id());

        registry.unregister(match);

        assertNull(registry.getMatch(match.id()));
        assertNull(registry.getByGuild(10));
        assertNull(registry.getByGuild(20));
        assertTrue(registry.allMatches().isEmpty());
        assertTrue(registry.isPlayerLinked(PLAYER_A));
    }

    @Test
    void unlinkPlayer_removesPlayerIndexOnly() {
        WarMatch match = createMatch(10, 20);
        registry.register(match);
        registry.linkPlayer(PLAYER_A, match.id());

        registry.unlinkPlayer(PLAYER_A);

        assertNull(registry.getByPlayer(PLAYER_A));
        assertFalse(registry.isPlayerLinked(PLAYER_A));
        assertSame(match, registry.getByGuild(10));
    }

    @Test
    void countNonEnded_excludesEndedMatches() {
        WarMatch active = createMatch(10, 20);
        WarMatch ended = createMatch(30, 40);
        ended.setPhase(WarPhase.ENDED);
        registry.register(active);
        registry.register(ended);

        assertEquals(1, registry.countNonEnded());
    }

    @Test
    void isArenaWorld_trueForActiveArenaPhases() {
        WarMatch match = createMatch(10, 20);
        match.setWorldName("war_arena_1");
        match.setPhase(WarPhase.ACTIVE);
        registry.register(match);

        assertTrue(registry.isArenaWorld("war_arena_1"));
        assertFalse(registry.isArenaWorld("world"));
    }

    @Test
    void isArenaWorld_falseForPendingOrEnded() {
        WarMatch pending = createMatch(10, 20);
        pending.setWorldName("war_pending");
        pending.setPhase(WarPhase.PENDING);
        registry.register(pending);

        WarMatch ended = createMatch(30, 40);
        ended.setWorldName("war_ended");
        ended.setPhase(WarPhase.ENDED);
        registry.register(ended);

        assertFalse(registry.isArenaWorld("war_pending"));
        assertFalse(registry.isArenaWorld("war_ended"));
    }

    private static WarMatch createMatch(int guildAId, int guildBId) {
        return new WarMatch(
                guildAId, "Guild" + guildAId,
                guildBId, "Guild" + guildBId,
                CHALLENGER,
                "default",
                VictoryMode.FIRST_TO_SCORE,
                5, 10, 600);
    }
}
