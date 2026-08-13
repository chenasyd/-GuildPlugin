package com.guild.core.module;

import com.guild.GuildPlugin;
import com.guild.core.ServiceContainer;
import com.guild.core.events.EventBus;
import com.guild.core.gui.GUI;
import com.guild.core.gui.GUIManager;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.QuietLog;
import com.guild.core.utils.ScheduledTaskHandle;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.config.ModuleConfigSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 模块上下文 - 提供给模块的完整 SDK 能力访问入口
 */
public class ModuleContext {

    private final GuildPlugin plugin;
    private final ModuleDescriptor descriptor;
    private final GuildPluginAPI api;
    private final ModuleConfigSection config;
    private final Logger logger;

    /** Tracked Bukkit event listeners for auto-cleanup on module unload */
    private final List<Listener> trackedListeners = new CopyOnWriteArrayList<>();
    /** Tracked scheduled tasks for auto-cleanup on module unload */
    private final List<ScheduledTaskHandle> trackedTasks = new CopyOnWriteArrayList<>();

    public ModuleContext(GuildPlugin plugin, ModuleDescriptor descriptor, GuildPluginAPI sharedApi) {
        this.plugin = plugin;
        this.descriptor = descriptor;
        this.api = sharedApi;
        this.config = new ModuleConfigSection(plugin, descriptor.getId());
        this.logger = Logger.getLogger("GuildModule." + descriptor.getName());
    }

    // ==================== 核心服务访问 ====================

    /** 获取插件实例 */
    public GuildPlugin getPlugin() { return plugin; }

    /** 获取统一 API 门面（推荐方式） */
    public GuildPluginAPI getApi() { return api; }

    /** 获取服务容器 */
    public ServiceContainer getServiceContainer() { return plugin.getServiceContainer(); }

    /** 获取事件总线 */
    public EventBus getEventBus() { return plugin.getEventBus(); }

    /** 获取 GUI 管理器 */
    public GUIManager getGuiManager() { return plugin.getGuiManager(); }

    /** 获取语言管理器 */
    public LanguageManager getLanguageManager() { return plugin.getLanguageManager(); }

    /** 获取模块描述符 */
    public ModuleDescriptor getDescriptor() { return descriptor; }

    /** 获取模块私有配置段 */
    public ModuleConfigSection getConfig() { return config; }

    // ==================== 事件监听器注册（自动追踪） ====================

    /**
     * Register a Bukkit event listener with automatic tracking.
     * The listener will be auto-unregistered when the module is unloaded.
     * Preferred over direct Bukkit.getPluginManager().registerEvents().
     */
    public void registerEvents(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        trackedListeners.add(listener);
    }

    /**
     * Track an externally-created listener for auto-cleanup on unload.
     */
    public void trackListener(Listener listener) {
        trackedListeners.add(listener);
    }

    // ==================== 日志 ====================

    /** 获取模块专用 Logger（INFO+ 会进控制台；运行时细节请用 {@link #logDetail}） */
    public Logger getLogger() { return logger; }

    /**
     * 写入插件文件日志（{@code plugins/GuildPlugin/logs/}），默认不刷控制台。
     * 适合任务完成、奖励、进度等运行时细节。
     */
    public void logDetail(String message) {
        QuietLog.module(descriptor.getName(), message);
    }

    // ==================== 消息发送（本地化） ====================

    /**
     * 发送本地化消息给玩家
     *
     * @param player 目标玩家
     * @param key    消息键名
     * @param args   占位符参数（{0}, {1}, {2} ...），第一个参数同时作为 key 不存在时的 fallback
     */
    public void sendMessage(Player player, String key, Object... args) {
        String message = formatMessage(key, args);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    /**
     * 获取本地化消息文本（不直接发送）
     * <p>
     * 占位符参数按顺序替换 {0}, {1}, {2} ...
     * 当仅传入一个参数时，该参数同时作为 key 不存在时的 fallback 默认值。
     *
     * @param key  消息键名
     * @param args 占位符参数（第一个参数同时作为 fallback）
     */
    public String getMessage(String key, Object... args) {
        return formatMessage(key, args);
    }

    /**
     * 获取本地化消息文本（根据玩家语言解析）
     * <p>
     * 与 {@link #getMessage(String, Object...)} 不同，此方法会根据玩家的语言设置
     * 从对应语言的配置中查找消息，实现真正的按玩家本地化。
     *
     * @param player 目标玩家（用于获取其语言偏好）
     * @param key    消息键名
     * @param args   占位符参数（第一个参数同时作为 fallback）
     */
    public String getMessage(Player player, String key, Object... args) {
        String[] strArgs = null;
        String fallback = "";
        if (args != null && args.length > 0) {
            strArgs = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                strArgs[i] = args[i] != null ? args[i].toString() : "";
            }
            fallback = strArgs[0];
        }
        return ColorUtils.colorize(plugin.getLanguageManager().getModuleIndexedMessage(player, key, fallback, strArgs));
    }

    /**
     * 格式化消息（使用索引占位符 {0}, {1}, {2} ...）
     * <p>
     * 首个参数同时用作 getIndexedMessage 的 defaultValue，
     * 确保 key 不存在时返回有意义的文本而非空字符串。
     */
    private String formatMessage(String key, Object[] args) {
        String[] strArgs = null;
        String fallback = "";
        if (args != null && args.length > 0) {
            strArgs = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                strArgs[i] = args[i] != null ? args[i].toString() : "";
            }
            fallback = strArgs[0];
        }
        return ColorUtils.colorize(plugin.getLanguageManager().getModuleIndexedMessage(key, fallback, strArgs));
    }

    // ==================== 线程调度 ====================

    /** 在服务器主线程调度任务 */
    public void runSync(Runnable task) {
        CompatibleScheduler.runTask(plugin, task);
    }

    /**
     * 在指定实体所在区域线程调度任务。
     * <p>
     * 涉及玩家/实体 API（sendMessage、openInventory、teleport 等）的任务应使用此方法，
     * 以确保在 Folia 下于实体所属的区域线程执行。
     */
    public void runSync(Entity entity, Runnable task) {
        CompatibleScheduler.runTask(plugin, entity, task);
    }

    /** 异步调度任务 */
    public void runAsync(Runnable task) {
        CompatibleScheduler.runTaskAsync(plugin, task);
    }

    /** 延迟调度任务（主线程） */
    public ScheduledTaskHandle runLater(long delayTicks, Runnable task) {
        ScheduledTaskHandle handle = CompatibleScheduler.runTaskLater(plugin, task, delayTicks);
        trackedTasks.add(handle);
        return handle;
    }

    /**
     * 在指定实体所在区域线程延迟调度任务。
     * <p>
     * 涉及玩家/实体 API 的延迟任务应使用此方法，以确保在 Folia 下于实体所属的区域线程执行。
     */
    public void runLater(Entity entity, long delayTicks, Runnable task) {
        CompatibleScheduler.runTaskLater(plugin, entity, task, delayTicks);
    }

    /** 周期性调度任务（主线程） */
    public ScheduledTaskHandle runTimer(long delayTicks, long periodTicks, Runnable task) {
        ScheduledTaskHandle handle = CompatibleScheduler.runTaskTimer(plugin, task, delayTicks, periodTicks);
        trackedTasks.add(handle);
        return handle;
    }

    // ==================== GUI 导航 ====================

    /**
     * 打开 GUI 并自动压入导航栈（用于支持 navigateBack）
     *
     * @param player 目标玩家
     * @param gui    要打开的 GUI
     */
    public void openGUI(Player player, GUI gui) {
        plugin.getGuiManager().pushAndOpen(player, gui);
    }

    /**
     * 导航到上一个 GUI（弹出导航栈顶部并打开）
     *
     * @param player 目标玩家
     * @return 是否成功导航回上一页
     */
    public boolean navigateBack(Player player) {
        return plugin.getGuiManager().popAndOpen(player);
    }

    // ==================== GUI 刷新通知机制 ====================

    /**
     * GUI 刷新监听器接口
     */
    public interface GUIRefreshListener {
        /**
         * 当 GUI 需要刷新时调用
         *
         * @param guiType GUI 类型标识符
         * @param data    相关数据
         */
        void onGUIRefresh(String guiType, Map<String, Object> data);
    }

    private final Map<String, GUIRefreshListener> refreshListeners = new ConcurrentHashMap<>();

    /**
     * 注册 GUI 刷新监听器
     *
     * @param guiType  GUI 类型标识符
     * @param listener 刷新监听器
     */
    public void registerGUIRefreshListener(String guiType, GUIRefreshListener listener) {
        refreshListeners.put(guiType, listener);
    }

    /**
     * 取消注册 GUI 刷新监听器
     *
     * @param guiType GUI 类型标识符
     */
    public void unregisterGUIRefreshListener(String guiType) {
        refreshListeners.remove(guiType);
    }

    /**
     * 通知 GUI 刷新
     *
     * @param guiType GUI 类型标识符
     * @param data    相关数据
     */
    public void notifyGUIRefresh(String guiType, Map<String, Object> data) {
        GUIRefreshListener listener = refreshListeners.get(guiType);
        if (listener != null) {
            runSync(() -> listener.onGUIRefresh(guiType, data));
        }
    }

    // ==================== 注册追踪清理 ====================

    /**
     * Framework-internal: auto-cleanup all tracked registrations.
     * Called by ModuleManager during module unload, BEFORE module.onDisable().
     */
    public void cleanupTrackedRegistrations() {
        // Unregister all tracked Bukkit listeners
        for (Listener listener : trackedListeners) {
            HandlerList.unregisterAll(listener);
        }
        trackedListeners.clear();

        // Cancel all tracked scheduled tasks
        for (ScheduledTaskHandle handle : trackedTasks) {
            try { handle.cancel(); } catch (Exception ignored) {}
        }
        trackedTasks.clear();
    }
}
