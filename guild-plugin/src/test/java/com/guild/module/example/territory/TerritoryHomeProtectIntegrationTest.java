package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.sdk.config.ModuleConfigSection;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TerritoryHomeProtectIntegrationTest {

    @Test
    void normalizeMode_defaultsToDefer() {
        assertEquals(TerritoryHomeProtectIntegration.MODE_DEFER,
                TerritoryHomeProtectIntegration.normalizeMode(null));
        assertEquals(TerritoryHomeProtectIntegration.MODE_MERGE,
                TerritoryHomeProtectIntegration.normalizeMode("MERGE"));
    }

    @Test
    void deferAll_whenModeDeferAndWgReady() {
        TerritoryModule module = Mockito.mock(TerritoryModule.class);
        ModuleContext context = Mockito.mock(ModuleContext.class);
        ModuleConfigSection config = Mockito.mock(ModuleConfigSection.class);
        TerritoryBridge bridge = Mockito.mock(TerritoryBridge.class);

        when(context.getConfig()).thenReturn(config);
        when(config.getString("home-protect.mode", TerritoryHomeProtectIntegration.MODE_DEFER))
                .thenReturn(TerritoryHomeProtectIntegration.MODE_DEFER);
        when(module.getBridge()).thenReturn(bridge);
        when(bridge.isOperational()).thenReturn(true);

        TerritoryHomeProtectIntegration integration = new TerritoryHomeProtectIntegration(module, context);
        assertTrue(integration.deferAll());
    }

    @Test
    void deferAll_falseWhenModeMerge() {
        TerritoryModule module = Mockito.mock(TerritoryModule.class);
        ModuleContext context = Mockito.mock(ModuleContext.class);
        ModuleConfigSection config = Mockito.mock(ModuleConfigSection.class);
        TerritoryBridge bridge = Mockito.mock(TerritoryBridge.class);

        when(context.getConfig()).thenReturn(config);
        when(config.getString("home-protect.mode", TerritoryHomeProtectIntegration.MODE_DEFER))
                .thenReturn(TerritoryHomeProtectIntegration.MODE_MERGE);
        when(module.getBridge()).thenReturn(bridge);
        when(bridge.isOperational()).thenReturn(true);

        TerritoryHomeProtectIntegration integration = new TerritoryHomeProtectIntegration(module, context);
        assertFalse(integration.deferAll());
    }

    @Test
    void skipAt_whenMergeModeAndInsideTerritory() {
        TerritoryModule module = Mockito.mock(TerritoryModule.class);
        ModuleContext context = Mockito.mock(ModuleContext.class);
        ModuleConfigSection config = Mockito.mock(ModuleConfigSection.class);
        TerritoryBridge bridge = Mockito.mock(TerritoryBridge.class);
        TerritoryRepository repository = Mockito.mock(TerritoryRepository.class);

        when(context.getConfig()).thenReturn(config);
        when(config.getString("home-protect.mode", TerritoryHomeProtectIntegration.MODE_DEFER))
                .thenReturn(TerritoryHomeProtectIntegration.MODE_MERGE);
        when(module.getBridge()).thenReturn(bridge);
        when(module.getRepository()).thenReturn(repository);
        when(bridge.isOperational()).thenReturn(true);

        TerritoryRecord record = new TerritoryRecord(1, "G", "guild_1", "world",
                0, 0, 0, 10, 10, 10, System.currentTimeMillis());
        when(bridge.findTerritoryAt("world", 5, 64, 5)).thenReturn(Optional.of(record));

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        Location location = new Location(world, 5, 64, 5);
        org.bukkit.entity.Player player = mock(org.bukkit.entity.Player.class);

        TerritoryHomeProtectIntegration integration = new TerritoryHomeProtectIntegration(module, context);
        assertTrue(integration.skipAt(player, location));
    }

    @Test
    void skipHomeForGuild_whenMergeModeAndTerritoryExists() {
        TerritoryModule module = Mockito.mock(TerritoryModule.class);
        ModuleContext context = Mockito.mock(ModuleContext.class);
        ModuleConfigSection config = Mockito.mock(ModuleConfigSection.class);
        TerritoryBridge bridge = Mockito.mock(TerritoryBridge.class);
        TerritoryRepository repository = Mockito.mock(TerritoryRepository.class);

        when(context.getConfig()).thenReturn(config);
        when(config.getString("home-protect.mode", TerritoryHomeProtectIntegration.MODE_DEFER))
                .thenReturn(TerritoryHomeProtectIntegration.MODE_MERGE);
        when(module.getBridge()).thenReturn(bridge);
        when(module.getRepository()).thenReturn(repository);
        when(bridge.isOperational()).thenReturn(true);
        when(repository.get(7, "world")).thenReturn(Optional.of(
                new TerritoryRecord(7, "G", "guild_7", "world", 0, 0, 0, 1, 1, 1, 0L)));

        TerritoryHomeProtectIntegration integration = new TerritoryHomeProtectIntegration(module, context);
        assertTrue(integration.skipHomeForGuild(7, "world"));
    }
}
