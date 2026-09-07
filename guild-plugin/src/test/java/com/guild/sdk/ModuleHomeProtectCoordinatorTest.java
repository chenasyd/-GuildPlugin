package com.guild.sdk;

import com.guild.sdk.home.HomeProtectIntegration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleHomeProtectCoordinatorTest {

    @Test
    void isHomeProtectFullyDeferred_trueWhenAnyIntegrationDefers() {
        ModuleHomeProtectCoordinator coordinator = new ModuleHomeProtectCoordinator(Logger.getLogger("test"));
        Object owner = new Object();
        HomeProtectIntegration integration = mock(HomeProtectIntegration.class);
        when(integration.deferAll()).thenReturn(true);
        coordinator.register(owner, integration);

        assertTrue(coordinator.isHomeProtectFullyDeferred());
    }

    @Test
    void shouldSkipHomeProtectAt_aggregatesSkipFromIntegrations() {
        ModuleHomeProtectCoordinator coordinator = new ModuleHomeProtectCoordinator(Logger.getLogger("test"));
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        HomeProtectIntegration integration = mock(HomeProtectIntegration.class);
        when(integration.skipAt(player, location)).thenReturn(true);
        coordinator.register(new Object(), integration);

        assertTrue(coordinator.shouldSkipHomeProtectAt(player, location));
    }

    @Test
    void clearModuleHandlers_unregistersOwner() {
        ModuleHomeProtectCoordinator coordinator = new ModuleHomeProtectCoordinator(Logger.getLogger("test"));
        Object owner = new Object();
        HomeProtectIntegration integration = mock(HomeProtectIntegration.class);
        when(integration.deferAll()).thenReturn(true);
        coordinator.register(owner, integration);
        coordinator.clearModuleHandlers(owner);

        assertFalse(coordinator.isHomeProtectFullyDeferred());
    }
}
