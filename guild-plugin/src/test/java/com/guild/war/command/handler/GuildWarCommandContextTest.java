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
    void intFlag_parsesOrReturnsNull() {
        String[] args = {"--size", "10", "--bad", "x"};
        assertEquals(10, GuildWarCommandContext.intFlag(args, "--size"));
        assertNull(GuildWarCommandContext.intFlag(args, "--bad"));
        assertNull(GuildWarCommandContext.intFlag(args, "--none"));
    }
}
