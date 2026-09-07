package com.guild.warehouse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks one open warehouse session per guild and in-flight page saves.
 */
final class WarehouseSessionManager {

    private final ConcurrentHashMap<Integer, UUID> openSessions = new ConcurrentHashMap<>();
    /** In-flight page saves keyed by guild id; quit must not drop the session until these finish. */
    private final ConcurrentHashMap<Integer, CompletableFuture<Boolean>> pendingSaves = new ConcurrentHashMap<>();

    boolean tryAcquireSession(int guildId, UUID playerUuid) {
        UUID existing = openSessions.putIfAbsent(guildId, playerUuid);
        return existing == null || existing.equals(playerUuid);
    }

    void releaseSession(int guildId, UUID playerUuid) {
        openSessions.computeIfPresent(guildId, (id, holder) ->
                holder.equals(playerUuid) ? null : holder);
    }

    /**
     * Safety net for quit when InventoryClose never started a save.
     * If a save is still in flight, leave the session until the save completes.
     */
    void releaseSessionByPlayerIfIdle(UUID playerUuid) {
        openSessions.entrySet().removeIf(e -> {
            if (!e.getValue().equals(playerUuid)) {
                return false;
            }
            CompletableFuture<Boolean> pending = pendingSaves.get(e.getKey());
            return pending == null || pending.isDone();
        });
    }

    UUID getSessionHolder(int guildId) {
        return openSessions.get(guildId);
    }

    void trackPendingSave(int guildId, CompletableFuture<Boolean> future) {
        pendingSaves.put(guildId, future);
        future.whenComplete((ok, err) -> pendingSaves.remove(guildId, future));
    }
}
