package com.guild.core.language;

import com.guild.GuildPlugin;
import org.bukkit.command.CommandSender;

/** 携带 lang key 的业务异常，便于按玩家语言解析。 */
public final class LocalizedException extends RuntimeException {

    private final String key;
    private final String defaultMessage;
    private final String[] placeholders;

    public LocalizedException(String key, String defaultMessage, String... placeholders) {
        super(key);
        this.key = key;
        this.defaultMessage = defaultMessage != null ? defaultMessage : key;
        this.placeholders = placeholders != null ? placeholders : new String[0];
    }

    public String key() {
        return key;
    }

    public String resolve(GuildPlugin plugin, CommandSender sender) {
        return CoreMsg.raw(plugin, sender, key, defaultMessage, placeholders);
    }

    public String resolveDefault(GuildPlugin plugin) {
        return CoreMsg.rawDefault(plugin, key, defaultMessage, placeholders);
    }

    public static Throwable unwrap(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            if (c instanceof java.util.concurrent.CompletionException
                    || c instanceof java.util.concurrent.ExecutionException) {
                c = c.getCause();
            } else {
                break;
            }
        }
        return c;
    }

    public static String resolveThrowable(GuildPlugin plugin, CommandSender sender, Throwable t) {
        Throwable root = unwrap(t);
        if (root instanceof LocalizedException le) {
            return le.resolve(plugin, sender);
        }
        String msg = root.getMessage();
        if (msg != null && (msg.startsWith("world.") || msg.startsWith("war."))) {
            return CoreMsg.raw(plugin, sender, msg, msg);
        }
        return msg != null && !msg.isEmpty() ? msg : root.getClass().getSimpleName();
    }
}
