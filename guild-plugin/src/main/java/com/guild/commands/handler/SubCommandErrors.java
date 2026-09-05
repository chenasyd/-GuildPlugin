package com.guild.commands.handler;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/** 子命令 Handler 异步失败时的结构化日志与用户通知。 */
public final class SubCommandErrors {

    private SubCommandErrors() {
    }

    public static void logFailure(GuildPlugin plugin, String operation, Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Guild sub-command failed: " + operation, e);
    }

    public static void notifyPlayer(GuildPlugin plugin, LanguageManager languageManager,
                                    Player player, String messageKey, String fallback) {
        CompatibleScheduler.runTask(plugin, player, () -> {
            String message = languageManager.getCoreMessage(player, messageKey, fallback);
            player.sendMessage(ColorUtils.colorize(message));
        });
    }

    public static void logAndNotifyPlayer(GuildPlugin plugin, LanguageManager languageManager,
                                          Player player, String operation, Exception e,
                                          String messageKey, String fallback) {
        logFailure(plugin, operation, e);
        notifyPlayer(plugin, languageManager, player, messageKey, fallback);
    }

    public static void logAndNotifySender(GuildPlugin plugin, LanguageManager languageManager,
                                          CommandSender sender, String operation, Exception e,
                                          String messageKey, String fallback) {
        logFailure(plugin, operation, e);
        if (sender instanceof Player player) {
            notifyPlayer(plugin, languageManager, player, messageKey, fallback);
        } else {
            sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(messageKey, fallback)));
        }
    }
}
