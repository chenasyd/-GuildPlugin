package com.guild.core.language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LanguageConstantsTest {

    @Test
    void resolveModuleLangDir_mapsKnownAliases() {
        assertEquals("quest", LanguageConstants.resolveModuleLangDir("guild-quest"));
        assertEquals("territory", LanguageConstants.resolveModuleLangDir("guild-territory"));
        assertEquals("stats", LanguageConstants.resolveModuleLangDir("stats"));
    }

    @Test
    void resolveModuleLangDir_normalizesCaseAndWhitespace() {
        assertEquals("quest", LanguageConstants.resolveModuleLangDir("  GUILD-QUEST  "));
    }

    @Test
    void resolveModuleLangDir_nullOrBlankReturnsInput() {
        assertNull(LanguageConstants.resolveModuleLangDir(null));
        assertEquals("", LanguageConstants.resolveModuleLangDir(""));
        assertEquals("   ", LanguageConstants.resolveModuleLangDir("   "));
    }
}
