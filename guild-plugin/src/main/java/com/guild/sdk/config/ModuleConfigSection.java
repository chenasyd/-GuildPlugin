package com.guild.sdk.config;

import com.guild.GuildPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * 模块配置段读取器 - 提供模块私有的配置空间
 */
public class ModuleConfigSection {

    private final GuildPlugin plugin;
    private final String configPrefix;

    public ModuleConfigSection(GuildPlugin plugin, String moduleId) {
        this.plugin = plugin;
        this.configPrefix = "modules." + moduleId;
    }

    /** 获取字符串配置值 */
    public String getString(String key, String defaultValue) {
        ConfigurationSection config = getConfigSection();
        if (config == null || !config.contains(key)) {
            return defaultValue;
        }
        return config.getString(key, defaultValue);
    }

    /** 获取字符串配置值（默认为空字符串） */
    public String getString(String key) {
        return getString(key, "");
    }

    /** 获取整型配置值 */
    public int getInt(String key, int defaultValue) {
        ConfigurationSection config = getConfigSection();
        if (config == null || !config.contains(key)) {
            return defaultValue;
        }
        return config.getInt(key, defaultValue);
    }

    /** 获取布尔型配置值 */
    public boolean getBoolean(String key, boolean defaultValue) {
        ConfigurationSection config = getConfigSection();
        if (config == null || !config.contains(key)) {
            return defaultValue;
        }
        return config.getBoolean(key, defaultValue);
    }

    /** 获取长整型配置值 */
    public long getLong(String key, long defaultValue) {
        ConfigurationSection config = getConfigSection();
        if (config == null || !config.contains(key)) {
            return defaultValue;
        }
        return config.getLong(key, defaultValue);
    }

    /** 获取浮点型配置值 */
    public double getDouble(String key, double defaultValue) {
        ConfigurationSection config = getConfigSection();
        if (config == null || !config.contains(key)) {
            return defaultValue;
        }
        return config.getDouble(key, defaultValue);
    }

    /** 获取字符串列表配置值 */
    public List<String> getStringList(String key) {
        ConfigurationSection config = getConfigSection();
        if (config == null || !config.contains(key)) {
            return Collections.emptyList();
        }
        return config.getStringList(key);
    }

    /** 检查某个键是否存在 */
    public boolean contains(String key) {
        ConfigurationSection config = getConfigSection();
        return config != null && config.contains(key);
    }

    /** 直接获取整个配置段 */
    public ConfigurationSection getConfigSection() {
        return plugin.getConfigManager().getMainConfig()
                .getConfigurationSection(configPrefix);
    }

    /** 获取配置段完整路径 */
    public String getConfigPath() { return configPrefix; }
}
