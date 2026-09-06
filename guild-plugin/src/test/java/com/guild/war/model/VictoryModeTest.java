package com.guild.war.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VictoryModeTest {

    @Test
    void parse_acceptsEnglishAliases() {
        assertEquals(VictoryMode.FIRST_TO_SCORE, VictoryMode.parse("first"));
        assertEquals(VictoryMode.FIRST_TO_SCORE, VictoryMode.parse("first_to_score"));
        assertEquals(VictoryMode.TIMED_SCORE, VictoryMode.parse("timed"));
        assertEquals(VictoryMode.LAST_STANDING, VictoryMode.parse("last_standing"));
    }

    @Test
    void parse_acceptsChineseAliases() {
        assertEquals(VictoryMode.FIRST_TO_SCORE, VictoryMode.parse("积分"));
        assertEquals(VictoryMode.TIMED_SCORE, VictoryMode.parse("限时积分"));
        assertEquals(VictoryMode.LAST_STANDING, VictoryMode.parse("淘汰"));
    }

    @Test
    void parse_acceptsEnumName() {
        assertEquals(VictoryMode.TIMED_SCORE, VictoryMode.parse("TIMED_SCORE"));
    }

    @Test
    void parse_nullOrUnknownReturnsNull() {
        assertNull(VictoryMode.parse(null));
        assertNull(VictoryMode.parse(""));
        assertNull(VictoryMode.parse("unknown-mode"));
    }

    @Test
    void langKey_mapsToMessageKeys() {
        assertEquals("war.mode.first", VictoryMode.FIRST_TO_SCORE.langKey());
        assertEquals("war.mode.timed", VictoryMode.TIMED_SCORE.langKey());
        assertEquals("war.mode.survive", VictoryMode.LAST_STANDING.langKey());
    }
}
