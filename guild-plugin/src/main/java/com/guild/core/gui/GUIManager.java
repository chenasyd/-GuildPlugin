package com.guild.core.gui;

import com.guild.GuildPlugin;
import com.guild.core.geyser.PlayerConnectionService;
import com.guild.core.gui.imago.GuiImageLayoutApplier;
import com.guild.core.gui.imago.ImagoGuiIntegration;
import com.guild.core.gui.layout.GuiImageLayoutConfig;
import com.guild.core.gui.session.GuiClickDebouncer;
import com.guild.core.gui.session.GuiInputModeController;
import com.guild.core.gui.session.GuiNavigationStack;
import com.guild.core.gui.session.GuiSessionManager;
import com.guild.gui.GuildNameInputGUI;
import com.guild.sdk.gui.BedrockFormProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Function;

import com.guild.core.utils.CompatibleScheduler;

/**
 * GUI管理器 - 管理所有GUI界面
 */
public class GUIManager implements Listener {

    private final GuildPlugin plugin;
    private final Logger logger;
    private final GuiSessionManager sessions = new GuiSessionManager();
    private final GuiNavigationStack navigation = new GuiNavigationStack();
    private final GuiInputModeController inputModes = new GuiInputModeController();
    private final GuiClickDebouncer clickDebouncer = new GuiClickDebouncer();
    private final ImagoGuiIntegration imagoIntegration;
    private final GuiImageLayoutApplier imageLayoutApplier;

    public GUIManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.imagoIntegration = new ImagoGuiIntegration(plugin, logger);
        this.imageLayoutApplier = new GuiImageLayoutApplier(plugin, imagoIntegration);
    }

    /**
     * 初始化 ImagoCore 集成（软依赖）
     * 在 plugin onEnable 中调用，ImagoCore 不存在时静默跳过
     */
    public void initializeImagoHook() {
        imagoIntegration.initialize();
    }

    /**
     * 重新加载 ImagoCore 配置。
     * 处理 enabled 状态切换：true→false 时清除 hook，false→true 时重新检测。
     */
    public void reloadImagoConfig() {
        imagoIntegration.reload();
    }

    public boolean isImageLayoutActive(String guiType) {
        return imagoIntegration.isImageLayoutActive(guiType);
    }

    public boolean isImageLayoutActive(Player player, String guiType) {
        return imagoIntegration.isImageLayoutActive(player, guiType);
    }

    public GuiImageLayoutConfig getImageLayoutConfig() {
        return imagoIntegration.getImageLayoutConfig();
    }

    public boolean isImageGuiActive(String guiType) {
        return imagoIntegration.isImageGuiActive(guiType);
    }

    public boolean isImageGuiActive(Player player, String guiType) {
        return imagoIntegration.isImageGuiActive(player, guiType);
    }

    /**
     * 对已填充好的 Inventory 应用图像模式后处理。
     *
     * @see GuiImageLayoutApplier#applyIfNeeded(Player, Inventory, String)
     */
    public void applyImageModeIfNeeded(Player player, Inventory inventory, String guiType) {
        imageLayoutApplier.applyIfNeeded(player, inventory, guiType);
    }

    private boolean isDebugEnabled() {
        return plugin.getConfigManager().getMainConfig().getBoolean("debug.enabled", false);
    }

    /**
     * 初始化GUI管理器
     */
    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (isDebugEnabled()) {
            logger.info("GUI manager initialized");
        }
    }

    /**
     * 打开GUI
     */
    public void openGUI(Player player, GUI gui) {
        if (!CompatibleScheduler.isEntityThread(player)) {
            CompatibleScheduler.runTask(plugin, player, () -> openGUI(player, gui));
            return;
        }

        try {
            closeGUI(player);

            if (PlayerConnectionService.isBedrockPlayer(player) && gui.openBedrockForm(player)) {
                sessions.track(player, gui);
                if (plugin.getFileLogger() != null) {
                    plugin.getFileLogger().logGui(player.getName(),
                            "Opened " + gui.getGuiType() + " (Bedrock Form)");
                }
                if (isDebugEnabled()) {
                    logger.info("Player " + player.getName() + " opened Bedrock form: "
                            + gui.getClass().getSimpleName());
                }
                return;
            }

            if (PlayerConnectionService.isBedrockPlayer(player)) {
                String guiType = gui.getGuiType();
                BedrockFormProvider provider = plugin.getModuleManager().getSharedApi().getBedrockFormProvider(guiType);
                if (provider != null) {
                    provider.sendForm(player, java.util.Map.of());
                    sessions.track(player, gui);
                    if (isDebugEnabled()) {
                        logger.info("Player " + player.getName() + " opened module Bedrock form: " + guiType);
                    }
                    return;
                }
            }

            Inventory inventory = imagoIntegration.createInventory(player, gui, isDebugEnabled());

            gui.setupInventory(inventory);

            player.openInventory(inventory);

            sessions.track(player, gui);

            if (plugin.getFileLogger() != null) {
                plugin.getFileLogger().logGui(player.getName(),
                        "Opened " + gui.getGuiType());
            }

            if (isDebugEnabled()) {
                logger.info("Player " + player.getName() + " opened GUI: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error opening GUI", e);
        }
    }

    /**
     * 关闭GUI
     */
    public void closeGUI(Player player) {
        if (!CompatibleScheduler.isEntityThread(player)) {
            CompatibleScheduler.runTask(plugin, player, () -> closeGUI(player));
            return;
        }

        try {
            GUI gui = sessions.remove(player);
            if (gui != null) {
                if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
                    player.closeInventory();
                }

                if (isDebugEnabled()) {
                    logger.info("Player " + player.getName() + " closed GUI: " + gui.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error closing GUI", e);
        }
    }

    public GUI getOpenGUI(Player player) {
        return sessions.get(player);
    }

    public boolean hasOpenGUI(Player player) {
        return sessions.isOpen(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        GUI gui = sessions.get(player);
        if (gui == null) {
            return;
        }

        if (clickDebouncer.shouldIgnore(player)) {
            event.setCancelled(true);
            return;
        }
        try {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();

            if (isDebugEnabled()) {
                logger.info("Player " + player.getName() + " clicked GUI: " + gui.getClass().getSimpleName()
                        + " slot: " + slot);
            }

            if (plugin.getFileLogger() != null) {
                plugin.getFileLogger().logGui(player.getName(),
                        "Clicked " + gui.getGuiType() + " slot=" + slot);
            }

            ClickType adaptedClick = PlayerConnectionService.adaptClick(player, event.getClick());
            gui.onClick(player, slot, clickedItem, adaptedClick);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling GUI click", e);
            closeGUI(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        try {
            GUI gui = sessions.remove(player);
            if (gui != null) {
                if (inputModes.isActive(player)) {
                    clearInputMode(player);
                }

                gui.onClose(player);
                if (isDebugEnabled()) {
                    logger.info("Player " + player.getName() + " closed GUI: " + gui.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing GUI close", e);
        }
    }

    public void refreshGUI(Player player) {
        if (!CompatibleScheduler.isEntityThread(player)) {
            CompatibleScheduler.runTask(plugin, player, () -> refreshGUI(player));
            return;
        }

        try {
            GUI gui = sessions.get(player);
            if (gui != null) {
                closeGUI(player);
                openGUI(player, gui);

                if (isDebugEnabled()) {
                    logger.info("Player " + player.getName() + "'s GUI refreshed: " + gui.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error refreshing GUI", e);
        }
    }

    public void pushAndOpen(Player player, GUI newGui) {
        GUI current = sessions.get(player);
        if (current != null) {
            navigation.push(player, current);
        }
        openGUI(player, newGui);
    }

    public boolean popAndOpen(Player player) {
        GUI previous = navigation.pop(player);
        if (previous == null) {
            return false;
        }
        openGUI(player, previous);
        return true;
    }

    public void clearNavigation(Player player) {
        navigation.clear(player);
    }

    public void closeAllGUIs() {
        if (!plugin.isEnabled()) {
            logger.warning("Plugin disabled, skipping GUI close task scheduling");
            sessions.clear();
            return;
        }

        try {
            for (UUID playerUuid : sessions.snapshotPlayerIds()) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    closeGUI(player);
                }
            }
            sessions.clear();
            if (isDebugEnabled()) {
                logger.info("Closed all GUIs");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error closing all GUIs", e);
        }
    }

    public int getOpenGUICount() {
        return sessions.count();
    }

    public void setInputMode(Player player, Function<String, Boolean> inputHandler) {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> setInputMode(player, inputHandler));
            return;
        }

        try {
            inputModes.set(player, inputHandler);
            if (isDebugEnabled()) {
                logger.info("Player " + player.getName() + " entered input mode");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting input mode", e);
        }
    }

    public void setInputMode(Player player, String mode, GUI gui) {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> setInputMode(player, mode, gui));
            return;
        }

        try {
            if ("guild_name_input".equals(mode) && gui instanceof GuildNameInputGUI nameInputGUI) {
                inputModes.set(player, input -> {
                    String trimmed = input.trim();
                    if ("取消".equals(trimmed) || "Cancel".equalsIgnoreCase(trimmed)) {
                        nameInputGUI.handleCancel(player);
                        return true;
                    }
                    nameInputGUI.handleInputComplete(player, input);
                    return true;
                });
                if (isDebugEnabled()) {
                    logger.info("Player " + player.getName() + " entered guild name input mode");
                }
            } else {
                logger.warning("Unknown input mode: " + mode);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting input mode", e);
        }
    }

    public void clearInputMode(Player player) {
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> clearInputMode(player));
            return;
        }

        try {
            inputModes.clear(player);
            if (isDebugEnabled()) {
                logger.info("Player " + player.getName() + " exited input mode");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error clearing input mode", e);
        }
    }

    public boolean isInInputMode(Player player) {
        return inputModes.isActive(player);
    }

    public boolean handleInput(Player player, String input) {
        try {
            return inputModes.handle(player, input);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling player input", e);
            clearInputMode(player);
            return false;
        }
    }
}
