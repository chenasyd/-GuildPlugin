package com.guild.module.example.territory;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 领地模块独立选区（与 GuildWorld 管理员选区隔离）。 */
public final class TerritorySelectionManager {

    public static final class Session {
        public Location pos1;
        public Location pos2;
        public boolean wandMode;
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public Session of(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), id -> new Session());
    }

    public boolean hasCompleteSelection(Player player) {
        Session session = of(player);
        return session.pos1 != null && session.pos2 != null
                && session.pos1.getWorld() != null
                && session.pos1.getWorld().equals(session.pos2.getWorld());
    }

    public long selectionVolume(Player player) {
        Session session = of(player);
        if (!hasCompleteSelection(player)) {
            return 0L;
        }
        int dx = Math.abs(session.pos1.getBlockX() - session.pos2.getBlockX()) + 1;
        int dy = Math.abs(session.pos1.getBlockY() - session.pos2.getBlockY()) + 1;
        int dz = Math.abs(session.pos1.getBlockZ() - session.pos2.getBlockZ()) + 1;
        return (long) dx * dy * dz;
    }
}
