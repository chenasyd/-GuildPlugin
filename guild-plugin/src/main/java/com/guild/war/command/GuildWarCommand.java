package com.guild.war.command;

import com.guild.GuildPlugin;
import com.guild.war.GuildWarService;
import com.guild.war.command.handler.GuildWarCommandContext;
import com.guild.war.command.handler.WarAdminHandler;
import com.guild.war.command.handler.WarChallengeHandler;
import com.guild.war.command.handler.WarCommandTabCompleter;
import com.guild.war.command.handler.WarExportHandler;
import com.guild.war.command.handler.WarHelpHandler;
import com.guild.war.command.handler.WarPlayerActionHandler;
import com.guild.war.command.handler.WarReportHandler;
import com.guild.war.command.handler.WarSeasonHandler;
import com.guild.war.command.handler.WarStatusHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Locale;

/**
 * /guildwar — 小型固定地图公会战（路由 + Tab 补全；子命令见 {@code com.guild.war.command.handler}）。
 */
public final class GuildWarCommand implements CommandExecutor, TabCompleter {

    private final GuildWarCommandContext context;
    private final TabCompleter tabCompleter;

    private final WarChallengeHandler challengeHandler = new WarChallengeHandler();
    private final WarPlayerActionHandler acceptHandler =
            new WarPlayerActionHandler(WarPlayerActionHandler.Action.ACCEPT);
    private final WarPlayerActionHandler denyHandler =
            new WarPlayerActionHandler(WarPlayerActionHandler.Action.DENY);
    private final WarPlayerActionHandler joinHandler =
            new WarPlayerActionHandler(WarPlayerActionHandler.Action.JOIN);
    private final WarPlayerActionHandler leaveHandler =
            new WarPlayerActionHandler(WarPlayerActionHandler.Action.LEAVE);
    private final WarPlayerActionHandler readyHandler =
            new WarPlayerActionHandler(WarPlayerActionHandler.Action.READY);
    private final WarPlayerActionHandler cancelHandler =
            new WarPlayerActionHandler(WarPlayerActionHandler.Action.CANCEL);
    private final WarStatusHandler statusHandler = new WarStatusHandler();
    private final WarReportHandler reportHandler = new WarReportHandler();
    private final WarExportHandler exportHandler = new WarExportHandler();
    private final WarSeasonHandler seasonHandler = new WarSeasonHandler();
    private final WarAdminHandler adminHandler = new WarAdminHandler();
    private final WarHelpHandler helpHandler = new WarHelpHandler();

    public GuildWarCommand(GuildPlugin plugin, GuildWarService war) {
        this.context = new GuildWarCommandContext(plugin, war);
        this.tabCompleter = new WarCommandTabCompleter(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(GuildWarCommandContext.PERM)) {
            context.send(sender, "war.no-permission", "&c你没有权限使用公会战");
            return true;
        }
        if (!context.war().isEnabled()) {
            context.send(sender, "war.unavailable", "&c公会战不可用: {reason}",
                    "{reason}", context.war().unavailableReason());
            return true;
        }
        if (args.length == 0) {
            helpHandler.handle(context, sender, args);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "challenge", "c" -> challengeHandler.handle(context, sender, args);
            case "accept", "a" -> acceptHandler.handle(context, sender, args);
            case "deny", "d" -> denyHandler.handle(context, sender, args);
            case "join", "j" -> joinHandler.handle(context, sender, args);
            case "leave", "l" -> leaveHandler.handle(context, sender, args);
            case "ready", "r" -> readyHandler.handle(context, sender, args);
            case "cancel" -> cancelHandler.handle(context, sender, args);
            case "status", "s" -> statusHandler.handle(context, sender, args);
            case "report" -> reportHandler.handle(context, sender, args);
            case "export" -> exportHandler.handle(context, sender, args);
            case "season" -> seasonHandler.handle(context, sender, args);
            case "admin" -> adminHandler.handle(context, sender, args);
            case "help", "?" -> helpHandler.handle(context, sender, args);
            default -> helpHandler.handle(context, sender, args);
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return tabCompleter.onTabComplete(sender, command, alias, args);
    }
}
