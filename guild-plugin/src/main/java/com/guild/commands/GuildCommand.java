package com.guild.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.commands.handler.GuildAcceptHandler;
import com.guild.commands.handler.GuildApplicationsHandler;
import com.guild.commands.handler.GuildChatHandler;
import com.guild.commands.handler.GuildCommandContext;
import com.guild.commands.handler.GuildCreateHandler;
import com.guild.commands.handler.GuildDeclineHandler;
import com.guild.commands.handler.GuildDeleteHandler;
import com.guild.commands.handler.GuildDemoteHandler;
import com.guild.commands.handler.GuildEconomyHandler;
import com.guild.commands.handler.GuildHelpHandler;
import com.guild.commands.handler.GuildHomeHandler;
import com.guild.commands.handler.GuildInfoHandler;
import com.guild.commands.handler.GuildInviteHandler;
import com.guild.commands.handler.GuildKickHandler;
import com.guild.commands.handler.GuildLeaveHandler;
import com.guild.commands.handler.GuildLogsHandler;
import com.guild.commands.handler.GuildMembersHandler;
import com.guild.commands.handler.GuildPlaceholderHandler;
import com.guild.commands.handler.GuildPromoteHandler;
import com.guild.commands.handler.GuildRelationHandler;
import com.guild.commands.handler.GuildTimeHandler;
import com.guild.commands.handler.GuildWarehouseHandler;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.MainGuildGUI;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.command.ModuleCommandHandler;

/**
 * 公会主命令（路由 + Tab 补全）；子命令逻辑见 {@code com.guild.commands.handler}。
 */
public class GuildCommand implements CommandExecutor, TabCompleter {

    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final GuildPluginAPI api;
    private final GuildCommandContext commandContext;
    private final GuildCreateHandler createHandler = new GuildCreateHandler();
    private final GuildInfoHandler infoHandler = new GuildInfoHandler();
    private final GuildMembersHandler membersHandler = new GuildMembersHandler();
    private final GuildInviteHandler inviteHandler = new GuildInviteHandler();
    private final GuildKickHandler kickHandler = new GuildKickHandler();
    private final GuildPromoteHandler promoteHandler = new GuildPromoteHandler();
    private final GuildDemoteHandler demoteHandler = new GuildDemoteHandler();
    private final GuildAcceptHandler acceptHandler = new GuildAcceptHandler();
    private final GuildDeclineHandler declineHandler = new GuildDeclineHandler();
    private final GuildLeaveHandler leaveHandler = new GuildLeaveHandler();
    private final GuildDeleteHandler deleteHandler = new GuildDeleteHandler();
    private final GuildHomeHandler homeHandler = new GuildHomeHandler();
    private final GuildRelationHandler relationHandler = new GuildRelationHandler();
    private final GuildEconomyHandler economyHandler = new GuildEconomyHandler();
    private final GuildLogsHandler logsHandler = new GuildLogsHandler();
    private final GuildPlaceholderHandler placeholderHandler = new GuildPlaceholderHandler();
    private final GuildTimeHandler timeHandler = new GuildTimeHandler();
    private final GuildApplicationsHandler applicationsHandler = new GuildApplicationsHandler();
    private final GuildChatHandler chatHandler = new GuildChatHandler();
    private final GuildHelpHandler helpHandler = new GuildHelpHandler();
    private final GuildWarehouseHandler warehouseHandler = new GuildWarehouseHandler();

    public GuildCommand(GuildPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.api = plugin.getServiceContainer().get(com.guild.core.module.ModuleManager.class).getSharedApi();
        this.commandContext = new GuildCommandContext(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            String msg = languageManager.getCoreMessage("general.player-only", "&cThis command can only be executed by a player!");
            sender.sendMessage(ColorUtils.colorize(msg));
            return true;
        }

        // 记录玩家指令到文件日志
        if (plugin.getFileLogger() != null) {
            plugin.getFileLogger().logCommand(player.getName(),
                "/" + command.getName() + " " + String.join(" ", args));
        }

        if (args.length == 0) {
            // 打开主GUI
            MainGuildGUI mainGuildGUI = new MainGuildGUI(plugin, player);
            plugin.getGuiManager().openGUI(player, mainGuildGUI);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                createHandler.handle(commandContext, player, args);
                break;
            case "info":
                infoHandler.handle(commandContext, player, args);
                break;
            case "members":
                membersHandler.handle(commandContext, player, args);
                break;
            case "invite":
                inviteHandler.handle(commandContext, player, args);
                break;
            case "kick":
                kickHandler.handle(commandContext, player, args);
                break;
            case "promote":
                promoteHandler.handle(commandContext, player, args);
                break;
            case "demote":
                demoteHandler.handle(commandContext, player, args);
                break;
            case "accept":
                acceptHandler.handle(commandContext, player, args);
                break;
            case "decline":
                declineHandler.handle(commandContext, player, args);
                break;
            case "leave":
                leaveHandler.handle(commandContext, player, args);
                break;
            case "delete":
                deleteHandler.handle(commandContext, player, args);
                break;
            case "sethome":
            case "home":
                homeHandler.handle(commandContext, player, args);
                break;
            case "relation":
                relationHandler.handle(commandContext, player, args);
                break;
            case "economy":
            case "deposit":
            case "withdraw":
            case "transfer":
                economyHandler.handle(commandContext, player, args);
                break;
            case "logs":
                logsHandler.handle(commandContext, player, args);
                break;
            case "placeholder":
                placeholderHandler.handle(commandContext, player, args);
                break;
            case "time":
                timeHandler.handle(commandContext, player, args);
                break;
            case "help":
                helpHandler.handle(commandContext, player, args);
                break;
            case "applications":
                applicationsHandler.handle(commandContext, player, args);
                break;
            case "chat":
            case "c":
                chatHandler.handle(commandContext, player, args);
                break;
            case "warehouse":
            case "wh":
                warehouseHandler.handle(commandContext, player, args);
                break;
            default:
                // 检查是否为模块注册的子命令
                if (api.hasSubCommand("guild", args[0])) {
                    // 检查权限
                    String permission = api.getSubCommandPermission("guild", args[0]);
                    if (permission != null && !plugin.getPermissionManager().hasPermission(player, permission)) {
                        String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
                        player.sendMessage(ColorUtils.colorize(message));
                        return true;
                    }

                    // 执行模块命令
                    ModuleCommandHandler handler = api.getSubCommandHandler("guild", args[0]);
                    if (handler != null) {
                        // 去掉第一个参数（子命令名称），只传递子命令的参数
                        String[] subArgs = new String[args.length - 1];
                        if (args.length > 1) {
                            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                        }
                        handler.handle(player, subArgs);
                        return true;
                    }
                }

                player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "general.unknown-command", "&cUnknown command! Use /guild help for help.")));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList(
                "create", "info", "members", "invite", "kick", "promote", "demote", "accept", "decline", "leave", "delete", "sethome", "home", "relation", "economy", "deposit", "withdraw", "transfer", "logs", "placeholder", "time", "applications", "help", "chat", "warehouse"
            ));

            // 添加模块注册的子命令
            subCommands.addAll(api.getSubCommands("guild"));

            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "relation":
                    List<String> relationSubCommands = Arrays.asList("list", "create", "delete", "accept", "reject");
                    for (String cmd : relationSubCommands) {
                        if (cmd.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(cmd);
                        }
                    }
                    break;
                case "economy":
                    List<String> economySubCommands = Arrays.asList("info", "deposit", "withdraw", "transfer");
                    for (String cmd : economySubCommands) {
                        if (cmd.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(cmd);
                        }
                    }
                    break;
                case "delete":
                    List<String> deleteSubCommands = Arrays.asList("confirm", "cancel");
                    for (String cmd : deleteSubCommands) {
                        if (cmd.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(cmd);
                        }
                    }
                    break;
                case "warehouse":
                case "wh":
                    for (String cmd : Arrays.asList("perm", "info", "1", "2")) {
                        if (cmd.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(cmd);
                        }
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("relation")) {
                String relationSubCommand = args[1].toLowerCase();
                if (relationSubCommand.equals("create") || relationSubCommand.equals("delete") || relationSubCommand.equals("accept") || relationSubCommand.equals("reject")) {
                    // 这里可以添加公会名称的自动补全
                    // 暂时返回空列表
                }
            } else if (subCommand.equals("invite") || subCommand.equals("kick") || subCommand.equals("promote") || subCommand.equals("demote")) {
                // 这里可以添加在线玩家名称的自动补全
                // 暂时返回空列表
            } else if (subCommand.equals("warehouse") || subCommand.equals("wh")) {
                if (args[1].equalsIgnoreCase("perm")) {
                    for (String role : Arrays.asList("officer", "member")) {
                        if (role.startsWith(args[2].toLowerCase())) {
                            completions.add(role);
                        }
                    }
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            if ((subCommand.equals("warehouse") || subCommand.equals("wh"))
                    && args[1].equalsIgnoreCase("perm")) {
                for (String state : Arrays.asList("on", "off")) {
                    if (state.startsWith(args[3].toLowerCase())) {
                        completions.add(state);
                    }
                }
            }
        }

        return completions;
    }
}
