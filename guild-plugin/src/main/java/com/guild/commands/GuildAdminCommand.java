package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.commands.handler.admin.GuildAdminCommandContext;
import com.guild.commands.handler.admin.GuildAdminDeleteHandler;
import com.guild.commands.handler.admin.GuildAdminEconomyHandler;
import com.guild.commands.handler.admin.GuildAdminFreezeHandler;
import com.guild.commands.handler.admin.GuildAdminHelpHandler;
import com.guild.commands.handler.admin.GuildAdminInfoHandler;
import com.guild.commands.handler.admin.GuildAdminListHandler;
import com.guild.commands.handler.admin.GuildAdminRelationHandler;
import com.guild.commands.handler.admin.GuildAdminReloadHandler;
import com.guild.commands.handler.admin.GuildAdminTestHandler;
import com.guild.commands.handler.admin.GuildAdminTransferHandler;
import com.guild.commands.handler.admin.GuildAdminUnfreezeHandler;
import com.guild.commands.handler.admin.GuildAdminUpdateHandler;
import com.guild.core.language.LanguageManager;
import com.guild.core.module.ModuleManager;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.AdminGuildGUI;
import com.guild.models.Guild;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 公会管理员命令（路由 + Tab 补全）；子命令逻辑见 {@code com.guild.commands.handler.admin}。
 */
public class GuildAdminCommand implements CommandExecutor, TabCompleter {

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final GuildAdminCommandContext commandContext;

    private final GuildAdminListHandler listHandler = new GuildAdminListHandler();
    private final GuildAdminInfoHandler infoHandler = new GuildAdminInfoHandler();
    private final GuildAdminDeleteHandler deleteHandler = new GuildAdminDeleteHandler();
    private final GuildAdminFreezeHandler freezeHandler = new GuildAdminFreezeHandler();
    private final GuildAdminUnfreezeHandler unfreezeHandler = new GuildAdminUnfreezeHandler();
    private final GuildAdminTransferHandler transferHandler = new GuildAdminTransferHandler();
    private final GuildAdminEconomyHandler economyHandler = new GuildAdminEconomyHandler();
    private final GuildAdminRelationHandler relationHandler = new GuildAdminRelationHandler();
    private final GuildAdminReloadHandler reloadHandler = new GuildAdminReloadHandler();
    private final GuildAdminTestHandler testHandler = new GuildAdminTestHandler();
    private final GuildAdminUpdateHandler updateHandler = new GuildAdminUpdateHandler();
    private final GuildAdminHelpHandler helpHandler = new GuildAdminHelpHandler();

    public GuildAdminCommand(GuildPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.commandContext = new GuildAdminCommandContext(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guild.admin")) {
            String msg = languageManager.getCoreMessage("general.no-permission", "&cYou do not have permission to perform this action!");
            sender.sendMessage(ColorUtils.colorize(msg));
            return true;
        }

        if (plugin.getFileLogger() != null) {
            String sourceName = (sender instanceof Player) ? ((Player) sender).getName() : "Console";
            plugin.getFileLogger().logAdmin(sourceName,
                    "/" + command.getName() + " " + String.join(" ", args));
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                AdminGuildGUI adminGUI = new AdminGuildGUI(plugin, player);
                plugin.getGuiManager().openGUI(player, adminGUI);
            } else {
                helpHandler.handle(commandContext, sender, args);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> listHandler.handle(commandContext, sender, args);
            case "info" -> infoHandler.handle(commandContext, sender, args);
            case "delete" -> deleteHandler.handle(commandContext, sender, args);
            case "freeze" -> freezeHandler.handle(commandContext, sender, args);
            case "unfreeze" -> unfreezeHandler.handle(commandContext, sender, args);
            case "transfer" -> transferHandler.handle(commandContext, sender, args);
            case "economy" -> economyHandler.handle(commandContext, sender, args);
            case "relation" -> relationHandler.handle(commandContext, sender, args);
            case "reload" -> reloadHandler.handle(commandContext, sender, args);
            case "test" -> testHandler.handle(commandContext, sender, args);
            case "update" -> updateHandler.handle(commandContext, sender, args);
            case "help" -> helpHandler.handle(commandContext, sender, args);
            default -> sender.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(
                    "general.unknown-command", "&cUnknown command! Use /guild help for help.")));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("guild.admin")) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "info", "delete", "freeze", "unfreeze", "transfer", "economy", "relation", "reload", "test", "update", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "info", "delete", "freeze", "unfreeze", "transfer", "economy" -> {
                    plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                        for (Guild guild : guilds) {
                            completions.add(guild.getName());
                        }
                    });
                }
                case "update" -> completions.addAll(Arrays.asList("check", "download"));
                case "relation" -> completions.addAll(Arrays.asList("list", "create", "delete", "gui"));
                case "test" -> completions.addAll(Arrays.asList("gui", "economy", "relation", "lang"));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "transfer" -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
                case "economy" -> completions.addAll(Arrays.asList("set", "add", "remove", "info"));
                case "relation" -> {
                    if ("create".equals(args[1])) {
                        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                            for (Guild guild : guilds) {
                                completions.add(guild.getName());
                            }
                        });
                    }
                }
                case "test" -> {
                    if ("lang".equals(args[1])) {
                        completions.addAll(Arrays.asList(
                                "lookup", "files", "module-context", "force-load", "dump",
                                "button-state", "module-reload"));
                    }
                }
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "relation" -> {
                    if ("create".equals(args[1])) {
                        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                            for (Guild guild : guilds) {
                                completions.add(guild.getName());
                            }
                        });
                    }
                }
                case "test" -> {
                    if ("lang".equals(args[1])) {
                        switch (args[2].toLowerCase()) {
                            case "button-state" -> completions.addAll(Arrays.asList(
                                    "GuildSettingsGUI", "GuildInfoGUI", "MainGuildGUI"));
                            case "module-reload" -> {
                                ModuleManager mm = plugin.getModuleManager();
                                if (mm != null) {
                                    completions.addAll(mm.getRegistry().getModuleIds());
                                }
                            }
                            case "lookup" -> completions.addAll(Arrays.asList(
                                    "announcement", "quest", "stats", "member-rank", "apitest", "testlang"));
                            case "dump" -> completions.addAll(Arrays.asList("en", "zh", "pl", "br"));
                        }
                    }
                }
            }
        } else if (args.length == 5 && "relation".equalsIgnoreCase(args[0])
                && "create".equalsIgnoreCase(args[1])) {
            completions.addAll(Arrays.asList("ally", "enemy", "war", "truce", "neutral"));
        }

        return completions;
    }
}
