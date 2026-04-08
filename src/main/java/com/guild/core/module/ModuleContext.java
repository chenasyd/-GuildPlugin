package com.guild.core.module;

import com.guild.GuildPlugin;
import com.guild.core.ServiceContainer;
import com.guild.core.events.EventBus;
import com.guild.core.gui.GUIManager;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.config.ModuleConfigSection;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
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

    public ModuleContext(GuildPlugin plugin, ModuleDescriptor descriptor) {
        this.plugin = plugin;
        this.descriptor = descriptor;
        this.api = new GuildPluginAPI(plugin);
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
     * @param args   占位符参数（键值对交替形式）
     */
    public void sendMessage(Player player, String key, Object... args) {
        String message = formatMessage(key, args);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    /**
     * 获取本地化消息文本（不直接发送）
     */
    public String getMessage(String key, Object... args) {
        return formatMessage(key, args);
    }

    /**
     * 格式化消息
     */
    private String formatMessage(String key, Object[] args) {
        if (args == null || args.length == 0) {
            return plugin.getLanguageManager().getMessage(key, "");
        }
        // 逐个展开为 varargs
        String a0 = args.length > 0 ? (args[0] != null ? args[0].toString() : "null") : "";
        String a1 = args.length > 1 ? (args[1] != null ? args[1].toString() : "null") : "";
        String a2 = args.length > 2 ? (args[2] != null ? args[2].toString() : "null") : "";
        String a3 = args.length > 3 ? (args[3] != null ? args[3].toString() : "null") : "";
        String a4 = args.length > 4 ? (args[4] != null ? args[4].toString() : "null") : "";

        switch (args.length) {
            case 1: return plugin.getLanguageManager().getMessage(key, "", a0);
            case 2: return plugin.getLanguageManager().getMessage(key, "", a0, a1);
            case 3: return plugin.getLanguageManager().getMessage(key, "", a0, a1, a2);
            case 4: return plugin.getLanguageManager().getMessage(key, "", a0, a1, a2, a3);
            default: return plugin.getLanguageManager().getMessage(key, "", a0, a1, a2, a3, a4);
        }
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
}
