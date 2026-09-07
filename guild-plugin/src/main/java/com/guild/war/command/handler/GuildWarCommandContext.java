package com.guild.war.command.handler;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.language.LocalizedException;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.war.GuildWarService;
import com.guild.war.model.WarPhase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /guildwar 子命令共享依赖与消息发送。
 */
public final class GuildWarCommandContext {

    public static final String PERM = "guild.war";
    public static final String PERM_ADMIN = "guild.war.admin";

    private final GuildPlugin plugin;
    private final GuildWarService war;

    public GuildWarCommandContext(GuildPlugin plugin, GuildWarService war) {
        this.plugin = plugin;
        this.war = war;
    }

    public GuildPlugin plugin() {
        return plugin;
    }

    public GuildWarService war() {
        return war;
    }

    public void send(CommandSender sender, String path, String def, String... ph) {
        String prefix = CoreMsg.raw(plugin, sender, "war.prefix", "&c[公会战] &r");
        String body = CoreMsg.raw(plugin, sender, path, def, ph);
        sender.sendMessage(ColorUtils.colorize(prefix + body));
    }

    public void reply(Player player, Throwable err, String okPath, String okDef) {
        if (err != null) {
            String body = LocalizedException.resolveThrowable(plugin, player, err);
            String prefix = CoreMsg.raw(plugin, player, "war.prefix", "&c[公会战] &r");
            player.sendMessage(ColorUtils.colorize(prefix + body));
        } else {
            send(player, okPath, okDef);
        }
    }

    public void requirePlayer(CommandSender sender, PlayerAction action) {
        if (!(sender instanceof Player player)) {
            send(sender, "war.player-only", "&c仅玩家可用");
            return;
        }
        action.run(player);
    }

    public String phaseName(CommandSender sender, WarPhase phase) {
        return switch (phase) {
            case PENDING -> CoreMsg.raw(plugin, sender, "war.phase.pending", "等待接受");
            case SIGNUP -> CoreMsg.raw(plugin, sender, "war.phase.signup", "报名中");
            case PREPARING -> CoreMsg.raw(plugin, sender, "war.phase.preparing", "准备中");
            case COUNTDOWN -> CoreMsg.raw(plugin, sender, "war.phase.countdown", "倒计时");
            case ACTIVE -> CoreMsg.raw(plugin, sender, "war.phase.active", "激战中");
            case ENDED -> CoreMsg.raw(plugin, sender, "war.phase.ended", "已结束");
        };
    }

    public void runOnSenderThread(CommandSender sender, Runnable action) {
        if (sender instanceof Player player) {
            CompatibleScheduler.runTask(plugin, player, action);
        } else {
            action.run();
        }
    }

    public static String flag(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(name)) {
                return args[i + 1];
            }
        }
        return null;
    }

    public static Integer intFlag(String[] args, String name) {
        String v = flag(args, name);
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FunctionalInterface
    public interface PlayerAction {
        void run(Player player);
    }
}
