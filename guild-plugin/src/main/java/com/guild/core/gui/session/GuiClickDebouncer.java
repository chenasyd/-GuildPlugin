package com.guild.core.gui.session;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI 点击防抖（默认 200ms 内重复点击忽略）。
 */
public final class GuiClickDebouncer {

    public static final long DEFAULT_DEBOUNCE_MS = 200L;

    private final long debounceMs;
    private final Map<UUID, Long> lastClickTime = new HashMap<>();

    public GuiClickDebouncer() {
        this(DEFAULT_DEBOUNCE_MS);
    }

    public GuiClickDebouncer(long debounceMs) {
        this.debounceMs = Math.max(0L, debounceMs);
    }

    /**
     * @return {@code true} 表示应忽略本次点击（过快）
     */
    public boolean shouldIgnore(Player player) {
        if (player == null) {
            return true;
        }
        return shouldIgnore(player.getUniqueId());
    }

    boolean shouldIgnore(UUID playerId) {
        if (playerId == null) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastClickTime.get(playerId);
        if (lastClick != null && currentTime - lastClick < debounceMs) {
            return true;
        }
        lastClickTime.put(playerId, currentTime);
        return false;
    }
}
