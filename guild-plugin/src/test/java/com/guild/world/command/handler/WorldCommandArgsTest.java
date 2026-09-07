package com.guild.world.command.handler;

import com.guild.world.model.WorldStatus;
import com.guild.world.model.WorldType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldCommandArgsTest {

    @Test
    void flagValue_readsFlagAndValue() {
        String[] args = {"create", "arena", "--type", "battle", "--preset", "castle"};
        assertEquals("battle", WorldCommandArgs.flagValue(args, 2, "--type"));
        assertEquals("castle", WorldCommandArgs.flagValue(args, 2, "--preset"));
        assertNull(WorldCommandArgs.flagValue(args, 2, "--guild"));
    }

    @Test
    void containsFlag_detectsPresence() {
        String[] args = {"delete", "world1", "--force"};
        assertTrue(WorldCommandArgs.containsFlag(args, "--force"));
        assertFalse(WorldCommandArgs.containsFlag(args, "--load"));
    }

    @Test
    void parseType_defaultsAndParses() {
        assertEquals(WorldType.BATTLE, WorldCommandArgs.parseType(null));
        assertEquals(WorldType.EDIT, WorldCommandArgs.parseType("edit"));
        assertEquals(WorldType.BATTLE, WorldCommandArgs.parseType("invalid"));
    }

    @Test
    void statusText_mapsKnownStatuses() {
        assertEquals("&aREADY", WorldCommandArgs.statusText(WorldStatus.READY));
        assertEquals("&4STALE", WorldCommandArgs.statusText(WorldStatus.STALE));
    }
}
