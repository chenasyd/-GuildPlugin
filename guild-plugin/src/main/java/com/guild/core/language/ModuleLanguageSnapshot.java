package com.guild.core.language;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 模块语言异步/批量重载快照。 */
public final class ModuleLanguageSnapshot {

    private final Map<String, FileConfiguration> moduleConfigs = new HashMap<>();
    private final Set<String> moduleSupportedLanguages = new HashSet<>();
    private String moduleDefaultLanguage = LanguageConstants.LANG_EN;
    private final Set<String> loadedModuleIds = new HashSet<>();

    public Map<String, FileConfiguration> getModuleConfigs() {
        return moduleConfigs;
    }

    public Set<String> getModuleSupportedLanguages() {
        return moduleSupportedLanguages;
    }

    public String getModuleDefaultLanguage() {
        return moduleDefaultLanguage;
    }

    public void setModuleDefaultLanguage(String moduleDefaultLanguage) {
        this.moduleDefaultLanguage = moduleDefaultLanguage;
    }

    public Set<String> getLoadedModuleIds() {
        return loadedModuleIds;
    }
}
