package com.guild.world.command;

import com.guild.GuildPlugin;
import com.guild.core.utils.ServerUtils;
import com.guild.world.GuildWorldService;
import com.guild.world.command.handler.GuildWorldCommandContext;
import com.guild.world.command.handler.WorldCreateHandler;
import com.guild.world.command.handler.WorldDeleteHandler;
import com.guild.world.command.handler.WorldEditHandler;
import com.guild.world.command.handler.WorldHelpHandler;
import com.guild.world.command.handler.WorldInfoHandler;
import com.guild.world.command.handler.WorldListHandler;
import com.guild.world.command.handler.WorldLoadHandler;
import com.guild.world.command.handler.WorldPresetHandler;
import com.guild.world.command.handler.WorldRestoreHandler;
import com.guild.world.command.handler.WorldTpHandler;
import com.guild.world.command.handler.WorldUnloadHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 多世界管理命令：/guildworld（路由 + Tab 补全；子命令见 {@code com.guild.world.command.handler}）。
 *
 * <p>权限：{@code guild.admin.world}
 */
public final class GuildWorldCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "guild.admin.world";

    private final GuildPlugin plugin;
    private final GuildWorldCommandContext context;
    private final TabCompleter tabCompleter;

    private final WorldCreateHandler createHandler = new WorldCreateHandler();
    private final WorldListHandler listHandler = new WorldListHandler();
    private final WorldInfoHandler infoHandler = new WorldInfoHandler();
    private final WorldLoadHandler loadHandler = new WorldLoadHandler();
    private final WorldUnloadHandler unloadHandler = new WorldUnloadHandler();
    private final WorldDeleteHandler deleteHandler = new WorldDeleteHandler();
    private final WorldRestoreHandler restoreHandler = new WorldRestoreHandler();
    private final WorldTpHandler tpHandler = new WorldTpHandler();
    private final WorldEditHandler editHandler = new WorldEditHandler();
    private final WorldPresetHandler presetHandler = new WorldPresetHandler();
    private final WorldHelpHandler helpHandler = new WorldHelpHandler();

    public GuildWorldCommand(GuildPlugin plugin, GuildWorldService worldService) {
        this.plugin = plugin;
        this.context = new GuildWorldCommandContext(plugin, worldService);
        this.tabCompleter = new WorldCommandTabCompleter(worldService);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            context.sendPrefixed(sender, "world.no-permission", "&c您没有权限执行此操作！");
            return true;
        }

        if (plugin.getFileLogger() != null) {
            String sourceName = (sender instanceof Player) ? ((Player) sender).getName() : "Console";
            plugin.getFileLogger().logAdmin(sourceName, "/" + command.getName() + " " + String.join(" ", args));
        }

        if (args.length == 0) {
            helpHandler.handle(context, sender, args);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (!context.worldService().isEnabled() && isMutatingSubcommand(sub)) {
            context.sendPrefixed(sender, "world.disabled.folia-unsupported",
                    "&c当前 Folia 版本 ({version}) 不支持 gworld",
                    "{version}", ServerUtils.getMinecraftVersion());
            return true;
        }

        switch (sub) {
            case "create" -> createHandler.handle(context, sender, args);
            case "list" -> listHandler.handle(context, sender, args);
            case "info" -> infoHandler.handle(context, sender, args);
            case "load" -> loadHandler.handle(context, sender, args);
            case "unload" -> unloadHandler.handle(context, sender, args);
            case "delete" -> deleteHandler.handle(context, sender, args);
            case "restore" -> restoreHandler.handle(context, sender, args);
            case "tp", "goto", "enter" -> tpHandler.handle(context, sender, args);
            case "edit" -> editHandler.handle(context, sender, args);
            case "preset" -> presetHandler.handle(context, sender, args);
            case "help" -> helpHandler.handle(context, sender, args);
            default -> context.sendPrefixed(sender, "world.unknown-subcommand",
                    "&c未知子命令！使用 /guildworld help 查看帮助。");
        }
        return true;
    }

    private static boolean isMutatingSubcommand(String sub) {
        return switch (sub) {
            case "create", "load", "unload", "delete", "restore", "tp", "goto", "enter", "edit", "preset" -> true;
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return tabCompleter.onTabComplete(sender, command, alias, args);
    }
}
