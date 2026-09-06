package com.guild.module.example.territory;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class TerritoryFlagDefaultsTest {

    @Test
    void applyStateFlag_parsesAllowDenyAndSkipsNone() {
        ProtectedCuboidRegion region = new ProtectedCuboidRegion("test",
                com.sk89q.worldedit.math.BlockVector3.at(0, 0, 0),
                com.sk89q.worldedit.math.BlockVector3.at(1, 1, 1));

        TerritoryFlagDefaults.applyStateFlag(region, com.sk89q.worldguard.protection.flags.Flags.PVP, "deny");
        assertEquals(StateFlag.State.DENY, region.getFlag(com.sk89q.worldguard.protection.flags.Flags.PVP));

        TerritoryFlagDefaults.applyStateFlag(region, com.sk89q.worldguard.protection.flags.Flags.PVP, "allow");
        assertEquals(StateFlag.State.ALLOW, region.getFlag(com.sk89q.worldguard.protection.flags.Flags.PVP));

        TerritoryFlagDefaults.applyStateFlag(region, com.sk89q.worldguard.protection.flags.Flags.PVP, "none");
        assertEquals(StateFlag.State.ALLOW, region.getFlag(com.sk89q.worldguard.protection.flags.Flags.PVP));
    }

    @Test
    void settingsReadFlagDefaultsFromConfigSection() {
        com.guild.core.module.ModuleContext context = Mockito.mock(com.guild.core.module.ModuleContext.class);
        com.guild.sdk.config.ModuleConfigSection config = Mockito.mock(com.guild.sdk.config.ModuleConfigSection.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString("flags.pvp", "deny")).thenReturn("allow");
        when(config.getInt("region.priority", 10)).thenReturn(15);

        TerritorySettings settings = new TerritorySettings(context);
        assertEquals("allow", settings.getFlag("pvp", "deny"));
        assertEquals(15, settings.getRegionPriority());
    }
}
