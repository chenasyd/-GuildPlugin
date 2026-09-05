package com.guild.module.example.territory;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class TerritorySelectionManagerTest {

    private TerritorySelectionManager selections;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        selections = new TerritorySelectionManager();
        player = Mockito.mock(Player.class);
        world = Mockito.mock(World.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
    }

    @Test
    void volumeAndCompleteSelection() {
        when(world.getName()).thenReturn("world");
        Location pos1 = new Location(world, 0, 0, 0);
        Location pos2 = new Location(world, 9, 4, 4);

        TerritorySelectionManager.Session session = selections.of(player);
        session.pos1 = pos1;
        session.pos2 = pos2;

        assertTrue(selections.hasCompleteSelection(player));
        assertEquals(250L, selections.selectionVolume(player));
    }

    @Test
    void incompleteWhenDifferentWorlds() {
        World other = Mockito.mock(World.class);
        TerritorySelectionManager.Session session = selections.of(player);
        session.pos1 = new Location(world, 0, 0, 0);
        session.pos2 = new Location(other, 1, 1, 1);

        assertFalse(selections.hasCompleteSelection(player));
        assertEquals(0L, selections.selectionVolume(player));
    }
}
