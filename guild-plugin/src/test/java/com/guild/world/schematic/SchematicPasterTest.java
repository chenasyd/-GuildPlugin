package com.guild.world.schematic;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SchematicPasterTest {

    @Test
    void offsetToWorld_addsDelta() {
        World world = mock(World.class);
        Location pasteAt = new Location(world, 0.5, 64.0, 0.5, 0f, 0f);

        Location result = SchematicPaster.offsetToWorld(pasteAt, 10.0, 1.0, -5.0, 90f, 15f);

        assertEquals(world, result.getWorld());
        assertEquals(10.5, result.getX(), 0.001);
        assertEquals(65.0, result.getY(), 0.001);
        assertEquals(-4.5, result.getZ(), 0.001);
        assertEquals(90f, result.getYaw(), 0.001);
        assertEquals(15f, result.getPitch(), 0.001);
    }
}
