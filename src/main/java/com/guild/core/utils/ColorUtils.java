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

    /**
     * 在替换占位符时隔离被替换内容的颜色，避免其影响后续文本。
     * 做法：在占位符位置插入 content + &r + activeColor（activeColor 为占位符前的最近颜色码）。
     * 传入的 template 使用 & 颜色码；返回结果已进行颜色转换。
     */
    public static String replaceWithColorIsolation(String template, String placeholder, String content) {
        if (template == null) {
            return "";
        }
        if (placeholder == null || placeholder.isEmpty()) {
            return colorize(template);
        }
        int idx = template.indexOf(placeholder);
        if (idx < 0) {
            return colorize(template);
        }
        String prefix = template.substring(0, idx);
        String suffix = template.substring(idx + placeholder.length());

        // 计算占位符前的有效颜色码（最后一次出现的 &0-&f 或 &a-&f 等颜色代码，忽略样式码）
        String activeColor = extractLastColorCode(prefix);
        String injected = (content != null ? content : "") + "&r" + (activeColor != null ? activeColor : "");
        String result = prefix + injected + suffix;
        return colorize(result);
    }

    private static String extractLastColorCode(String text) {
        if (text == null || text.isEmpty()) return null;
        String last = null;
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            char n = text.charAt(i + 1);
            if (c == '&') {
                char lower = Character.toLowerCase(n);
                if (lower == 'r') {
                    last = null; // 重置
                } else if ((lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f')) {
                    last = "&" + lower; // 记录最后颜色码
                }
            }
        }
        return last;
    }
}
