package com.guild.core.language;

import com.guild.GuildPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class LanguageManager {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<String, FileConfiguration> languageConfigs = new HashMap<>();
    private final Map<String, FileConfiguration> guiConfigs = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLanguage = "en";
    
    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";
    public static final String LANG_PL = "pl";
    public static final String LANG_BR = "br";
    
    private static final String MESSAGE_FILE_PREFIX = "messages_";
    private static final String MESSAGE_FILE_SUFFIX = ".yml";
    
    public LanguageManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadLanguages();
    }
    
    private void loadLanguages() {
        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        defaultLanguage = mainConfig.getString("language.default", "en");

        discoverAndLoadLanguageFiles();

        if (!languageConfigs.containsKey(defaultLanguage)) {
            logger.warning("language.default \u8bbe\u7f6e\u4e3a '" + defaultLanguage + "' \u4f46\u672a\u627e\u5230\u5bf9\u5e94\u7684\u8bed\u8a00\u6587\u4ef6 messages_" + defaultLanguage + ".yml");
            if (languageConfigs.containsKey("en")) {
                logger.warning("\u56de\u9000\u5230\u9ed8\u8ba4\u8bed\u8a00: en");
                defaultLanguage = "en";
            } else if (!languageConfigs.isEmpty()) {
                String fallback = languageConfigs.keySet().iterator().next();
                logger.warning("\u56de\u9000\u5230\u53ef\u7528\u8bed\u8a00: " + fallback);
                defaultLanguage = fallback;
            } else {
                logger.severe("\u672a\u52a0\u8f7d\u4efb\u4f55\u8bed\u8a00\u6587\u4ef6\uff0c\u63d2\u4ef6\u5c06\u4f7f\u7528\u786c\u7f16\u7801\u9ed8\u8ba4\u503c");
                defaultLanguage = "en";
            }
        }

        List<String> additionalLangs = mainConfig.getStringList("language.additional-languages");
        for (String lang : additionalLangs) {
            lang = lang.trim().toLowerCase();
            if (!languageConfigs.containsKey(lang)) {
                loadLanguageFile(lang);
            }
        }

        logger.info("\u8bed\u8a00\u7cfb\u7edf\u5df2\u52a0\u8f7d\uff0c\u9ed8\u8ba4\u8bed\u8a00: " + defaultLanguage
            + "\uff0c\u5df2\u52a0\u8f7d\u8bed\u8a00: " + String.join(", ", getLoadedLanguages()));
    }
    
    private void discoverAndLoadLanguageFiles() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File[] existingFiles = dataFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(MESSAGE_FILE_PREFIX) && name.endsWith(MESSAGE_FILE_SUFFIX);
            }
        });

        if (existingFiles != null) {
            for (File file : existingFiles) {
                String fileName = file.getName();
                String langCode = fileName.substring(
                    MESSAGE_FILE_PREFIX.length(),
                    fileName.length() - MESSAGE_FILE_SUFFIX.length()
                );
                langCode = langCode.toLowerCase();
                loadLanguageFileFromDisk(langCode, file);
            }
        }

        discoverBundledLanguages();
    }
    
    private void discoverBundledLanguages() {
        try (InputStream indexStream = plugin.getResource("languages.index")) {
            if (indexStream != null) {
                byte[] bytes = indexStream.readAllBytes();
                String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                String[] langs = content.split("[\\r\\n]+");
                for (String lang : langs) {
                    lang = lang.trim().toLowerCase();
                    if (!lang.isEmpty() && !languageConfigs.containsKey(lang)) {
                        loadLanguageFile(lang);
                    }
                }
                return;
            }
        } catch (IOException e) {
            // ignore
        }

        String[] knownBundledLangs = {LANG_EN, LANG_ZH, LANG_PL, LANG_BR};
        for (String lang : knownBundledLangs) {
            if (!languageConfigs.containsKey(lang)) {
                loadLanguageFile(lang);
            }
        }
    }
    
    private void loadLanguageFile(String lang) {
        String fileName = MESSAGE_FILE_PREFIX + lang + MESSAGE_FILE_SUFFIX;
        File langFile = new File(plugin.getDataFolder(), fileName);

        if (!langFile.exists()) {
            try (InputStream resource = plugin.getResource(fileName)) {
                if (resource != null) {
                    plugin.saveResource(fileName, false);
                } else {
                    logger.warning("\u672a\u627e\u5230\u5185\u7f6e\u8bed\u8a00\u6587\u4ef6: " + fileName + "\uff0c\u8df3\u8fc7\u52a0\u8f7d");
                    return;
                }
            } catch (Exception e) {
                logger.warning("\u65e0\u6cd5\u91ca\u653e\u8bed\u8a00\u6587\u4ef6 " + fileName + ": " + e.getMessage());
                return;
            }
        }

        loadLanguageFileFromDisk(lang, langFile);
    }
    
    private void loadLanguageFileFromDisk(String lang, File langFile) {
        if (!langFile.exists()) {
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            if (config.getKeys(false).isEmpty()) {
                logger.warning("\u8bed\u8a00\u6587\u4ef6\u4e3a\u7a7a: " + langFile.getName() + "\uff0c\u8df3\u8fc7");
                return;
            }
            languageConfigs.put(lang.toLowerCase(), config);
            logger.info("\u52a0\u8f7d\u8bed\u8a00\u6587\u4ef6: " + langFile.getName());
        } catch (Exception e) {
            logger.warning("\u52a0\u8f7d\u8bed\u8a00\u6587\u4ef6\u5931\u8d25: " + langFile.getName() + " - " + e.getMessage());
        }
    }
    
    @Deprecated
    private void loadGuiLanguageFile(String lang) {
    }
    
    public boolean isLanguageSupported(String lang) {
        return lang != null && languageConfigs.containsKey(lang.toLowerCase());
    }
    
    public List<String> getLoadedLanguages() {
        return new ArrayList<>(languageConfigs.keySet());
    }
    
    public List<String> getAvailableLanguageNames() {
        List<String> names = new ArrayList<>();
        Map<String, String> codeToName = new HashMap<>();
        codeToName.put("en", "English");
        codeToName.put("zh", "\u4e2d\u6587");
        codeToName.put("pl", "Polski");
        codeToName.put("br", "Portugu\u00eas (BR)");
        codeToName.put("de", "Deutsch");
        codeToName.put("fr", "Fran\u00e7ais");
        codeToName.put("es", "Espa\u00f1ol");
        codeToName.put("ja", "\u65e5\u672c\u8a9e");
        codeToName.put("ko", "\ud55c\uad6d\uc5b4");
        codeToName.put("ru", "\u0420\u0443\u0441\u0441\u043a\u0438\u0439");
        codeToName.put("it", "Italiano");
        codeToName.put("nl", "Nederlands");
        codeToName.put("sv", "Svenska");
        codeToName.put("tr", "T\u00fcrk\u00e7e");
        codeToName.put("vi", "Ti\u1ebfng Vi\u1ec7t");
        codeToName.put("th", "\u0e44\u0e17\u0e22");
        codeToName.put("ar", "\u0627\u0644\u0639\u0631\u0628\u064a\u0629");
        codeToName.put("cs", "\u010ce\u0161tina");
        codeToName.put("pt", "Portugu\u00eas");
        codeToName.put("uk", "\u0423\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430");
        codeToName.put("ro", "Rom\u00e2n\u0103");
        codeToName.put("hu", "Magyar");
        codeToName.put("da", "Dansk");
        codeToName.put("fi", "Suomi");
        codeToName.put("no", "Norsk");

        for (String code : languageConfigs.keySet()) {
            String displayName = codeToName.getOrDefault(code, code.toUpperCase());
            names.add(code + " (" + displayName + ")");
        }
        Collections.sort(names);
        return names;
    }
    
    public String getPlayerLanguage(Player player) {
        if (player == null) {
            return defaultLanguage;
        }
        return playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguage);
    }
    
    public void setPlayerLanguage(Player player, String lang) {
        if (player == null || !isLanguageSupported(lang)) {
            return;
        }
        playerLanguages.put(player.getUniqueId(), lang.toLowerCase());
    }
    
    public void setPlayerLanguage(UUID uuid, String lang) {
        if (uuid == null || !isLanguageSupported(lang)) {
            return;
        }
        playerLanguages.put(uuid, lang.toLowerCase());
    }
    
    public String getMessage(String lang, String path, String defaultValue) {
        if (lang != null) {
            lang = lang.toLowerCase();
        }
        FileConfiguration config = languageConfigs.get(lang);
        if (config == null) {
            config = languageConfigs.get(defaultLanguage);
        }
        
        if (config == null) {
            return defaultValue;
        }
        
        String message = config.getString(path, defaultValue);
        return message != null ? message : defaultValue;
    }
    
    public String getMessage(String path, String defaultValue) {
        return getMessage(defaultLanguage, path, defaultValue);
    }
    
    public String getMessage(Player player, String path, String defaultValue) {
        String lang = getPlayerLanguage(player);
        return getMessage(lang, path, defaultValue);
    }
    
    public String getMessage(String lang, String path, String defaultValue, String... placeholders) {
        String message = getMessage(lang, path, defaultValue);
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                message = message.replace(placeholder, value != null ? value : "");
            }
        }
        
        return message;
    }
    
    public String getMessage(Player player, String path, String defaultValue, String... placeholders) {
        String lang = getPlayerLanguage(player);
        return getMessage(lang, path, defaultValue, placeholders);
    }
    
    public String getIndexedMessage(String lang, String path, String defaultValue, String[] args) {
        String message = getMessage(lang, path, defaultValue);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", args[i] != null ? args[i] : "");
            }
        }
        return message;
    }

    public String getIndexedMessage(String path, String defaultValue, String... args) {
        String message = getMessage(defaultLanguage, path, defaultValue);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", args[i] != null ? args[i] : "");
            }
        }
        return message;
    }

    public String getIndexedMessage(Player player, String path, String defaultValue, String... args) {
        String message = getMessage(getPlayerLanguage(player), path, defaultValue);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", args[i] != null ? args[i] : "");
            }
        }
        return message;
    }
    
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    public void setDefaultLanguage(String lang) {
        if (isLanguageSupported(lang)) {
            this.defaultLanguage = lang.toLowerCase();
        }
    }
    
    public void reloadLanguages() {
        languageConfigs.clear();
        loadLanguages();
        logger.info("\u91cd\u65b0\u52a0\u8f7d\u6240\u6709\u8bed\u8a00\u6587\u4ef6");
    }
    
    public FileConfiguration getLanguageConfig(String lang) {
        return languageConfigs.get(lang != null ? lang.toLowerCase() : null);
    }

    public FileConfiguration getGuiConfig(String lang) {
        FileConfiguration config = guiConfigs.get(lang);
        if (config == null) {
            config = guiConfigs.get(defaultLanguage);
        }
        return config;
    }

    public String getGuiMessage(String lang, String path, String defaultValue) {
        FileConfiguration config = getGuiConfig(lang);

        if (config == null) {
            return defaultValue;
        }

        String message = config.getString(path, defaultValue);
        return message != null ? message : defaultValue;
    }

    public String getGuiMessage(String path, String defaultValue) {
        return getGuiMessage(defaultLanguage, path, defaultValue);
    }

    public String getGuiMessage(Player player, String path, String defaultValue) {
        String lang = getPlayerLanguage(player);
        return getGuiMessage(lang, path, defaultValue);
    }

    public String getGuiMessage(String lang, String path, String defaultValue, String... placeholders) {
        String message = getGuiMessage(lang, path, defaultValue);

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                message = message.replace(placeholder, value != null ? value : "");
            }
        }

        return message;
    }

    public String getGuiMessage(Player player, String path, String defaultValue, String... placeholders) {
        String lang = getPlayerLanguage(player);
        return getGuiMessage(lang, path, defaultValue, placeholders);
    }

    public String getGuiColoredMessage(String lang, String path, String defaultValue) {
        String message = getGuiMessage(lang, path, defaultValue);
        return message.replace("&", "\u00a7");
    }

    public String getGuiColoredMessage(Player player, String path, String defaultValue) {
        String message = getGuiMessage(player, path, defaultValue);
        return message.replace("&", "\u00a7");
    }

    public String getGuiColoredMessage(String lang, String path, String defaultValue, String... placeholders) {
        String message = getGuiMessage(lang, path, defaultValue, placeholders);
        return message.replace("&", "\u00a7");
    }

    public String getGuiColoredMessage(Player player, String path, String defaultValue, String... placeholders) {
        String message = getGuiMessage(player, path, defaultValue, placeholders);
        return message.replace("&", "\u00a7");
    }
}
