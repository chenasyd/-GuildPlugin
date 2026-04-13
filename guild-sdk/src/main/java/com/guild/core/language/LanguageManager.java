package com.guild.core.language;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class LanguageManager {
    public String getIndexedMessage(String key, String defaultValue, String[] args) {
        return defaultValue;
    }

    public String getMessage(Player player, String key, String defaultValue) {
        return defaultValue;
    }

    public String getMessage(String key, String defaultValue) {
        return defaultValue;
    }

    public boolean isLanguageSupported(String lang) {
        return false;
    }

    public List<String> getLoadedLanguages() {
        return java.util.Collections.emptyList();
    }

    public List<String> getAvailableLanguageNames() {
        return java.util.Collections.emptyList();
    }

    public String getDefaultLanguage() {
        return "en";
    }

    public String getPlayerLanguage(Player player) {
        return "en";
    }

    public void setPlayerLanguage(Player player, String lang) {
    }

    public FileConfiguration getLanguageConfig(String lang) {
        return null;
    }
}
