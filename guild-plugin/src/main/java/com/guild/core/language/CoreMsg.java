package com.guild.core.language;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 核心语言包快捷读取（world / war 等模块共用）。
 */
public final class CoreMsg {

    private CoreMsg() {
    }

    public static String raw(GuildPlugin plugin, CommandSender sender, String path, String def, String... placeholders) {
        if (plugin == null || plugin.getLanguageManager() == null) {
            return apply(def, placeholders);
        }
        LanguageManager lm = plugin.getLanguageManager();
        if (sender instanceof Player player) {
            return lm.getCoreMessage(player, path, def, placeholders);
        }
        return lm.getCoreMessage(lm.getDefaultLanguage(), path, def, placeholders);
    }

    public static String colored(GuildPlugin plugin, CommandSender sender, String path, String def, String... placeholders) {
        return ColorUtils.colorize(raw(plugin, sender, path, def, placeholders));
    }

    public static String rawDefault(GuildPlugin plugin, String path, String def, String... placeholders) {
        if (plugin == null || plugin.getLanguageManager() == null) {
            return apply(def, placeholders);
        }
        LanguageManager lm = plugin.getLanguageManager();
        return lm.getCoreMessage(lm.getDefaultLanguage(), path, def, placeholders);
    }

    public static String coloredDefault(GuildPlugin plugin, String path, String def, String... placeholders) {
        return ColorUtils.colorize(rawDefault(plugin, path, def, placeholders));
    }

    /** 若 text 是 lang key（以 world./war. 开头），则翻译；否则原样返回。 */
    public static String resolveKeyOrText(GuildPlugin plugin, CommandSender sender, String text) {
        if (text == null) {
            return "";
        }
        if (text.startsWith("world.") || text.startsWith("war.")) {
            return raw(plugin, sender, text, text);
        }
        return text;
    }

    private static String apply(String message, String... placeholders) {
        if (message == null) {
            return "";
        }
        String out = message;
        if (placeholders != null) {
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                String k = placeholders[i];
                String v = placeholders[i + 1];
                out = out.replace(k, v != null ? v : "");
            }
        }
        return out;
    }
}
