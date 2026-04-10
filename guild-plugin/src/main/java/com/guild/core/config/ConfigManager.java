package com.guild.core.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.guild.GuildPlugin;

/**
 * 配置管理器 - 管理插件的所有配置文件
 */
public class ConfigManager {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();
    
    public ConfigManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadConfigs();
    }
    
    /**
     * 加载所有配置文件
     */
    private void loadConfigs() {
        // 主配置文件
        loadConfig("config.yml");

        // GUI配置文件（已废弃，现在使用多语言GUI配置）
        // gui.yml已废弃，所有内容已迁移到messages.yml

        // 数据库配置文件
        loadConfig("database.yml");
    }
    
    /**
     * 加载指定配置文件
     */
    public void loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        
        // 如果配置文件不存在，从jar中复制默认配置
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        configs.put(fileName, config);
        configFiles.put(fileName, configFile);
        
        logger.info("加载配置文件: " + fileName);
    }
    
    /**
     * 获取配置文件
     */
    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }
    
    /**
     * 获取主配置文件
     */
    public FileConfiguration getMainConfig() {
        return getConfig("config.yml");
    }
    
    /**
     * 获取GUI配置文件（已废弃，请使用LanguageManager.getMessage）
     * @deprecated 请使用 {@code LanguageManager.getMessage()} 方法
     */
    @Deprecated
    public FileConfiguration getGuiConfig() {
        // gui.yml已废弃，所有内容已迁移到messages.yml
        // 直接返回空配置，不再加载gui.yml文件
        return new YamlConfiguration();
    }
    
    /**
     * 获取数据库配置文件
     */
    public FileConfiguration getDatabaseConfig() {
        return getConfig("database.yml");
    }
    
    /**
     * 保存主配置文件
     */
    public void saveMainConfig() {
        saveConfig("config.yml");
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        File configFile = configFiles.get(fileName);
        
        if (config != null && configFile != null) {
            try {
                config.save(configFile);
                logger.info("保存配置文件: " + fileName);
            } catch (IOException e) {
                logger.severe("保存配置文件失败: " + fileName + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * 重新加载配置文件
     */
    public void reloadConfig(String fileName) {
        loadConfig(fileName);
        logger.info("重新加载配置文件: " + fileName);
    }
    
    /**
     * 重新加载所有配置文件
     */
    public void reloadAllConfigs() {
        configs.clear();
        configFiles.clear();
        loadConfigs();
        logger.info("重新加载所有配置文件");
    }
    
    /**
     * 获取字符串配置，支持颜色代码
     */
    public String getString(String fileName, String path, String defaultValue) {
        FileConfiguration config = getConfig(fileName);
        if (config == null) return defaultValue;
        
        String value = config.getString(path, defaultValue);
        return value != null ? value.replace("&", "§") : defaultValue;
    }
    
    /**
     * 获取整数配置
     */
    public int getInt(String fileName, String path, int defaultValue) {
        FileConfiguration config = getConfig(fileName);
        if (config == null) return defaultValue;
        
        return config.getInt(path, defaultValue);
    }
    
    /**
     * 获取布尔配置
     */
    public boolean getBoolean(String fileName, String path, boolean defaultValue) {
        FileConfiguration config = getConfig(fileName);
        if (config == null) return defaultValue;
        
        return config.getBoolean(path, defaultValue);
    }
}
