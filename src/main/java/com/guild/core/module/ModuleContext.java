package com.guild.core.module;

import com.guild.GuildPlugin;
import com.guild.core.ServiceContainer;
import com.guild.core.events.EventBus;
import com.guild.core.gui.GUI;
import com.guild.core.gui.GUIManager;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.config.ModuleConfigSection;
import org.bukkit.entity.Player;

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

    // ==================== 日志 ====================

    /** 获取模块专用 Logger */
    public Logger getLogger() { return logger; }

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
        return plugin.getLanguageManager().getIndexedMessage(key, fallback, strArgs);
    }

    // ==================== 线程调度 ====================

    /** 在服务器主线程调度任务 */
    public void runSync(Runnable task) {
        CompatibleScheduler.runTask(plugin, task);
    }

    /** 异步调度任务 */
    public void runAsync(Runnable task) {
        CompatibleScheduler.runTaskAsync(plugin, task);
    }

    /** 延迟调度任务（主线程） */
    public void runLater(long delayTicks, Runnable task) {
        CompatibleScheduler.runTaskLater(plugin, task, delayTicks);
    }

    /** 周期性调度任务（主线程） */
    public void runTimer(long delayTicks, long periodTicks, Runnable task) {
        CompatibleScheduler.runTaskTimer(plugin, task, delayTicks, periodTicks);
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
}
