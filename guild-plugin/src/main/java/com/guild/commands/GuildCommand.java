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
            String msg = languageManager.getMessage("general.player-only", "&c此命令只能由玩家执行！");
            sender.sendMessage(ColorUtils.colorize(msg));
            return true;
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
            default:
                // 检查是否为模块注册的子命令
                if (api.hasSubCommand("guild", args[0])) {
                    // 检查权限
                    String permission = api.getSubCommandPermission("guild", args[0]);
                    if (permission != null && !plugin.getPermissionManager().hasPermission(player, permission)) {
                        String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
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
                
                player.sendMessage(ColorUtils.colorize(languageManager.getMessage(player, "general.unknown-command", "&c未知命令！使用 /guild help 查看帮助。")));
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList(
                "create", "info", "members", "invite", "kick", "promote", "demote", "accept", "decline", "leave", "delete", "sethome", "home", "relation", "economy", "deposit", "withdraw", "transfer", "logs", "placeholder", "time", "help"
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
            }
        }
        
        return completions;
    }
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.create.usage", "&c用法: /guild create <公会名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.create")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String guildTag = guildName.substring(0, Math.min(guildName.length(), 3)).toUpperCase();
        String description = "欢迎加入我们的公会！";
        
        if (guildName.length() < 2 || guildName.length() > 10) {
            String message = languageManager.getMessage(player, "guild.create.name-length", "&c公会名称长度必须在2-10个字符之间！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!guildName.matches("[a-zA-Z0-9\\u4e00-\\u9fa5]")) {
            String message = languageManager.getMessage(player, "guild.create.invalid-name", "&c公会名称只能包含字母、数字和中文！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                boolean success = guildService.createGuild(guildName, guildTag, description, player.getUniqueId(), player.getName());
                if (success) {
                    String message = languageManager.getMessage(player, "guild.create.success", "&a公会创建成功！");
                    player.sendMessage(ColorUtils.colorize(message));
                    
                    // 打开公会信息GUI
                    MainGuildGUI mainGuildGUI = new MainGuildGUI(plugin, player);
                    plugin.getGuiManager().openGUI(player, mainGuildGUI);
                } else {
                    String message = languageManager.getMessage(player, "guild.create.exists", "&c该公会名称已存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.create.error", "&c创建公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleInfo(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.info.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                String message = languageManager.getMessage(player, "guild.info.message", "&a公会信息：\n&b名称: &f{0}\n&b等级: &f{1}\n&b会长: &f{2}\n&b成员数量: &f{3}\n&b创建时间: &f{4}");
                message = message.replace("{0}", guild.getName());
                message = message.replace("{1}", String.valueOf(guild.getLevel()));
                message = message.replace("{2}", guild.getLeaderName());
                message = message.replace("{3}", String.valueOf(guildService.getGuildMemberCount(guild.getId())));
                message = message.replace("{4}", guild.getCreatedAt().toString());
                
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.info.error", "&c获取公会信息时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleMembers(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.members.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                List<GuildMember> members = guildService.getGuildMembers(guild.getId());
                if (members.isEmpty()) {
                    String message = languageManager.getMessage(player, "guild.members.empty", "&c公会中没有成员！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                String message = languageManager.getMessage(player, "guild.members.title", "&a公会成员列表：");
                player.sendMessage(ColorUtils.colorize(message));
                
                for (GuildMember m : members) {
                    String memberMessage = languageManager.getMessage(player, "guild.members.member", "&b{0} - &f{1}");
                    memberMessage = memberMessage.replace("{0}", m.getPlayerName());
                    memberMessage = memberMessage.replace("{1}", m.getRole() == Role.LEADER ? "会长" : (m.getRole() == Role.OFFICER ? "副会长" : "成员"));
                    player.sendMessage(ColorUtils.colorize(memberMessage));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.members.error", "&c获取成员列表时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.invite.usage", "&c用法: /guild invite <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.invite")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getMessage(player, "guild.invite.player-not-found", "&c玩家不在线！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = languageManager.getMessage(player, "guild.invite.self", "&c您不能邀请自己！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.invite.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.invite.no-permission", "&c您没有邀请成员的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getPlayerGuild(targetPlayer.getUniqueId());
                if (targetGuild != null) {
                    String message = languageManager.getMessage(player, "guild.invite.already-in-guild", "&c该玩家已经在一个公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查公会成员数量是否达到上限
                int memberCount = guildService.getGuildMemberCount(guild.getId());
                int maxMembers = guild.getMaxMembers();
                if (memberCount >= maxMembers) {
                    String message = languageManager.getMessage(player, "guild.invite.full", "&c公会成员已满！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 发送邀请
                String inviteMessage = InviteMessageUtils.formatInviteReceived(plugin, targetPlayer, player, guild);
                targetPlayer.sendMessage(inviteMessage);
                
                String message = languageManager.getMessage(player, "guild.invite.success", "&a邀请已发送！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.invite.error", "&c发送邀请时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.kick.usage", "&c用法: /guild kick <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.kick")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getMessage(player, "guild.kick.player-not-found", "&c玩家不在线！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.kick.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.kick.no-permission", "&c您没有踢出成员的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
                if (targetMember == null) {
                    String message = languageManager.getMessage(player, "guild.kick.player-not-found", "&c该玩家不在公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (targetMember.getRole() == Role.LEADER) {
                    String message = languageManager.getMessage(player, "guild.kick.cannot-kick-master", "&c您不能踢出会长！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                boolean success = guildService.removeGuildMember(targetPlayer.getUniqueId(), player.getUniqueId());
                if (success) {
                    String message = languageManager.getMessage(player, "guild.kick.success", "&a已成功踢出玩家！");
                    player.sendMessage(ColorUtils.colorize(message));
                    
                    // 通知被踢出的玩家
                    String kickMessage = languageManager.getMessage(targetPlayer, "guild.kick.kicked", "&c您已被踢出公会！");
                    targetPlayer.sendMessage(ColorUtils.colorize(kickMessage));
                } else {
                    String message = languageManager.getMessage(player, "guild.kick.error", "&c踢出玩家时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.kick.error", "&c踢出玩家时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.promote.usage", "&c用法: /guild promote <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.promote")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getMessage(player, "guild.promote.player-not-found", "&c玩家不在线！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.promote.only-master", "&c只有会长可以提升成员！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
                if (targetMember == null) {
                    String message = languageManager.getMessage(player, "guild.promote.player-not-found", "&c该玩家不在公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (targetMember.getRole() == Role.LEADER) {
                    String message = languageManager.getMessage(player, "guild.promote.already-master", "&c该玩家已经是会长！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 提升为副会长
                boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), Role.OFFICER, player.getUniqueId());
                if (success) {
                    String message = languageManager.getMessage(player, "guild.promote.success", "&a已成功提升玩家为副会长！");
                    player.sendMessage(ColorUtils.colorize(message));
                    
                    // 通知被提升的玩家
                    String promoteMessage = languageManager.getMessage(targetPlayer, "guild.promote.promoted", "&a您已被提升为副会长！");
                    targetPlayer.sendMessage(ColorUtils.colorize(promoteMessage));
                } else {
                    String message = languageManager.getMessage(player, "guild.promote.error", "&c提升玩家时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.promote.error", "&c提升玩家时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.demote.usage", "&c用法: /guild demote <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.demote")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getMessage(player, "guild.demote.player-not-found", "&c玩家不在线！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.demote.only-master", "&c只有会长可以降级成员！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
                if (targetMember == null) {
                    String message = languageManager.getMessage(player, "guild.demote.player-not-found", "&c该玩家不在公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (targetMember.getRole() == Role.LEADER) {
                    String message = languageManager.getMessage(player, "guild.demote.cannot-demote-master", "&c您不能降级会长！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 降级为普通成员
                boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), Role.MEMBER, player.getUniqueId());
                if (success) {
                    String message = languageManager.getMessage(player, "guild.demote.success", "&a已成功降级玩家为普通成员！");
                    player.sendMessage(ColorUtils.colorize(message));
                    
                    // 通知被降级的玩家
                    String demoteMessage = languageManager.getMessage(targetPlayer, "guild.demote.demoted", "&c您已被降级为普通成员！");
                    targetPlayer.sendMessage(ColorUtils.colorize(demoteMessage));
                } else {
                    String message = languageManager.getMessage(player, "guild.demote.error", "&c降级玩家时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.demote.error", "&c降级玩家时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.accept.usage", "&c用法: /guild accept <公会名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild existingGuild = guildService.getPlayerGuild(player.getUniqueId());
                if (existingGuild != null) {
                    String message = languageManager.getMessage(player, "guild.accept.already-in-guild", "&c您已经在一个公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild guild = guildService.getGuildByName(guildName);
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.accept.guild-not-found", "&c公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 这里应该检查玩家是否有该公会的邀请
                // 暂时简化处理
                
                boolean success = guildService.addGuildMember(guild.getId(), player.getUniqueId(), player.getName(), Role.MEMBER);
                if (success) {
                    String message = languageManager.getMessage(player, "guild.accept.success", "&a已成功加入公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = languageManager.getMessage(player, "guild.accept.error", "&c加入公会时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.accept.error", "&c加入公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDecline(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.decline.usage", "&c用法: /guild decline <公会名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getGuildByName(guildName);
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.decline.guild-not-found", "&c公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 这里应该移除玩家的邀请
                // 暂时简化处理
                
                String message = languageManager.getMessage(player, "guild.decline.success", "&a已拒绝加入公会！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.decline.error", "&c拒绝加入公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleLeave(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                GuildMember member = guildService.getGuildMember(player.getUniqueId());
                if (member == null) {
                    String message = languageManager.getMessage(player, "guild.leave.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (member.getRole() == Role.LEADER) {
                    String message = languageManager.getMessage(player, "guild.leave.cannot-leave-as-master", "&c会长不能离开公会，请先转让会长或删除公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                boolean success = guildService.removeGuildMember(player.getUniqueId(), player.getUniqueId());
                if (success) {
                    String message = languageManager.getMessage(player, "guild.leave.success", "&a已成功离开公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = languageManager.getMessage(player, "guild.leave.error", "&c离开公会时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.leave.error", "&c离开公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDelete(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.delete.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.delete.only-master", "&c只有会长可以删除公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 打开确认删除GUI
                ConfirmDeleteGuildGUI confirmGUI = new ConfirmDeleteGuildGUI(plugin, guild, player);
                plugin.getGuiManager().openGUI(player, confirmGUI);
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.delete.error", "&c删除公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDeleteConfirm(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.delete.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.delete.only-master", "&c只有会长可以删除公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                boolean success = guildService.deleteGuild(guild.getId(), player.getUniqueId());
                if (success) {
                    String message = languageManager.getMessage(player, "guild.delete.success", "&a公会已成功删除！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = languageManager.getMessage(player, "guild.delete.error", "&c删除公会时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.delete.error", "&c删除公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDeleteCancel(Player player) {
        String message = languageManager.getMessage(player, "guild.delete.cancel", "&a已取消删除公会！");
        player.sendMessage(ColorUtils.colorize(message));
    }
    
    private void handleSetHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.sethome")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.sethome.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.sethome.no-permission", "&c您没有设置公会 home 的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 设置公会 home 位置
                plugin.getGuildService().setGuildHome(guild.getId(), player.getLocation(), player.getUniqueId());
                
                String message = languageManager.getMessage(player, "guild.sethome.success", "&a公会 home 位置已设置！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.sethome.error", "&c设置公会 home 位置时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.home")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.home.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                org.bukkit.Location homeLocation = plugin.getGuildService().getGuildHome(guild.getId());
                if (homeLocation == null) {
                    String message = languageManager.getMessage(player, "guild.home.not-set", "&c公会 home 位置未设置！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 传送玩家到公会 home
                player.teleport(homeLocation);
                
                String message = languageManager.getMessage(player, "guild.home.success", "&a已传送到公会 home！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.home.error", "&c传送到公会 home 时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleRelation(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.relation.usage", "&c用法: /guild relation <list|create|delete|accept|reject>");
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
                    String message = languageManager.getMessage(player, "guild.relation.create.usage", "&c用法: /guild relation create <公会名称> <关系类型>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                String targetGuildName = args[2];
                String relationType = args.length >= 4 ? args[3] : "alliance";
                handleRelationCreate(player, targetGuildName, relationType);
                break;
            case "delete":
                if (args.length < 3) {
                    String message = languageManager.getMessage(player, "guild.relation.delete.usage", "&c用法: /guild relation delete <公会名称>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                targetGuildName = args[2];
                handleRelationDelete(player, targetGuildName);
                break;
            case "accept":
                if (args.length < 3) {
                    String message = languageManager.getMessage(player, "guild.relation.accept.usage", "&c用法: /guild relation accept <公会名称>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                targetGuildName = args[2];
                handleRelationAccept(player, targetGuildName);
                break;
            case "reject":
                if (args.length < 3) {
                    String message = languageManager.getMessage(player, "guild.relation.reject.usage", "&c用法: /guild relation reject <公会名称>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                targetGuildName = args[2];
                handleRelationReject(player, targetGuildName);
                break;
            default:
                String message = languageManager.getMessage(player, "guild.relation.invalid-subcommand", "&c无效的子命令！");
                player.sendMessage(ColorUtils.colorize(message));
                break;
        }
    }
    
    private void handleRelationList(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                List<GuildRelation> relations = plugin.getGuildService().getGuildRelationsAsync(guild.getId()).join();
                if (relations.isEmpty()) {
                    String message = languageManager.getMessage(player, "guild.relation.no-relations", "&c公会没有任何关系！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                String message = languageManager.getMessage(player, "guild.relation.list.title", "&a公会关系列表：");
                player.sendMessage(ColorUtils.colorize(message));
                
                for (GuildRelation relation : relations) {
                    Guild targetGuild = guildService.getGuildById(relation.getOtherGuildId(guild.getId()));
                    if (targetGuild != null) {
                        String relationMessage = languageManager.getMessage(player, "guild.relation.list.item", "&b{0} - &f{1}");
                        relationMessage = relationMessage.replace("{0}", targetGuild.getName());
                        relationMessage = relationMessage.replace("{1}", relation.getType().name());
                        player.sendMessage(ColorUtils.colorize(relationMessage));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.relation.error", "&c获取公会关系时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleRelationCreate(Player player, String targetGuildName, String relationType) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.relation.no-permission", "&c您没有管理公会关系的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getMessage(player, "guild.relation.guild-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (targetGuild.getId() == guild.getId()) {
                    String message = languageManager.getMessage(player, "guild.relation.cannot-relate-self", "&c您不能与自己的公会建立关系！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查是否已存在关系
                GuildRelation existingRelation = plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId()).join();
                if (existingRelation != null) {
                    String message = languageManager.getMessage(player, "guild.relation.already-exists", "&c与该公会的关系已存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 创建关系
                boolean success = plugin.getGuildService().createGuildRelationAsync(guild.getId(), targetGuild.getId(), guild.getName(), targetGuild.getName(), GuildRelation.RelationType.valueOf(relationType.toUpperCase()), player.getUniqueId(), player.getName()).join();
                
                String message = languageManager.getMessage(player, "guild.relation.create.success", "&a关系请求已发送！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.relation.error", "&c创建公会关系时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleRelationDelete(Player player, String targetGuildName) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.relation.no-permission", "&c您没有管理公会关系的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getMessage(player, "guild.relation.guild-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查关系是否存在
                GuildRelation relation = plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId()).join();
                if (relation == null) {
                    String message = languageManager.getMessage(player, "guild.relation.not-found", "&c与该公会的关系不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 删除关系
                boolean success = plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).join();
                
                String message = languageManager.getMessage(player, "guild.relation.delete.success", "&a关系已删除！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.relation.error", "&c删除公会关系时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleRelationAccept(Player player, String targetGuildName) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.relation.no-permission", "&c您没有管理公会关系的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getMessage(player, "guild.relation.guild-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查是否有待处理的关系请求
                GuildRelation relation = plugin.getGuildService().getGuildRelationAsync(targetGuild.getId(), guild.getId()).join();
                if (relation == null || relation.getStatus() != GuildRelation.RelationStatus.PENDING) {
                    String message = languageManager.getMessage(player, "guild.relation.no-pending-request", "&c没有来自该公会的待处理关系请求！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 接受关系请求
                relation.setStatus(GuildRelation.RelationStatus.ACTIVE);
                boolean success = plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE).join();
                
                String message = languageManager.getMessage(player, "guild.relation.accept.success", "&a关系请求已接受！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.relation.error", "&c接受公会关系请求时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleRelationReject(Player player, String targetGuildName) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.relation.no-permission", "&c您没有管理公会关系的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getMessage(player, "guild.relation.guild-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查是否有待处理的关系请求
                GuildRelation relation = plugin.getGuildService().getGuildRelationAsync(targetGuild.getId(), guild.getId()).join();
                if (relation == null || relation.getStatus() != GuildRelation.RelationStatus.PENDING) {
                    String message = languageManager.getMessage(player, "guild.relation.no-pending-request", "&c没有来自该公会的待处理关系请求！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 拒绝关系请求
                boolean success = plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).join();
                
                String message = languageManager.getMessage(player, "guild.relation.reject.success", "&a关系请求已拒绝！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.relation.error", "&c拒绝公会关系请求时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleEconomy(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.economy.usage", "&c用法: /guild economy <info|deposit|withdraw|transfer>");
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
                    String message = languageManager.getMessage(player, "guild.economy.deposit.usage", "&c用法: /guild economy deposit <金额>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    handleDeposit(player, amount);
                } catch (NumberFormatException e) {
                    String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
                break;
            case "withdraw":
                if (args.length < 3) {
                    String message = languageManager.getMessage(player, "guild.economy.withdraw.usage", "&c用法: /guild economy withdraw <金额>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    handleWithdraw(player, amount);
                } catch (NumberFormatException e) {
                    String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
                break;
            case "transfer":
                if (args.length < 4) {
                    String message = languageManager.getMessage(player, "guild.economy.transfer.usage", "&c用法: /guild economy transfer <公会名称> <金额>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                String targetGuildName = args[2];
                try {
                    double amount = Double.parseDouble(args[3]);
                    handleTransfer(player, targetGuildName, amount);
                } catch (NumberFormatException e) {
                    String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
                break;
            default:
                String message = languageManager.getMessage(player, "guild.economy.invalid-subcommand", "&c无效的子命令！");
                player.sendMessage(ColorUtils.colorize(message));
                break;
        }
    }
    
    private void handleEconomyInfo(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.economy.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                double balance = guild.getBalance();
                
                String message = languageManager.getMessage(player, "guild.economy.info", "&a公会经济信息：\n&b余额: &f{0} 金币");
                message = message.replace("{0}", String.format("%.2f", balance));
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.economy.error", "&c获取公会经济信息时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.deposit.usage", "&c用法: /guild deposit <金额>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            handleDeposit(player, amount);
        } catch (NumberFormatException e) {
            String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    private void handleDeposit(Player player, double amount) {
        if (amount <= 0) {
            String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.deposit.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!plugin.getEconomyManager().hasBalance(player, amount)) {
                    String message = languageManager.getMessage(player, "guild.deposit.insufficient-funds", "&c您的余额不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 扣除玩家余额
                if (!plugin.getEconomyManager().withdraw(player, amount)) {
                    String message = languageManager.getMessage(player, "guild.deposit.error", "&c存入金币时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 增加公会余额
                boolean success = plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() + amount).join();
                
                String message = languageManager.getMessage(player, "guild.deposit.success", "&a已成功存入 {0} 金币到公会账户！");
                message = message.replace("{0}", String.format("%.2f", amount));
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.deposit.error", "&c存入金币时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.withdraw.usage", "&c用法: /guild withdraw <金额>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            handleWithdraw(player, amount);
        } catch (NumberFormatException e) {
            String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    private void handleWithdraw(Player player, double amount) {
        if (amount <= 0) {
            String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.withdraw.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.withdraw.only-master", "&c只有会长可以从公会账户提现！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (guild.getBalance() < amount) {
                    String message = languageManager.getMessage(player, "guild.withdraw.insufficient-funds", "&c公会账户余额不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 增加玩家余额
                if (!plugin.getEconomyManager().deposit(player, amount)) {
                    String message = languageManager.getMessage(player, "guild.withdraw.error", "&c提现金币时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 减少公会余额
                plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() - amount).join();
                
                String message = languageManager.getMessage(player, "guild.withdraw.success", "&a已成功从公会账户提现 {0} 金币！");
                message = message.replace("{0}", String.format("%.2f", amount));
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.withdraw.error", "&c提现金币时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleTransfer(Player player, String[] args) {
        if (args.length < 3) {
            String message = languageManager.getMessage(player, "guild.transfer.usage", "&c用法: /guild transfer <玩家名称> <金额>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        try {
            double amount = Double.parseDouble(args[2]);
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer == null) {
                String message = languageManager.getMessage(player, "guild.transfer.player-not-found", "&c目标玩家不在线！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            handleTransfer(player, targetPlayer, amount);
        } catch (NumberFormatException e) {
            String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    private void handleTransfer(Player player, String targetGuildName, double amount) {
        if (amount <= 0) {
            String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild sourceGuild = guildService.getPlayerGuild(player.getUniqueId());
                if (sourceGuild == null) {
                    String message = languageManager.getMessage(player, "guild.transfer.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.transfer.only-master", "&c只有会长可以进行公会间转账！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getMessage(player, "guild.transfer.target-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (sourceGuild.getId() == targetGuild.getId()) {
                    String message = languageManager.getMessage(player, "guild.transfer.same-guild", "&c您不能向自己的公会转账！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (sourceGuild.getBalance() < amount) {
                    String message = languageManager.getMessage(player, "guild.transfer.insufficient-funds", "&c公会账户余额不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 减少源公会余额
                boolean sourceSuccess = plugin.getGuildService().updateGuildBalanceAsync(sourceGuild.getId(), sourceGuild.getBalance() - amount).join();
                
                // 增加目标公会余额
                boolean targetSuccess = plugin.getGuildService().updateGuildBalanceAsync(targetGuild.getId(), targetGuild.getBalance() + amount).join();
                
                String message = languageManager.getMessage(player, "guild.transfer.success", "&a已成功转账 {0} 金币到 {1}！");
                message = message.replace("{0}", String.format("%.2f", amount));
                message = message.replace("{1}", targetGuild.getName());
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.transfer.error", "&c转账时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleTransfer(Player player, Player targetPlayer, double amount) {
        if (amount <= 0) {
            String message = languageManager.getMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.transfer.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.transfer.no-permission", "&c您没有转账的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (guild.getBalance() < amount) {
                    String message = languageManager.getMessage(player, "guild.transfer.insufficient-funds", "&c公会账户余额不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 减少公会余额
                plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() - amount).join();
                
                // 增加目标玩家余额
                if (!plugin.getEconomyManager().deposit(targetPlayer, amount)) {
                    // 如果转账失败，恢复公会余额
                    plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() + amount).join();
                    String message = languageManager.getMessage(player, "guild.transfer.error", "&c转账时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                String message = languageManager.getMessage(player, "guild.transfer.success", "&a已成功转账 {0} 金币给 {1}！");
                message = message.replace("{0}", String.format("%.2f", amount));
                message = message.replace("{1}", targetPlayer.getName());
                player.sendMessage(ColorUtils.colorize(message));
                
                // 通知目标玩家
                String targetMessage = languageManager.getMessage(targetPlayer, "guild.transfer.received", "&a您收到了 {0} 金币的转账！");
                targetMessage = targetMessage.replace("{0}", String.format("%.2f", amount));
                targetPlayer.sendMessage(ColorUtils.colorize(targetMessage));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.transfer.error", "&c转账时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleLogs(Player player, String[] args) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.logs.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getMessage(player, "guild.logs.no-permission", "&c您没有查看日志的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 这里应该显示公会日志
                // 暂时简化处理
                String message = languageManager.getMessage(player, "guild.logs.title", "&a公会日志：");
                player.sendMessage(ColorUtils.colorize(message));
                player.sendMessage(ColorUtils.colorize("&b- 日志功能正在开发中..."));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.logs.error", "&c获取公会日志时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handlePlaceholder(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "guild.placeholder.usage", "&c用法: /guild placeholder <player|guild|rank>");
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
                        String message = languageManager.getMessage(player, "guild.placeholder.player", "&a玩家占位符: &f{0}");
                        message = message.replace("{0}", playerPlaceholder);
                        player.sendMessage(ColorUtils.colorize(message));
                        break;
                    case "guild":
                        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            String message1 = languageManager.getMessage(player, "guild.placeholder.not-in-guild", "&c您不在任何公会中！");
                            player.sendMessage(ColorUtils.colorize(message1));
                            return;
                        }
                        String guildPlaceholder = String.format("{guild_%s}", guild.getName().toLowerCase().replace(" ", "_"));
                        String message2 = languageManager.getMessage(player, "guild.placeholder.guild", "&a公会占位符: &f{0}");
                        message2 = message2.replace("{0}", guildPlaceholder);
                        player.sendMessage(ColorUtils.colorize(message2));
                        break;
                    case "rank":
                        GuildMember member = guildService.getGuildMember(player.getUniqueId());
                        if (member == null) {
                            String message3 = languageManager.getMessage(player, "guild.placeholder.not-in-guild", "&c您不在任何公会中！");
                            player.sendMessage(ColorUtils.colorize(message3));
                            return;
                        }
                        String rankPlaceholder = String.format("{guild_rank_%s}", member.getRole().name().toLowerCase());
                        String message4 = languageManager.getMessage(player, "guild.placeholder.rank", "&a职位占位符: &f{0}");
                        message4 = message4.replace("{0}", rankPlaceholder);
                        player.sendMessage(ColorUtils.colorize(message4));
                        break;
                    default:
                        String message5 = languageManager.getMessage(player, "guild.placeholder.invalid-type", "&c无效的占位符类型！");
                        player.sendMessage(ColorUtils.colorize(message5));
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.placeholder.error", "&c获取占位符时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleTime(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getMessage(player, "guild.time.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.Duration duration = java.time.Duration.between(guild.getCreatedAt(), now);
                long days = duration.toDays();
                long hours = duration.toHours() % 24;
                
                String message = languageManager.getMessage(player, "guild.time.age", "&a公会创建时间：{0}\n&a公会年龄：&f{1} 天 {2} 小时");
                message = message.replace("{0}", guild.getCreatedAt().toString());
                message = message.replace("{1}", String.valueOf(days));
                message = message.replace("{2}", String.valueOf(hours));
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getMessage(player, "guild.time.error", "&c获取公会时间信息时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleHelp(Player player) {
        String message = languageManager.getMessage(player, "guild.help.title", "&a公会命令帮助：");
        player.sendMessage(ColorUtils.colorize(message));
        
        player.sendMessage(ColorUtils.colorize("&b/guild &f- 打开公会主菜单"));
        player.sendMessage(ColorUtils.colorize("&b/guild create <名称> &f- 创建公会"));
        player.sendMessage(ColorUtils.colorize("&b/guild info &f- 查看公会信息"));
        player.sendMessage(ColorUtils.colorize("&b/guild members &f- 查看公会成员"));
        player.sendMessage(ColorUtils.colorize("&b/guild invite <玩家> &f- 邀请玩家加入公会"));
        player.sendMessage(ColorUtils.colorize("&b/guild kick <玩家> &f- 踢出公会成员"));
        player.sendMessage(ColorUtils.colorize("&b/guild promote <玩家> &f- 提升成员职位"));
        player.sendMessage(ColorUtils.colorize("&b/guild demote <玩家> &f- 降级成员职位"));
        player.sendMessage(ColorUtils.colorize("&b/guild accept <公会> &f- 接受公会邀请"));
        player.sendMessage(ColorUtils.colorize("&b/guild decline <公会> &f- 拒绝公会邀请"));
        player.sendMessage(ColorUtils.colorize("&b/guild leave &f- 离开公会"));
        player.sendMessage(ColorUtils.colorize("&b/guild delete &f- 删除公会"));
        player.sendMessage(ColorUtils.colorize("&b/guild sethome &f- 设置公会 home"));
        player.sendMessage(ColorUtils.colorize("&b/guild home &f- 传送到公会 home"));
        player.sendMessage(ColorUtils.colorize("&b/guild relation <list|create|delete|accept|reject> &f- 管理公会关系"));
        player.sendMessage(ColorUtils.colorize("&b/guild economy <info|deposit|withdraw|transfer> &f- 管理公会经济"));
        player.sendMessage(ColorUtils.colorize("&b/guild deposit <金额> &f- 存入金币到公会"));
        player.sendMessage(ColorUtils.colorize("&b/guild withdraw <金额> &f- 从公会提现金币"));
        player.sendMessage(ColorUtils.colorize("&b/guild transfer <玩家|公会> <金额> &f- 转账"));
        player.sendMessage(ColorUtils.colorize("&b/guild logs &f- 查看公会日志"));
        player.sendMessage(ColorUtils.colorize("&b/guild placeholder <player|guild|rank> &f- 获取占位符"));
        player.sendMessage(ColorUtils.colorize("&b/guild time &f- 查看公会时间信息"));
        player.sendMessage(ColorUtils.colorize("&b/guild help &f- 查看此帮助"));
    }
}