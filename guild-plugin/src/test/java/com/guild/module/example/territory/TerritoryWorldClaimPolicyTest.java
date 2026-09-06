package com.guild.module.example.territory;

import com.guild.sdk.config.ModuleConfigSection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class TerritoryWorldClaimPolicyTest {

    @Test
    void allMode_allowsEveryWorld() {
        ModuleConfigSection config = mockConfig("all", List.of());
        TerritoryWorldClaimPolicy policy = TerritoryWorldClaimPolicy.from(config);

        assertEquals(TerritoryWorldClaimPolicy.Mode.ALL, policy.getMode());
        assertTrue(policy.isAllowed("world"));
        assertTrue(policy.isAllowed("nether"));
    }

    @Test
    void whitelistMode_onlyListedWorldsAllowed() {
        ModuleConfigSection config = mockConfig("whitelist", List.of("world", "plots"));
        TerritoryWorldClaimPolicy policy = TerritoryWorldClaimPolicy.from(config);

        assertTrue(policy.isAllowed("world"));
        assertTrue(policy.isAllowed("PLOTS"));
        assertFalse(policy.isAllowed("nether"));
    }

    @Test
    void blacklistMode_blocksListedWorlds() {
        ModuleConfigSection config = mockConfig("blacklist", List.of("spawn", "lobby"));
        TerritoryWorldClaimPolicy policy = TerritoryWorldClaimPolicy.from(config);

        assertFalse(policy.isAllowed("spawn"));
        assertFalse(policy.isAllowed("LOBBY"));
        assertTrue(policy.isAllowed("world"));
    }

    @Test
    void emptyWhitelist_blocksAllWorlds() {
        ModuleConfigSection config = mockConfig("whitelist", List.of());
        TerritoryWorldClaimPolicy policy = TerritoryWorldClaimPolicy.from(config);

        assertFalse(policy.isAllowed("world"));
    }

    @Test
    void legacyAllowedWorlds_mapsToWhitelist() {
        ModuleConfigSection config = Mockito.mock(ModuleConfigSection.class);
        when(config.getString("claim.worlds.mode", "")).thenReturn("");
        when(config.getStringList("claim.worlds.list")).thenReturn(List.of());
        when(config.getStringList("claim.allowed-worlds")).thenReturn(List.of("survival"));
        when(config.getStringList("claim.denied-worlds")).thenReturn(List.of());

        TerritoryWorldClaimPolicy policy = TerritoryWorldClaimPolicy.from(config);

        assertEquals(TerritoryWorldClaimPolicy.Mode.WHITELIST, policy.getMode());
        assertTrue(policy.isAllowed("survival"));
        assertFalse(policy.isAllowed("creative"));
    }

    @Test
    void legacyDeniedWorlds_mapsToBlacklist() {
        ModuleConfigSection config = Mockito.mock(ModuleConfigSection.class);
        when(config.getString("claim.worlds.mode", "")).thenReturn("");
        when(config.getStringList("claim.worlds.list")).thenReturn(List.of());
        when(config.getStringList("claim.allowed-worlds")).thenReturn(List.of());
        when(config.getStringList("claim.denied-worlds")).thenReturn(List.of("hub"));

        TerritoryWorldClaimPolicy policy = TerritoryWorldClaimPolicy.from(config);

        assertEquals(TerritoryWorldClaimPolicy.Mode.BLACKLIST, policy.getMode());
        assertFalse(policy.isAllowed("hub"));
        assertTrue(policy.isAllowed("world"));
    }

    @Test
    void defaultAllModeWithLegacyAllowedWorlds_prefersWhitelist() {
        ModuleConfigSection config = Mockito.mock(ModuleConfigSection.class);
        when(config.getString("claim.worlds.mode", "")).thenReturn("all");
        when(config.getStringList("claim.worlds.list")).thenReturn(List.of());
        when(config.getStringList("claim.allowed-worlds")).thenReturn(List.of("survival"));
        when(config.getStringList("claim.denied-worlds")).thenReturn(List.of());

        TerritoryWorldClaimPolicy policy = TerritoryWorldClaimPolicy.from(config);

        assertEquals(TerritoryWorldClaimPolicy.Mode.WHITELIST, policy.getMode());
        assertTrue(policy.isAllowed("survival"));
    }

    @Test
    void explicitModeWithListOverridesLegacyLists() {
        ModuleConfigSection config = Mockito.mock(ModuleConfigSection.class);
        when(config.getString("claim.worlds.mode", "")).thenReturn("blacklist");
        when(config.getStringList("claim.worlds.list")).thenReturn(List.of("world"));
        when(config.getStringList("claim.allowed-worlds")).thenReturn(List.of("legacy"));
        when(config.getStringList("claim.denied-worlds")).thenReturn(List.of());

        TerritoryWorldClaimPolicy policy = TerritoryWorldClaimPolicy.from(config);

        assertEquals(TerritoryWorldClaimPolicy.Mode.BLACKLIST, policy.getMode());
        assertFalse(policy.isAllowed("world"));
        assertTrue(policy.isAllowed("legacy"));
    }

    @Test
    void blockedMessageKey_matchesMode() {
        assertEquals("module.territory.world-blocked-whitelist",
                TerritoryWorldClaimPolicy.from(mockConfig("whitelist", List.of("a"))).blockedMessageKey());
        assertEquals("module.territory.world-blocked-blacklist",
                TerritoryWorldClaimPolicy.from(mockConfig("blacklist", List.of("a"))).blockedMessageKey());
    }

    private static ModuleConfigSection mockConfig(String mode, List<String> worlds) {
        ModuleConfigSection config = Mockito.mock(ModuleConfigSection.class);
        when(config.getString("claim.worlds.mode", "")).thenReturn(mode);
        when(config.getStringList("claim.worlds.list")).thenReturn(worlds);
        when(config.getStringList("claim.allowed-worlds")).thenReturn(List.of());
        when(config.getStringList("claim.denied-worlds")).thenReturn(List.of());
        return config;
    }
}
