package com.guild.sdk.config;

import com.guild.GuildPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

public class ModuleConfigSection {
    private final String configPrefix;

    public ModuleConfigSection(GuildPlugin plugin, String moduleId) {
        this.configPrefix = "modules." + moduleId;
    }

    public String getString(String key, String defaultValue) { return defaultValue; }

    public String getString(String key) { return ""; }

    public int getInt(String key, int defaultValue) { return defaultValue; }

    public boolean getBoolean(String key, boolean defaultValue) { return defaultValue; }

    public long getLong(String key, long defaultValue) { return defaultValue; }

    public double getDouble(String key, double defaultValue) { return defaultValue; }

    public List<String> getStringList(String key) { return Collections.emptyList(); }

    public boolean contains(String key) { return false; }

    public ConfigurationSection getConfigSection() { return null; }

    public String getConfigPath() { return configPrefix; }
}
