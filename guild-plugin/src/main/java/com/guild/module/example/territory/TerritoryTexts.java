package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 领地模块文案：经 {@link com.guild.core.language.LanguageManager} 解析，
 * 占位符参数与 fallback 分离（与 {@code QuestTexts} / Announcement GUI 一致）。
 */
public final class TerritoryTexts {

    private final ModuleContext context;

    public TerritoryTexts(ModuleContext context) {
        this.context = context;
    }

    public void send(Player player, String key, String fallback, Object... formatArgs) {
        player.sendMessage(format(player, key, fallback, formatArgs));
    }

    public void send(CommandSender sender, String key, String fallback, Object... formatArgs) {
        sender.sendMessage(format(key, fallback, formatArgs));
    }

    public String format(Player player, String key, String fallback, Object... formatArgs) {
        if (formatArgs == null || formatArgs.length == 0) {
            return ColorUtils.colorize(
                    context.getLanguageManager().getModuleMessage(player, key, fallback));
        }
        return ColorUtils.colorize(
                context.getLanguageManager().getModuleIndexedMessage(
                        player, key, fallback, toStringArgs(formatArgs)));
    }

    public String format(String key, String fallback, Object... formatArgs) {
        if (formatArgs == null || formatArgs.length == 0) {
            return ColorUtils.colorize(
                    context.getLanguageManager().getModuleMessage(key, fallback));
        }
        return ColorUtils.colorize(
                context.getLanguageManager().getModuleIndexedMessage(
                        key, fallback, toStringArgs(formatArgs)));
    }

    private static String[] toStringArgs(Object[] formatArgs) {
        String[] strArgs = new String[formatArgs.length];
        for (int i = 0; i < formatArgs.length; i++) {
            strArgs[i] = formatArgs[i] != null ? formatArgs[i].toString() : "";
        }
        return strArgs;
    }
}
