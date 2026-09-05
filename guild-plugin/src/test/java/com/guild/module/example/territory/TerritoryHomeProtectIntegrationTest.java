package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.sdk.config.ModuleConfigSection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
