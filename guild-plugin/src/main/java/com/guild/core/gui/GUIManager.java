package com.guild.core.gui;

import com.guild.GuildPlugin;
import com.guild.core.geyser.PlayerConnectionService;
import com.guild.core.hook.ImagoCoreHook;
import com.guild.core.hook.ImagoGuiConfig;
import com.guild.core.gui.layout.GuiImageLayoutConfig;
import com.guild.gui.GuildNameInputGUI;
import org.a.imagoCore.image.display.gui.GuiTitleRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // ImagoCore integration (null if not available)
    private ImagoCoreHook imagoHook;
    private ImagoGuiConfig imagoConfig;
    private GuiImageLayoutConfig imageLayoutConfig;
    
    public GUIManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 初始化 ImagoCore 集成（软依赖）
     * 在 plugin onEnable 中调用，ImagoCore 不存在时静默跳过
     */
    public void initializeImagoHook() {
        // 始终先清除旧 hook，防止 enabled:false 时残留
        this.imagoHook = null;

        this.imagoConfig = new ImagoGuiConfig(plugin.getDataFolder(), logger);
        this.imagoConfig.load();

        // 图像布局配置（独立于 ImagoCore 是否存在）
        this.imageLayoutConfig = new GuiImageLayoutConfig(plugin.getDataFolder(), logger);
        this.imageLayoutConfig.load();

        if (!imagoConfig.isEnabled()) {
            logger.info("[ImagoCore] Integration disabled in imago-gui.yml");
            return;
        }

        this.imagoHook = ImagoCoreHook.detect(logger);
        if (imagoHook == null) {
            logger.info("[ImagoCore] Plugin not found, GUI image integration skipped.");
            return;
        }

        // Register bindings from config (skip explicitly disabled GUIs)
        for (Map.Entry<String, String> entry : imagoConfig.getAllBindings().entrySet()) {
            if ("false".equalsIgnoreCase(entry.getValue())) continue;
            imagoHook.bind(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 重新加载 ImagoCore 配置。
     * 处理 enabled 状态切换：true→false 时清除 hook，false→true 时重新检测。
     */
    public void reloadImagoConfig() {
        // 完整重新初始化（内部会先清除旧 hook）
        initializeImagoHook();
    }

    /**
     * 检查指定 GUI 是否处于图像布局模式。
     * 条件：ImagoCore 已连接 + enabled + 有绑定 + 有布局配置。
     *
     * @param guiType GUI 类型名（如 "MainGuildGUI"）
     * @return true 表示该 GUI 应使用图像布局（透明载体 + 多槽位）
     */
    public boolean isImageLayoutActive(String guiType) {
        return imagoHook != null
                && imagoConfig != null
                && imagoConfig.isEnabled()
                && imagoConfig.hasConfig(guiType)
                && imageLayoutConfig != null
                && imageLayoutConfig.hasLayout(guiType);
    }

    /**
     * 获取图像布局配置实例。
     *
     * @return 布局配置，若未初始化则返回 null
     */
    public GuiImageLayoutConfig getImageLayoutConfig() {
        return imageLayoutConfig;
    }

    /**
     * 检查指定 GUI 是否启用了图像模式（不要求有布局配置）。
     * 条件：ImagoCore 已连接 + enabled + 有绑定。
     * 用于不需要多槽位布局、只需透明化物品的 GUI。
     */
    public boolean isImageGuiActive(String guiType) {
        return imagoHook != null
                && imagoConfig != null
                && imagoConfig.isEnabled()
                && imagoConfig.hasConfig(guiType);
    }

    /**
     * 对已填充好的 Inventory 应用图像模式后处理。
     *
     * <p>操作：
     * <ol>
     *   <li>移除所有玻璃板 / 填充物（视觉由背景图替代）</li>
     *   <li>将剩余非空物品转换为透明载体（保留名称和 lore，
     *       替换材质为配置的透明物品 + CustomModelData）</li>
     * </ol>
     *
     * <p>在 GUI 的 {@code setupInventory()} 末尾调用即可：
     * <pre>{@code
     * plugin.getGuiManager().applyImageModeIfNeeded(inventory, getGuiType());
     * }</pre>
     *
     * @param inventory 已设置好内容的 inventory
     * @param guiType   GUI 类型名
     */
    public void applyImageModeIfNeeded(Inventory inventory, String guiType) {
        if (!isImageGuiActive(guiType)) return;

        Material transMat = imageLayoutConfig != null
                ? imageLayoutConfig.getTransparentMaterial() : Material.BARRIER;
        int modelData = imageLayoutConfig != null
                ? imageLayoutConfig.getTransparentModelData() : 10001;

        // 有布局配置时：精确转换配置中的槽位，保留动态内容
        if (imageLayoutConfig != null && imageLayoutConfig.hasLayout(guiType)) {
            // 收集布局中所有功能槽位
            Set<Integer> layoutSlots = new HashSet<>();
            for (List<Integer> slots : imageLayoutConfig.getLayout(guiType).values()) {
                layoutSlots.addAll(slots);
            }

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;

                // 移除所有填充物（玻璃板类）
                if (isFillerItem(item.getType())) {
                    inventory.setItem(i, null);
                    continue;
                }

                // 仅转换布局配置中的功能槽位为透明载体
                if (layoutSlots.contains(i)) {
                    inventory.setItem(i, toTransparentCarrier(item, transMat, modelData));
                }
                // 其余槽位（动态查询内容）保持原样
            }
            return;
        }

        // 无布局配置时的回退行为：转换所有非填充物品
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            if (isFillerItem(item.getType())) {
                inventory.setItem(i, null);
                continue;
            }

            inventory.setItem(i, toTransparentCarrier(item, transMat, modelData));
        }
    }

    /**
     * 将物品转换为透明载体（保留名称和 lore）。
     */
    private ItemStack toTransparentCarrier(ItemStack original, Material transMat, int modelData) {
        ItemStack transparent = new ItemStack(transMat);
        org.bukkit.inventory.meta.ItemMeta oldMeta = original.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta newMeta = transparent.getItemMeta();
        if (oldMeta != null && newMeta != null) {
            if (oldMeta.hasDisplayName()) {
                newMeta.setDisplayName(oldMeta.getDisplayName());
            }
            if (oldMeta.hasLore()) {
                newMeta.setLore(oldMeta.getLore());
            }
            newMeta.setCustomModelData(modelData);
            transparent.setItemMeta(newMeta);
        }
        return transparent;
    }

    /**
     * 判断是否为填充/边框物品（玻璃板类）。
     */
    private boolean isFillerItem(Material mat) {
        return mat == Material.BLACK_STAINED_GLASS_PANE
                || mat == Material.GRAY_STAINED_GLASS_PANE
                || mat == Material.WHITE_STAINED_GLASS_PANE
                || mat == Material.GLASS_PANE
                || mat.name().endsWith("_STAINED_GLASS_PANE");
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
        // Folia: 玩家 API 必须在实体所属的区域线程执行，而非全局区域线程
        if (!CompatibleScheduler.isEntityThread(player)) {
            CompatibleScheduler.runTask(plugin, player, () -> openGUI(player, gui));
            return;
        }
        
        try {
            // 关闭玩家当前打开的GUI
            closeGUI(player);
            
            // 创建新的GUI — 优先使用 ImagoCore 图片标题
            Inventory inventory = createInventoryForGui(gui);
            
            // 设置GUI内容
            gui.setupInventory(inventory);
            
            // 打开GUI
            player.openInventory(inventory);
            
            // 记录打开的GUI
            openGuis.put(player.getUniqueId(), gui);

            // 文件日志：记录 GUI 打开操作
            if (plugin.getFileLogger() != null) {
                plugin.getFileLogger().logGui(player.getName(),
                        "打开 " + gui.getGuiType());
            }
            
            if (isDebugEnabled()) {
                logger.info("Player " + player.getName() + " opened GUI: " + gui.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.severe("Error opening GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建 GUI 的 Inventory 实例。
     * 如果 ImagoCore 可用且该 GUI 类型有绑定，则使用图片标题；
     * 否则使用原始字符串标题（完全兼容无 ImagoCore 环境）。
     */
    private Inventory createInventoryForGui(GUI gui) {
        if (imagoHook != null && imagoConfig != null && imagoConfig.isEnabled()) {
            String guiType = gui.getGuiType();
            if (imagoConfig.hasConfig(guiType)) {
                // 构建叠加层（如果有配置）
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
                            if (isDebugEnabled()) {
                                logger.info("[ImagoCore] " + guiType + ": 使用图片标题 + "
                                        + specs.size() + " 个叠加层");
                            }
                            return inv;
                        }
                    }
                }

                // 纯背景（无叠加层）
                Inventory inv = imagoHook.createTitledInventory(gui.getSize(), guiType);
                if (inv != null) {
                    if (isDebugEnabled()) {
                        logger.info("[ImagoCore] " + guiType + ": 使用纯背景图片标题");
                    }
                    return inv;
                }
            }
        }

        // 回退：标准字符串标题（无 ImagoCore 或无绑定时）
        if (isDebugEnabled()) {
            logger.info("[ImagoCore] " + gui.getGuiType() + ": 回退到字符串标题 \""
                    + gui.getTitle() + "\"");
        }
        return Bukkit.createInventory(null, gui.getSize(), gui.getTitle());
    }
    
    /**
     * 关闭GUI
     */
    public void closeGUI(Player player) {
        // Folia: 玩家 API 必须在实体所属的区域线程执行
        if (!CompatibleScheduler.isEntityThread(player)) {
            CompatibleScheduler.runTask(plugin, player, () -> closeGUI(player));
            return;
        }
        
        try {
            GUI gui = openGuis.get(player.getUniqueId());
            if (gui != null) {
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

            // 文件日志：记录 GUI 点击操作
            if (plugin.getFileLogger() != null) {
                plugin.getFileLogger().logGui(player.getName(),
                        "点击 " + gui.getGuiType() + " slot=" + slot);
            }
            
            // 处理所有点击，包括空物品的点击
            // 基岩版玩家的右键点击不可靠，统一映射为左键
            ClickType adaptedClick = PlayerConnectionService.adaptClick(player, event.getClick());
            gui.onClick(player, slot, clickedItem, adaptedClick);
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
        // Folia: 玩家 API 必须在实体所属的区域线程执行
        if (!CompatibleScheduler.isEntityThread(player)) {
            CompatibleScheduler.runTask(plugin, player, () -> refreshGUI(player));
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
        // 如果插件已禁用，直接清理记录，不尝试调度任务
        if (!plugin.isEnabled()) {
            logger.warning("Plugin disabled, skipping GUI close task scheduling");
            openGuis.clear();
            return;
        }

        try {
            // 快照避免 ConcurrentModificationException（closeGUI 会修改 openGuis）
            for (UUID playerUuid : new java.util.ArrayList<>(openGuis.keySet())) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    // closeGUI 内部会检查 isEntityThread 并调度到正确的区域线程
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
