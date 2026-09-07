package com.guild.world.selection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelectionManagerTest {

    private static final UUID EDITOR = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Test
    void hasCompleteSelection_falseWhenMissingPos() {
        SelectionManager manager = new SelectionManager();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(EDITOR);

        assertFalse(manager.hasCompleteSelection(player));
    }

    @Test
    void hasCompleteSelection_falseWhenDifferentWorlds() {
        SelectionManager manager = new SelectionManager();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(EDITOR);

        World worldA = mock(World.class);
        World worldB = mock(World.class);
        SelectionManager.Session session = manager.of(player);
        session.pos1 = new Location(worldA, 0, 64, 0);
        session.pos2 = new Location(worldB, 5, 64, 5);

        assertFalse(manager.hasCompleteSelection(player));
    }

    @Test
    void hasCompleteSelection_trueWhenBothSetInSameWorld() {
        SelectionManager manager = new SelectionManager();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(EDITOR);

        World world = mock(World.class);
        SelectionManager.Session session = manager.of(player);
        session.pos1 = new Location(world, 0, 64, 0);
        session.pos2 = new Location(world, 5, 70, 5);

        assertTrue(manager.hasCompleteSelection(player));
    }
}
