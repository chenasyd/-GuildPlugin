package com.guild.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftVersionSupportTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "1.20", "1.20.1", "1.20.6", "1.21", "1.21.11", "26.1", "26.1.2", "26.2"
    })
    void officialVersions_resolveAsOfficial(String version) {
        assertEquals(MinecraftVersionSupport.CompatibilityLevel.OFFICIAL,
                MinecraftVersionSupport.resolve(version));
        assertTrue(MinecraftVersionSupport.isOfficialVersion(version));
        assertTrue(MinecraftVersionSupport.isFoliaGworldCompatible(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"26.1.4", "25.1", "1.20.7", "26.2.1"})
    void nonListedButEligibleVersions_areBestEffort(String version) {
        assertEquals(MinecraftVersionSupport.CompatibilityLevel.BEST_EFFORT,
                MinecraftVersionSupport.resolve(version));
        assertFalse(MinecraftVersionSupport.isOfficialVersion(version));
        assertTrue(MinecraftVersionSupport.isFoliaGworldCompatible(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.19.4", "1.18.2", "24.9"})
    void belowMinimumVersions_areUnsupported(String version) {
        assertEquals(MinecraftVersionSupport.CompatibilityLevel.UNSUPPORTED,
                MinecraftVersionSupport.resolve(version));
        assertFalse(MinecraftVersionSupport.isFoliaGworldCompatible(version));
    }

    @Test
    void patternMatching_supportsSeriesWildcard() {
        assertTrue(MinecraftVersionSupport.matchesVersionPattern("26.1.4", "26.1.x"));
        assertTrue(MinecraftVersionSupport.matchesVersionPattern("26.1", "26.1.x"));
        assertFalse(MinecraftVersionSupport.matchesVersionPattern("26.2", "26.1.x"));
    }
}
