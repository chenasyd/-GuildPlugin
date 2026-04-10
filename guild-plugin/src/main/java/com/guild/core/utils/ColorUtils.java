package com.guild.core.utils;

import org.bukkit.ChatColor;

/**
 * 颜色处理工具类
 */
public class ColorUtils {
    
    /**
     * 转换颜色符号
     * @param message 原始消息
     * @return 转换后的消息
     */
    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 转换颜色符号并替换占位符
     * @param message 原始消息
     * @param placeholders 占位符映射
     * @return 转换后的消息
     */
    public static String colorize(String message, String... placeholders) {
        if (message == null) {
            return "";
        }
        
        String result = message;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                result = result.replace(placeholder, value != null ? value : "");
            }
        }
        
        return colorize(result);
    }
    
    /**
     * 移除颜色符号
     * @param message 原始消息
     * @return 移除颜色符号后的消息
     */
    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(colorize(message));
    }
}
