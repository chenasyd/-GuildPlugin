package com.guild.sdk;

import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.home.HomeProtectIntegration;
import com.guild.sdk.placeholder.PlaceholderProvider;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleLifecycleCleanupTest {

    private ModuleEventBus events;
    private ModuleHomeProtectCoordinator homeProtect;
    private ModuleExtensionRegistry extensions;
    private ModuleLifecycleCleanup lifecycle;

    @BeforeEach
    void setUp() {
        events = new ModuleEventBus(Logger.getLogger("test"));
        homeProtect = new ModuleHomeProtectCoordinator(Logger.getLogger("test"));
        extensions = new ModuleExtensionRegistry(
                new ModuleCommandRegistry(),
                new ModulePlaceholderRegistry(),
                new ModuleGuiRegistry(mock(com.guild.GuildPlugin.class), Logger.getLogger("test")));
        lifecycle = new ModuleLifecycleCleanup(events, homeProtect, extensions);
    }

    @Test
    void clearOnUnload_removesEventsHomeProtectAndExtensions() {
        Object module = new Object();
        AtomicInteger eventCalls = new AtomicInteger();
        events.onGuildCreate(new GuildEventHandler() {
            @Override
            public void onEvent(GuildEventData data) {
                eventCalls.incrementAndGet();
            }

            @Override
            public Object getModuleInstance() {
                return module;
            }
        });

        HomeProtectIntegration integration = mock(HomeProtectIntegration.class);
        when(integration.deferAll()).thenReturn(true);
        homeProtect.register(module, integration);

        extensions.registerSubCommand("mod-z", "guild", "x", (s, a) -> { }, null);
        extensions.registerPlaceholderProvider("mod-z", new PlaceholderProvider() {
            @Override
            public String getIdentifier() {
                return "ph_z";
            }

            @Override
            public String onRequest(Player player, String params) {
                return "";
            }
        });
        extensions.registerCustomGUI("mod-z", "gui-z", mock(ModuleGUIFactory.class));

        lifecycle.clearOnUnload("mod-z", module);

        events.fireGuildCreate(1, "G", "L");
        assertEquals(0, eventCalls.get());
        assertFalse(homeProtect.isHomeProtectFullyDeferred());
        assertFalse(extensions.hasSubCommand("guild", "x"));
        assertFalse(extensions.getPlaceholderProviders().containsKey("ph_z"));
    }

    @Test
    void clearAll_clearsEventsAndExtensions() {
        AtomicInteger eventCalls = new AtomicInteger();
        events.onGuildCreate(data -> eventCalls.incrementAndGet());
        extensions.registerSubCommand("guild", "y", (s, a) -> { }, null);

        lifecycle.clearAll();

        events.fireGuildCreate(1, "G", "L");
        assertEquals(0, eventCalls.get());
        assertFalse(extensions.hasSubCommand("guild", "y"));
    }
}
