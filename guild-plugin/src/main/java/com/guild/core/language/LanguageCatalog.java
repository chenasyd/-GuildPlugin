package com.guild.core.language;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内存中的语言配置索引（core / gui / legacy / module）与语言码解析。
 */
public final class LanguageCatalog {

    private final Map<String, FileConfiguration> legacyConfigs = new HashMap<>();
    private final Map<String, FileConfiguration> coreConfigs = new HashMap<>();
    private final Map<String, FileConfiguration> guiConfigs = new HashMap<>();
    private final Map<String, FileConfiguration> moduleConfigs = new HashMap<>();
    private final Set<String> supportedLanguages = new HashSet<>();
    private final Set<String> moduleSupportedLanguages = new HashSet<>();
    private final Set<String> loadedModuleIds = new HashSet<>();

    private String defaultLanguage = LanguageConstants.LANG_EN;
    private String moduleDefaultLanguage = LanguageConstants.LANG_EN;

    public void applyPluginSnapshot(LanguageResourceSnapshot snapshot) {
        legacyConfigs.clear();
        coreConfigs.clear();
        guiConfigs.clear();
        supportedLanguages.clear();

        coreConfigs.putAll(snapshot.getCore());
        legacyConfigs.putAll(snapshot.getCore());
        guiConfigs.putAll(snapshot.getGui());
        supportedLanguages.addAll(snapshot.getLanguages());
        defaultLanguage = snapshot.getDefaultLanguage();
    }

    public void clearModuleData() {
        moduleConfigs.clear();
        loadedModuleIds.clear();
        moduleSupportedLanguages.clear();
    }

    public void applyModuleSnapshot(ModuleLanguageSnapshot snapshot) {
        clearModuleData();
        moduleConfigs.putAll(snapshot.getModuleConfigs());
        moduleSupportedLanguages.addAll(snapshot.getModuleSupportedLanguages());
        moduleDefaultLanguage = snapshot.getModuleDefaultLanguage();
        loadedModuleIds.addAll(snapshot.getLoadedModuleIds());
    }

    public void setModuleDefaultLanguage(String lang) {
        if (lang != null && !lang.isBlank()) {
            moduleDefaultLanguage = lang.trim().toLowerCase();
        }
    }

    public void mergeModuleConfig(String lang, FileConfiguration config) {
        FileConfiguration existing = moduleConfigs.get(lang);
        if (existing == null) {
            moduleConfigs.put(lang, config);
        } else {
            LanguageConfigMerge.mergeLeafKeys(existing, config);
        }
    }

    public void markModuleLoaded(String moduleDirName) {
        if (moduleDirName != null && !moduleDirName.isBlank()) {
            loadedModuleIds.add(moduleDirName.toLowerCase());
        }
    }

    public boolean isModuleLoaded(String moduleDirName) {
        return moduleDirName != null && loadedModuleIds.contains(moduleDirName.toLowerCase());
    }

    public void registerModuleLanguage(String lang) {
        if (lang != null && !lang.isBlank()) {
            moduleSupportedLanguages.add(lang.toLowerCase());
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

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String lang) {
        if (isLanguageSupported(lang)) {
            defaultLanguage = lang.toLowerCase();
        }
    }

    public String getModuleDefaultLanguage() {
        return moduleDefaultLanguage;
    }

    public int getModuleConfigCount() {
        return moduleConfigs.size();
    }

    public Set<String> getModuleSupportedLanguages() {
        return new LinkedHashSet<>(moduleSupportedLanguages);
    }

    public Set<String> getLoadedModuleIds() {
        return new LinkedHashSet<>(loadedModuleIds);
    }

    public FileConfiguration getRawLanguageConfig(String lang) {
        return legacyConfigs.get(lang != null ? lang.toLowerCase() : null);
    }

    public FileConfiguration getLegacyConfig(String lang) {
        if (lang != null) {
            lang = lang.toLowerCase();
        }
        FileConfiguration config = legacyConfigs.get(lang);
        if (config == null) {
            config = legacyConfigs.get(defaultLanguage);
        }
        return config;
    }

    public FileConfiguration getCoreConfig(String lang) {
        return getScopedConfig(coreConfigs, lang);
    }

    public FileConfiguration getGuiConfig(String lang) {
        return getScopedConfig(guiConfigs, lang);
    }

    public FileConfiguration getModuleConfig(String lang) {
        String resolved = resolveModuleLanguage(lang);
        FileConfiguration config = moduleConfigs.get(resolved);
        if (config == null && !LanguageConstants.LANG_EN.equals(resolved)) {
            config = moduleConfigs.get(LanguageConstants.LANG_EN);
        }
        return config;
    }

    public FileConfiguration getRawModuleConfig(String lang) {
        return moduleConfigs.get(lang != null ? lang.toLowerCase() : null);
    }

    private FileConfiguration getScopedConfig(Map<String, FileConfiguration> configs, String lang) {
        String resolved = resolveLanguage(lang);
        FileConfiguration config = configs.get(resolved);
        if (config == null && !LanguageConstants.LANG_EN.equals(resolved)) {
            config = configs.get(LanguageConstants.LANG_EN);
        }
        return config;
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
        if (supportedLanguages.contains(LanguageConstants.LANG_EN)) {
            return LanguageConstants.LANG_EN;
        }
        return supportedLanguages.stream().findFirst().orElse(LanguageConstants.LANG_EN);
    }

    private String resolveModuleLanguage(String lang) {
        if (lang != null) {
            lang = lang.toLowerCase();
        }
        if (lang != null && moduleSupportedLanguages.contains(lang)) {
            return lang;
        }
        if (moduleSupportedLanguages.contains(moduleDefaultLanguage)) {
            return moduleDefaultLanguage;
        }
        if (moduleSupportedLanguages.contains(LanguageConstants.LANG_EN)) {
            return LanguageConstants.LANG_EN;
        }
        return moduleSupportedLanguages.stream().findFirst().orElse(LanguageConstants.LANG_EN);
    }
}
