package com.guild.world.schematic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Size3iTest {

    @Test
    void volume_multipliesDimensions() {
        assertEquals(24, new Size3i(4, 3, 2).volume());
    }
}
