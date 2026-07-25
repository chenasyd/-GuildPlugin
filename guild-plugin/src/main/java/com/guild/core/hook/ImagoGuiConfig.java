package com.guild.core.hook;

import org.a.imagoCore.image.display.gui.GuiTitleRenderer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the ImagoCore GUI integration configuration for Guild Plugin.
 *
 * <p>Configuration file: {@code plugins/GuildPlugin/imago-gui.yml}
 *
 * <h3>Example configuration:</h3>
 * <pre>{@code
 * # ImagoCore GUI Integration
 * # Maps Guild Plugin GUI types to ImagoCore background images
 * # and decoration overlays.
 *
 * enabled: true
 *
 * # GUI type → ImagoCore entry ID mapping
 * # The entry ID format is "<slots>-<name>" matching ImagoCore's gui/ folder
 * bindings:
 *   MainGuildGUI: "54-default"
 *   GuildInfoGUI: "54-default"
 *
 * # Decoration overlays (optional)
 * # char:  ImagoCore char/ 下的图片名（不含 .png）
 * # x:     距背景左边缘的水平像素偏移
 * # ascent: 垂直位置覆盖（可选，负值下移；省略则用 char.yml 默认值）
 * overlays:
 *   MainGuildGUI:
 *     - char: "guild_banner"
 *       x: 30
 *       ascent: -40
 *     - char: "corner_ornament"
 *       x: 5
 * }</pre>
 */
public class ImagoGuiConfig {

    private final File configFile;
    private final Logger logger;

    private boolean enabled;
    private final Map<String, String> bindings = new LinkedHashMap<>();
    private final Map<String, List<OverlayConfig>> overlays = new LinkedHashMap<>();

    public ImagoGuiConfig(File dataFolder, Logger logger) {
        this.configFile = new File(dataFolder, "imago-gui.yml");
        this.logger = logger;
    }

    /**
     * Loads (or reloads) the configuration.
     * Creates a default config file if none exists.
     */
    public void load() {
        if (!configFile.exists()) {
            createDefault();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);

        bindings.clear();
        ConfigurationSection bindSec = config.getConfigurationSection("bindings");
        if (bindSec != null) {
            for (String guiType : bindSec.getKeys(false)) {
                String entryId = bindSec.getString(guiType);
                if (entryId != null && !entryId.isEmpty()) {
                    bindings.put(guiType, entryId);
                }
            }
        }

        overlays.clear();
        ConfigurationSection overlaySec = config.getConfigurationSection("overlays");
        if (overlaySec != null) {
            for (String guiType : overlaySec.getKeys(false)) {
                List<Map<?, ?>> list = overlaySec.getMapList(guiType);
                List<OverlayConfig> overlayList = new ArrayList<>();
                for (Map<?, ?> map : list) {
                    String charName = (String) map.get("char");
                    int x = map.containsKey("x") ? ((Number) map.get("x")).intValue() : 0;
                    Integer ascent = map.containsKey("ascent")
                            ? ((Number) map.get("ascent")).intValue() : null;
                    if (charName != null && !charName.isEmpty()) {
                        overlayList.add(new OverlayConfig(charName, x, ascent));
                    }
                }
                if (!overlayList.isEmpty()) {
                    overlays.put(guiType, overlayList);
                }
            }
        }

        logger.info("[ImagoGuiConfig] Loaded " + bindings.size() + " bindings, "
                + overlays.size() + " overlay groups. Enabled: " + enabled);
    }

    private void createDefault() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("enabled", true);

        // Default bindings for common GUIs
        config.set("bindings.MainGuildGUI", "54-default");
        config.set("bindings.GuildInfoGUI", "54-default");
        config.set("bindings.GuildSettingsGUI", "54-default");
        config.set("bindings.MemberGuildGUI", "54-default");
        config.set("bindings.GuildFundsGUI", "54-default");
        config.set("bindings.GuildLogsGUI", "54-default");
        config.set("bindings.GuildListGUI", "54-default");
        config.set("bindings.GuildRelationsGUI", "54-default");
        config.set("bindings.CreateGuildGUI", "54-default");
        config.set("bindings.EconomyManagementGUI", "54-default");

        // Example overlay (commented out by default)
        config.set("overlays", null);

        try {
            configFile.getParentFile().mkdirs();
            config.save(configFile);
            logger.info("[ImagoGuiConfig] Created default imago-gui.yml");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create imago-gui.yml", e);
        }
    }

    // ── Accessors ───────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the ImagoCore entry ID bound to a GUI type.
     *
     * @param guiType the GUI class simple name (e.g. "MainGuildGUI")
     * @return the entry ID, or null if not bound
     */
    public String getBinding(String guiType) {
        return bindings.get(guiType);
    }

    /**
     * Gets the overlay configurations for a GUI type.
     *
     * @param guiType the GUI class simple name
     * @return list of overlay configs, or empty list
     */
    public List<OverlayConfig> getOverlays(String guiType) {
        return overlays.getOrDefault(guiType, Collections.emptyList());
    }

    /**
     * Checks if a GUI type has any ImagoCore configuration.
     */
    public boolean hasConfig(String guiType) {
        return bindings.containsKey(guiType);
    }

    /**
     * Returns all configured GUI type → entry ID bindings.
     */
    public Map<String, String> getAllBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    // ── Overlay config record ───────────────────────────────────

    /**
     * A single overlay decoration configuration.
     */
    public static class OverlayConfig {
        private final String charName;
        private final int x;
        private final Integer ascent; // null = use char.yml default

        public OverlayConfig(String charName, int x, Integer ascent) {
            this.charName = charName;
            this.x = x;
            this.ascent = ascent;
        }

        /** The char image name (filename without .png in ImagoCore/char/). */
        public String getCharName() {
            return charName;
        }

        /** Horizontal offset from background left edge in pixels. */
        public int getX() {
            return x;
        }

        /**
         * Vertical position override (ascent). Negative values move the
         * image downward into the item area. Null means use the char's
         * default ascent from char.yml.
         */
        public Integer getAscent() {
            return ascent;
        }
    }
}
