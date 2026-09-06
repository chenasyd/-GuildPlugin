package com.guild.core.gui.imago;

import com.guild.GuildPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImagoGuiIntegrationTest {

    @Test
    void beforeInitialize_imagoQueriesReturnFalse() {
        GuildPlugin plugin = Mockito.mock(GuildPlugin.class);
        ImagoGuiIntegration integration = new ImagoGuiIntegration(plugin, Logger.getLogger("test"));

        assertFalse(integration.isAvailable());
        assertFalse(integration.isImageGuiActive("MainGuildGUI"));
        assertFalse(integration.isImageLayoutActive("MainGuildGUI"));
        assertNull(integration.getImageLayoutConfig());
    }

    @Test
    void isImageLayoutActive_falseForBedrockPlayerWithoutServerCheck() {
        GuildPlugin plugin = Mockito.mock(GuildPlugin.class);
        ImagoGuiIntegration integration = new ImagoGuiIntegration(plugin, Logger.getLogger("test"));

        assertFalse(integration.isImageLayoutActive(null, "MainGuildGUI"));
    }
}
