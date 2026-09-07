package com.guild.world.schematic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SchematicCodecTest {

    @TempDir
    Path tempDir;

    @Test
    void writeRead_roundTrip_preservesData() throws Exception {
        SchematicData original = new SchematicData();
        original.origin = new Vec3i(2, 3, 4);
        original.size = new Size3i(5, 6, 7);
        original.palette = List.of("minecraft:stone", "minecraft:air");
        original.blocks = List.of(new int[]{0, 10}, new int[]{1, 5});

        Path file = tempDir.resolve("test.gws");
        SchematicCodec.write(file, original);
        SchematicData loaded = SchematicCodec.read(file);

        assertNotNull(loaded);
        assertEquals(2, loaded.origin.x());
        assertEquals(6, loaded.size.dy());
        assertEquals(2, loaded.palette.size());
        assertEquals(10, loaded.blocks.get(0)[1]);
    }
}
