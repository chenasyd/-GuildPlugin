package com.guild.core.placeholder;

import com.guild.GuildPlugin;
import com.guild.services.GuildService;

/**
 * Menedżer placeholderów - zarządza integracją z PlaceholderAPI
 */
public class PlaceholderManager {

    private final GuildPlugin plugin;
    private GuildService guildService;
    private GuildPlaceholderExpansion placeholderExpansion;
    private boolean placeholderApiAvailable = false;

    public PlaceholderManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.guildService = null; // Tymczasowo null, aby uniknąć zależności cyklicznych
    }

    /**
     * Ustaw usługę gildii (wywoływane po inicjalizacji kontenera usług)
     */
    public void setGuildService(GuildService guildService) {
        this.guildService = guildService;
    }

    /**
     * Zarejestruj placeholdery
     */
    public void registerPlaceholders() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                // Utwórz i zarejestruj PlaceholderExpansion
                placeholderExpansion = new GuildPlaceholderExpansion(plugin, guildService);
                placeholderExpansion.register();
                placeholderApiAvailable = true;
                plugin.getLogger().info("Placeholdery PlaceholderAPI zarejestrowane pomyślnie");
            } catch (Exception e) {
                plugin.getLogger().warning("Inicjalizacja PlaceholderAPI nie powiodła się: " + e.getMessage());
                placeholderApiAvailable = false;
            }
        } else {
            plugin.getLogger().warning("Nie znaleziono PlaceholderAPI, funkcje placeholderów będą niedostępne");
            placeholderApiAvailable = false;
        }
    }



    /**
     * Sprawdź, czy PlaceholderAPI jest dostępne
     */
    public boolean isPlaceholderApiAvailable() {
        return placeholderApiAvailable;
    }
}
