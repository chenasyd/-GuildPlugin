package com.guild.core.gui.session;

import com.guild.core.gui.GUI;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 每位玩家的 GUI 返回栈（push 当前页后打开新页；pop 恢复上一页）。
 */
public final class GuiNavigationStack {

    private final Map<UUID, Deque<GUI>> stacks = new HashMap<>();

    public void push(Player player, GUI current) {
        if (player == null || current == null) {
            return;
        }
        stacks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>()).push(current);
    }

    /**
     * 弹出栈顶 GUI；栈空时返回 {@code null} 并清理条目。
     */
    public GUI pop(Player player) {
        if (player == null) {
            return null;
        }
        UUID playerId = player.getUniqueId();
        Deque<GUI> stack = stacks.remove(playerId);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        GUI previous = stack.pop();
        if (!stack.isEmpty()) {
            stacks.put(playerId, stack);
        }
        return previous;
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        stacks.remove(player.getUniqueId());
    }
}
