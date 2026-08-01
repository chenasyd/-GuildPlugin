package com.guild.sdk.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Runtime configuration override interface for module GUIs.
 * <p>
 * Server administrators can override item materials, display names, lore, and
 * text values via an external config file at:
 * {@code plugins/GuildPlugin/modules/{moduleId}/gui-config.yml}
 * <p>
 * Modules implement this interface (or use {@code DefaultModuleGUIConfig}) and
 * call {@link #getDisplayItem} / {@link #getDisplayText} in {@code setupInventory}
 * to resolve the final display values.
 */
public interface ModuleGUIConfig {

    /**
     * Get the configuration-overridden item.
     * <p>
     * If the admin configured material/name/lore for the given key in
     * gui-config.yml, returns the overridden version; otherwise returns fallback.
     *
     * @param key      item identifier (e.g. "stats-button", "ranking-header")
     * @param fallback original item (returned when no override exists)
     * @param player   target player (for per-player language resolution)
     * @return final display item
     */
    ItemStack getDisplayItem(String key, ItemStack fallback, Player player);

    /**
     * Get a configuration-overridden string value.
     * Used for titles, hint texts, and other non-item configuration.
     *
     * @param key      config key
     * @param fallback default value
     * @return overridden value, or fallback if not configured
     */
    String getDisplayText(String key, String fallback);

    /**
     * Get a configuration-overridden boolean flag.
     * Used to control visibility of certain GUI elements.
     *
     * @param key      config key
     * @param fallback default value
     * @return overridden value, or fallback if not configured
     */
    boolean getDisplayFlag(String key, boolean fallback);
}
