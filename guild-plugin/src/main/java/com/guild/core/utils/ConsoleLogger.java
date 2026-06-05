package com.guild.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

/**
 * 控制台彩色日志工具
 * <p>
 * 通过 {@link ConsoleCommandSender#sendMessage} 输出带颜色的控制台消息，
 * 支持 {@code &} 颜色代码（与 ColorUtils.colorize 一致）。
 * <p>
 * 使用方式：
 * <pre>
 * ConsoleLogger.info("&a[模块系统] 加载成功");
 * ConsoleLogger.warn("&e[模块系统] 警告信息");
 * ConsoleLogger.severe("&c[模块系统] 严重错误");
 * </pre>
 */
public class ConsoleLogger {

    private static ConsoleCommandSender CONSOLE;

    static {
        try {
            CONSOLE = Bukkit.getConsoleSender();
        } catch (Exception ignored) {
        }
    }

    /**
     * 输出 INFO 级别彩色消息
     */
    public static void info(String message) {
        if (CONSOLE != null && message != null) {
            CONSOLE.sendMessage(ColorUtils.colorize(message));
        }
    }

    /**
     * 输出 WARN 级别彩色消息
     */
    public static void warn(String message) {
        if (CONSOLE != null && message != null) {
            CONSOLE.sendMessage(ColorUtils.colorize(message));
        }
    }

    /**
     * 输出 SEVERE 级别彩色消息
     */
    public static void severe(String message) {
        if (CONSOLE != null && message != null) {
            CONSOLE.sendMessage(ColorUtils.colorize(message));
        }
    }

    /**
     * 先替换占位符再输出彩色消息
     *
     * @param message 消息模板，例如 "&a[模块] 加载 {0} 成功"
     * @param args    占位符参数，按索引替换 {0}, {1}, {2} ...
     */
    public static void info(String message, String... args) {
        info(replaceIndexed(message, args));
    }

    public static void warn(String message, String... args) {
        warn(replaceIndexed(message, args));
    }

    public static void severe(String message, String... args) {
        severe(replaceIndexed(message, args));
    }

    private static String replaceIndexed(String message, String... args) {
        if (message == null) return "";
        if (args == null) return message;
        String result = message;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", args[i] != null ? args[i] : "");
        }
        return result;
    }
}
