package com.guild.world.preset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetServiceTest {

    @TempDir
    File worldsDir;

    private PresetService presets;

    @BeforeEach
    void setUp() {
        presets = new PresetService(worldsDir, Logger.getLogger("test"));
    }

    @Test
    void anchor_parseAndSerialize_roundTrip() {
        PresetService.Anchor anchor = PresetService.Anchor.parse("1.5,64.0,-2.0,90.0,10.0");
        assertNotNull(anchor);
        assertEquals("1.5,64.0,-2.0,90.0,10.0", anchor.serialize());
    }

    @Test
    void anchor_parseInvalid_returnsNull() {
        assertNull(PresetService.Anchor.parse(""));
        assertNull(PresetService.Anchor.parse("not,valid"));
    }

    @Test
    void saveMeta_andGet_roundTrip() {
        PresetService.Anchor spawnA = new PresetService.Anchor(0, 1, 0, 0, 0);
        PresetService.Anchor spawnB = new PresetService.Anchor(10, 1, 0, 180, 0);
        PresetService.PresetMeta saved = presets.saveMeta(
                "Castle",
                "gw_edit",
                "admin-uuid",
                "test preset",
                true,
                16, 8, 16,
                2,
                "gw_edit,0,64,0",
                spawnA, spawnB, null
        );

        assertEquals("castle", saved.name());
        assertTrue(saved.hasSchematic());
        assertEquals(16, saved.sizeX());

        PresetService.PresetMeta loaded = presets.get("castle");
        assertNotNull(loaded);
        assertEquals("admin-uuid", loaded.createdBy());
        assertEquals(10.0, loaded.spawnB().dx());
    }

    @Test
    void list_returnsSortedPresets() {
        presets.save("zebra", "w1", "u1", "");
        presets.save("alpha", "w2", "u2", "");
        assertEquals(2, presets.list().size());
        assertEquals("alpha", presets.list().iterator().next().name());
    }

    @Test
    void validateName_rejectsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> presets.saveMeta("", "w", "u", "", false, 0, 0, 0, 0, "", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> presets.saveMeta("bad name!", "w", "u", "", false, 0, 0, 0, 0, "", null, null, null));
    }

    @Test
    void delete_removesYamlFile() throws Exception {
        presets.saveMeta("tmp", "w", "u", "", false, 0, 0, 0, 0, "", null, null, null);
        assertTrue(presets.exists("tmp"));
        assertTrue(presets.delete("tmp"));
        assertFalse(presets.exists("tmp"));
    }
}
