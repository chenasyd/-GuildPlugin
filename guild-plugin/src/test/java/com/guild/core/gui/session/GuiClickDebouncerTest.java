package com.guild.core.gui.session;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiClickDebouncerTest {

    @Test
    void firstClickIsAccepted() {
        GuiClickDebouncer debouncer = new GuiClickDebouncer(200L);
        UUID playerId = UUID.randomUUID();

        assertFalse(debouncer.shouldIgnore(playerId));
    }

    @Test
    void rapidSecondClickIsIgnored() {
        GuiClickDebouncer debouncer = new GuiClickDebouncer(200L);
        UUID playerId = UUID.randomUUID();

        assertFalse(debouncer.shouldIgnore(playerId));
        assertTrue(debouncer.shouldIgnore(playerId));
    }

    @Test
    void clickAfterDebounceWindowIsAccepted() throws InterruptedException {
        GuiClickDebouncer debouncer = new GuiClickDebouncer(30L);
        UUID playerId = UUID.randomUUID();

        assertFalse(debouncer.shouldIgnore(playerId));
        Thread.sleep(35L);
        assertFalse(debouncer.shouldIgnore(playerId));
    }
}
