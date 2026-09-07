package com.guild.world.command.handler;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.language.LocalizedException;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.world.GuildWorldService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /guildworld 子命令共享依赖与消息发送。
 */
public final class GuildWorldCommandContext {

    private final GuildPlugin plugin;
    private final GuildWorldService worldService;

    public GuildWorldCommandContext(GuildPlugin plugin, GuildWorldService worldService) {
        this.plugin = plugin;
        this.worldService = worldService;
    }

    public GuildPlugin plugin() {
        return plugin;
    }

    public GuildWorldService worldService() {
        return worldService;
    }

    public String prefix(CommandSender sender) {
        return CoreMsg.raw(plugin, sender, "world.prefix", "&6[GuildWorld] &r");
    }

    public String t(CommandSender sender, String path, String def, String... ph) {
        return CoreMsg.raw(plugin, sender, path, def, ph);
    }

    public void sendPrefixed(CommandSender sender, String path, String def, String... ph) {
        sendMessage(sender, prefix(sender) + t(sender, path, def, ph));
    }

    public void sendPlain(CommandSender sender, String path, String def, String... ph) {
        sendMessage(sender, t(sender, path, def, ph));
    }

    public String resolveError(CommandSender sender, Throwable ex) {
        return LocalizedException.resolveThrowable(plugin, sender, ex);
    }

    public void teleportPlayer(Player player, String worldInput) {
        String name = worldService.buildWorldName(worldInput);
        sendPrefixed(player, "world.tp.working", "&e正在传送到 &f{world} &e...", "{world}", name);
        worldService.teleportToWorld(player, name).thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                sendPrefixed(player, "world.tp.success", "&a已传送到 &f{world}", "{world}", name);
            } else {
                sendPrefixed(player, "world.tp.failed", "&c传送失败。");
            }
        }).exceptionally(ex -> {
            sendPrefixed(player, "world.tp.failed-error", "&c传送失败: {error}",
                    "{error}", resolveError(player, ex));
            return null;
        });
    }

    /** Folia 安全的消息发送（玩家在其区域线程接收）。 */
    public void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            CompatibleScheduler.runTask(plugin, player, () -> player.sendMessage(ColorUtils.colorize(message)));
        } else {
            sender.sendMessage(ColorUtils.colorize(message));
        }
    }
}
