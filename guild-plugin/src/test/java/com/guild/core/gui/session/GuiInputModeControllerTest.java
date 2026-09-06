package com.guild.core.gui.session;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GuiInputModeControllerTest {

    @Test
    void handle_clearsModeWhenHandlerReturnsTrue() {
        GuiInputModeController controller = new GuiInputModeController();
        Player player = mockPlayer(UUID.randomUUID());
        AtomicBoolean invoked = new AtomicBoolean(false);

        controller.set(player, input -> {
            invoked.set(true);
            return true;
        });

        assertTrue(controller.handle(player, "hello"));
        assertTrue(invoked.get());
        assertFalse(controller.isActive(player));
    }

    @Test
    void handle_keepsModeWhenHandlerReturnsFalse() {
        GuiInputModeController controller = new GuiInputModeController();
        Player player = mockPlayer(UUID.randomUUID());

        controller.set(player, input -> false);

        assertFalse(controller.handle(player, "retry"));
        assertTrue(controller.isActive(player));
    }

    private static Player mockPlayer(UUID uuid) {
        Player player = Mockito.mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }
}
