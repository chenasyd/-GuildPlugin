package com.guild.war;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarMatchSchedulerTest {

    private WarMatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new WarMatchScheduler(null);
    }

    @Test
    void cancel_cancelsAndRemovesHandle() {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        scheduler.setTimerForTest(1, () -> cancelled.set(true));

        scheduler.cancel(1);

        assertTrue(cancelled.get());
        assertEquals(0, scheduler.timerCountForTest());
    }

    @Test
    void cancelAll_cancelsEveryHandle() {
        AtomicBoolean first = new AtomicBoolean(false);
        AtomicBoolean second = new AtomicBoolean(false);
        scheduler.setTimerForTest(1, () -> first.set(true));
        scheduler.setTimerForTest(2, () -> second.set(true));

        scheduler.cancelAll();

        assertTrue(first.get());
        assertTrue(second.get());
        assertEquals(0, scheduler.timerCountForTest());
    }

    @Test
    void cancel_isIdempotentWhenNoTimer() {
        scheduler.cancel(99);
        assertEquals(0, scheduler.timerCountForTest());
    }
}
