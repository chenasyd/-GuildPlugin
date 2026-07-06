package com.guild.core.gui;

import com.guild.GuildPlugin;
import com.guild.gui.GuildNameInputGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.function.Function;

import com.guild.core.utils.CompatibleScheduler;

/**
 * GUI管理器 - 管理所有GUI界面
 */
public class GUIManager implements Listener {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<UUID, GUI> openGuis = new HashMap<>();
    private final Map<UUID, Function<String, Boolean>> inputModes = new HashMap<>();
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Deque<GUI>> navigationStacks = new HashMap<>();
    
    public GUIManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 检查是否启用了详细调试日志
     */
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
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> openGUI(player, gui));
            return;
        }
        
        try {
            // 关闭玩家当前打开的GUI
            closeGUI(player);
            
            // 创建新的GUI
            Inventory inventory = Bukkit.createInventory(null, gui.getSize(), gui.getTitle());
            
            // 设置GUI内容
            gui.setupInventory(inventory);
            
            // 打开GUI
            player.openInventory(inventory);
            
            // 记录打开的GUI
            openGuis.put(player.getUniqueId(), gui);

            // 通知 ImagoCore 桥接：GUI 已打开
            plugin.notifyBridgeGuiEvent("gui.image.bind", player, gui);
            
            if (isDebugEnabled()) {
                logger.info("Player " + player.getName() + " opened GUI: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("Error opening GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 关闭GUI
     */
    public void closeGUI(Player player) {
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> closeGUI(player));
            return;
        }
        
        try {
            GUI gui = openGuis.get(player.getUniqueId());
            if (gui != null) {
                // 通知 ImagoCore 桥接：GUI 即将关闭
                plugin.notifyBridgeGuiEvent("gui.image.unbind", player, gui);

                // 从记录中移除
                openGuis.remove(player.getUniqueId());

                // 关闭库存
                if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
                    player.closeInventory();
                }
                
                if (isDebugEnabled()) {
                    logger.info("玩家 " + player.getName() + " 关闭了GUI: " + gui.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            logger.severe("Error closing GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取玩家当前打开的GUI
     */
    public GUI getOpenGUI(Player player) {
        return openGuis.get(player.getUniqueId());
    }
    
    /**
     * 检查玩家是否打开了GUI
     */
    public boolean hasOpenGUI(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }
    
    /**
     * 处理GUI点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        GUI gui = openGuis.get(player.getUniqueId());
        if (gui == null) {
            return;
        }
        
        // 防止快速点击
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastClickTime.get(player.getUniqueId());
        if (lastClick != null && currentTime - lastClick < 200) { // 200ms防抖
            event.setCancelled(true);
            return;
        }
        lastClickTime.put(player.getUniqueId(), currentTime);
        
        try {
            // 阻止玩家移动物品
            event.setCancelled(true);
            
            // 处理GUI点击
            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();
            
            // 添加调试日志
                if (isDebugEnabled()) {
                    logger.info("Player " + player.getName() + " clicked GUI: " + gui.getClass().getSimpleName() + " slot: " + slot);
                }
            
            // 处理所有点击，包括空物品的点击
            gui.onClick(player, slot, clickedItem, event.getClick());
        } catch (Exception e) {
            logger.severe("Error handling GUI click: " + e.getMessage());
            e.printStackTrace();
            // 发生错误时关闭GUI
            closeGUI(player);
        }
    }
    
    /**
     * 处理GUI关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        try {
            GUI gui = openGuis.remove(player.getUniqueId());
            if (gui != null) {
                // 只有在玩家确实在输入模式时才清理
                if (inputModes.containsKey(player.getUniqueId())) {
                    clearInputMode(player);
                }
                
                gui.onClose(player);
                if (isDebugEnabled()) {
                    logger.info("Player " + player.getName() + " closed GUI: " + gui.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            logger.severe("Error processing GUI close: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 刷新GUI
     */
    public void refreshGUI(Player player) {
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> refreshGUI(player));
            return;
        }
        
        try {
            GUI gui = openGuis.get(player.getUniqueId());
            if (gui != null) {
                // 关闭当前GUI
                closeGUI(player);
                
                // 重新打开GUI
                openGUI(player, gui);
                
                if (isDebugEnabled()) {
                    logger.info("Player " + player.getName() + "'s GUI refreshed: " + gui.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            logger.severe("Error refreshing GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== GUI 导航栈 ====================

    /**
     * 打开 GUI 并将当前 GUI 压入导航栈
     */
    public void pushAndOpen(Player player, GUI newGui) {
        GUI current = openGuis.get(player.getUniqueId());
        if (current != null) {
            getNavStack(player).push(current);
        }
        openGUI(player, newGui);
    }

    /**
     * 弹出导航栈顶部并打开
     * @return 是否成功导航回上一页
     */
    public boolean popAndOpen(Player player) {
        Deque<GUI> stack = navigationStacks.remove(player.getUniqueId());
        if (stack == null || stack.isEmpty()) return false;
        GUI previous = stack.pop();
        if (!stack.isEmpty()) {
            navigationStacks.put(player.getUniqueId(), stack);
        }
        openGUI(player, previous);
        return true;
    }

    private Deque<GUI> getNavStack(Player player) {
        return navigationStacks.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
    }

    /**
     * 清除玩家的导航栈
     */
    public void clearNavigation(Player player) {
        navigationStacks.remove(player.getUniqueId());
    }

    /**
     * 关闭所有GUI
     */
    public void closeAllGUIs() {
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            // 如果插件已禁用，直接返回，不尝试调度任务
            if (!plugin.isEnabled()) {
                logger.warning("Plugin disabled, skipping GUI close task scheduling");
                openGuis.clear();
                return;
            }
            CompatibleScheduler.runTask(plugin, this::closeAllGUIs);
            return;
        }

        try {
            for (UUID playerUuid : openGuis.keySet()) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    closeGUI(player);
                }
            }
            openGuis.clear();
            if (isDebugEnabled()) {
                logger.info("Closed all GUIs");
            }
        } catch (Exception e) {
            logger.severe("Error closing all GUIs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取打开的GUI数量
     */
    public int getOpenGUICount() {
        return openGuis.size();
    }
    
    /**
     * 设置玩家输入模式
     */
    public void setInputMode(Player player, Function<String, Boolean> inputHandler) {
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> setInputMode(player, inputHandler));
            return;
        }
        
        try {
            inputModes.put(player.getUniqueId(), inputHandler);
            if (isDebugEnabled()) {
                logger.info("Player " + player.getName() + " entered input mode");
            }
        } catch (Exception e) {
            logger.severe("Error setting input mode: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 设置玩家输入模式（带GUI对象）
     */
    public void setInputMode(Player player, String mode, GUI gui) {
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> setInputMode(player, mode, gui));
            return;
        }
        
        try {
            // 为工会名称输入创建特殊的输入处理器
            if ("guild_name_input".equals(mode) && gui instanceof GuildNameInputGUI) {
                GuildNameInputGUI nameInputGUI = (GuildNameInputGUI) gui;
                inputModes.put(player.getUniqueId(), input -> {
                    if ("Cancel".equals(input.trim())) {
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
            logger.severe("Error setting input mode: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清除玩家输入模式
     */
    public void clearInputMode(Player player) {
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
            CompatibleScheduler.runTask(plugin, () -> clearInputMode(player));
            return;
        }
        
        try {
            inputModes.remove(player.getUniqueId());
            if (isDebugEnabled()) {
                logger.info("Player " + player.getName() + " exited input mode");
            }
        } catch (Exception e) {
            logger.severe("Error clearing input mode: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查玩家是否在输入模式
     */
    public boolean isInInputMode(Player player) {
        return inputModes.containsKey(player.getUniqueId());
    }
    
    /**
     * 处理玩家输入
     */
    public boolean handleInput(Player player, String input) {
        try {
            Function<String, Boolean> handler = inputModes.get(player.getUniqueId());
            if (handler != null) {
                boolean result = handler.apply(input);
                if (result) {
                    inputModes.remove(player.getUniqueId());
                }
                return result;
            }
            return false;
        } catch (Exception e) {
            logger.severe("Error handling player input: " + e.getMessage());
            e.printStackTrace();
            // 发生错误时清除输入模式
            clearInputMode(player);
            return false;
        }
    }
}
