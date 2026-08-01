package com.guild.core.hook;

import org.a.imagoCore.ImagoCore;
import org.a.imagoCore.config.CharEntry;
import org.a.imagoCore.config.GuiEntry;
import org.a.imagoCore.gui.GuiController;
import org.a.imagoCore.image.display.gui.GuiTitleRenderer;
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

        // Build the full title (background + overlays) and pass it directly
        net.kyori.adventure.text.Component title =
                GuiTitleRenderer.buildWithOverlays(entry, overlays);
        return guiController.createTitledInventory(size, title, guiType);
    }

    /**
     * Creates an inventory with the image title from a specific GuiEntry,
     * bypassing the guiType binding lookup.
     *
     * <p>Used by module GUI registrations that reference an ImagoCore entry
     * directly.
     *
     * @param size    inventory size (multiple of 9)
     * @param entryId the ImagoCore GUI entry ID (e.g. "54-default")
     * @return the inventory with image title, or null if entry not found
     */
    public Inventory createTitledInventoryByEntry(int size, String entryId) {
        GuiEntry entry = getGuiEntry(entryId);
        if (entry == null) return null;

        net.kyori.adventure.text.Component title =
                GuiTitleRenderer.buildWithOverlays(entry, List.of());
        return guiController.createTitledInventory(size, title, entryId);
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
     * <p>Lookup order: CharRegistry ({@code char/} directory) first,
     * then GuiRegistry ({@code gui/} directory) as fallback.  This
     * allows both dedicated char images and GUI background textures
     * to be used as overlay decorations.
     *
     * @param charName the image name (filename without .png)
     * @param x        horizontal offset from background left edge (pixels)
     * @return the overlay spec, or null if image not found
     */
    public GuiTitleRenderer.OverlaySpec buildOverlay(String charName, int x) {
        // 1. Try CharRegistry (char/ directory)
        CharEntry entry = getCharEntry(charName);
        if (entry != null) {
            return GuiTitleRenderer.OverlaySpec.from(entry, x, 222);
        }

        // 2. Fallback: GuiRegistry (gui/ directory)
        GuiEntry guiEntry = findGuiEntryByName(charName);
        if (guiEntry != null) {
            logger.info("[ImagoCoreHook] Overlay '" + charName
                    + "' resolved from GuiRegistry (gui/ directory).");
            return GuiTitleRenderer.OverlaySpec.fromGuiEntry(guiEntry, x);
        }

        logger.warning("[ImagoCoreHook] Overlay image not found: " + charName
                + " (searched char/ and gui/ directories)");
        return null;
    }

    /**
     * Builds an OverlaySpec with an optional ascent (Y position) override.
     * When ascent is non-null and the image is a char entry, a variant
     * font provider is created in ImagoCore so the same image renders
     * at a different vertical position.
     *
     * <p><b>Note:</b> Ascent variants are only supported for images in
     * the {@code char/} directory.  GUI background images ({@code gui/}
     * directory) used as overlays will render at their default ascent;
     * a warning is logged if an ascent override is requested.
     *
     * @param charName the image name
     * @param x        horizontal offset from background left edge (pixels)
     * @param ascent   vertical position override, or null for default
     * @return the overlay spec, or null if image not found
     */
    public GuiTitleRenderer.OverlaySpec buildOverlay(String charName, int x,
                                                      Integer ascent) {
        if (ascent != null) {
            // Variant system only works with CharRegistry entries
            CharEntry variant = imagoCore.getOrCreateCharVariant(charName, ascent);
            if (variant != null) {
                return GuiTitleRenderer.OverlaySpec.from(variant, x, 222);
            }
            // Not a char entry — might be a GUI entry; ascent override unavailable
            logger.warning("[ImagoCoreHook] Ascent variant unavailable for '"
                    + charName + "' (ascent=" + ascent + "). "
                    + "Place the image in plugins/ImagoCore/char/ for Y-position control. "
                    + "Falling back to default ascent.");
        }
        return buildOverlay(charName, x);
    }

    /**
     * Searches GuiRegistry for an entry whose entry name matches the
     * given name.  For example, {@code "gui2"} matches the entry with
     * ID {@code "54-gui2"}.
     *
     * @param name the entry name (without folder prefix)
     * @return the matching GuiEntry, or null
     */
    private GuiEntry findGuiEntryByName(String name) {
        for (GuiEntry e : imagoCore.getGuiRegistry().getEntries()) {
            if (e.getEntryName().equals(name)) {
                return e;
            }
        }
        return null;
    }

    /**
     * @return the underlying ImagoCore plugin instance
     */
    public ImagoCore getImagoCore() {
        return imagoCore;
    }
}
