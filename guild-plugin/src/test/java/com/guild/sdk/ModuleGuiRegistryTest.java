package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.sdk.gui.ModuleGUIFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ModuleGuiRegistryTest {

    private ModuleGuiRegistry registry;
    private ModuleGUIFactory factory;

    @BeforeEach
    void setUp() {
        registry = new ModuleGuiRegistry(mock(GuildPlugin.class), Logger.getLogger("test"));
        factory = mock(ModuleGUIFactory.class);
    }

    @Test
    void registerCustomGUI_duplicateGuiIdThrows() {
        registry.registerCustomGUI("mod-a", "panel", factory);

        assertThrows(IllegalArgumentException.class,
                () -> registry.registerCustomGUI("mod-b", "panel", factory));
    }

    @Test
    void clearModuleRegistrations_allowsReRegisterSameGuiId() {
        registry.registerCustomGUI("mod-a", "panel", factory);
        registry.clearModuleRegistrations("mod-a");

        assertDoesNotThrow(() -> registry.registerCustomGUI("mod-a", "panel", factory));
    }
}
