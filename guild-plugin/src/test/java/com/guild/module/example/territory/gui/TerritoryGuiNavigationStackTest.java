package com.guild.module.example.territory.gui;

import com.guild.core.gui.GUI;
import com.guild.core.gui.session.GuiNavigationStack;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 回归：确认框取消时不应把 Confirm 压栈，否则管理面板「返回」会误开 Confirm。
 */
class TerritoryGuiNavigationStackTest {

    @Test
    void backFromManagement_popsParentNotOverlay() {
        GuiNavigationStack stack = new GuiNavigationStack();
        Player player = mockPlayer(UUID.randomUUID());
        GUI guildSettings = Mockito.mock(GUI.class);
        GUI management = Mockito.mock(GUI.class);
        GUI confirm = Mockito.mock(GUI.class);

        // 设置 -> 管理（openGUI push）
        stack.push(player, guildSettings);
        // 管理 -> 确认（openGUI push）
        stack.push(player, management);

        // 确认取消：应 navigateBack -> 管理（pop management）
        assertEquals(management, stack.pop(player));

        // 管理返回：应 navigateBack -> 设置（pop guildSettings）
        assertEquals(guildSettings, stack.pop(player));

        // 错误做法：取消时再次 openGUI 会把 confirm 压栈
        stack.push(player, guildSettings);
        stack.push(player, management);
        stack.push(player, confirm);

        assertEquals(confirm, stack.pop(player));
    }

    private static Player mockPlayer(UUID uuid) {
        Player player = Mockito.mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }
}
