package com.guild.world.selection;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 每位管理员的编辑选区与临时锚点。 */
public class SelectionManager {

    public static final class Session {
        public Location pos1;
        public Location pos2;
        public Location spawnA;
        public Location spawnB;
        public Location spectator;
        public boolean wandMode;
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public Session of(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), id -> new Session());
    }

    public Session get(UUID id) {
        return sessions.get(id);
    }

    public void clear(UUID id) {
        sessions.remove(id);
    }

    public boolean hasCompleteSelection(Player player) {
        Session s = of(player);
        return s.pos1 != null && s.pos2 != null
                && s.pos1.getWorld() != null
                && s.pos1.getWorld().equals(s.pos2.getWorld());
    }
}
