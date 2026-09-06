package com.guild.core.language;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * 语言消息查词与占位符替换（成对 key/value 与 {@code {0}} 索引占位符）。
 */
public final class MessageResolver {

    private MessageResolver() {
    }

    public static String resolve(FileConfiguration config, String path, String defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        String message = config.getString(path, defaultValue);
        return message != null ? message : defaultValue;
    }

    public static String withPlaceholders(String message, String... placeholders) {
        if (message == null) {
            return "";
        }
        String out = message;
        if (placeholders != null) {
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                out = out.replace(placeholder, value != null ? value : "");
            }
        }
        return out;
    }

    public static String withIndexedArgs(String message, String... args) {
        if (message == null) {
            return "";
        }
        if (args == null) {
            return message;
        }
        String out = message;
        for (int i = 0; i < args.length; i++) {
            out = out.replace("{" + i + "}", args[i] != null ? args[i] : "");
        }
        return out;
    }

    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        return message.replace("&", "\u00a7");
    }

    public static String resolveWithPlaceholders(FileConfiguration config, String path, String defaultValue,
                                                 String... placeholders) {
        return withPlaceholders(resolve(config, path, defaultValue), placeholders);
    }

    public static String resolveWithIndexedArgs(FileConfiguration config, String path, String defaultValue,
                                                String... args) {
        return withIndexedArgs(resolve(config, path, defaultValue), args);
    }
}
