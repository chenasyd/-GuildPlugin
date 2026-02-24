package com.guild.core.language;

import com.guild.GuildPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 语言管理器 - 管理插件的多语言系统
 */
public class LanguageManager {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<String, FileConfiguration> languageConfigs = new HashMap<>();
    private final Map<String, FileConfiguration> guiConfigs = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLanguage = "en";
    
    // 支持的语言列表
    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";
    public static final String LANG_PL = "pl";
    
    public LanguageManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadLanguages();
    }
    
    /**
     * 加载所有语言文件
     */
    private void loadLanguages() {
        // 从config.yml读取默认语言
        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        defaultLanguage = mainConfig.getString("language.default", "en");

        // 验证默认语言是否支持
        if (!isLanguageSupported(defaultLanguage)) {
            logger.warning("不支持的语言: " + defaultLanguage + "，使用默认语言: en");
            defaultLanguage = "en";
        }
        
        // 加载所有支持的语言文件
        loadLanguageFile(LANG_EN);
        loadLanguageFile(LANG_ZH);
        loadLanguageFile(LANG_PL);

        // gui.yml已废弃，所有内容已迁移到messages.yml

        logger.info("语言系统已加载，默认语言: " + defaultLanguage);
    }
    
    /**
     * 加载指定语言文件
     */
    private void loadLanguageFile(String lang) {
        String fileName = "messages_" + lang + ".yml";
        File langFile = new File(plugin.getDataFolder(), fileName);

        // 如果语言文件不存在，从jar中复制默认配置
        if (!langFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
        languageConfigs.put(lang, config);
        logger.info("加载语言文件: " + fileName);
    }

    /**
     * 加载指定GUI语言文件（已废弃，内容已合并到messages.yml）
     * @deprecated GUI配置已合并到messages.yml，不再需要单独加载
     */
    @Deprecated
    private void loadGuiLanguageFile(String lang) {
        // gui.yml已废弃，所有内容已迁移到messages.yml
        // 此方法保留以保持向后兼容性
    }
    
    /**
     * 检查语言是否支持
     */
    public boolean isLanguageSupported(String lang) {
        return lang != null && (lang.equals(LANG_EN) || lang.equals(LANG_ZH) || lang.equals(LANG_PL));
    }
    
    /**
     * 获取玩家的语言设置
     */
    public String getPlayerLanguage(Player player) {
        if (player == null) {
            return defaultLanguage;
        }
        return playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguage);
    }
    
    /**
     * 设置玩家的语言
     */
    public void setPlayerLanguage(Player player, String lang) {
        if (player == null || !isLanguageSupported(lang)) {
            return;
        }
        playerLanguages.put(player.getUniqueId(), lang);
    }
    
    /**
     * 设置玩家的语言（通过UUID）
     */
    public void setPlayerLanguage(UUID uuid, String lang) {
        if (uuid == null || !isLanguageSupported(lang)) {
            return;
        }
        playerLanguages.put(uuid, lang);
    }
    
    /**
     * 获取本地化消息
     */
    public String getMessage(String lang, String path, String defaultValue) {
        FileConfiguration config = languageConfigs.get(lang);
        if (config == null) {
            // 如果语言文件不存在，使用默认语言
            config = languageConfigs.get(defaultLanguage);
        }
        
        if (config == null) {
            return defaultValue;
        }
        
        String message = config.getString(path, defaultValue);
        return message != null ? message : defaultValue;
    }
    
    /**
     * 获取本地化消息（使用默认语言）
     */
    public String getMessage(String path, String defaultValue) {
        return getMessage(defaultLanguage, path, defaultValue);
    }
    
    /**
     * 获取玩家的本地化消息
     */
    public String getMessage(Player player, String path, String defaultValue) {
        String lang = getPlayerLanguage(player);
        return getMessage(lang, path, defaultValue);
    }
    
    /**
     * 获取本地化消息并替换占位符
     */
    public String getMessage(String lang, String path, String defaultValue, String... placeholders) {
        String message = getMessage(lang, path, defaultValue);
        
        // 替换占位符
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                message = message.replace(placeholder, value != null ? value : "");
            }
        }
        
        return message;
    }
    
    /**
     * 获取玩家的本地化消息并替换占位符
     */
    public String getMessage(Player player, String path, String defaultValue, String... placeholders) {
        String lang = getPlayerLanguage(player);
        return getMessage(lang, path, defaultValue, placeholders);
    }
    
    /**
     * 获取默认语言
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    /**
     * 设置默认语言
     */
    public void setDefaultLanguage(String lang) {
        if (isLanguageSupported(lang)) {
            this.defaultLanguage = lang;
        }
    }
    
    /**
     * 重新加载所有语言文件
     */
    public void reloadLanguages() {
        languageConfigs.clear();
        loadLanguages();
        logger.info("重新加载所有语言文件");
    }
    
    /**
     * 获取语言配置
     */
    public FileConfiguration getLanguageConfig(String lang) {
        return languageConfigs.get(lang);
    }

    /**
     * 获取GUI配置
     */
    public FileConfiguration getGuiConfig(String lang) {
        FileConfiguration config = guiConfigs.get(lang);
        if (config == null) {
            // 如果语言文件不存在，使用默认语言
            config = guiConfigs.get(defaultLanguage);
        }
        return config;
    }

    /**
     * 获取GUI消息
     */
    public String getGuiMessage(String lang, String path, String defaultValue) {
        FileConfiguration config = getGuiConfig(lang);

        if (config == null) {
            return defaultValue;
        }

        String message = config.getString(path, defaultValue);
        return message != null ? message : defaultValue;
    }

    /**
     * 获取GUI消息（使用默认语言）
     */
    public String getGuiMessage(String path, String defaultValue) {
        return getGuiMessage(defaultLanguage, path, defaultValue);
    }

    /**
     * 获取玩家的GUI消息
     */
    public String getGuiMessage(Player player, String path, String defaultValue) {
        String lang = getPlayerLanguage(player);
        return getGuiMessage(lang, path, defaultValue);
    }

    /**
     * 获取GUI消息并替换占位符
     */
    public String getGuiMessage(String lang, String path, String defaultValue, String... placeholders) {
        String message = getGuiMessage(lang, path, defaultValue);

        // 替换占位符
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                message = message.replace(placeholder, value != null ? value : "");
            }
        }

        return message;
    }

    /**
     * 获取玩家的GUI消息并替换占位符
     */
    public String getGuiMessage(Player player, String path, String defaultValue, String... placeholders) {
        String lang = getPlayerLanguage(player);
        return getGuiMessage(lang, path, defaultValue, placeholders);
    }

    /**
     * 获取GUI配置（带颜色代码转换）
     */
    public String getGuiColoredMessage(String lang, String path, String defaultValue) {
        String message = getGuiMessage(lang, path, defaultValue);
        return message.replace("&", "§");
    }

    /**
     * 获取玩家的GUI配置（带颜色代码转换）
     */
    public String getGuiColoredMessage(Player player, String path, String defaultValue) {
        String message = getGuiMessage(player, path, defaultValue);
        return message.replace("&", "§");
    }

    /**
     * 获取GUI配置（带颜色代码转换和占位符替换）
     */
    public String getGuiColoredMessage(String lang, String path, String defaultValue, String... placeholders) {
        String message = getGuiMessage(lang, path, defaultValue, placeholders);
        return message.replace("&", "§");
    }

    /**
     * 获取玩家的GUI配置（带颜色代码转换和占位符替换）
     */
    public String getGuiColoredMessage(Player player, String path, String defaultValue, String... placeholders) {
        String message = getGuiMessage(player, path, defaultValue, placeholders);
        return message.replace("&", "§");
    }
}

