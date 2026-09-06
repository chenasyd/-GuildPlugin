package com.guild.core.gui.session;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * 玩家聊天/命令输入模式（如公会名输入）的状态与处理器。
 */
public final class GuiInputModeController {

    private final Map<UUID, Function<String, Boolean>> inputModes = new HashMap<>();

    public void set(Player player, Function<String, Boolean> inputHandler) {
        if (player == null || inputHandler == null) {
            return;
        }
        inputModes.put(player.getUniqueId(), inputHandler);
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        inputModes.remove(player.getUniqueId());
    }

    public boolean isActive(Player player) {
        return player != null && inputModes.containsKey(player.getUniqueId());
    }

    /**
     * @return 是否已消费输入；处理器返回 {@code true} 时自动清除模式
     */
    public boolean handle(Player player, String input) {
        if (player == null) {
            return false;
        }
        Function<String, Boolean> handler = inputModes.get(player.getUniqueId());
        if (handler == null) {
            return false;
        }
        boolean result = handler.apply(input);
        if (result) {
            inputModes.remove(player.getUniqueId());
        }
        return result;
    }
}
