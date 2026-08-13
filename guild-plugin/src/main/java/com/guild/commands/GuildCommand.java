package com.guild.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.core.permissions.PermissionManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.DebugLog;
import com.guild.core.utils.QuietLog;
import com.guild.gui.ConfirmDeleteGuildGUI;
import com.guild.gui.MainGuildGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import com.guild.models.GuildRelation;
import com.guild.services.GuildService;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.util.InviteMessageUtils;
import com.guild.util.NotifyUtils;

/**
 * 工会主命令
 */
public class GuildCommand implements CommandExecutor, TabCompleter {
    
    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final GuildPluginAPI api;
    private final GuildService guildService;
    
    public GuildCommand(GuildPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.api = plugin.getServiceContainer().get(com.guild.core.module.ModuleManager.class).getSharedApi();
        this.guildService = plugin.getGuildService();
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
                handleCreate(player, args);
                break;
            case "info":
                handleInfo(player);
                break;
            case "members":
                handleMembers(player);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "promote":
                handlePromote(player, args);
                break;
            case "demote":
                handleDemote(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "decline":
                handleDecline(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "delete":
                if (args.length >= 2) {
                    if (args[1].equalsIgnoreCase("confirm")) {
                        handleDeleteConfirm(player);
                    } else if (args[1].equalsIgnoreCase("cancel")) {
                        handleDeleteCancel(player);
                    } else {
                        handleDelete(player);
                    }
                } else {
                    handleDelete(player);
                }
                break;
            case "sethome":
                handleSetHome(player);
                break;
            case "home":
                handleHome(player);
                break;
            case "relation":
                handleRelation(player, args);
                break;
            case "economy":
                handleEconomy(player, args);
                break;
            case "deposit":
                handleDeposit(player, args);
                break;
            case "withdraw":
                handleWithdraw(player, args);
                break;
            case "transfer":
                handleTransfer(player, args);
                break;
            case "logs":
                handleLogs(player, args);
                break;
            case "placeholder":
                handlePlaceholder(player, args);
                break;
            case "time":
                handleTime(player);
                break;
            case "help":
                handleHelp(player);
                break;
            case "applications":
                handleApplications(player);
                break;
            case "chat":
            case "c":
                handleChat(player, args);
                break;
            case "warehouse":
            case "wh":
                handleWarehouse(player, args);
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
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.create.usage", "&cUsage: /guild create <guild name> [tag] [description]");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.create")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 解析参数：名称（必填）、标签（可选）、描述（可选）
        // 支持引号包裹包含空格的内容，Bukkit 自动处理引号分割
        String guildName = args[1].replaceAll("[\"']", "").trim();
        String guildTag = args.length >= 3 ? args[2].replaceAll("[\"']", "").trim() : null;
        String guildDescription = args.length >= 4
            ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)).replaceAll("[\"']", "").trim()
            : null;
        
        // 从配置文件读取长度限制
        int minNameLength = plugin.getConfigManager().getMainConfig().getInt("guild.min-name-length", 3);
        int maxNameLength = plugin.getConfigManager().getMainConfig().getInt("guild.max-name-length", 20);
        int maxTagLength = plugin.getConfigManager().getMainConfig().getInt("guild.max-tag-length", 6);
        int maxDescriptionLength = plugin.getConfigManager().getMainConfig().getInt("guild.max-description-length", 100);
        
        // 名称验证（去掉正则限制，与GUI一致，支持颜色字符等特殊字符）
        if (guildName.isEmpty()) {
            String message = languageManager.getCoreMessage(player, "guild.create.name-required", "&cPlease enter a guild name first!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (guildName.length() < minNameLength || guildName.length() > maxNameLength) {
            String message = languageManager.getCoreMessage(player, "guild.create.name-length", "&cGuild name must be {min}-{max} characters long!" + minNameLength + "-" + maxNameLength + " characters!");
            message = message.replace("{min}", String.valueOf(minNameLength)).replace("{max}", String.valueOf(maxNameLength));
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 标签验证（空字符串视为未设置，传递 null）
        if (guildTag != null && !guildTag.isEmpty()) {
            if (guildTag.length() > maxTagLength) {
                String message = languageManager.getCoreMessage(player, "guild.create.tag-too-long", "&cGuild tag is too long! Maximum {max} characters allowed.");
                message = message.replace("{max}", String.valueOf(maxTagLength));
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
        } else {
            guildTag = null;
        }
        
        // 描述验证（空字符串视为未设置，传递 null）
        if (guildDescription != null && !guildDescription.isEmpty()) {
            if (guildDescription.length() > maxDescriptionLength) {
                String message = languageManager.getCoreMessage(player, "guild.create.description-too-long", "&cGuild description cannot exceed {max} characters!");
                message = message.replace("{max}", String.valueOf(maxDescriptionLength));
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
        } else {
            guildDescription = null;
        }
        
        final String finalTag = guildTag;
        final String finalDescription = guildDescription;
        
        CompletableFuture.runAsync(() -> {
            try {
                // 检查玩家是否已在公会中
                Guild existingGuild = guildService.getPlayerGuild(player.getUniqueId());
                if (existingGuild != null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "create.already-in-guild", "&cYou are already in a guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 经济系统检查
                boolean vaultAvailable = plugin.getEconomyManager().isVaultAvailable();
                boolean noEconomyMode = plugin.getEconomyManager().isNoEconomyMode();
                
                if (!vaultAvailable && !noEconomyMode) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.create.economy-not-available", "&cEconomy system is not available, cannot create guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 获取创建费用（无经济模式下费用为0）
                double creationCost = vaultAvailable
                    ? plugin.getConfigManager().getMainConfig().getDouble("guild.creation-cost", 1000.0)
                    : 0.0;
                
                // 仅在有经济系统时检查余额并扣费
                if (vaultAvailable && !noEconomyMode) {
                    if (!plugin.getEconomyManager().hasBalance(player, creationCost)) {
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            String message = languageManager.getCoreMessage(player, "guild.create.insufficient-funds", "&cInsufficient balance! Creating a guild requires {amount}!");
                            String msg = message.replace("{amount}", plugin.getEconomyManager().format(creationCost));
                            player.sendMessage(ColorUtils.colorize(msg));
                        });
                        return;
                    }
                    
                    if (!plugin.getEconomyManager().withdraw(player, creationCost)) {
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            String message = languageManager.getCoreMessage(player, "guild.create.payment-failed", "&cFailed to deduct creation fee!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }
                }
                
                final double finalCost = creationCost;
                boolean success = guildService.createGuild(guildName, finalTag, finalDescription, player.getUniqueId(), player.getName());
                if (success) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.create.success", "&aGuild created successfully!");
                        player.sendMessage(ColorUtils.colorize(message));
                        
                        // 打开公会信息GUI
                        MainGuildGUI mainGuildGUI = new MainGuildGUI(plugin, player);
                        plugin.getGuiManager().openGUI(player, mainGuildGUI);
                    });
                } else {
                    // 如果创建失败且有扣费，退还费用
                    if (vaultAvailable && !noEconomyMode && finalCost > 0) {
                        plugin.getEconomyManager().deposit(player, finalCost);
                    }
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        if (vaultAvailable && !noEconomyMode && finalCost > 0) {
                            String refundMessage = languageManager.getCoreMessage(player, "guild.create.payment-refunded", "&eCreation fee {amount} has been refunded.");
                            refundMessage = refundMessage.replace("{amount}", plugin.getEconomyManager().format(finalCost));
                            player.sendMessage(ColorUtils.colorize(refundMessage));
                        }
                        
                        String message = languageManager.getCoreMessage(player, "guild.create.exists", "&cThat guild name already exists!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.create.error", "&cAn error occurred while creating the guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleInfo(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.info.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                String message = languageManager.getCoreMessage(player, "guild.info.message", "&aGuild Info:\n&bName: &f{0}\n&bLevel: &f{1}\n&bLeader: &f{2}\n&bMembers: &f{3}\n&bCreated: &f{4}");
                message = message.replace("{0}", guild.getName());
                message = message.replace("{1}", String.valueOf(guild.getLevel()));
                message = message.replace("{2}", guild.getLeaderName());
                message = message.replace("{3}", String.valueOf(guildService.getGuildMemberCount(guild.getId())));
                message = message.replace("{4}", guild.getCreatedAt().toString());
                
                String finalMessage = message;
                CompatibleScheduler.runTask(plugin, player, () -> {
                    player.sendMessage(ColorUtils.colorize(finalMessage));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.info.error", "&cAn error occurred while fetching guild info!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleMembers(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.members.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                List<GuildMember> members = guildService.getGuildMembers(guild.getId());
                if (members.isEmpty()) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.members.empty", "&cThere are no members in the guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.members.title", "&aGuild Member List:");
                    player.sendMessage(ColorUtils.colorize(message));
                    
                    for (GuildMember m : members) {
                        String memberMessage = languageManager.getCoreMessage(player, "guild.members.member", "&b{0} - &f{1}");
                        memberMessage = memberMessage.replace("{0}", m.getPlayerName());
                        memberMessage = memberMessage.replace("{1}", m.getRole() == Role.LEADER ? "会长" : (m.getRole() == Role.OFFICER ? "副会长" : "成员"));
                        player.sendMessage(ColorUtils.colorize(memberMessage));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.members.error", "&cAn error occurred while fetching member list!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.invite.usage", "&cUsage: /guild invite <player name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.invite")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getCoreMessage(player, "guild.invite.player-not-found", "&cPlayer is not online!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = languageManager.getCoreMessage(player, "guild.invite.self", "&cYou cannot invite yourself!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.invite.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.invite.no-permission", "&cYou do not have permission to invite members!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                Guild targetGuild = guildService.getPlayerGuild(targetPlayer.getUniqueId());
                if (targetGuild != null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.invite.already-in-guild", "&cThat player is already in a guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 检查公会成员数量是否达到上限
                int memberCount = guildService.getGuildMemberCount(guild.getId());
                int maxMembers = guild.getMaxMembers();
                if (memberCount >= maxMembers) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.invite.full", "&cGuild is full!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 发送邀请
                String inviteMessage = InviteMessageUtils.formatInviteReceived(plugin, targetPlayer, player, guild);
                CompatibleScheduler.runTask(plugin, targetPlayer, () -> {
                    targetPlayer.sendMessage(inviteMessage);
                });
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.invite.success", "&aInvitation sent!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.invite.error", "&cAn error occurred while sending invitation!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.kick.usage", "&cUsage: /guild kick <player name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.kick")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getCoreMessage(player, "guild.kick.player-not-found", "&cPlayer is not online!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.kick.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.kick.no-permission", "&cYou do not have permission to kick members!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
                if (targetMember == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.kick.player-not-found", "&cPlayer is not online!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (targetMember.getRole() == Role.LEADER) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.kick.cannot-kick-master", "&cYou cannot kick the leader!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                boolean success = guildService.removeGuildMember(targetPlayer.getUniqueId(), player.getUniqueId());
                if (success) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.kick.success", "&aSuccessfully kicked the player!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    
                    // 通知被踢出的玩家
                    CompatibleScheduler.runTask(plugin, targetPlayer, () -> {
                        String kickMessage = languageManager.getCoreMessage(targetPlayer, "guild.kick.kicked", "&cYou have been kicked from the guild!");
                        targetPlayer.sendMessage(ColorUtils.colorize(kickMessage));
                    });
                } else {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.kick.error", "&cAn error occurred while kicking the player!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.kick.error", "&cAn error occurred while kicking the player!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.promote.usage", "&cUsage: /guild promote <player name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.promote")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getCoreMessage(player, "guild.promote.player-not-found", "&cPlayer is not online!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.promote.only-master", "&cOnly the leader can promote members!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
                if (targetMember == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.promote.player-not-found", "&cPlayer is not online!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (targetMember.getRole() == Role.LEADER) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.promote.already-master", "&cThat player is already the leader!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 提升为副会长
                boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), Role.OFFICER, player.getUniqueId());
                if (success) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.promote.success", "&aSuccessfully promoted player to officer!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    
                    // 通知被提升的玩家
                    CompatibleScheduler.runTask(plugin, targetPlayer, () -> {
                        String promoteMessage = languageManager.getCoreMessage(targetPlayer, "guild.promote.promoted", "&aYou have been promoted to officer!");
                        targetPlayer.sendMessage(ColorUtils.colorize(promoteMessage));
                    });
                } else {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.promote.error", "&cAn error occurred while promoting the player!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.promote.error", "&cAn error occurred while promoting the player!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.demote.usage", "&cUsage: /guild demote <player name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.demote")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getCoreMessage(player, "guild.demote.player-not-found", "&cPlayer is not online!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.demote.only-master", "&cOnly the leader can demote members!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
                if (targetMember == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.demote.player-not-found", "&cPlayer is not online!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (targetMember.getRole() == Role.LEADER) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.demote.cannot-demote-master", "&cYou cannot demote the leader!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 降级为普通成员
                boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), Role.MEMBER, player.getUniqueId());
                if (success) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.demote.success", "&aSuccessfully demoted player to member!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    
                    // 通知被降级的玩家
                    CompatibleScheduler.runTask(plugin, targetPlayer, () -> {
                        String demoteMessage = languageManager.getCoreMessage(targetPlayer, "guild.demote.demoted", "&cYou have been demoted to member!");
                        targetPlayer.sendMessage(ColorUtils.colorize(demoteMessage));
                    });
                } else {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.demote.error", "&cAn error occurred while demoting the player!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.demote.error", "&cAn error occurred while demoting the player!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.accept.usage", "&cUsage: /guild accept <guild name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replaceAll("[\"']", "").trim();
        
        guildService.getPlayerGuildAsync(player.getUniqueId()).thenAccept(existingGuild -> {
            if (existingGuild != null) {
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.accept.already-in-guild", "&cYou are already in a guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
                return;
            }
            
            guildService.getGuildByNameAsync(guildName).thenAccept(guild -> {
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.accept.guild-not-found", "&cGuild does not exist!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 检查玩家是否有该公会的有效邀请
                guildService.getPendingInvitationAsync(player.getUniqueId(), guild.getId()).thenAccept(invitation -> {
                    if (invitation == null) {
                        plugin.getLogger().warning("[Accept-Debug] 玩家 " + player.getName() + " 没有来自 " + guild.getName() + " 的邀请");
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            String message = languageManager.getCoreMessage(player, "guild.accept.no-invitation", "&cYou don't have an invitation from this guild or it has expired!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }
                    
                    DebugLog.info(plugin.getLogger(), "[Accept-Debug] 找到邀请 ID=" + invitation.getId() + " 从 " + invitation.getInviterName() + " 到 " + invitation.getTargetName());
                    
                    // 处理邀请接受
                    guildService.processInvitationDirectAsync(invitation, true).thenAccept(success -> {
                        if (success) {
                            DebugLog.info(plugin.getLogger(), "[Accept-Debug] 邀请处理成功，玩家 " + player.getName() + " 已加入 " + guild.getName());
                            QuietLog.system("Player " + player.getName() + " accepted invitation and joined " + guild.getName());
                            CompatibleScheduler.runTask(plugin, player, () -> {
                                String message = languageManager.getCoreMessage(player, "guild.accept.success", "&aYou have successfully joined the guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            
                            // 通知邀请者
                            NotifyUtils.notifyInviterInvitationProcessed(plugin, invitation.getInviterUuid(), 
                                invitation.getInviterName(), player.getName(), guild, true);
                        } else {
                            plugin.getLogger().warning("[Accept-Debug] 邀请处理失败，邀请ID=" + invitation.getId());
                            CompatibleScheduler.runTask(plugin, player, () -> {
                                String message = languageManager.getCoreMessage(player, "guild.accept.error", "&cError joining the guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                        }
                    });
                });
            });
        });
    }
    
    private void handleDecline(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.decline.usage", "&cUsage: /guild decline <guild name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replaceAll("[\"']", "").trim();
        
        guildService.getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.decline.guild-not-found", "&cGuild does not exist!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
                return;
            }
            
            // 检查玩家是否有该公会的有效邀请
            guildService.getPendingInvitationAsync(player.getUniqueId(), guild.getId()).thenAccept(invitation -> {
                if (invitation == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.decline.no-invitation", "&cYou don't have an invitation from this guild or it has expired!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 处理邀请拒绝
                guildService.processInvitationDirectAsync(invitation, false).thenAccept(success -> {
                    if (success) {
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            String message = languageManager.getCoreMessage(player, "guild.decline.success", "&aYou have declined the invitation!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        
                        // 通知邀请者
                        NotifyUtils.notifyInviterInvitationProcessed(plugin, invitation.getInviterUuid(), 
                            invitation.getInviterName(), player.getName(), guild, false);
                    } else {
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            String message = languageManager.getCoreMessage(player, "guild.decline.error", "&cError declining the invitation!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });
            });
        });
    }
    
    private void handleLeave(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                GuildMember member = guildService.getGuildMember(player.getUniqueId());
                if (member == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.leave.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (member.getRole() == Role.LEADER) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.leave.cannot-leave-as-master", "&cThe leader cannot leave the guild! Transfer leadership or delete the guild first!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                boolean success = guildService.removeGuildMember(player.getUniqueId(), player.getUniqueId());
                if (success) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.leave.success", "&aYou have left the guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                } else {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.leave.error", "&cAn error occurred while leaving the guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.leave.error", "&cAn error occurred while leaving the guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleDelete(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.delete.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.delete.only-master", "&cOnly the leader can delete the guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 打开确认删除GUI
                CompatibleScheduler.runTask(plugin, player, () -> {
                    ConfirmDeleteGuildGUI confirmGUI = new ConfirmDeleteGuildGUI(plugin, guild, player);
                    plugin.getGuiManager().openGUI(player, confirmGUI);
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.delete.error", "&cAn error occurred while deleting the guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleDeleteConfirm(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.delete.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.delete.only-master", "&cOnly the leader can delete the guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                boolean success = guildService.deleteGuild(guild.getId(), player.getUniqueId());
                if (success) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.delete.success", "&aGuild has been deleted successfully!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                } else {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.delete.error", "&cAn error occurred while deleting the guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.delete.error", "&cAn error occurred while deleting the guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleDeleteCancel(Player player) {
        String message = languageManager.getCoreMessage(player, "guild.delete.cancel", "&aGuild deletion cancelled!");
        player.sendMessage(ColorUtils.colorize(message));
    }
    
    private void handleSetHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.sethome")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.sethome.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.sethome.no-permission", "&cYou do not have permission to set the guild home!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 设置公会 home 位置
                plugin.getGuildService().setGuildHome(guild.getId(), player.getLocation(), player.getUniqueId());
                if (plugin.getGuildHomeProtectListener() != null) {
                    plugin.getGuildHomeProtectListener().refreshHomesAsync();
                }
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.sethome.success", "&aGuild home has been set!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.sethome.error", "&cAn error occurred while setting the guild home!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.home")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 校验玩家是否为公会成员
        com.guild.models.GuildMember member = guildService.getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = languageManager.getCoreMessage(player, "guild.home.not-in-guild", "&cYou are not in any guild!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getCoreMessage(player, "guild.home.not-in-guild", "&cYou are not in any guild!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        plugin.getGuildService().getGuildHomeAsync(guild.getId()).thenAccept(location -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (location != null) {
                    startHomeTeleportDelay(player, location);
                } else {
                    String message = languageManager.getCoreMessage(player, "home.not-set", "&cGuild home has not been set yet!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void startHomeTeleportDelay(Player player, org.bukkit.Location targetLocation) {
        com.guild.util.GuildHomeTeleport.start(plugin, player, targetLocation, false,
                () -> {
                    String message = languageManager.getCoreMessage(player, "home.success", "&aTeleported to guild home!");
                    player.sendMessage(ColorUtils.colorize(message));
                },
                reason -> {
                    String message = languageManager.getCoreMessage(player, "home.teleport-failed",
                            "&cTeleport failed, please try again!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
    }
    
    private void handleRelation(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.relation.usage", "&cUsage: /guild relation <list|create|delete|accept|reject>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "list":
                handleRelationList(player);
                break;
            case "create":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.create.usage", "&cUsage: /guild relation create <guild name> <relation type>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                String targetGuildName = args[2];
                String relationType = args.length >= 4 ? args[3] : "alliance";
                handleRelationCreate(player, targetGuildName, relationType);
                break;
            case "delete":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.delete.usage", "&cUsage: /guild relation delete <guild name>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                targetGuildName = args[2];
                handleRelationDelete(player, targetGuildName);
                break;
            case "accept":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.accept.usage", "&cUsage: /guild relation accept <guild name>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                targetGuildName = args[2];
                handleRelationAccept(player, targetGuildName);
                break;
            case "reject":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.reject.usage", "&cUsage: /guild relation reject <guild name>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                targetGuildName = args[2];
                handleRelationReject(player, targetGuildName);
                break;
            default:
                String message = languageManager.getCoreMessage(player, "guild.relation.invalid-subcommand", "&cInvalid subcommand!");
                player.sendMessage(ColorUtils.colorize(message));
                break;
        }
    }
    
    private void handleRelationList(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                List<GuildRelation> relations = plugin.getGuildService().getGuildRelationsAsync(guild.getId()).join();
                if (relations.isEmpty()) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.no-relations", "&cThis guild has no relations!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.list.title", "&aGuild Relations List:");
                    player.sendMessage(ColorUtils.colorize(message));
                });
                
                for (GuildRelation relation : relations) {
                    Guild targetGuild = guildService.getGuildById(relation.getOtherGuildId(guild.getId()));
                    if (targetGuild != null) {
                        String relationMessage = languageManager.getCoreMessage(player, "guild.relation.list.item", "&b{0} - &f{1}");
                        relationMessage = relationMessage.replace("{0}", targetGuild.getName());
                        relationMessage = relationMessage.replace("{1}", relation.getType().name());
                        String finalRelationMessage = relationMessage;
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            player.sendMessage(ColorUtils.colorize(finalRelationMessage));
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleRelationCreate(Player player, String targetGuildName, String relationType) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.no-permission", "&cYou do not have permission to manage guild relations!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.guild-not-found", "&cTarget guild does not exist!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (targetGuild.getId() == guild.getId()) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.cannot-relate-self", "&cYou cannot establish a relation with your own guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 检查是否已存在关系
                GuildRelation existingRelation = plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId()).join();
                if (existingRelation != null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.already-exists", "&cA relation with this guild already exists!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 创建关系
                boolean success = plugin.getGuildService().createGuildRelationAsync(guild.getId(), targetGuild.getId(), guild.getName(), targetGuild.getName(), GuildRelation.RelationType.valueOf(relationType.toUpperCase()), player.getUniqueId(), player.getName()).join();
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.create.success", "&aRelation request sent!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleRelationDelete(Player player, String targetGuildName) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.no-permission", "&cYou do not have permission to manage guild relations!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.guild-not-found", "&cTarget guild does not exist!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 检查关系是否存在
                GuildRelation relation = plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId()).join();
                if (relation == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.not-found", "&cNo relation found with this guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 删除关系
                boolean success = plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).join();
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.delete.success", "&aRelation deleted!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleRelationAccept(Player player, String targetGuildName) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.no-permission", "&cYou do not have permission to manage guild relations!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.guild-not-found", "&cTarget guild does not exist!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 检查是否有待处理的关系请求
                GuildRelation relation = plugin.getGuildService().getGuildRelationAsync(targetGuild.getId(), guild.getId()).join();
                if (relation == null || relation.getStatus() != GuildRelation.RelationStatus.PENDING) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.no-pending-request", "&cNo pending relation request from this guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 接受关系请求
                relation.setStatus(GuildRelation.RelationStatus.ACTIVE);
                boolean success = plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE).join();
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.accept.success", "&aRelation request accepted!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleRelationReject(Player player, String targetGuildName) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.no-permission", "&cYou do not have permission to manage guild relations!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.guild-not-found", "&cTarget guild does not exist!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 检查是否有待处理的关系请求
                GuildRelation relation = plugin.getGuildService().getGuildRelationAsync(targetGuild.getId(), guild.getId()).join();
                if (relation == null || relation.getStatus() != GuildRelation.RelationStatus.PENDING) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.relation.no-pending-request", "&cNo pending relation request from this guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 拒绝关系请求
                boolean success = plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).join();
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.reject.success", "&aRelation request rejected!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.relation.error", "&cAn error occurred while managing relations!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleEconomy(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.economy.usage", "&cUsage: /guild economy <info|deposit|withdraw|transfer>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "info":
                handleEconomyInfo(player);
                break;
            case "deposit":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.deposit.usage", "&cUsage: /guild economy deposit <amount>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    handleDeposit(player, amount);
                } catch (NumberFormatException e) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
                break;
            case "withdraw":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.withdraw.usage", "&cUsage: /guild economy withdraw <amount>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    handleWithdraw(player, amount);
                } catch (NumberFormatException e) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
                break;
            case "transfer":
                if (args.length < 4) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.transfer.usage", "&cUsage: /guild economy transfer <guild name> <amount>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                String targetGuildName = args[2];
                try {
                    double amount = Double.parseDouble(args[3]);
                    handleTransfer(player, targetGuildName, amount);
                } catch (NumberFormatException e) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
                break;
            default:
                String message = languageManager.getCoreMessage(player, "guild.economy.invalid-subcommand", "&cInvalid subcommand!");
                player.sendMessage(ColorUtils.colorize(message));
                break;
        }
    }
    
    private void handleEconomyInfo(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.economy.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                double balance = guild.getBalance();
                
                String message = languageManager.getCoreMessage(player, "guild.economy.info", "&aGuild Economy Info:\n&bBalance: &f{0} coins");
                message = message.replace("{0}", String.format("%.2f", balance));
                String finalMessage = message;
                CompatibleScheduler.runTask(plugin, player, () -> {
                    player.sendMessage(ColorUtils.colorize(finalMessage));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.economy.error", "&cAn error occurred while fetching economy info!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.deposit.usage", "&cUsage: /guild deposit <amount>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            handleDeposit(player, amount);
        } catch (NumberFormatException e) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    private void handleDeposit(Player player, double amount) {
        if (amount <= 0) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.deposit.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!plugin.getEconomyManager().hasBalance(player, amount)) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.deposit.insufficient-funds", "&cYou don't have enough money!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 扣除玩家余额
                if (!plugin.getEconomyManager().withdraw(player, amount)) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.deposit.error", "&cAn error occurred while depositing!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 增加公会余额（传入操作者信息，避免 updateGuildBalanceAsync 内部产生 SYSTEM 匿名日志）
                boolean success = plugin.getGuildService().updateGuildBalanceAsync(
                        guild.getId(), guild.getBalance() + amount,
                        player.getUniqueId().toString(), player.getName()).join();
                if (success) {
                    // 记录投资
                    plugin.getGuildInvestmentService().recordDeposit(guild.getId(), player.getUniqueId(), player.getName(), amount);
                    // 写入 guild_contributions 表（供 GuildFundsGUI 展示）
                    plugin.getGuildService().addGuildContributionAsync(guild.getId(), player.getUniqueId(),
                            player.getName(), amount,
                            com.guild.models.GuildContribution.ContributionType.DEPOSIT,
                            languageManager.getCoreMessage(player, "deposit.contribution-desc",
                                    "{player} deposited {amount}")
                                    .replace("{player}", player.getName())
                                    .replace("{amount}", String.format("%.2f", amount)));
                    // 分发存款事件给模块
                    plugin.getGuildService().notifyEconomyDeposit(guild.getId(), guild.getName(), player.getUniqueId(), player.getName(), amount);
                }
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.deposit.success", "&aSuccessfully deposited {0} coins into the guild account!");
                    String msg = message.replace("{0}", String.format("%.2f", amount));
                    player.sendMessage(ColorUtils.colorize(msg));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.deposit.error", "&cAn error occurred while depositing!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.withdraw.usage", "&cUsage: /guild withdraw <amount>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            handleWithdraw(player, amount);
        } catch (NumberFormatException e) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    private void handleWithdraw(Player player, double amount) {
        if (amount <= 0) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.withdraw.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.withdraw.only-master", "&cOnly the leader can withdraw from the guild account!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (guild.getBalance() < amount) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.withdraw.insufficient-funds", "&cInsufficient guild account balance!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 增加玩家余额
                if (!plugin.getEconomyManager().deposit(player, amount)) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.withdraw.error", "&cAn error occurred while withdrawing!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 减少公会余额（传入操作者信息，避免产生 SYSTEM 匿名日志）
                plugin.getGuildService().updateGuildBalanceAsync(
                        guild.getId(), guild.getBalance() - amount,
                        player.getUniqueId().toString(), player.getName()).join();
                // 记录取款
                plugin.getGuildInvestmentService().recordWithdraw(guild.getId(), player.getUniqueId(), amount);
                // 分发取款事件给模块
                plugin.getGuildService().notifyEconomyWithdraw(guild.getId(), guild.getName(), player.getUniqueId(), player.getName(), amount);
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.withdraw.success", "&aSuccessfully withdrew {0} coins from the guild account!");
                    String msg = message.replace("{0}", String.format("%.2f", amount));
                    player.sendMessage(ColorUtils.colorize(msg));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.withdraw.error", "&cAn error occurred while withdrawing!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleTransfer(Player player, String[] args) {
        if (args.length < 3) {
            String message = languageManager.getCoreMessage(player, "guild.transfer.usage", "&cUsage: /guild transfer <player name> <amount>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        try {
            double amount = Double.parseDouble(args[2]);
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer == null) {
                String message = languageManager.getCoreMessage(player, "guild.transfer.player-not-found", "&cTarget player is not online!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            handleTransfer(player, targetPlayer, amount);
        } catch (NumberFormatException e) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    private void handleTransfer(Player player, String targetGuildName, double amount) {
        if (amount <= 0) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild sourceGuild = guildService.getPlayerGuild(player.getUniqueId());
                if (sourceGuild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.transfer.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.transfer.only-master", "&cOnly the leader can transfer between guilds!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.transfer.target-not-found", "&cTarget guild does not exist!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (sourceGuild.getId() == targetGuild.getId()) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.transfer.same-guild", "&cYou cannot transfer to your own guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (sourceGuild.getBalance() < amount) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.transfer.insufficient-funds", "&cInsufficient guild account balance!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 减少源公会余额
                boolean sourceSuccess = plugin.getGuildService().updateGuildBalanceAsync(sourceGuild.getId(), sourceGuild.getBalance() - amount).join();
                
                // 增加目标公会余额
                boolean targetSuccess = plugin.getGuildService().updateGuildBalanceAsync(targetGuild.getId(), targetGuild.getBalance() + amount).join();
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.success", "&aSuccessfully transferred {0} coins to {1}!");
                    String msg = message.replace("{0}", String.format("%.2f", amount));
                    msg = msg.replace("{1}", targetGuild.getName());
                    player.sendMessage(ColorUtils.colorize(msg));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.error", "&cAn error occurred while transferring!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleTransfer(Player player, Player targetPlayer, double amount) {
        if (amount <= 0) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&cInvalid amount!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.transfer.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.transfer.no-permission", "&cYou do not have permission to transfer!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (guild.getBalance() < amount) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.transfer.insufficient-funds", "&cInsufficient guild account balance!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 减少公会余额
                plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() - amount).join();
                
                // 增加目标玩家余额
                if (!plugin.getEconomyManager().deposit(targetPlayer, amount)) {
                    // 如果转账失败，恢复公会余额
                    plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() + amount).join();
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.transfer.error", "&cAn error occurred while transferring!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.success", "&aSuccessfully transferred {0} coins to {1}!");
                    String msg = message.replace("{0}", String.format("%.2f", amount));
                    msg = msg.replace("{1}", targetPlayer.getName());
                    player.sendMessage(ColorUtils.colorize(msg));
                });
                
                // 通知目标玩家
                CompatibleScheduler.runTask(plugin, targetPlayer, () -> {
                    String targetMessage = languageManager.getCoreMessage(targetPlayer, "guild.transfer.received", "&aYou received {0} coins!");
                    String msg = targetMessage.replace("{0}", String.format("%.2f", amount));
                    targetPlayer.sendMessage(ColorUtils.colorize(msg));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.error", "&cAn error occurred while transferring!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleLogs(Player player, String[] args) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.logs.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.logs.no-permission", "&cYou do not have permission to view logs!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                // 这里应该显示公会日志
                // 暂时简化处理
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.logs.title", "&aGuild Logs:");
                    player.sendMessage(ColorUtils.colorize(message));
                    player.sendMessage(ColorUtils.colorize("&b- 日志功能正在开发中..."));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.logs.error", "&cAn error occurred while fetching guild logs!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handlePlaceholder(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.placeholder.usage", "&cUsage: /guild placeholder <player|guild|rank>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String type = args[1].toLowerCase();
        
        CompletableFuture.runAsync(() -> {
            try {
                switch (type) {
                    case "player":
                        String playerName = player.getName();
                        String playerPlaceholder = String.format("{guild_player_%s}", playerName.toLowerCase());
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            String message = languageManager.getCoreMessage(player, "guild.placeholder.player", "&aPlayer placeholder: &f{0}");
                            String msg = message.replace("{0}", playerPlaceholder);
                            player.sendMessage(ColorUtils.colorize(msg));
                        });
                        break;
                    case "guild":
                        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(plugin, player, () -> {
                                String message1 = languageManager.getCoreMessage(player, "guild.placeholder.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message1));
                            });
                            return;
                        }
                        String guildPlaceholder = String.format("{guild_%s}", guild.getName().toLowerCase().replace(" ", "_"));
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            String message2 = languageManager.getCoreMessage(player, "guild.placeholder.guild", "&aGuild placeholder: &f{0}");
                            String msg2 = message2.replace("{0}", guildPlaceholder);
                            player.sendMessage(ColorUtils.colorize(msg2));
                        });
                        break;
                    case "rank":
                        GuildMember member = guildService.getGuildMember(player.getUniqueId());
                        if (member == null) {
                            CompatibleScheduler.runTask(plugin, player, () -> {
                                String message3 = languageManager.getCoreMessage(player, "guild.placeholder.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message3));
                            });
                            return;
                        }
                        String rankPlaceholder = String.format("{guild_rank_%s}", member.getRole().name().toLowerCase());
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            String message4 = languageManager.getCoreMessage(player, "guild.placeholder.rank", "&aRank placeholder: &f{0}");
                            String msg4 = message4.replace("{0}", rankPlaceholder);
                            player.sendMessage(ColorUtils.colorize(msg4));
                        });
                        break;
                    default:
                        CompatibleScheduler.runTask(plugin, player, () -> {
                            String message5 = languageManager.getCoreMessage(player, "guild.placeholder.invalid-type", "&cInvalid placeholder type!");
                            player.sendMessage(ColorUtils.colorize(message5));
                        });
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.placeholder.error", "&cAn error occurred while getting placeholders!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }
    
    private void handleTime(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        String message = languageManager.getCoreMessage(player, "guild.time.not-in-guild", "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }
                
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.Duration duration = java.time.Duration.between(guild.getCreatedAt(), now);
                long days = duration.toDays();
                long hours = duration.toHours() % 24;
                
                String message = languageManager.getCoreMessage(player, "guild.time.age", "&aGuild created: {0}\n&aGuild age: &f{1} days {2} hours");
                message = message.replace("{0}", guild.getCreatedAt().toString());
                message = message.replace("{1}", String.valueOf(days));
                message = message.replace("{2}", String.valueOf(hours));
                String finalMessage = message;
                CompatibleScheduler.runTask(plugin, player, () -> {
                    player.sendMessage(ColorUtils.colorize(finalMessage));
                });
            } catch (Exception e) {
                e.printStackTrace();
                CompatibleScheduler.runTask(plugin, player, () -> {
                    String message = languageManager.getCoreMessage(player, "guild.time.error", "&cAn error occurred while fetching guild time info!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            }
        });
    }

    /**
     * /guild applications — 打开申请管理GUI（仅会长/官员可访问）
     */
    private void handleApplications(Player player) {
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (guild == null) {
                    String msg = languageManager.getCoreMessage(player, "general.no-guild", "&cYou are not in any guild!");
                    player.sendMessage(ColorUtils.colorize(msg));
                    return;
                }
                // 异步检查角色权限（与 MainGuildGUI.openApplicationManagementGUI 一致）
                plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        if (member == null || !member.getRole().canInvite()) {
                            String msg = languageManager.getCoreMessage(player, "general.no-permission", "&cInsufficient role permission!");
                            player.sendMessage(ColorUtils.colorize(msg));
                            return;
                        }
                        plugin.getGuiManager().openGUI(player,
                            new com.guild.gui.ApplicationManagementGUI(plugin, guild, player));
                    });
                });
            });
        });
    }

    /**
     * /guild chat — 切换公会聊天模式或发送单条消息
     */
    private void handleChat(Player player, String[] args) {
        com.guild.chat.GuildChatManager chatManager = plugin.getGuildChatManager();
        if (chatManager == null) {
            player.sendMessage(ColorUtils.colorize("&cGuild chat is not available."));
            return;
        }

        // /guild chat <消息> — 直接发送一条公会消息（不切换模式）
        if (args.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(" ");
                sb.append(args[i]);
            }
            String msg = sb.toString();
            com.guild.models.GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) {
                String err = languageManager.getCoreMessage(player, "guild.chat.not-in-guild",
                    "&cYou are not in a guild!");
                player.sendMessage(ColorUtils.colorize(err));
                return;
            }
            com.guild.models.Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            if (guild == null) {
                player.sendMessage(ColorUtils.colorize("&cGuild not found!"));
                return;
            }
            String formatted = chatManager.formatMessage(player, member.getRole(), msg);
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                com.guild.models.GuildMember pm = guildService.getGuildMember(p.getUniqueId());
                if (pm != null && pm.getGuildId() == guild.getId()) {
                    p.sendMessage(ColorUtils.colorize(formatted));
                }
            }
            return;
        }

        // /guild chat — 切换聊天模式
        boolean enabled = chatManager.toggleChatMode(player);
        if (enabled) {
            String msg = languageManager.getCoreMessage(player, "guild.chat.enabled",
                "&aGuild chat &aenabled&a. Your messages will be sent to guild members.");
            player.sendMessage(ColorUtils.colorize(msg));
        } else {
            String msg = languageManager.getCoreMessage(player, "guild.chat.disabled",
                "&eGuild chat &cdisabled&e. Your messages will be sent to global chat.");
            player.sendMessage(ColorUtils.colorize(msg));
        }
    }

    private void handleHelp(Player player) {
        String message = languageManager.getCoreMessage(player, "help.title", "&a=== Guild System Help ===");
        player.sendMessage(ColorUtils.colorize(message));
        
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.main-menu", "&e/guild &7- Open guild main menu")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.create", "&e/guild create <name> [tag] [description] &7- Create guild")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.info", "&e/guild info &7- View guild information")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.members", "&e/guild members &7- View guild members")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.invite", "&e/guild invite <player> &7- Invite player to join guild")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.kick", "&e/guild kick <player> &7- Kick guild member")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.promote", "&e/guild promote <player> &7- Promote guild member")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.demote", "&e/guild demote <player> &7- Demote guild member")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.accept", "&e/guild accept <inviter> &7- Accept guild invitation")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.decline", "&e/guild decline <inviter> &7- Decline guild invitation")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.leave", "&e/guild leave &7- Leave guild")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.delete", "&e/guild delete &7- Delete guild")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.sethome", "&e/guild sethome &7- Set guild home")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.home", "&e/guild home &7- Teleport to guild home")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.relation", "&e/guild relation &7- Manage guild relations")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.economy", "&e/guild economy &7- Manage guild economy")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.deposit", "&e/guild deposit <amount> &7- Deposit funds to guild")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.withdraw", "&e/guild withdraw <amount> &7- Withdraw funds from guild")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.transfer", "&e/guild transfer <guild> <amount> &7- Transfer funds to another guild")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.logs", "&e/guild logs &7- View guild operation logs")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.placeholder", "&e/guild placeholder <player|guild|rank> &7- Get placeholders")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.time", "&e/guild time &7- View guild time info")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.applications", "&e/guild applications &7- Manage guild applications")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.chat", "&e/guild chat &7- Toggle guild chat mode &7| &e/guild chat <msg> &7- Send guild message")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.warehouse", "&e/guild warehouse [page|perm|info] &7- Open guild warehouse / permissions / info")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.help", "&e/guild help &7- Show this help")));
    }

    private void handleWarehouse(Player player, String[] args) {
        var warehouse = plugin.getGuildWarehouseService();
        if (warehouse == null) {
            player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.unavailable",
                    "&cGuild warehouse is currently unavailable.")));
            return;
        }

        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "general.no-guild",
                    "&cYou have not joined any guild yet!")));
            return;
        }

        if (args.length == 1) {
            warehouse.openWarehouse(player, guild, 1);
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "info" -> handleWarehouseInfo(player, guild, warehouse);
            case "perm" -> handleWarehousePerm(player, guild, warehouse, args);
            default -> {
                try {
                    int page = Integer.parseInt(sub);
                    warehouse.openWarehouse(player, guild, page);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player,
                            "warehouse.usage",
                            "&eUsage: /guild warehouse [page] | /guild warehouse info | /guild warehouse perm <officer|member> <on|off>")));
                }
            }
        }
    }

    private void handleWarehouseInfo(Player player, Guild guild, com.guild.warehouse.GuildWarehouseService warehouse) {
        int peak = guild.getPeakLevel();
        int slots = warehouse.resolveSlots(guild);
        int pages = warehouse.resolvePageCount(guild);
        Boolean officerOverride = warehouse.getRoleOpenOverrideSync(guild.getId(), Role.OFFICER);
        Boolean memberOverride = warehouse.getRoleOpenOverrideSync(guild.getId(), Role.MEMBER);
        boolean officer = officerOverride != null
                ? officerOverride
                : plugin.getPermissionManager().getDefaultCanWarehouse(Role.OFFICER);
        boolean member = memberOverride != null
                ? memberOverride
                : plugin.getPermissionManager().getDefaultCanWarehouse(Role.MEMBER);

        String on = languageManager.getCoreMessage(player, "warehouse.state-on", "&aON");
        String off = languageManager.getCoreMessage(player, "warehouse.state-off", "&cOFF");
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.info-header",
                "&a=== Guild Warehouse Info ===")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.info-peak",
                        "&7Peak level: &e{peak}")
                .replace("{peak}", String.valueOf(peak))));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.info-slots",
                        "&7Slots: &e{slots}")
                .replace("{slots}", String.valueOf(slots))));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.info-pages",
                        "&7Pages: &e{pages} &7(/guild warehouse <page>)")
                .replace("{pages}", String.valueOf(pages))));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.info-officer",
                        "&7Officer access: {state}")
                .replace("{state}", officer ? on : off)));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.info-member",
                        "&7Member access: {state}")
                .replace("{state}", member ? on : off)));
        if (!warehouse.isAvailable()) {
            player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.nbtapi-missing",
                    "&cGuild warehouse requires the NBTAPI plugin.")));
        }
    }

    private void handleWarehousePerm(Player player, Guild guild,
                                     com.guild.warehouse.GuildWarehouseService warehouse, String[] args) {
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        boolean isLeader = member != null && member.getRole() == Role.LEADER;
        boolean isAdmin = plugin.getPermissionManager().hasPermission(player, "guild.admin");
        if (!isLeader && !isAdmin) {
            player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.perm-leader-only",
                    "&cOnly the guild leader can change warehouse access.")));
            return;
        }
        if (args.length < 4) {
            player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.perm-usage",
                    "&eUsage: /guild warehouse perm <officer|member> <on|off>")));
            return;
        }

        Role targetRole;
        if (args[2].equalsIgnoreCase("officer")) {
            targetRole = Role.OFFICER;
        } else if (args[2].equalsIgnoreCase("member")) {
            targetRole = Role.MEMBER;
        } else {
            player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.perm-usage",
                    "&eUsage: /guild warehouse perm <officer|member> <on|off>")));
            return;
        }

        boolean enable;
        if (args[3].equalsIgnoreCase("on") || args[3].equalsIgnoreCase("true") || args[3].equalsIgnoreCase("1")) {
            enable = true;
        } else if (args[3].equalsIgnoreCase("off") || args[3].equalsIgnoreCase("false") || args[3].equalsIgnoreCase("0")) {
            enable = false;
        } else {
            player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.perm-usage",
                    "&eUsage: /guild warehouse perm <officer|member> <on|off>")));
            return;
        }

        warehouse.setRoleOpenPermission(guild.getId(), targetRole, enable, player, guild.getName()).thenAccept(ok ->
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (ok) {
                        String roleName = targetRole == Role.OFFICER
                                ? languageManager.getCoreMessage(player, "warehouse.role-officer", "officer")
                                : languageManager.getCoreMessage(player, "warehouse.role-member", "member");
                        String state = enable
                                ? languageManager.getCoreMessage(player, "warehouse.state-on", "&aON")
                                : languageManager.getCoreMessage(player, "warehouse.state-off", "&cOFF");
                        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.perm-updated",
                                        "&aSet {role} warehouse access to {state}")
                                .replace("{role}", roleName)
                                .replace("{state}", state)));
                    } else {
                        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "warehouse.perm-failed",
                                "&cFailed to update warehouse permission.")));
                    }
                }));
    }
}