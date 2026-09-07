package com.guild.activity;

import com.guild.GuildPlugin;
import com.guild.core.config.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivitySettingsTest {

    @Test
    void reload_readsEnabledAndWeights() {
        GuildPlugin plugin = mock(GuildPlugin.class);
        ConfigManager configManager = mock(ConfigManager.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("guild-activity.enabled", false);
        cfg.set("guild-activity.register-info-button", false);
        cfg.set("guild-activity.score-weight-activity", 3.5);
        cfg.set("guild-activity.tick-interval-seconds", 10);
        cfg.set("guild-activity.daily-active-minutes", 2);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);

        ActivitySettings settings = new ActivitySettings(plugin);

        assertFalse(settings.isEnabled());
        assertFalse(settings.isRegisterInfoButton());
        assertEquals(3.5, settings.getScoreWeightActivity());
        assertEquals(15, settings.getTickIntervalSeconds());
        assertEquals(2, settings.getDailyActiveMinutesThreshold());
    }

    @Test
    void reload_clampsInvalidValues() {
        GuildPlugin plugin = mock(GuildPlugin.class);
        ConfigManager configManager = mock(ConfigManager.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("guild-activity.score-weight-activity", -1);
        cfg.set("guild-activity.tick-interval-seconds", 1);
        cfg.set("guild-activity.daily-active-minutes", 0);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);

        ActivitySettings settings = new ActivitySettings(plugin);

        assertEquals(0.0, settings.getScoreWeightActivity());
        assertEquals(15, settings.getTickIntervalSeconds());
        assertEquals(1, settings.getDailyActiveMinutesThreshold());
    }
}
