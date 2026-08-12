package com.guild.core.utils;

import com.guild.GuildPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * 详细后台日志开关（对应 {@code config.yml → debug.enabled}，
 * 可在 SystemSettingsGUI「详细后台信息显示」切换）。
 */
public final class DebugLog {

    private DebugLog() {
    }

    public static boolean isEnabled() {
        try {
            GuildPlugin plugin = GuildPlugin.getInstance();
            if (plugin == null || plugin.getConfigManager() == null) {
                return false;
            }
            FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
            return cfg != null && cfg.getBoolean("debug.enabled", false);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void info(Logger logger, String message) {
        if (isEnabled() && logger != null) {
            logger.info(message);
        }
    }

    public static void warning(Logger logger, String message) {
        if (isEnabled() && logger != null) {
            logger.warning(message);
        }
    }
}
