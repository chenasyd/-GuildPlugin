package com.guild.war.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 文档化对局阶段流转顺序（纯模型层，不涉及调度器）。
 */
class WarPhaseFlowTest {

    @Test
    void typicalFlow_pendingThroughEnded() {
        WarMatch match = new WarMatch(
                1, "A", 2, "B",
                java.util.UUID.randomUUID(), "preset", VictoryMode.FIRST_TO_SCORE,
                5, 10, 600);

        assertEquals(WarPhase.PENDING, match.phase());

        match.setPhase(WarPhase.SIGNUP);
        match.setPhase(WarPhase.PREPARING);
        match.setPhase(WarPhase.COUNTDOWN);
        match.setPhase(WarPhase.ACTIVE);
        match.setStartedAt(System.currentTimeMillis());

        match.setWinnerGuildId(1);
        match.setEndReason("war.reason.wipe");
        match.setPhase(WarPhase.ENDED);

        assertEquals(WarPhase.ENDED, match.phase());
        assertEquals(1, match.winnerGuildId());
        assertEquals("war.reason.wipe", match.endReason());
    }
}
