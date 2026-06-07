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
import java.util.HashSet;
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
    /** lang/core/ 目录下的核心（非GUI）消息配置 */
    private final Map<String, FileConfiguration> coreConfigs = new HashMap<>();
    /** lang/modules/ 目录下的模块消息配置（合并存储，按语言索引） */
    private final Map<String, FileConfiguration> moduleConfigs = new HashMap<>();
    private final Set<String> supportedLanguages = new HashSet<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLanguage = "en";
    
    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh";
    public static final String LANG_PL = "pl";
    public static final String LANG_BR = "br";
    
    private static final String MESSAGE_FILE_PREFIX = "messages_";
    private static final String MESSAGE_FILE_SUFFIX = ".yml";
    private static final String GUI_LANG_PATH = "lang/gui/";
    private static final String CORE_LANG_PATH = "lang/core/";
    private static final String MODULES_LANG_PATH = "lang/modules/";
    private static final String LANG_FILE_SUFFIX = ".yml";
    
    public LanguageManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadLanguages();
        loadCoreLanguages();
        loadModuleLanguages();
        loadGuiLanguages();
    }
    
    private void loadLanguages() {
        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        defaultLanguage = mainConfig.getString("language.default", LANG_EN).toLowerCase();

        discoverAndLoadLanguageFiles();

        if (!languageConfigs.containsKey(defaultLanguage)) {
            logger.warning("language.default is set to '" + defaultLanguage + "' but the corresponding external or bundled language file was not loaded.");
            if (languageConfigs.containsKey(LANG_EN)) {
                logger.warning("Falling back to default language: en");
                defaultLanguage = LANG_EN;
            } else if (!languageConfigs.isEmpty()) {
                String fallback = languageConfigs.keySet().iterator().next();
                logger.warning("Falling back to available language: " + fallback);
                defaultLanguage = fallback;
            } else {
                logger.severe("No language files loaded, plugin will use hardcoded defaults.");
                defaultLanguage = LANG_EN;
            }
        }

        List<String> additionalLangs = mainConfig.getStringList("language.additional-languages");
        for (String lang : additionalLangs) {
            lang = lang.trim().toLowerCase();
            if (!languageConfigs.containsKey(lang)) {
                loadLanguageFile(lang);
            }
        }

        logger.info("Language system loaded, default language: " + defaultLanguage
            + ", loaded languages: " + String.join(", ", getLoadedLanguages()));
    }

    /**
     * 从插件 JAR 内的 lang/core/ 目录加载核心（非GUI）语言文件
     */
    private void loadCoreLanguages() {
        loadExternalCoreLanguages();

        String[] knownLangs = {LANG_EN, LANG_ZH, LANG_PL, LANG_BR};
        for (String lang : knownLangs) {
            String resourcePath = CORE_LANG_PATH + lang + LANG_FILE_SUFFIX;
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in != null) {
                    String yaml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    FileConfiguration config = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
                    if (!config.getKeys(false).isEmpty()) {
                        coreConfigs.putIfAbsent(lang, config);
                        supportedLanguages.add(lang);
                        logger.info("Loaded core language file: " + resourcePath);
                    }
                } else {
                    logger.warning("Core language file not found in JAR: " + resourcePath);
                }
            } catch (Exception e) {
                logger.warning("Failed to load core language file " + resourcePath + ": " + e.getMessage());
            }
        }
    }

    /**
     * 从插件数据目录和 JAR 内的 lang/modules/{module_name}/{lang}.yml 加载模块语言文件
     */
    private void loadModuleLanguages() {
        loadExternalModuleLanguages();

        // 已知模块目录列表（后续可由模块自身注册）
        String[] moduleDirs = {"system", "announcement", "quest", "member-rank", "stats"};
        String[] knownLangs = {LANG_EN, LANG_ZH, LANG_PL, LANG_BR};

        for (String moduleDir : moduleDirs) {
            for (String lang : knownLangs) {
                String resourcePath = MODULES_LANG_PATH + moduleDir + "/" + lang + LANG_FILE_SUFFIX;
                try (InputStream in = plugin.getResource(resourcePath)) {
                    if (in != null) {
                        String yaml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        FileConfiguration config = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
                        if (!config.getKeys(false).isEmpty()) {
                            mergeModuleConfig(lang, config);
                            supportedLanguages.add(lang);
                            logger.info("Loaded module language file: " + resourcePath);
                        }
                    }
                } catch (Exception e) {
                    logger.warning("Failed to load module language file " + resourcePath + ": " + e.getMessage());
                }
            }
        }
    }

    private void loadExternalCoreLanguages() {
        File coreDir = new File(plugin.getDataFolder(), CORE_LANG_PATH);
        if (!coreDir.exists() || !coreDir.isDirectory()) {
            return;
        }

        File[] langFiles = coreDir.listFiles((dir, name) -> name.endsWith(LANG_FILE_SUFFIX));
        if (langFiles == null) {
            return;
        }

        for (File langFile : langFiles) {
            String fileName = langFile.getName();
            String lang = fileName.substring(0, fileName.length() - LANG_FILE_SUFFIX.length()).toLowerCase();
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                if (!config.getKeys(false).isEmpty()) {
                    coreConfigs.put(lang, config);
                    supportedLanguages.add(lang);
                    logger.info("Loaded external core language file: " + langFile.getPath());
                }
            } catch (Exception e) {
                logger.warning("Failed to load external core language file " + langFile.getPath() + ": " + e.getMessage());
            }
        }
    }

    private void loadExternalGuiLanguages() {
        File guiDir = new File(plugin.getDataFolder(), GUI_LANG_PATH);
        if (!guiDir.exists() || !guiDir.isDirectory()) {
            return;
        }

        File[] langFiles = guiDir.listFiles((dir, name) -> name.endsWith(LANG_FILE_SUFFIX));
        if (langFiles == null) {
            return;
        }

        for (File langFile : langFiles) {
            String fileName = langFile.getName();
            String lang = fileName.substring(0, fileName.length() - LANG_FILE_SUFFIX.length()).toLowerCase();
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                if (!config.getKeys(false).isEmpty()) {
                    guiConfigs.put(lang, config);
                    supportedLanguages.add(lang);
                    logger.info("Loaded external GUI language file: " + langFile.getPath());
                }
            } catch (Exception e) {
                logger.warning("Failed to load external GUI language file " + langFile.getPath() + ": " + e.getMessage());
            }
        }
    }

    private void loadExternalModuleLanguages() {
        File modulesRoot = new File(plugin.getDataFolder(), MODULES_LANG_PATH);
        if (!modulesRoot.exists() || !modulesRoot.isDirectory()) {
            return;
        }

        File[] moduleDirs = modulesRoot.listFiles(File::isDirectory);
        if (moduleDirs == null) {
            return;
        }

        for (File moduleDir : moduleDirs) {
            File[] langFiles = moduleDir.listFiles((dir, name) -> name.endsWith(LANG_FILE_SUFFIX));
            if (langFiles == null) {
                continue;
            }
            for (File langFile : langFiles) {
                String fileName = langFile.getName();
                String lang = fileName.substring(0, fileName.length() - LANG_FILE_SUFFIX.length()).toLowerCase();
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                    if (!config.getKeys(false).isEmpty()) {
                        mergeModuleConfig(lang, config);
                        supportedLanguages.add(lang);
                        logger.info("Loaded external module language file: " + langFile.getPath());
                    }
                } catch (Exception e) {
                    logger.warning("Failed to load external module language file " + langFile.getPath() + ": " + e.getMessage());
                }
            }
        }
    }

    private void mergeModuleConfig(String lang, FileConfiguration config) {
        FileConfiguration existing = moduleConfigs.get(lang);
        if (existing == null) {
            moduleConfigs.put(lang, config);
        } else {
            for (String key : config.getKeys(true)) {
                existing.set(key, config.get(key));
            }
        }
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

        if (langFile.exists()) {
            if (loadLanguageFileFromDisk(lang, langFile)) {
                return;
            }
            logger.warning("Failed to load external language file: " + fileName + ". Trying bundled fallback.");
        }

        try (InputStream resource = plugin.getResource(fileName)) {
            if (resource == null) {
                logger.warning("Language file not found externally and no bundled fallback available: " + fileName);
                return;
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(resource, java.nio.charset.StandardCharsets.UTF_8));
            if (config.getKeys(false).isEmpty()) {
                logger.warning("Bundled language file is empty: " + fileName + ", skipping.");
                return;
            }
            languageConfigs.put(lang.toLowerCase(), config);
            supportedLanguages.add(lang.toLowerCase());
            logger.info("Loaded bundled language file: " + fileName);
        } catch (Exception e) {
            logger.warning("Failed to load bundled language file: " + fileName + " - " + e.getMessage());
        }
    }

    private boolean loadLanguageFileFromDisk(String lang, File langFile) {
        if (!langFile.exists()) {
            return false;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            if (config.getKeys(false).isEmpty()) {
                logger.warning("Language file is empty: " + langFile.getName() + ", skipping.");
                return false;
            }
            languageConfigs.put(lang.toLowerCase(), config);
            supportedLanguages.add(lang.toLowerCase());
            logger.info("Loaded external language file: " + langFile.getName());
            return true;
        } catch (Exception e) {
            logger.warning("Failed to load external language file: " + langFile.getName() + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 从插件 JAR 内的 lang/gui/ 目录加载 GUI 专用语言文件
     */
    private void loadGuiLanguages() {
        loadExternalGuiLanguages();

        String[] knownLangs = {LANG_EN, LANG_ZH, LANG_PL, LANG_BR};
        for (String lang : knownLangs) {
            if (guiConfigs.containsKey(lang)) {
                continue;
            }
            String resourcePath = GUI_LANG_PATH + lang + LANG_FILE_SUFFIX;
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in != null) {
                    String yaml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    FileConfiguration config = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
                    if (!config.getKeys(false).isEmpty()) {
                        guiConfigs.put(lang, config);
                        supportedLanguages.add(lang);
                        logger.info("Loaded bundled GUI language file: " + resourcePath);
                    }
                } else {
                    logger.warning("Bundled GUI language file not found: " + resourcePath);
                }
            } catch (Exception e) {
                logger.warning("Failed to load bundled GUI language file " + resourcePath + ": " + e.getMessage());
            }
        }
    }
    
    public boolean isLanguageSupported(String lang) {
        return lang != null && supportedLanguages.contains(lang.toLowerCase());
    }
    
    public List<String> getLoadedLanguages() {
        List<String> languages = new ArrayList<>(supportedLanguages);
        Collections.sort(languages);
        return languages;
    }
    
    private String resolveLanguage(String lang) {
        if (lang != null) {
            lang = lang.toLowerCase();
        }
        if (lang != null && supportedLanguages.contains(lang)) {
            return lang;
        }
        if (supportedLanguages.contains(defaultLanguage)) {
            return defaultLanguage;
        }
        if (supportedLanguages.contains(LANG_EN)) {
            return LANG_EN;
        }
        return supportedLanguages.stream().findFirst().orElse(LANG_EN);
    }

    private FileConfiguration getConfig(Map<String, FileConfiguration> configs, String lang) {
        String resolved = resolveLanguage(lang);
        FileConfiguration config = configs.get(resolved);
        if (config == null && !LANG_EN.equals(resolved)) {
            config = configs.get(LANG_EN);
        }
        return config;
    }
    
    public List<String> getAvailableLanguageNames() {
        List<String> names = new ArrayList<>();
        Map<String, String> codeToName = new HashMap<>();
        codeToName.put("en", "English");
        codeToName.put("zh", "Chinese");
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
        coreConfigs.clear();
        moduleConfigs.clear();
        guiConfigs.clear();
        supportedLanguages.clear();
        loadLanguages();
        loadCoreLanguages();
        loadModuleLanguages();
        loadGuiLanguages();
        logger.info("Reloaded all language files (main + core + modules + gui)");
    }
    
    public FileConfiguration getLanguageConfig(String lang) {
        return languageConfigs.get(lang != null ? lang.toLowerCase() : null);
    }

    public FileConfiguration getGuiConfig(String lang) {
        return getConfig(guiConfigs, lang);
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

    // ==================== Module 消息（lang/modules/）====================

    public String getModuleMessage(String lang, String path, String defaultValue) {
        FileConfiguration config = getConfig(moduleConfigs, lang);
        if (config == null) {
            return defaultValue;
        }
        String message = config.getString(path, defaultValue);
        return message != null ? message : defaultValue;
    }

    public String getModuleMessage(String path, String defaultValue) {
        return getModuleMessage(defaultLanguage, path, defaultValue);
    }

    public String getModuleMessage(Player player, String path, String defaultValue) {
        String lang = getPlayerLanguage(player);
        return getModuleMessage(lang, path, defaultValue);
    }

    public String getModuleMessage(String lang, String path, String defaultValue, String... placeholders) {
        String message = getModuleMessage(lang, path, defaultValue);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                message = message.replace(placeholder, value != null ? value : "");
            }
        }
        return message;
    }

    public String getModuleMessage(Player player, String path, String defaultValue, String... placeholders) {
        String lang = getPlayerLanguage(player);
        return getModuleMessage(lang, path, defaultValue, placeholders);
    }

    public String getModuleIndexedMessage(String lang, String path, String defaultValue, String[] args) {
        String message = getModuleMessage(lang, path, defaultValue);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", args[i] != null ? args[i] : "");
            }
        }
        return message;
    }

    public String getModuleIndexedMessage(String path, String defaultValue, String... args) {
        return getModuleIndexedMessage(defaultLanguage, path, defaultValue, args);
    }

    public String getModuleIndexedMessage(Player player, String path, String defaultValue, String... args) {
        String lang = getPlayerLanguage(player);
        return getModuleIndexedMessage(lang, path, defaultValue, args);
    }

    // ==================== Core 消息（lang/core/）====================

    public FileConfiguration getCoreConfig(String lang) {
        return getConfig(coreConfigs, lang);
    }

    public String getCoreMessage(String lang, String path, String defaultValue) {
        FileConfiguration config = getCoreConfig(lang);
        if (config == null) {
            return defaultValue;
        }
        String message = config.getString(path, defaultValue);
        return message != null ? message : defaultValue;
    }

    public String getCoreMessage(String path, String defaultValue) {
        return getCoreMessage(defaultLanguage, path, defaultValue);
    }

    public String getCoreMessage(Player player, String path, String defaultValue) {
        String lang = getPlayerLanguage(player);
        return getCoreMessage(lang, path, defaultValue);
    }

    public String getCoreMessage(String lang, String path, String defaultValue, String... placeholders) {
        String message = getCoreMessage(lang, path, defaultValue);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                message = message.replace(placeholder, value != null ? value : "");
            }
        }
        return message;
    }

    public String getCoreMessage(Player player, String path, String defaultValue, String... placeholders) {
        String lang = getPlayerLanguage(player);
        return getCoreMessage(lang, path, defaultValue, placeholders);
    }

    public String getCoreIndexedMessage(String lang, String path, String defaultValue, String[] args) {
        String message = getCoreMessage(lang, path, defaultValue);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", args[i] != null ? args[i] : "");
            }
        }
        return message;
    }

    public String getCoreIndexedMessage(String path, String defaultValue, String... args) {
        return getCoreIndexedMessage(defaultLanguage, path, defaultValue, args);
    }

    public String getCoreIndexedMessage(Player player, String path, String defaultValue, String... args) {
        String lang = getPlayerLanguage(player);
        return getCoreIndexedMessage(lang, path, defaultValue, args);
    }

    // ==================== GUI Indexed 消息（lang/gui/，支持 {0}{1} 索引占位符）====================

    public String getGuiIndexedMessage(String lang, String path, String defaultValue, String[] args) {
        String message = getGuiMessage(lang, path, defaultValue);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", args[i] != null ? args[i] : "");
            }
        }
        return message;
    }

    public String getGuiIndexedMessage(String path, String defaultValue, String... args) {
        return getGuiIndexedMessage(defaultLanguage, path, defaultValue, args);
    }

    public String getGuiIndexedMessage(Player player, String path, String defaultValue, String... args) {
        String lang = getPlayerLanguage(player);
        return getGuiIndexedMessage(lang, path, defaultValue, args);
    }
}
