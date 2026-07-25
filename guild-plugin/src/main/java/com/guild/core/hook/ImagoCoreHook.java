package com.guild.core.hook;

import org.a.imagoCore.ImagoCore;
import org.a.imagoCore.config.CharEntry;
import org.a.imagoCore.config.GuiEntry;
import org.a.imagoCore.gui.GuiController;
import org.a.imagoCore.image.display.gui.GuiTitleRenderer;
import org.a.imagoCore.image.display.gui.TitleComposition;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

/**
 * Soft-dependency bridge to ImagoCore.
 *
 * <p>This class is only instantiated when ImagoCore is detected on the
 * server. It provides Guild Plugin with the ability to create inventories
 * whose titles are rendered as resource-pack font images (backgrounds
 * and decoration overlays).
 *
 * <h3>Usage in GUIManager:</h3>
 * <pre>{@code
 * ImagoCoreHook hook = ImagoCoreHook.detect(logger);
 * if (hook != null) {
 *     Inventory inv = hook.createTitledInventory(size, guiType, overlays);
 *     if (inv != null) return inv;
 * }
 * // fallback: Bukkit.createInventory(null, size, title)
 * }</pre>
 *
 * <p><b>Thread safety:</b> All methods delegate to ImagoCore's
 * GuiController which uses ConcurrentHashMap internally.
 */
public class ImagoCoreHook {

    private final ImagoCore imagoCore;
    private final GuiController guiController;
    private final Logger logger;

    private ImagoCoreHook(ImagoCore imagoCore, Logger logger) {
        this.imagoCore = imagoCore;
        this.guiController = imagoCore.getGuiController();
        this.logger = logger;
    }

    /**
     * Attempts to detect and hook into ImagoCore.
     *
     * @param logger the plugin logger for status messages
     * @return a hook instance if ImagoCore is present and enabled, null otherwise
     */
    public static ImagoCoreHook detect(Logger logger) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ImagoCore");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        if (!(plugin instanceof ImagoCore core)) {
            logger.warning("[ImagoCoreHook] Plugin 'ImagoCore' is not the expected type.");
            return null;
        }
        logger.info("[ImagoCoreHook] Hooked into ImagoCore v"
                + core.getDescription().getVersion());
        return new ImagoCoreHook(core, logger);
    }

    // ── Binding management ──────────────────────────────────────

    /**
     * Binds a Guild Plugin GUI type to an ImagoCore GUI entry.
     *
     * @param guiType  the GUI type identifier (e.g. "MainGuildGUI")
     * @param entryId  the ImagoCore entry ID (e.g. "54-default")
     * @return true if binding succeeded
     */
    public boolean bind(String guiType, String entryId) {
        GuiEntry entry = imagoCore.getGuiRegistry().getEntry(entryId);
        if (entry == null) {
            logger.warning("[ImagoCoreHook] GUI entry not found: " + entryId);
            return false;
        }
        guiController.bind(guiType, entry);
        return true;
    }

    /**
     * Binds a Guild Plugin GUI type directly to a GuiEntry.
     */
    public void bind(String guiType, GuiEntry entry) {
        guiController.bind(guiType, entry);
    }

    /**
     * Checks if a GUI type has a bound background image.
     */
    public boolean hasBinding(String guiType) {
        return guiController.hasBinding(guiType);
    }

    // ── Inventory creation ──────────────────────────────────────

    /**
     * Creates an inventory with the bound background image title.
     *
     * @param size    inventory size (multiple of 9)
     * @param guiType the GUI type identifier (must be bound)
     * @return the inventory with image title, or null if no binding
     */
    public Inventory createTitledInventory(int size, String guiType) {
        return guiController.createTitledInventory(size, guiType);
    }

    /**
     * Creates an inventory with background + overlay decorations.
     *
     * @param size       inventory size
     * @param guiType    the GUI type (background binding)
     * @param overlays   overlay specifications (char images with positions)
     * @return the inventory with multi-layer title, or null if no binding
     */
    public Inventory createTitledInventory(int size, String guiType,
                                           List<GuiTitleRenderer.OverlaySpec> overlays) {
        if (!guiController.hasBinding(guiType)) {
            return null;
        }
        GuiEntry entry = guiController.getBinding(guiType);
        if (entry == null) return null;

        if (overlays == null || overlays.isEmpty()) {
            return guiController.createTitledInventory(size, guiType);
        }

        net.kyori.adventure.text.Component title =
                GuiTitleRenderer.buildWithOverlays(entry, overlays);
        return guiController.createTitledInventory(size,
                TitleComposition.builder().background(entry).build());
    }

    // ── Char image access ───────────────────────────────────────

    /**
     * Gets a registered character image entry by name.
     *
     * @param name the char image name (filename without .png)
     * @return the entry, or null if not registered
     */
    public CharEntry getCharEntry(String name) {
        return imagoCore.getCharRegistry().getEntry(name);
    }

    /**
     * Gets a registered GUI entry by ID.
     *
     * @param id the entry ID (e.g. "54-default")
     * @return the entry, or null if not registered
     */
    public GuiEntry getGuiEntry(String id) {
        return imagoCore.getGuiRegistry().getEntry(id);
    }

    /**
     * Builds an OverlaySpec from a char image name and position.
     *
     * @param charName the char image name
     * @param x        horizontal offset from background left edge (pixels)
     * @return the overlay spec, or null if char not found
     */
    public GuiTitleRenderer.OverlaySpec buildOverlay(String charName, int x) {
        CharEntry entry = getCharEntry(charName);
        if (entry == null) return null;
        return GuiTitleRenderer.OverlaySpec.from(entry, x, 222);
    }

    /**
     * @return the underlying ImagoCore plugin instance
     */
    public ImagoCore getImagoCore() {
        return imagoCore;
    }
}
