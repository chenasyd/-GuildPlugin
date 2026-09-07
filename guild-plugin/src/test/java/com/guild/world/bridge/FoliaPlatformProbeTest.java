package com.guild.world.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoliaPlatformProbeTest {

    @Test
    void isPaper26Family_detects26x() {
        assertTrue(FoliaPlatformProbe.isPaper26Family("26"));
        assertTrue(FoliaPlatformProbe.isPaper26Family("26.1"));
        assertTrue(FoliaPlatformProbe.isPaper26Family("26.1.4"));
    }

    @Test
    void isPaper26Family_rejectsOlderBranches() {
        assertFalse(FoliaPlatformProbe.isPaper26Family(null));
        assertFalse(FoliaPlatformProbe.isPaper26Family("1.21.4"));
        assertFalse(FoliaPlatformProbe.isPaper26Family("1.20.6"));
        assertFalse(FoliaPlatformProbe.isPaper26Family("25.1"));
    }
}
