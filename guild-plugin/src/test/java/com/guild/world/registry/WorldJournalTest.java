package com.guild.world.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldJournalTest {

    @TempDir
    File worldsDir;

    private WorldJournal journal;

    @BeforeEach
    void setUp() {
        journal = new WorldJournal(worldsDir, Logger.getLogger("test"));
    }

    @Test
    void pending_emptyWhenNoFile() {
        assertTrue(journal.pending().isEmpty());
        assertFalse(journal.hasPending());
    }

    @Test
    void beginWithoutDone_isPending() {
        journal.begin(WorldJournal.Op.CREATE, "gw_crash");
        List<WorldJournal.PendingOp> pending = journal.pending();
        assertEquals(1, pending.size());
        assertEquals("gw_crash", pending.get(0).world());
        assertEquals(WorldJournal.Op.CREATE, pending.get(0).op());
        assertTrue(journal.hasPending());
    }

    @Test
    void doneAfterBegin_clearsPending() {
        journal.begin(WorldJournal.Op.LOAD, "gw_ok");
        journal.done(WorldJournal.Op.LOAD, "gw_ok");
        assertTrue(journal.pending().isEmpty());
        assertFalse(journal.hasPending());
    }

    @Test
    void multipleStarts_countCorrectly() {
        journal.begin(WorldJournal.Op.DELETE, "gw_x");
        journal.begin(WorldJournal.Op.DELETE, "gw_x");
        journal.done(WorldJournal.Op.DELETE, "gw_x");
        List<WorldJournal.PendingOp> pending = journal.pending();
        assertEquals(1, pending.size());
        assertEquals(1, pending.get(0).count());
    }

    @Test
    void clear_removesPendingState() {
        journal.begin(WorldJournal.Op.PASTE, "gw_p");
        journal.clear();
        assertTrue(journal.pending().isEmpty());
    }
}
