package com.guild.sdk;

import com.guild.sdk.command.ModuleCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleCommandRegistryTest {

    private ModuleCommandRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ModuleCommandRegistry();
    }

    @Test
    void registerSubCommand_hasSubCommandAndPermission() {
        ModuleCommandHandler handler = (sender, args) -> { };
        registry.registerSubCommand("mod-a", "guild", "quest", handler, "guild.quest");

        assertTrue(registry.hasSubCommand("guild", "quest"));
        assertNotNull(registry.getSubCommandHandler("guild", "quest"));
        assertEquals("guild.quest", registry.getSubCommandPermission("guild", "quest"));
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
}
