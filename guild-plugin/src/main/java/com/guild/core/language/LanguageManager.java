package com.guild.core.language;

import com.guild.GuildPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    
    private static final String GUI_LANG_PATH = "lang/gui/";
    private static final String CORE_LANG_PATH = "lang/core/";
    private static final String MODULES_LANG_PATH = "lang/modules/";
    private static final String LANG_FILE_SUFFIX = ".yml";
    private static final String[] KNOWN_LANGS = {
        "en", "zh", "pl", "br",
        // 欧洲语系 & 服务器常见
        "de", "fr", "ru", "zh_tw", "ms",
        // 常见 Minecraft 用户语言
        "ja", "ko", "es", "pt", "it", "nl", "sv", "tr",
        "vi", "th", "ar", "cs", "uk", "ro", "hu", "da", "fi", "no"
    };
    
    public LanguageManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        releaseBundledLanguageFiles();
        loadLanguages();
        loadCoreLanguages();
        loadModuleLanguages();
        loadGuiLanguages();
    }
    
    private void releaseBundledLanguageFiles() {
        prepareLanguageFolders();

        for (String lang : KNOWN_LANGS) {
            saveBundledLanguageResource(CORE_LANG_PATH + lang + LANG_FILE_SUFFIX);
            saveBundledLanguageResource(GUI_LANG_PATH + lang + LANG_FILE_SUFFIX);
        }
    }
    
    private void prepareLanguageFolders() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        createDirectory(CORE_LANG_PATH);
        createDirectory(GUI_LANG_PATH);
        createDirectory(MODULES_LANG_PATH);
    }
    
    private void createDirectory(String relativePath) {
        File dir = new File(plugin.getDataFolder(), relativePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    private void saveBundledLanguageResource(String resourcePath) {
        if (plugin.getResource(resourcePath) == null) {
            return;
        }

        File file = new File(plugin.getDataFolder(), resourcePath);
        if (file.exists()) {
            return;
        }

        try {
            plugin.saveResource(resourcePath, false);
            logger.info("Extracted bundled language file: " + resourcePath);
        } catch (IllegalArgumentException e) {
            logger.warning("Bundled language resource not found: " + resourcePath);
        } catch (Exception e) {
            logger.warning("Failed to extract bundled language file " + resourcePath + ": " + e.getMessage());
        }
    }
    
    private void loadLanguages() {
        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        defaultLanguage = mainConfig.getString("language.default", LANG_EN).toLowerCase();
        if (defaultLanguage != null) {
            defaultLanguage = defaultLanguage.trim();
        }

        Set<String> requestedLanguages = new java.util.LinkedHashSet<>();
        requestedLanguages.add(defaultLanguage != null ? defaultLanguage : LANG_EN);
        List<String> additionalLangs = mainConfig.getStringList("language.additional-languages");
        for (String lang : additionalLangs) {
            if (lang != null) {
                lang = lang.trim().toLowerCase();
                if (!lang.isEmpty()) {
                    requestedLanguages.add(lang);
                }
            }
        }

        for (String lang : requestedLanguages) {
            if (!supportedLanguages.contains(lang)) {
                loadAdditionalLanguage(lang);
            }
        }

        if (!supportedLanguages.contains(defaultLanguage)) {
            logger.warning("language.default is set to '" + defaultLanguage + "' but the corresponding language data was not loaded.");
            if (supportedLanguages.contains(LANG_EN)) {
                logger.warning("Falling back to default language: en");
                defaultLanguage = LANG_EN;
            } else if (!supportedLanguages.isEmpty()) {
                String fallback = supportedLanguages.iterator().next();
                logger.warning("Falling back to available language: " + fallback);
                defaultLanguage = fallback;
            } else {
                logger.severe("No language files loaded, plugin will use hardcoded defaults.");
                defaultLanguage = LANG_EN;
            }
        }

        logger.info("Language system loaded, default language: " + defaultLanguage
            + ", loaded languages: " + String.join(", ", getLoadedLanguages()));
    }

    private void loadAdditionalLanguage(String lang) {
        if (lang == null || lang.isEmpty()) {
            return;
        }
        lang = lang.toLowerCase();
        if (supportedLanguages.contains(lang)) {
            return;
        }

        loadBundledCoreLanguage(lang);
        loadBundledGuiLanguage(lang);
    }

    private void registerCoreLanguageConfig(String lang, FileConfiguration config) {
        coreConfigs.putIfAbsent(lang, config);
        languageConfigs.putIfAbsent(lang, config);
        supportedLanguages.add(lang);
    }

    private void loadBundledCoreLanguage(String lang) {
        String resourcePath = CORE_LANG_PATH + lang + LANG_FILE_SUFFIX;
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in != null) {
                String yaml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                FileConfiguration config = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
                if (!config.getKeys(false).isEmpty()) {
                    registerCoreLanguageConfig(lang, config);
                    logger.info("Loaded bundled core language file: " + resourcePath);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to load bundled core language file " + resourcePath + ": " + e.getMessage());
        }
    }

    private void loadBundledGuiLanguage(String lang) {
        String resourcePath = GUI_LANG_PATH + lang + LANG_FILE_SUFFIX;
        if (guiConfigs.containsKey(lang)) {
            return;
        }
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in != null) {
                String yaml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                FileConfiguration config = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
                if (!config.getKeys(false).isEmpty()) {
                    guiConfigs.put(lang, config);
                    supportedLanguages.add(lang);
                    logger.info("Loaded bundled GUI language file: " + resourcePath);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to load bundled GUI language file " + resourcePath + ": " + e.getMessage());
        }
    }

    /**
     * 从插件 JAR 内的 lang/core/ 目录加载核心（非GUI）语言文件
     */
    private void loadCoreLanguages() {
        loadExternalCoreLanguages();

        for (String lang : KNOWN_LANGS) {
            String resourcePath = CORE_LANG_PATH + lang + LANG_FILE_SUFFIX;
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in != null) {
                    String yaml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    FileConfiguration config = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
                    if (!config.getKeys(false).isEmpty()) {
                        registerCoreLanguageConfig(lang, config);
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
    }

    private void loadExternalCoreLanguages() {
        File coreDir = new File(plugin.getDataFolder(), CORE_LANG_PATH);
        if (!coreDir.exists() || !coreDir.isDirectory()) {
            return;
        }

        File[] langFiles = coreDir.listFiles((dir, name) -> name.endsWith(LANG_FILE_SUFFIX));
        if (langFiles == null || langFiles.length == 0) {
            return;
        }
        // 对文件进行排序，确保输出顺序一致
        java.util.Arrays.sort(langFiles, java.util.Comparator.comparing(File::getName));

        java.util.List<String> loadedLangs = new java.util.ArrayList<>();
        for (File langFile : langFiles) {
            String fileName = langFile.getName();
            String lang = fileName.substring(0, fileName.length() - LANG_FILE_SUFFIX.length()).toLowerCase();
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                if (!config.getKeys(false).isEmpty()) {
                    registerCoreLanguageConfig(lang, config);
                    loadedLangs.add(lang);
                }
            } catch (Exception e) {
                logger.warning("Failed to load external core language file " + langFile.getPath() + ": " + e.getMessage());
            }
        }
        if (!loadedLangs.isEmpty()) {
            logger.info("Loaded external core languages: " + String.join(", ", loadedLangs));
        }
    }

    private void loadExternalGuiLanguages() {
        File guiDir = new File(plugin.getDataFolder(), GUI_LANG_PATH);
        if (!guiDir.exists() || !guiDir.isDirectory()) {
            return;
        }

        File[] langFiles = guiDir.listFiles((dir, name) -> name.endsWith(LANG_FILE_SUFFIX));
        if (langFiles == null || langFiles.length == 0) {
            return;
        }
        // 对文件进行排序，确保输出顺序一致
        java.util.Arrays.sort(langFiles, java.util.Comparator.comparing(File::getName));

        java.util.List<String> loadedLangs = new java.util.ArrayList<>();
        for (File langFile : langFiles) {
            String fileName = langFile.getName();
            String lang = fileName.substring(0, fileName.length() - LANG_FILE_SUFFIX.length()).toLowerCase();
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                if (!config.getKeys(false).isEmpty()) {
                    guiConfigs.put(lang, config);
                    supportedLanguages.add(lang);
                    loadedLangs.add(lang);
                }
            } catch (Exception e) {
                logger.warning("Failed to load external GUI language file " + langFile.getPath() + ": " + e.getMessage());
            }
        }
        if (!loadedLangs.isEmpty()) {
            logger.info("Loaded external GUI languages: " + String.join(", ", loadedLangs));
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
        // 对模块目录进行排序，确保输出顺序一致
        java.util.Arrays.sort(moduleDirs, java.util.Comparator.comparing(File::getName));

        for (File moduleDir : moduleDirs) {
            loadExternalModuleLanguageDirectory(moduleDir);
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

    public boolean loadModuleLanguageResourcesForModule(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return false;
        }
        String moduleDirName = moduleId.toLowerCase();
        File moduleDir = new File(plugin.getDataFolder(), MODULES_LANG_PATH + moduleDirName);

        boolean loaded = false;
        if (moduleDir.exists() && moduleDir.isDirectory()) {
            loaded = loadExternalModuleLanguageDirectory(moduleDir);
        }

        if (!loaded) {
            loaded = loadBundledModuleLanguagesForModule(moduleDirName);
        }

        return loaded;
    }

    public boolean releaseModuleLanguageResourcesForModule(String moduleId) {
        if (moduleId == null || moduleId.trim().isEmpty()) {
            return false;
        }

        String moduleDirName = moduleId.toLowerCase();
        File moduleDir = new File(plugin.getDataFolder(), MODULES_LANG_PATH + moduleDirName);
        if (!moduleDir.exists()) {
            moduleDir.mkdirs();
        }

        boolean extracted = false;
        for (String lang : KNOWN_LANGS) {
            String resourcePath = MODULES_LANG_PATH + moduleDirName + "/" + lang + LANG_FILE_SUFFIX;
            if (plugin.getResource(resourcePath) != null) {
                File file = new File(plugin.getDataFolder(), resourcePath);
                if (!file.exists()) {
                    try {
                        plugin.saveResource(resourcePath, false);
                        extracted = true;
                        logger.info("Extracted bundled module language file: " + resourcePath);
                    } catch (IllegalArgumentException e) {
                        // no bundled resource for this path
                    } catch (Exception e) {
                        logger.warning("Failed to extract bundled module language file " + resourcePath + ": " + e.getMessage());
                    }
                }
            }
        }

        if (extracted) {
            logger.info("Released bundled module language resources for module '" + moduleDirName + "'.");
        }
        return extracted;
    }

    private boolean loadExternalModuleLanguageDirectory(File moduleDir) {
        File[] langFiles = moduleDir.listFiles((dir, name) -> name.endsWith(LANG_FILE_SUFFIX));
        if (langFiles == null || langFiles.length == 0) {
            return false;
        }

        java.util.Arrays.sort(langFiles, java.util.Comparator.comparing(File::getName));
        java.util.List<String> loadedLangs = new java.util.ArrayList<>();

        for (File langFile : langFiles) {
            String fileName = langFile.getName();
            String lang = fileName.substring(0, fileName.length() - LANG_FILE_SUFFIX.length()).toLowerCase();
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                if (!config.getKeys(false).isEmpty()) {
                    mergeModuleConfig(lang, config);
                    supportedLanguages.add(lang);
                    loadedLangs.add(lang);
                }
            } catch (Exception e) {
                logger.warning("Failed to load external module language file " + langFile.getPath() + ": " + e.getMessage());
            }
        }

        if (!loadedLangs.isEmpty()) {
            logger.info("Loaded external module '" + moduleDir.getName() + "' languages: " + String.join(", ", loadedLangs));
            return true;
        }
        return false;
    }

    private boolean loadBundledModuleLanguagesForModule(String moduleDirName) {
        boolean anyLoaded = false;
        java.util.List<String> loadedLangs = new java.util.ArrayList<>();

        for (String lang : KNOWN_LANGS) {
            String resourcePath = MODULES_LANG_PATH + moduleDirName + "/" + lang + LANG_FILE_SUFFIX;
            File moduleLangFile = new File(plugin.getDataFolder(), resourcePath);

            if (!moduleLangFile.exists()) {
                saveBundledLanguageResource(resourcePath);
            }

            if (!moduleLangFile.exists()) {
                continue;
            }

            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(moduleLangFile);
                if (!config.getKeys(false).isEmpty()) {
                    mergeModuleConfig(lang, config);
                    supportedLanguages.add(lang);
                    loadedLangs.add(lang);
                    anyLoaded = true;
                }
            } catch (Exception e) {
                logger.warning("Failed to load bundled module language file " + moduleLangFile.getPath() + ": " + e.getMessage());
            }
        }

        if (anyLoaded) {
            logger.info("Loaded bundled module languages for module '" + moduleDirName + "': " + String.join(", ", loadedLangs));
        }

        return anyLoaded;
    }
    
    /**
     * 从插件 JAR 内的 lang/gui/ 目录加载 GUI 专用语言文件
     */
    private void loadGuiLanguages() {
        loadExternalGuiLanguages();

        for (String lang : KNOWN_LANGS) {
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
        codeToName.put("zh", "\u4e2d\u6587"); // 中文
        codeToName.put("pl", "Polski");
        codeToName.put("br", "Portugu\u00eas (BR)");
        codeToName.put("de", "Deutsch");
        codeToName.put("fr", "Fran\u00e7ais");
        codeToName.put("es", "Espa\u00f1ol");
        codeToName.put("ja", "\u65e5\u672c\u8a9e");
        codeToName.put("ko", "\ud55c\uad6d\uc5b4");
        codeToName.put("ru", "\u0420\u0443\u0441\u0441\u043a\u0438\u0439");
        codeToName.put("zh_tw", "\u7e41\u9ad4\u4e2d\u6587"); // 繁體中文
        codeToName.put("ms", "Bahasa Melayu"); // 马来语
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

        for (String code : supportedLanguages) {
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
