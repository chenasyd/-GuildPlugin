package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.placeholder.PlaceholderProvider;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ModuleExtensionRegistryTest {

    private ModuleExtensionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ModuleExtensionRegistry(mock(GuildPlugin.class), Logger.getLogger("test"));
    }

    @Test
    void registerSubCommand_hasSubCommandAndPermission() {
        ModuleCommandHandler handler = (sender, args) -> { };
        registry.registerSubCommand("mod-a", "guild", "quest", handler, "guild.quest");

        assertTrue(registry.hasSubCommand("guild", "quest"));
        assertNotNull(registry.getSubCommandHandler("guild", "quest"));
        assertEqualsPermission("guild.quest", registry.getSubCommandPermission("guild", "quest"));
    }

    private static void assertEqualsPermission(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    @Test
    void getSubCommands_returnsRegisteredNames() {
        registry.registerSubCommand("guild", "a", (sender, args) -> { }, null);
        registry.registerSubCommand("guild", "b", (sender, args) -> { }, null);

        assertTrue(registry.getSubCommands("guild").contains("a"));
        assertTrue(registry.getSubCommands("guild").contains("b"));
    }

    @Test
    void clearModuleRegistrations_removesOwnedCommands() {
        registry.registerSubCommand("mod-x", "guild", "war", (sender, args) -> { }, "perm.war");

        registry.clearModuleRegistrations("mod-x");

        assertFalse(registry.hasSubCommand("guild", "war"));
    }

    @Test
    void registerPlaceholderProvider_andClearByModule() {
        PlaceholderProvider provider = new PlaceholderProvider() {
            @Override
            public String getIdentifier() {
                return "test_ph";
            }

            @Override
            public String onRequest(Player player, String params) {
                return "ok";
            }
        };
        registry.registerPlaceholderProvider("mod-y", provider);

        assertTrue(registry.getPlaceholderProviders().containsKey("test_ph"));

        registry.clearModuleRegistrations("mod-y");

        assertFalse(registry.getPlaceholderProviders().containsKey("test_ph"));
    }

    @Test
    void unregisterPlaceholderProvider_removesEntry() {
        PlaceholderProvider provider = new PlaceholderProvider() {
            @Override
            public String getIdentifier() {
                return "id2";
            }

            @Override
            public String onRequest(Player player, String params) {
                return "";
            }
        };
        registry.registerPlaceholderProvider(provider);
        registry.unregisterPlaceholderProvider("id2");

        assertNull(registry.getPlaceholderProviders().get("id2"));
    }
}
