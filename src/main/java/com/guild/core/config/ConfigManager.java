package com.guild.core.config;

import com.guild.GuildPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Menedżer konfiguracji - zarządza wszystkimi plikami konfiguracyjnymi pluginu
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
     * Załaduj wszystkie pliki konfiguracyjne
     */
    private void loadConfigs() {
        // Główna konfiguracja
        loadConfig("config.yml");

        // Konfiguracja wiadomości
        loadConfig("messages.yml");

        // Konfiguracja GUI
        loadConfig("gui.yml");

        // Konfiguracja bazy danych
        loadConfig("database.yml");
    }

    /**
     * Załaduj określoną konfigurację
     */
    public void loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);

        // Jeśli plik konfiguracyjny nie istnieje, skopiuj domyślny z jar
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        configs.put(fileName, config);
        configFiles.put(fileName, configFile);

        logger.info("Załadowano konfigurację: " + fileName);
    }

    /**
     * Pobierz konfigurację
     */
    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }

    /**
     * Pobierz główną konfigurację
     */
    public FileConfiguration getMainConfig() {
        return getConfig("config.yml");
    }

    /**
     * Pobierz konfigurację wiadomości
     */
    public FileConfiguration getMessagesConfig() {
        return getConfig("messages.yml");
    }

    /**
     * Pobierz konfigurację GUI
     */
    public FileConfiguration getGuiConfig() {
        return getConfig("gui.yml");
    }

    /**
     * Pobierz konfigurację bazy danych
     */
    public FileConfiguration getDatabaseConfig() {
        return getConfig("database.yml");
    }

    /**
     * Zapisz główną konfigurację
     */
    public void saveMainConfig() {
        saveConfig("config.yml");
    }

    /**
     * Zapisz konfigurację
     */
    public void saveConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        File configFile = configFiles.get(fileName);

        if (config != null && configFile != null) {
            try {
                config.save(configFile);
                logger.info("Zapisano konfigurację: " + fileName);
            } catch (IOException e) {
                logger.severe("Błąd zapisu konfiguracji: " + fileName + " - " + e.getMessage());
            }
        }
    }

    /**
     * Przeładuj konfigurację
     */
    public void reloadConfig(String fileName) {
        loadConfig(fileName);
        logger.info("Przeładowano konfigurację: " + fileName);
    }

    /**
     * Przeładuj wszystkie konfiguracje
     */
    public void reloadAllConfigs() {
        configs.clear();
        configFiles.clear();
        loadConfigs();
        logger.info("Przeładowano wszystkie konfiguracje");
    }

    /**
     * Pobierz ciąg konfiguracji, obsługuje kody kolorów
     */
    public String getString(String fileName, String path, String defaultValue) {
        FileConfiguration config = getConfig(fileName);
        if (config == null) return defaultValue;

        String value = config.getString(path, defaultValue);
        return value != null ? value.replace("&", "§") : defaultValue;
    }

    /**
     * Pobierz liczbę całkowitą
     */
    public int getInt(String fileName, String path, int defaultValue) {
        FileConfiguration config = getConfig(fileName);
        if (config == null) return defaultValue;

        return config.getInt(path, defaultValue);
    }

    /**
     * Pobierz wartość logiczną
     */
    public boolean getBoolean(String fileName, String path, boolean defaultValue) {
        FileConfiguration config = getConfig(fileName);
        if (config == null) return defaultValue;

        return config.getBoolean(path, defaultValue);
    }
}
