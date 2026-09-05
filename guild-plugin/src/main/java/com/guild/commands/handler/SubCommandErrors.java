package com.guild.commands.handler;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
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

    /** 在 CompletableFuture 回调中包裹业务逻辑，失败时结构化日志 + 通知执行者。 */
    public static void guardAsync(GuildPlugin plugin, LanguageManager languageManager, CommandSender sender,
                                  String operation, String messageKey, String fallback, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            logAndNotifySender(plugin, languageManager, sender, operation, e, messageKey, fallback);
        }
    }

    public static <T> Function<Throwable, T> handleAsyncFailure(GuildPlugin plugin, LanguageManager languageManager,
                                                                CommandSender sender, String operation,
                                                                String messageKey, String fallback) {
        return throwable -> {
            Throwable cause = throwable;
            if (throwable instanceof CompletionException && throwable.getCause() != null) {
                cause = throwable.getCause();
            }
            Exception e = cause instanceof Exception ex ? ex : new Exception(String.valueOf(cause), cause);
            logAndNotifySender(plugin, languageManager, sender, operation, e, messageKey, fallback);
            return null;
        };
    }

    /** 在 CompletableFuture 链末尾挂载玩家侧失败处理。 */
    public static <T> CompletableFuture<T> guardPlayerFuture(GuildPlugin plugin, LanguageManager languageManager,
                                                             Player player, String operation,
                                                             String messageKey, String fallback,
                                                             CompletableFuture<T> future) {
        return future.exceptionally(handleAsyncFailure(plugin, languageManager, player, operation, messageKey, fallback));
    }

    /** 在后台线程执行业务逻辑，同步异常时结构化日志 + 通知玩家。 */
    public static void runPlayerAsync(GuildPlugin plugin, LanguageManager languageManager, Player player,
                                      String operation, String messageKey, String fallback, Runnable action) {
        CompletableFuture.runAsync(() ->
                guardAsync(plugin, languageManager, player, operation, messageKey, fallback, action));
    }
}
