package com.guild.core.gui.session;

import com.guild.core.gui.GUI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 跟踪玩家当前打开的 GUI 会话（内存索引，不含打开/关闭 Bukkit Inventory 逻辑）。
 */
public final class GuiSessionManager {

    private final Map<UUID, GUI> openGuis = new HashMap<>();

    public void track(Player player, GUI gui) {
        if (player == null || gui == null) {
            return;
        }
        openGuis.put(player.getUniqueId(), gui);
    }

    public GUI get(Player player) {
        if (player == null) {
            return null;
        }
        return openGuis.get(player.getUniqueId());
    }

    public GUI get(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return openGuis.get(playerId);
    }

    public boolean isOpen(Player player) {
        return player != null && openGuis.containsKey(player.getUniqueId());
    }

    public GUI remove(Player player) {
        if (player == null) {
            return null;
        }
        return openGuis.remove(player.getUniqueId());
    }

    public int count() {
        return openGuis.size();
    }

    public List<UUID> snapshotPlayerIds() {
        return new ArrayList<>(openGuis.keySet());
    }

    public void clear() {
        openGuis.clear();
    }
}
