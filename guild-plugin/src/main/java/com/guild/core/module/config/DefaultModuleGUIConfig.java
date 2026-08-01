package com.guild.core.module.config;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.sdk.gui.ModuleGUIConfig;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Default implementation of {@link ModuleGUIConfig} backed by a YAML file.
 * <p>
 * Configuration file location: {@code plugins/GuildPlugin/modules/{moduleId}/gui-config.yml}
 * <p>
 * File format:
 * <pre>
 * items:
 *   stats-button:
 *     material: DIAMOND
 *     name: "&6Stats Panel"
 *     lore:
 *       - "&7Click to view stats"
 *     custom-model-data: 10002  # optional
 *
 * texts:
 *   gui-title: "&6Guild Stats"
 *   no-data: "&cNo data available"
 *
 * flags:
 *   show-ranking: true
 *   show-economy-chart: false
 * </pre>
 * <p>
 * Server administrators can edit this file to customize module GUI appearance
 * without modifying module code. Changes take effect after {@code /guildadmin reload}
 * or by calling {@link #reload()}.
 */
public class DefaultModuleGUIConfig implements ModuleGUIConfig {

    private final File configFile;
    private final Logger logger;
    private YamlConfiguration yaml;

    /**
     * Create a config override instance for a module.
     *
     * @param moduleId module identifier (used to locate the config directory)
     * @param plugin   GuildPlugin instance (for data folder access)
     */
    public DefaultModuleGUIConfig(String moduleId, GuildPlugin plugin) {
        File moduleDir = new File(plugin.getDataFolder(), "modules" + File.separator + moduleId);
        this.configFile = new File(moduleDir, "gui-config.yml");
        this.logger = plugin.getLogger();
        load();
    }

    /**
     * Load (or reload) the configuration file.
     * Creates the parent directory and an empty file if they don't exist.
     */
    public void load() {
        if (!configFile.exists()) {
            try {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                configFile.createNewFile();
            } catch (IOException e) {
                logger.warning("[ModuleGUIConfig] Failed to create config file: " + configFile.getPath());
            }
        }
        this.yaml = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Reload the configuration from disk.
     * Called by /guildadmin reload or programmatically.
     */
    public void reload() {
        load();
    }

    @Override
    public ItemStack getDisplayItem(String key, ItemStack fallback, Player player) {
        if (yaml == null) return fallback;

        String basePath = "items." + key;
        if (!yaml.contains(basePath)) return fallback;

        // Start with a clone of the fallback
        ItemStack result = fallback.clone();

        // Override material
        String materialName = yaml.getString(basePath + ".material");
        if (materialName != null) {
            try {
                Material mat = Material.valueOf(materialName.toUpperCase());
                result.setType(mat);
            } catch (IllegalArgumentException e) {
                logger.warning("[ModuleGUIConfig] Invalid material '" + materialName + "' for key '" + key + "'");
            }
        }

        ItemMeta meta = result.getItemMeta();
        if (meta == null) return result;

        // Override display name
        String name = yaml.getString(basePath + ".name");
        if (name != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
        }

        // Override lore
        List<String> lore = yaml.getStringList(basePath + ".lore");
        if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtils.colorize(line));
            }
            meta.setLore(coloredLore);
        }

        // Override custom model data
        if (yaml.contains(basePath + ".custom-model-data")) {
            int cmd = yaml.getInt(basePath + ".custom-model-data");
            meta.setCustomModelData(cmd);
        }

        result.setItemMeta(meta);
        return result;
    }

    @Override
    public String getDisplayText(String key, String fallback) {
        if (yaml == null) return fallback;
        String value = yaml.getString("texts." + key);
        return value != null ? ColorUtils.colorize(value) : fallback;
    }

    @Override
    public boolean getDisplayFlag(String key, boolean fallback) {
        if (yaml == null) return fallback;
        return yaml.getBoolean("flags." + key, fallback);
    }
}
