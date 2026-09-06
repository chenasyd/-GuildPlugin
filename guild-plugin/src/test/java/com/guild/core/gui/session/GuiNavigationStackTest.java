package com.guild.core.gui.session;

import com.guild.core.gui.GUI;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GuiNavigationStackTest {

    @Test
    void pop_returnsNullWhenEmpty() {
        GuiNavigationStack stack = new GuiNavigationStack();
        Player player = mockPlayer(UUID.randomUUID());

        assertNull(stack.pop(player));
    }

    @Test
    void pushAndPop_restoresLastPushedGui() {
        GuiNavigationStack stack = new GuiNavigationStack();
        Player player = mockPlayer(UUID.randomUUID());
        GUI first = Mockito.mock(GUI.class);
        GUI second = Mockito.mock(GUI.class);

        stack.push(player, first);
        stack.push(player, second);

        assertEquals(second, stack.pop(player));
        assertEquals(first, stack.pop(player));
        assertNull(stack.pop(player));
    }

    @Test
    void clear_removesPendingNavigation() {
        GuiNavigationStack stack = new GuiNavigationStack();
        Player player = mockPlayer(UUID.randomUUID());
        stack.push(player, Mockito.mock(GUI.class));

        stack.clear(player);

        assertNull(stack.pop(player));
    }

    private static Player mockPlayer(UUID uuid) {
        Player player = Mockito.mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }
}
