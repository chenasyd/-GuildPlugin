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
    private final Map<UUID, Long> lastClickTime = new HashMap<>(); // 防止快速点击
    
    public GUIManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 初始化GUI管理器
     */
    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        logger.info("GUI管理器初始化完成");
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
            
            logger.info("玩家 " + player.getName() + " 打开了GUI: " + gui.getClass().getSimpleName());
        } catch (Exception e) {
            logger.severe("打开GUI时发生错误: " + e.getMessage());
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
            GUI gui = openGuis.remove(player.getUniqueId());
            if (gui != null) {
                // 关闭库存
                if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null) {
                    player.closeInventory();
                }
                
                logger.info("玩家 " + player.getName() + " 关闭了GUI: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("关闭GUI时发生错误: " + e.getMessage());
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
            logger.info("玩家 " + player.getName() + " 点击了GUI: " + gui.getClass().getSimpleName() + " 槽位: " + slot);
            
            // 处理所有点击，包括空物品的点击
            gui.onClick(player, slot, clickedItem, event.getClick());
        } catch (Exception e) {
            logger.severe("处理GUI点击时发生错误: " + e.getMessage());
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
                logger.info("玩家 " + player.getName() + " 关闭了GUI: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("处理GUI关闭时发生错误: " + e.getMessage());
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
                
                logger.info("玩家 " + player.getName() + " 的GUI已刷新: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("刷新GUI时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 关闭所有GUI
     */
    public void closeAllGUIs() {
        // 确保在主线程中执行
        if (!CompatibleScheduler.isPrimaryThread()) {
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
            logger.info("已关闭所有GUI");
        } catch (Exception e) {
            logger.severe("关闭所有GUI时发生错误: " + e.getMessage());
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
            logger.info("玩家 " + player.getName() + " 进入输入模式");
        } catch (Exception e) {
            logger.severe("设置输入模式时发生错误: " + e.getMessage());
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
                    if ("取消".equals(input.trim())) {
                        nameInputGUI.handleCancel(player);
                        return true;
                    }
                    nameInputGUI.handleInputComplete(player, input);
                    return true;
                });
                logger.info("玩家 " + player.getName() + " 进入工会名称输入模式");
            } else {
                logger.warning("未知的输入模式: " + mode);
            }
        } catch (Exception e) {
            logger.severe("设置输入模式时发生错误: " + e.getMessage());
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
            logger.info("玩家 " + player.getName() + " 退出输入模式");
        } catch (Exception e) {
            logger.severe("清除输入模式时发生错误: " + e.getMessage());
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
            logger.severe("处理玩家输入时发生错误: " + e.getMessage());
            e.printStackTrace();
            // 发生错误时清除输入模式
            clearInputMode(player);
            return false;
        }
    }
}
