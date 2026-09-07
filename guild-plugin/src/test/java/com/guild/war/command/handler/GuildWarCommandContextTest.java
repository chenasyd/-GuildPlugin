package com.guild.war.command.handler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuildWarCommandContextTest {

    @Test
    void flag_parsesCaseInsensitivePairs() {
        String[] args = {"challenge", "Other", "--mode", "timed", "--size", "5"};
        assertEquals("timed", GuildWarCommandContext.flag(args, "--mode"));
        assertEquals("5", GuildWarCommandContext.flag(args, "--size"));
        assertNull(GuildWarCommandContext.flag(args, "--missing"));
    }

    @Test
    void flag_isCaseInsensitive() {
        String[] args = {"--MODE", "last", "--Season", "s1"};
        assertEquals("last", GuildWarCommandContext.flag(args, "--mode"));
        assertEquals("s1", GuildWarCommandContext.flag(args, "--season"));
    }

    @Test
    void flag_returnsNullWhenValueMissing() {
        String[] args = {"challenge", "Other", "--mode"};
        assertNull(GuildWarCommandContext.flag(args, "--mode"));
        assertNull(GuildWarCommandContext.flag(args, "--export"));
    }

    @Test
    void intFlag_parsesOrReturnsNull() {
        String[] args = {"--size", "10", "--bad", "x"};
        assertEquals(10, GuildWarCommandContext.intFlag(args, "--size"));
        assertNull(GuildWarCommandContext.intFlag(args, "--bad"));
        assertNull(GuildWarCommandContext.intFlag(args, "--none"));
    }

    @Test
    void intFlag_parsesSeasonLimitFlag() {
        String[] args = {"season", "--limit", "15"};
        assertEquals(15, GuildWarCommandContext.intFlag(args, "--limit"));
    }
}
