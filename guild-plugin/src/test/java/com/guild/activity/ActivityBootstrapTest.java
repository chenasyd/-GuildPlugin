package com.guild.activity;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.guild.GuildPlugin;
import com.guild.core.config.ConfigManager;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.ModuleRegistry;
import com.guild.core.module.hook.GUIExtensionHook;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityBootstrapTest {

    private ServerMock server;
    private GuildPlugin plugin;
    private YamlConfiguration cfg;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = mock(GuildPlugin.class);
        ConfigManager configManager = mock(ConfigManager.class);
        cfg = new YamlConfiguration();
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ActivityBootstrapTest"));
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getServer()).thenReturn(server);
        ActivityTrackerTest.stubPluginLoader(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void exposesScoreServiceAndSettings() {
        ActivityBootstrap bootstrap = new ActivityBootstrap(plugin);

        assertNotNull(bootstrap.getScoreService());
        assertNotNull(bootstrap.getSettings());
    }

    @Test
    void start_whenDisabled_skipsTracker() {
        cfg.set("guild-activity.enabled", false);
        ActivityBootstrap bootstrap = new ActivityBootstrap(plugin);

        bootstrap.start();
        bootstrap.shutdown();
    }

    @Test
    void start_whenEnabled_startsTracker() {
        cfg.set("guild-activity.enabled", true);
        ActivityBootstrap bootstrap = new ActivityBootstrap(plugin);

        bootstrap.start();
        bootstrap.shutdown();
    }

    @Test
    void reload_disablesRunningTracker() {
        cfg.set("guild-activity.enabled", true);
        ActivityBootstrap bootstrap = new ActivityBootstrap(plugin);
        bootstrap.start();

        cfg.set("guild-activity.enabled", false);
        bootstrap.reload();
        bootstrap.shutdown();
    }

    @Test
    void registerInfoButton_noOpsWhenModuleManagerMissing() {
        cfg.set("guild-activity.enabled", true);
        cfg.set("guild-activity.register-info-button", true);
        when(plugin.getModuleManager()).thenReturn(null);

        ActivityBootstrap bootstrap = new ActivityBootstrap(plugin);
        bootstrap.registerInfoButton();
    }

    @Test
    void reload_unregistersInfoButtonWhenToggleOff() throws Exception {
        ModuleManager moduleManager = mock(ModuleManager.class);
        ModuleRegistry registry = mock(ModuleRegistry.class);
        GUIExtensionHook hook = mock(GUIExtensionHook.class);
        when(plugin.getModuleManager()).thenReturn(moduleManager);
        when(moduleManager.getRegistry()).thenReturn(registry);
        when(registry.getGuiExtensionHook()).thenReturn(hook);

        ActivityBootstrap bootstrap = new ActivityBootstrap(plugin);
        Field registered = ActivityBootstrap.class.getDeclaredField("buttonRegistered");
        registered.setAccessible(true);
        registered.setBoolean(bootstrap, true);

        cfg.set("guild-activity.enabled", false);
        cfg.set("guild-activity.register-info-button", false);
        bootstrap.reload();

        verify(hook).unregisterByModule(ActivityBootstrap.MODULE_ID);
    }
}
