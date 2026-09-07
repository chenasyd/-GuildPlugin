package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.placeholder.PlaceholderProvider;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ModuleExtensionRegistryTest {

    private ModuleExtensionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ModuleExtensionRegistry(
                new ModuleCommandRegistry(),
                new ModulePlaceholderRegistry(),
                new ModuleGuiRegistry(mock(GuildPlugin.class), Logger.getLogger("test")));
    }

    @Test
    void clearModuleRegistrations_clearsAllDomainsForModule() {
        registry.registerSubCommand("mod-all", "guild", "x", (ModuleCommandHandler) (s, a) -> { }, null);
        registry.registerPlaceholderProvider("mod-all", new PlaceholderProvider() {
            @Override
            public String getIdentifier() {
                return "ph_all";
            }

            @Override
            public String onRequest(Player player, String params) {
                return "";
            }
        });
        registry.registerCustomGUI("mod-all", "gui-all", mock(ModuleGUIFactory.class));

        registry.clearModuleRegistrations("mod-all");

        assertFalse(registry.hasSubCommand("guild", "x"));
        assertFalse(registry.getPlaceholderProviders().containsKey("ph_all"));
        assertDoesNotThrow(() -> registry.registerCustomGUI("mod-all", "gui-all", mock(ModuleGUIFactory.class)));
    }

    @Test
    void clearAll_clearsCommandsAndPlaceholders() {
        registry.registerSubCommand("guild", "y", (s, a) -> { }, null);
        registry.registerPlaceholderProvider(new PlaceholderProvider() {
            @Override
            public String getIdentifier() {
                return "ph_clear";
            }

            @Override
            public String onRequest(Player player, String params) {
                return "";
            }
        });

        registry.clearAll();

        assertFalse(registry.hasSubCommand("guild", "y"));
        assertFalse(registry.getPlaceholderProviders().containsKey("ph_clear"));
    }
}
