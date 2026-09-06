package com.guild.core.gui.imago;

import com.guild.GuildPlugin;
import com.guild.core.geyser.PlayerConnectionService;
import com.guild.core.hook.ImagoCoreHook;
import com.guild.core.hook.ImagoGuiConfig;
import com.guild.core.gui.GUI;
import com.guild.core.gui.layout.GuiImageLayoutConfig;
import com.guild.sdk.gui.ModuleGUIRegistration;
import org.a.imagoCore.image.display.gui.GuiTitleRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * ImagoCore 软依赖集成：配置加载、GUI 图片标题 Inventory 创建。
 */
public final class ImagoGuiIntegration {

    private final GuildPlugin plugin;
    private final Logger logger;

    private ImagoCoreHook imagoHook;
    private ImagoGuiConfig imagoConfig;
    private GuiImageLayoutConfig imageLayoutConfig;
    private boolean imagoAvailable;

    public ImagoGuiIntegration(GuildPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void initialize() {
        imagoHook = null;
        imagoAvailable = false;

        imagoConfig = new ImagoGuiConfig(plugin.getDataFolder(), logger);
        imagoConfig.load();

        imageLayoutConfig = new GuiImageLayoutConfig(plugin.getDataFolder(), logger);
        imageLayoutConfig.load();

        if (!imagoConfig.isEnabled()) {
            logger.info("[ImagoCore] Integration disabled in imago-gui.yml");
            return;
        }

        imagoHook = ImagoCoreHook.detect(logger);
        if (imagoHook == null) {
            logger.info("[ImagoCore] Plugin not found, GUI image integration skipped.");
            return;
        }

        for (Map.Entry<String, String> entry : imagoConfig.getAllBindings().entrySet()) {
            if ("false".equalsIgnoreCase(entry.getValue())) {
                continue;
            }
            imagoHook.bind(entry.getKey(), entry.getValue());
        }

        imagoAvailable = true;
        logger.info("[ImagoCore] Integration active — image titles and layouts enabled.");
    }

    public void reload() {
        initialize();
    }

    public boolean isAvailable() {
        return imagoAvailable;
    }

    public GuiImageLayoutConfig getImageLayoutConfig() {
        return imageLayoutConfig;
    }

    ImagoGuiConfig getImagoConfig() {
        return imagoConfig;
    }

    public boolean isImageLayoutActive(String guiType) {
        return imagoAvailable
                && imagoConfig != null
                && imagoConfig.hasConfig(guiType)
                && imageLayoutConfig != null
                && imageLayoutConfig.hasLayout(guiType);
    }

    public boolean isImageLayoutActive(Player player, String guiType) {
        if (player != null && PlayerConnectionService.isBedrockPlayer(player)) {
            return false;
        }
        return isImageLayoutActive(guiType);
    }

    public boolean isImageGuiActive(String guiType) {
        return imagoAvailable && imagoConfig != null && imagoConfig.hasConfig(guiType);
    }

    public boolean isImageGuiActive(Player player, String guiType) {
        if (player != null && PlayerConnectionService.isBedrockPlayer(player)) {
            return false;
        }
        return isImageGuiActive(guiType);
    }

    /**
     * 创建 GUI Inventory：优先 Imago 图片标题，不可用时回退字符串标题。
     */
    public Inventory createInventory(Player player, GUI gui, boolean debugEnabled) {
        boolean bedrockPlayer = player != null && PlayerConnectionService.isBedrockPlayer(player);
        if (!imagoAvailable || bedrockPlayer || imagoHook == null || imagoConfig == null) {
            return Bukkit.createInventory(null, gui.getSize(), gui.getTitle());
        }

        String guiType = gui.getGuiType();
        if (imagoConfig.hasConfig(guiType)) {
            List<ImagoGuiConfig.OverlayConfig> overlayConfigs = imagoConfig.getOverlays(guiType);
            if (!overlayConfigs.isEmpty()) {
                List<GuiTitleRenderer.OverlaySpec> specs = new ArrayList<>();
                for (ImagoGuiConfig.OverlayConfig oc : overlayConfigs) {
                    GuiTitleRenderer.OverlaySpec spec = imagoHook.buildOverlay(
                            oc.getCharName(), oc.getX(), oc.getAscent());
                    if (spec != null) {
                        specs.add(spec);
                    }
                }
                if (!specs.isEmpty()) {
                    Inventory inv = imagoHook.createTitledInventory(gui.getSize(), guiType, specs);
                    if (inv != null) {
                        if (debugEnabled) {
                            logger.info("[ImagoCore] " + guiType + ": image title + "
                                    + specs.size() + " overlay(s)");
                        }
                        return inv;
                    }
                }
            }

            Inventory inv = imagoHook.createTitledInventory(gui.getSize(), guiType);
            if (inv != null) {
                if (debugEnabled) {
                    logger.info("[ImagoCore] " + guiType + ": image title (background only)");
                }
                return inv;
            }
        }

        ModuleGUIRegistration moduleReg = plugin.getModuleManager().getRegistry().getCustomGUIRegistration(guiType);
        if (moduleReg != null && moduleReg.getImageEntryId() != null) {
            Inventory inv = imagoHook.createTitledInventoryByEntry(gui.getSize(), moduleReg.getImageEntryId());
            if (inv != null) {
                if (debugEnabled) {
                    logger.info("[ImagoCore] Module GUI " + guiType + ": image title via entry '"
                            + moduleReg.getImageEntryId() + "'");
                }
                return inv;
            }
        }

        return Bukkit.createInventory(null, gui.getSize(), gui.getTitle());
    }

    /** 供测试或诊断使用。 */
    File getDataFolder() {
        return plugin.getDataFolder();
    }
}
