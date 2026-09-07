package com.guild.warehouse;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseSessionManagerTest {

    private static final UUID PLAYER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PLAYER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void tryAcquireSession_allowsFirstHolderAndSamePlayer() {
        WarehouseSessionManager sessions = new WarehouseSessionManager();

        assertTrue(sessions.tryAcquireSession(1, PLAYER_A));
        assertTrue(sessions.tryAcquireSession(1, PLAYER_A));
        assertFalse(sessions.tryAcquireSession(1, PLAYER_B));
        assertEquals(PLAYER_A, sessions.getSessionHolder(1));
    }

    @Test
    void releaseSession_onlyRemovesMatchingHolder() {
        WarehouseSessionManager sessions = new WarehouseSessionManager();
        sessions.tryAcquireSession(1, PLAYER_A);

        sessions.releaseSession(1, PLAYER_B);
        assertEquals(PLAYER_A, sessions.getSessionHolder(1));

        sessions.releaseSession(1, PLAYER_A);
        assertNull(sessions.getSessionHolder(1));
    }

    @Test
    void releaseSessionByPlayerIfIdle_keepsSessionWhileSavePending() {
        WarehouseSessionManager sessions = new WarehouseSessionManager();
        sessions.tryAcquireSession(1, PLAYER_A);

        CompletableFuture<Boolean> pending = new CompletableFuture<>();
        sessions.trackPendingSave(1, pending);

        sessions.releaseSessionByPlayerIfIdle(PLAYER_A);
        assertEquals(PLAYER_A, sessions.getSessionHolder(1));

        pending.complete(true);
        sessions.releaseSessionByPlayerIfIdle(PLAYER_A);
        assertNull(sessions.getSessionHolder(1));
    }
}
