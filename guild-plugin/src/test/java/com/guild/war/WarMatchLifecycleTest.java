package com.guild.war;

import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class WarMatchLifecycleTest {

    private static final UUID CHALLENGER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private WarMatchRegistry registry;
    private WarMatchScheduler scheduler;
    private WarMatchLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        registry = new WarMatchRegistry();
        scheduler = new WarMatchScheduler(null);
        lifecycle = new WarMatchLifecycle(null, null, registry, null, scheduler, null, () -> null);
    }

    @Test
    void cleanupMatch_setsEndedAndUnregisters() {
        WarMatch match = createMatch(10, 20);
        registry.register(match);

        lifecycle.cleanupMatch(match, false);

        assertEquals(WarPhase.ENDED, match.phase());
        assertNull(registry.getMatch(match.id()));
    }

    @Test
    void endMatch_isIdempotentWhenAlreadyEnded() {
        WarMatch match = createMatch(10, 20);
        match.setPhase(WarPhase.ENDED);
        match.setWinnerGuildId(10);
        match.setEndReason("war.reason.wipe");

        lifecycle.endMatch(match, 20, "war.reason.other");

        assertEquals(10, match.winnerGuildId());
        assertEquals("war.reason.wipe", match.endReason());
    }

    @Test
    void beginPreparing_ignoresWrongPhase() {
        WarMatch match = createMatch(10, 20);
        match.setPhase(WarPhase.PENDING);
        AtomicBoolean countdownStarted = new AtomicBoolean(false);

        lifecycle.beginPreparing(match, () -> countdownStarted.set(true));

        assertEquals(WarPhase.PENDING, match.phase());
        assertFalse(countdownStarted.get());
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
