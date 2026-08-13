package com.guild.core.utils;

import com.guild.GuildPlugin;

/**
 * 静默日志：写入 {@code plugins/GuildPlugin/logs/}，默认不刷控制台。
 *
 * <p>通过 {@link PluginFileLogger} 落盘，JUL 镜像为 {@code FINE}，
 * 在 Bukkit 默认 INFO 级别下控制台不可见。
 */
public final class QuietLog {

    private QuietLog() {
    }

    /** 系统类运行时细节（加入/离开公会、事件总线等）。 */
    public static void system(String message) {
        write(PluginFileLogger.Category.SYSTEM, "System", message);
    }

    /** 模块运行时细节（任务完成、奖励发放等）。 */
    public static void module(String moduleName, String message) {
        write(PluginFileLogger.Category.MODULE,
                moduleName != null ? moduleName : "Module", message);
    }

    public static void write(PluginFileLogger.Category category, String source, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        try {
            GuildPlugin plugin = GuildPlugin.getInstance();
            if (plugin != null && plugin.getFileLogger() != null) {
                plugin.getFileLogger().log(category, source, message);
                return;
            }
            if (plugin != null) {
                plugin.getLogger().fine("[" + category.name() + "] " + message);
            }
        } catch (Throwable ignored) {
            // Never let logging break gameplay
        }
    }
}
