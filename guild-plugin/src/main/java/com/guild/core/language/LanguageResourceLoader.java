package com.guild.core.language;

import com.guild.GuildPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 语言文件磁盘/JAR 读取与 bundled 资源释放。
 */
public final class LanguageResourceLoader {

    private final GuildPlugin plugin;
    private final Logger logger;

    public LanguageResourceLoader(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void releaseBundledLanguageFiles() {
        prepareLanguageFolders();

        for (String lang : LanguageConstants.KNOWN_LANGS) {
            saveBundledLanguageResource(LanguageConstants.CORE_LANG_PATH + lang + LanguageConstants.LANG_FILE_SUFFIX);
            saveBundledLanguageResource(LanguageConstants.GUI_LANG_PATH + lang + LanguageConstants.LANG_FILE_SUFFIX);
        }
        for (String moduleDir : LanguageConstants.MODULE_DIRS) {
            for (String lang : LanguageConstants.KNOWN_LANGS) {
                saveBundledLanguageResource(
                        LanguageConstants.MODULES_LANG_PATH + moduleDir + "/" + lang + LanguageConstants.LANG_FILE_SUFFIX);
            }
        }
    }

    public LanguageResourceSnapshot readPluginLanguageResourcesSnapshot() {
        LanguageResourceSnapshot snapshot = new LanguageResourceSnapshot();
        populatePluginLanguageResources(snapshot.getCore(), snapshot.getGui(), snapshot.getLanguages());

        String requestedDefault = loadDefaultLanguageFromConfig();
        snapshot.setDefaultLanguage(resolveDefaultLanguage(requestedDefault, snapshot.getLanguages()));
        return snapshot;
    }

    public ModuleLanguageSnapshot readModuleLanguageResourcesSnapshot() {
        ModuleLanguageSnapshot snapshot = new ModuleLanguageSnapshot();
        snapshot.setModuleDefaultLanguage(loadModuleDefaultLanguageFromConfig());
        readExternalModuleFiles(snapshot.getModuleConfigs(), snapshot.getModuleSupportedLanguages());

        for (String moduleDir : LanguageConstants.MODULE_DIRS) {
            for (String lang : LanguageConstants.KNOWN_LANGS) {
                readBundledModuleLang(moduleDir, lang, snapshot.getModuleConfigs(), snapshot.getModuleSupportedLanguages());
            }
            snapshot.getLoadedModuleIds().add(moduleDir);
        }
        return snapshot;
    }

    public String loadModuleDefaultLanguageFromConfig() {
        FileConfiguration cfg = plugin.getConfigManager().getModulesConfig();
        if (cfg == null) {
            return LanguageConstants.LANG_EN;
        }
        String lang = cfg.getString("language.default", LanguageConstants.LANG_EN);
        return (lang != null ? lang.trim().toLowerCase() : LanguageConstants.LANG_EN);
    }

    public boolean loadExternalModuleLanguageDirectory(File moduleDir, LanguageCatalog catalog) {
        File[] langFiles = moduleDir.listFiles((dir, name) -> name.endsWith(LanguageConstants.LANG_FILE_SUFFIX));
        if (langFiles == null || langFiles.length == 0) {
            return false;
        }

        Arrays.sort(langFiles, java.util.Comparator.comparing(File::getName));
        java.util.List<String> loadedLangs = new java.util.ArrayList<>();

        for (File langFile : langFiles) {
            String lang = langFile.getName()
                    .substring(0, langFile.getName().length() - LanguageConstants.LANG_FILE_SUFFIX.length())
                    .toLowerCase();
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                if (!config.getKeys(false).isEmpty()) {
                    catalog.mergeModuleConfig(lang, config);
                    catalog.registerModuleLanguage(lang);
                    loadedLangs.add(lang);
                }
            } catch (Exception e) {
                logger.warning("Failed to load external module language file " + langFile.getPath() + ": " + e.getMessage());
            }
        }

        if (!loadedLangs.isEmpty()) {
            catalog.markModuleLoaded(moduleDir.getName());
            return true;
        }
        return false;
    }

    public boolean loadBundledModuleLanguagesForModule(String moduleDirName, LanguageCatalog catalog) {
        boolean anyLoaded = false;

        for (String lang : LanguageConstants.KNOWN_LANGS) {
            String resourcePath = LanguageConstants.MODULES_LANG_PATH + moduleDirName + "/"
                    + lang + LanguageConstants.LANG_FILE_SUFFIX;
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
                    catalog.mergeModuleConfig(lang, config);
                    catalog.registerModuleLanguage(lang);
                    anyLoaded = true;
                }
            } catch (Exception e) {
                logger.warning("Failed to load bundled module language file " + moduleLangFile.getPath() + ": " + e.getMessage());
            }
        }

        if (anyLoaded) {
            logger.info("Loaded bundled module languages for module '" + moduleDirName + "'.");
        }

        return anyLoaded;
    }

    public boolean releaseModuleLanguageResourcesForModule(String moduleDirName) {
        File moduleDir = new File(plugin.getDataFolder(), LanguageConstants.MODULES_LANG_PATH + moduleDirName);
        if (!moduleDir.exists()) {
            moduleDir.mkdirs();
        }

        boolean extracted = false;
        for (String lang : LanguageConstants.KNOWN_LANGS) {
            String resourcePath = LanguageConstants.MODULES_LANG_PATH + moduleDirName + "/"
                    + lang + LanguageConstants.LANG_FILE_SUFFIX;
            if (plugin.getResource(resourcePath) != null) {
                File file = new File(plugin.getDataFolder(), resourcePath);
                if (!file.exists()) {
                    try {
                        plugin.saveResource(resourcePath, false);
                        extracted = true;
                        logger.info("Extracted bundled module language file: " + resourcePath);
                    } catch (IllegalArgumentException ignored) {
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

    private void prepareLanguageFolders() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        createDirectory(LanguageConstants.CORE_LANG_PATH);
        createDirectory(LanguageConstants.GUI_LANG_PATH);
        createDirectory(LanguageConstants.MODULES_LANG_PATH);
        for (String moduleDir : LanguageConstants.MODULE_DIRS) {
            createDirectory(LanguageConstants.MODULES_LANG_PATH + moduleDir + "/");
        }
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

    private void populatePluginLanguageResources(Map<String, FileConfiguration> coreTarget,
                                                 Map<String, FileConfiguration> guiTarget,
                                                 Set<String> langsTarget) {
        readExternalLangFiles(LanguageConstants.CORE_LANG_PATH, coreTarget, langsTarget);
        readExternalLangFiles(LanguageConstants.GUI_LANG_PATH, guiTarget, langsTarget);

        Set<String> bundleCandidates = new LinkedHashSet<>();
        bundleCandidates.addAll(Arrays.asList(LanguageConstants.KNOWN_LANGS));
        bundleCandidates.addAll(discoverLanguageCodesFromDisk());
        bundleCandidates.addAll(collectConfigLanguageCodes());

        for (String lang : bundleCandidates) {
            readBundledCoreLang(lang, coreTarget, langsTarget);
            readBundledGuiLang(lang, guiTarget, langsTarget);
        }
    }

    private Set<String> collectConfigLanguageCodes() {
        Set<String> codes = new LinkedHashSet<>();
        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        String configuredDefault = mainConfig.getString("language.default", LanguageConstants.LANG_EN);
        if (configuredDefault != null && !configuredDefault.trim().isEmpty()) {
            codes.add(configuredDefault.trim().toLowerCase());
        }
        for (String lang : mainConfig.getStringList("language.additional-languages")) {
            if (lang != null && !lang.trim().isEmpty()) {
                codes.add(lang.trim().toLowerCase());
            }
        }
        return codes;
    }

    private Set<String> discoverLanguageCodesFromDisk() {
        Set<String> codes = new LinkedHashSet<>();
        collectLanguageCodesFromDirectory(LanguageConstants.CORE_LANG_PATH, codes);
        collectLanguageCodesFromDirectory(LanguageConstants.GUI_LANG_PATH, codes);
        return codes;
    }

    private void collectLanguageCodesFromDirectory(String dirPath, Set<String> target) {
        File dir = new File(plugin.getDataFolder(), dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(LanguageConstants.LANG_FILE_SUFFIX));
        if (files == null) {
            return;
        }
        for (File file : files) {
            target.add(file.getName()
                    .substring(0, file.getName().length() - LanguageConstants.LANG_FILE_SUFFIX.length())
                    .toLowerCase());
        }
    }

    private String loadDefaultLanguageFromConfig() {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        String lang = cfg.getString("language.default", LanguageConstants.LANG_EN);
        return (lang != null ? lang.trim().toLowerCase() : LanguageConstants.LANG_EN);
    }

    private String resolveDefaultLanguage(String requested, Set<String> loadedLangs) {
        String lang = requested != null ? requested.trim().toLowerCase() : LanguageConstants.LANG_EN;
        if (loadedLangs.contains(lang)) {
            return lang;
        }

        logger.warning("language.default is set to '" + lang + "' but the corresponding language data was not loaded.");
        if (loadedLangs.contains(LanguageConstants.LANG_EN)) {
            logger.warning("Falling back to default language: en");
            return LanguageConstants.LANG_EN;
        }
        if (!loadedLangs.isEmpty()) {
            String fallback = loadedLangs.iterator().next();
            logger.warning("Falling back to available language: " + fallback);
            return fallback;
        }

        logger.severe("No language files loaded, plugin will use hardcoded defaults.");
        return LanguageConstants.LANG_EN;
    }

    private void readExternalLangFiles(String dirPath,
                                       Map<String, FileConfiguration> target,
                                       Set<String> langs) {
        File dir = new File(plugin.getDataFolder(), dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(LanguageConstants.LANG_FILE_SUFFIX));
        if (files == null || files.length == 0) {
            return;
        }
        Arrays.sort(files, java.util.Comparator.comparing(File::getName));
        for (File f : files) {
            String lang = f.getName().substring(0,
                    f.getName().length() - LanguageConstants.LANG_FILE_SUFFIX.length()).toLowerCase();
            try {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                if (!cfg.getKeys(false).isEmpty()) {
                    target.put(lang, cfg);
                    langs.add(lang);
                }
            } catch (Exception e) {
                logger.warning("Failed to read " + f.getPath() + ": " + e.getMessage());
            }
        }
    }

    private void readExternalModuleFiles(Map<String, FileConfiguration> target, Set<String> langs) {
        File root = new File(plugin.getDataFolder(), LanguageConstants.MODULES_LANG_PATH);
        if (!root.exists() || !root.isDirectory()) {
            return;
        }
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null) {
            return;
        }
        for (File moduleDir : dirs) {
            File[] files = moduleDir.listFiles((d, n) -> n.endsWith(LanguageConstants.LANG_FILE_SUFFIX));
            if (files == null) {
                continue;
            }
            for (File f : files) {
                String lang = f.getName().substring(0,
                        f.getName().length() - LanguageConstants.LANG_FILE_SUFFIX.length()).toLowerCase();
                try {
                    FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                    if (!cfg.getKeys(false).isEmpty()) {
                        LanguageConfigMerge.mergeIntoLanguageMap(lang, cfg, target);
                        langs.add(lang);
                    }
                } catch (Exception e) {
                    logger.warning("Failed to read " + f.getPath() + ": " + e.getMessage());
                }
            }
        }
    }

    private void readBundledCoreLang(String lang,
                                     Map<String, FileConfiguration> target,
                                     Set<String> langs) {
        if (target.containsKey(lang)) {
            return;
        }
        String path = LanguageConstants.CORE_LANG_PATH + lang + LanguageConstants.LANG_FILE_SUFFIX;
        readBundledYaml(path, lang, target, langs, "core");
    }

    private void readBundledGuiLang(String lang,
                                    Map<String, FileConfiguration> target,
                                    Set<String> langs) {
        if (target.containsKey(lang)) {
            return;
        }
        String path = LanguageConstants.GUI_LANG_PATH + lang + LanguageConstants.LANG_FILE_SUFFIX;
        readBundledYaml(path, lang, target, langs, "GUI");
    }

    private void readBundledModuleLang(String moduleDir, String lang,
                                       Map<String, FileConfiguration> target,
                                       Set<String> langs) {
        String path = LanguageConstants.MODULES_LANG_PATH + moduleDir + "/" + lang + LanguageConstants.LANG_FILE_SUFFIX;
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) {
                return;
            }
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
            if (!cfg.getKeys(false).isEmpty()) {
                LanguageConfigMerge.mergeIntoLanguageMap(lang, cfg, target);
                langs.add(lang);
            }
        } catch (Exception e) {
            logger.log(Level.FINE,
                    "Could not read bundled module language '" + moduleDir + "/" + lang + "': " + e.getMessage());
        }
    }

    private void readBundledYaml(String path, String lang,
                                 Map<String, FileConfiguration> target,
                                 Set<String> langs, String label) {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) {
                return;
            }
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
            if (!cfg.getKeys(false).isEmpty()) {
                target.put(lang, cfg);
                langs.add(lang);
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Could not read bundled " + label + " language '" + lang + "': " + e.getMessage());
        }
    }
}
