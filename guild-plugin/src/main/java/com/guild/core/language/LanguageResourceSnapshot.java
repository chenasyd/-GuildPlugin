package com.guild.core.language;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 插件本体（core + gui）语言加载快照。 */
public final class LanguageResourceSnapshot {

    private final Map<String, FileConfiguration> core = new HashMap<>();
    private final Map<String, FileConfiguration> gui = new HashMap<>();
    private final Set<String> languages = new HashSet<>();
    private String defaultLanguage = LanguageConstants.LANG_EN;

    public Map<String, FileConfiguration> getCore() {
        return core;
    }

    public Map<String, FileConfiguration> getGui() {
        return gui;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
}
