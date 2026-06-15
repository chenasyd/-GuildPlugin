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
import com.guild.core.utils.ScheduledTaskHandle;
import com.guild.core.utils.ServerUtils;
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
            String msg = languageManager.getCoreMessage("general.player-only", "&c此命令只能由玩家执行！");
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
            case "chat":
            case "c":
                handleChat(player, args);
                break;
            default:
                // 检查是否为模块注册的子命令
                if (api.hasSubCommand("guild", args[0])) {
                    // 检查权限
                    String permission = api.getSubCommandPermission("guild", args[0]);
                    if (permission != null && !plugin.getPermissionManager().hasPermission(player, permission)) {
                        String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
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
                
                player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "general.unknown-command", "&c未知命令！使用 /guild help 查看帮助。")));
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList(
                "create", "info", "members", "invite", "kick", "promote", "demote", "accept", "decline", "leave", "delete", "sethome", "home", "relation", "economy", "deposit", "withdraw", "transfer", "logs", "placeholder", "time", "help", "chat"
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
            String message = languageManager.getCoreMessage(player, "guild.create.usage", "&c用法: /guild create <公会名称> [标签] [描述]");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.create")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
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
            String message = languageManager.getCoreMessage(player, "guild.create.name-required", "&c请输入公会名称！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (guildName.length() < minNameLength || guildName.length() > maxNameLength) {
            String message = languageManager.getCoreMessage(player, "guild.create.name-length", "&c公会名称长度必须在" + minNameLength + "-" + maxNameLength + "个字符之间！");
            message = message.replace("{min}", String.valueOf(minNameLength)).replace("{max}", String.valueOf(maxNameLength));
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 标签验证（空字符串视为未设置，传递 null）
        if (guildTag != null && !guildTag.isEmpty()) {
            if (guildTag.length() > maxTagLength) {
                String message = languageManager.getCoreMessage(player, "guild.create.tag-too-long", "&c公会标签最多 {max} 个字符！");
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
                String message = languageManager.getCoreMessage(player, "guild.create.description-too-long", "&c公会描述不能超过 {max} 个字符！");
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
                    String message = languageManager.getCoreMessage(player, "create.already-in-guild", "&c您已经在一个公会中，无法创建新公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 经济系统检查
                boolean vaultAvailable = plugin.getEconomyManager().isVaultAvailable();
                boolean noEconomyMode = plugin.getEconomyManager().isNoEconomyMode();
                
                if (!vaultAvailable && !noEconomyMode) {
                    String message = languageManager.getCoreMessage(player, "guild.create.economy-not-available", "&c经济系统不可用，无法创建公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 获取创建费用（无经济模式下费用为0）
                double creationCost = vaultAvailable
                    ? plugin.getConfigManager().getMainConfig().getDouble("guild.creation-cost", 1000.0)
                    : 0.0;
                
                // 仅在有经济系统时检查余额并扣费
                if (vaultAvailable && !noEconomyMode) {
                    if (!plugin.getEconomyManager().hasBalance(player, creationCost)) {
                        String message = languageManager.getCoreMessage(player, "guild.create.insufficient-funds", "&c您的余额不足！创建公会需要 {amount} 金币！");
                        message = message.replace("{amount}", plugin.getEconomyManager().format(creationCost));
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }
                    
                    if (!plugin.getEconomyManager().withdraw(player, creationCost)) {
                        String message = languageManager.getCoreMessage(player, "guild.create.payment-failed", "&c扣除创建费用失败！");
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }
                }
                
                final double finalCost = creationCost;
                boolean success = guildService.createGuild(guildName, finalTag, finalDescription, player.getUniqueId(), player.getName());
                if (success) {
                    String message = languageManager.getCoreMessage(player, "guild.create.success", "&a公会创建成功！");
                    player.sendMessage(ColorUtils.colorize(message));
                    
                    // 打开公会信息GUI
                    MainGuildGUI mainGuildGUI = new MainGuildGUI(plugin, player);
                    plugin.getGuiManager().openGUI(player, mainGuildGUI);
                } else {
                    // 如果创建失败且有扣费，退还费用
                    if (vaultAvailable && !noEconomyMode && finalCost > 0) {
                        plugin.getEconomyManager().deposit(player, finalCost);
                        String refundMessage = languageManager.getCoreMessage(player, "guild.create.payment-refunded", "&e已退还创建费用 {amount}。");
                        refundMessage = refundMessage.replace("{amount}", plugin.getEconomyManager().format(finalCost));
                        player.sendMessage(ColorUtils.colorize(refundMessage));
                    }
                    
                    String message = languageManager.getCoreMessage(player, "guild.create.exists", "&c该公会名称已存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.create.error", "&c创建公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleInfo(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.info.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                String message = languageManager.getCoreMessage(player, "guild.info.message", "&a公会信息：\n&b名称: &f{0}\n&b等级: &f{1}\n&b会长: &f{2}\n&b成员数量: &f{3}\n&b创建时间: &f{4}");
                message = message.replace("{0}", guild.getName());
                message = message.replace("{1}", String.valueOf(guild.getLevel()));
                message = message.replace("{2}", guild.getLeaderName());
                message = message.replace("{3}", String.valueOf(guildService.getGuildMemberCount(guild.getId())));
                message = message.replace("{4}", guild.getCreatedAt().toString());
                
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.info.error", "&c获取公会信息时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleMembers(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.members.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                List<GuildMember> members = guildService.getGuildMembers(guild.getId());
                if (members.isEmpty()) {
                    String message = languageManager.getCoreMessage(player, "guild.members.empty", "&c公会中没有成员！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                String message = languageManager.getCoreMessage(player, "guild.members.title", "&a公会成员列表：");
                player.sendMessage(ColorUtils.colorize(message));
                
                for (GuildMember m : members) {
                    String memberMessage = languageManager.getCoreMessage(player, "guild.members.member", "&b{0} - &f{1}");
                    memberMessage = memberMessage.replace("{0}", m.getPlayerName());
                    memberMessage = memberMessage.replace("{1}", m.getRole() == Role.LEADER ? "会长" : (m.getRole() == Role.OFFICER ? "副会长" : "成员"));
                    player.sendMessage(ColorUtils.colorize(memberMessage));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.members.error", "&c获取成员列表时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.invite.usage", "&c用法: /guild invite <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.invite")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getCoreMessage(player, "guild.invite.player-not-found", "&c玩家不在线！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = languageManager.getCoreMessage(player, "guild.invite.self", "&c您不能邀请自己！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.invite.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.invite.no-permission", "&c您没有邀请成员的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getPlayerGuild(targetPlayer.getUniqueId());
                if (targetGuild != null) {
                    String message = languageManager.getCoreMessage(player, "guild.invite.already-in-guild", "&c该玩家已经在一个公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查公会成员数量是否达到上限
                int memberCount = guildService.getGuildMemberCount(guild.getId());
                int maxMembers = guild.getMaxMembers();
                if (memberCount >= maxMembers) {
                    String message = languageManager.getCoreMessage(player, "guild.invite.full", "&c公会成员已满！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 发送邀请
                String inviteMessage = InviteMessageUtils.formatInviteReceived(plugin, targetPlayer, player, guild);
                targetPlayer.sendMessage(inviteMessage);
                
                String message = languageManager.getCoreMessage(player, "guild.invite.success", "&a邀请已发送！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.invite.error", "&c发送邀请时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.kick.usage", "&c用法: /guild kick <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.kick")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getCoreMessage(player, "guild.kick.player-not-found", "&c玩家不在线！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.kick.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.kick.no-permission", "&c您没有踢出成员的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
                if (targetMember == null) {
                    String message = languageManager.getCoreMessage(player, "guild.kick.player-not-found", "&c该玩家不在公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (targetMember.getRole() == Role.LEADER) {
                    String message = languageManager.getCoreMessage(player, "guild.kick.cannot-kick-master", "&c您不能踢出会长！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                boolean success = guildService.removeGuildMember(targetPlayer.getUniqueId(), player.getUniqueId());
                if (success) {
                    String message = languageManager.getCoreMessage(player, "guild.kick.success", "&a已成功踢出玩家！");
                    player.sendMessage(ColorUtils.colorize(message));
                    
                    // 通知被踢出的玩家
                    String kickMessage = languageManager.getCoreMessage(targetPlayer, "guild.kick.kicked", "&c您已被踢出公会！");
                    targetPlayer.sendMessage(ColorUtils.colorize(kickMessage));
                } else {
                    String message = languageManager.getCoreMessage(player, "guild.kick.error", "&c踢出玩家时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.kick.error", "&c踢出玩家时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.promote.usage", "&c用法: /guild promote <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.promote")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getCoreMessage(player, "guild.promote.player-not-found", "&c玩家不在线！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.promote.only-master", "&c只有会长可以提升成员！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
                if (targetMember == null) {
                    String message = languageManager.getCoreMessage(player, "guild.promote.player-not-found", "&c该玩家不在公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (targetMember.getRole() == Role.LEADER) {
                    String message = languageManager.getCoreMessage(player, "guild.promote.already-master", "&c该玩家已经是会长！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 提升为副会长
                boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), Role.OFFICER, player.getUniqueId());
                if (success) {
                    String message = languageManager.getCoreMessage(player, "guild.promote.success", "&a已成功提升玩家为副会长！");
                    player.sendMessage(ColorUtils.colorize(message));
                    
                    // 通知被提升的玩家
                    String promoteMessage = languageManager.getCoreMessage(targetPlayer, "guild.promote.promoted", "&a您已被提升为副会长！");
                    targetPlayer.sendMessage(ColorUtils.colorize(promoteMessage));
                } else {
                    String message = languageManager.getCoreMessage(player, "guild.promote.error", "&c提升玩家时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.promote.error", "&c提升玩家时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.demote.usage", "&c用法: /guild demote <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getPermissionManager().hasPermission(player, "guild.demote")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            String message = languageManager.getCoreMessage(player, "guild.demote.player-not-found", "&c玩家不在线！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.demote.only-master", "&c只有会长可以降级成员！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
                if (targetMember == null) {
                    String message = languageManager.getCoreMessage(player, "guild.demote.player-not-found", "&c该玩家不在公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (targetMember.getRole() == Role.LEADER) {
                    String message = languageManager.getCoreMessage(player, "guild.demote.cannot-demote-master", "&c您不能降级会长！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 降级为普通成员
                boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), Role.MEMBER, player.getUniqueId());
                if (success) {
                    String message = languageManager.getCoreMessage(player, "guild.demote.success", "&a已成功降级玩家为普通成员！");
                    player.sendMessage(ColorUtils.colorize(message));
                    
                    // 通知被降级的玩家
                    String demoteMessage = languageManager.getCoreMessage(targetPlayer, "guild.demote.demoted", "&c您已被降级为普通成员！");
                    targetPlayer.sendMessage(ColorUtils.colorize(demoteMessage));
                } else {
                    String message = languageManager.getCoreMessage(player, "guild.demote.error", "&c降级玩家时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.demote.error", "&c降级玩家时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.accept.usage", "&c用法: /guild accept <公会名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replaceAll("[\"']", "").trim();
        
        guildService.getPlayerGuildAsync(player.getUniqueId()).thenAccept(existingGuild -> {
            if (existingGuild != null) {
                String message = languageManager.getCoreMessage(player, "guild.accept.already-in-guild", "&c您已经在一个公会中！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            guildService.getGuildByNameAsync(guildName).thenAccept(guild -> {
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.accept.guild-not-found", "&c公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查玩家是否有该公会的有效邀请
                guildService.getPendingInvitationAsync(player.getUniqueId(), guild.getId()).thenAccept(invitation -> {
                    if (invitation == null) {
                        plugin.getLogger().warning("[Accept-Debug] 玩家 " + player.getName() + " 没有来自 " + guild.getName() + " 的邀请");
                        String message = languageManager.getCoreMessage(player, "guild.accept.no-invitation", "&c您没有该公会的邀请或邀请已过期！");
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }
                    
                    plugin.getLogger().info("[Accept-Debug] 找到邀请 ID=" + invitation.getId() + " 从 " + invitation.getInviterName() + " 到 " + invitation.getTargetName());
                    
                    // 处理邀请接受
                    guildService.processInvitationDirectAsync(invitation, true).thenAccept(success -> {
                        if (success) {
                            plugin.getLogger().info("[Accept-Debug] 邀请处理成功，玩家 " + player.getName() + " 已加入 " + guild.getName());
                            String message = languageManager.getCoreMessage(player, "guild.accept.success", "&a已成功加入公会！");
                            player.sendMessage(ColorUtils.colorize(message));
                            
                            // 通知邀请者
                            NotifyUtils.notifyInviterInvitationProcessed(plugin, invitation.getInviterUuid(), 
                                invitation.getInviterName(), player.getName(), guild, true);
                        } else {
                            plugin.getLogger().warning("[Accept-Debug] 邀请处理失败，邀请ID=" + invitation.getId());
                            String message = languageManager.getCoreMessage(player, "guild.accept.error", "&c加入公会时发生错误！");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                    });
                });
            });
        });
    }
    
    private void handleDecline(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.decline.usage", "&c用法: /guild decline <公会名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replaceAll("[\"']", "").trim();
        
        guildService.getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                String message = languageManager.getCoreMessage(player, "guild.decline.guild-not-found", "&c公会不存在！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 检查玩家是否有该公会的有效邀请
            guildService.getPendingInvitationAsync(player.getUniqueId(), guild.getId()).thenAccept(invitation -> {
                if (invitation == null) {
                    String message = languageManager.getCoreMessage(player, "guild.decline.no-invitation", "&c您没有该公会的邀请或邀请已过期！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 处理邀请拒绝
                guildService.processInvitationDirectAsync(invitation, false).thenAccept(success -> {
                    if (success) {
                        String message = languageManager.getCoreMessage(player, "guild.decline.success", "&a已拒绝加入公会！");
                        player.sendMessage(ColorUtils.colorize(message));
                        
                        // 通知邀请者
                        NotifyUtils.notifyInviterInvitationProcessed(plugin, invitation.getInviterUuid(), 
                            invitation.getInviterName(), player.getName(), guild, false);
                    } else {
                        String message = languageManager.getCoreMessage(player, "guild.decline.error", "&c拒绝邀请时发生错误！");
                        player.sendMessage(ColorUtils.colorize(message));
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
                    String message = languageManager.getCoreMessage(player, "guild.leave.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (member.getRole() == Role.LEADER) {
                    String message = languageManager.getCoreMessage(player, "guild.leave.cannot-leave-as-master", "&c会长不能离开公会，请先转让会长或删除公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                boolean success = guildService.removeGuildMember(player.getUniqueId(), player.getUniqueId());
                if (success) {
                    String message = languageManager.getCoreMessage(player, "guild.leave.success", "&a已成功离开公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = languageManager.getCoreMessage(player, "guild.leave.error", "&c离开公会时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.leave.error", "&c离开公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDelete(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.delete.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.delete.only-master", "&c只有会长可以删除公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 打开确认删除GUI
                ConfirmDeleteGuildGUI confirmGUI = new ConfirmDeleteGuildGUI(plugin, guild, player);
                plugin.getGuiManager().openGUI(player, confirmGUI);
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.delete.error", "&c删除公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDeleteConfirm(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.delete.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.delete.only-master", "&c只有会长可以删除公会！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                boolean success = guildService.deleteGuild(guild.getId(), player.getUniqueId());
                if (success) {
                    String message = languageManager.getCoreMessage(player, "guild.delete.success", "&a公会已成功删除！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = languageManager.getCoreMessage(player, "guild.delete.error", "&c删除公会时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.delete.error", "&c删除公会时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDeleteCancel(Player player) {
        String message = languageManager.getCoreMessage(player, "guild.delete.cancel", "&a已取消删除公会！");
        player.sendMessage(ColorUtils.colorize(message));
    }
    
    private void handleSetHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.sethome")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.sethome.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.sethome.no-permission", "&c您没有设置公会 home 的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 设置公会 home 位置
                plugin.getGuildService().setGuildHome(guild.getId(), player.getLocation(), player.getUniqueId());
                
                String message = languageManager.getCoreMessage(player, "guild.sethome.success", "&a公会 home 位置已设置！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.sethome.error", "&c设置公会 home 位置时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.home")) {
            String message = languageManager.getCoreMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Folia 环境下传送功能因线程隔离无法使用，直接禁用
        if (ServerUtils.isFolia()) {
            String message = languageManager.getCoreMessage(player, "home.folia-disabled", "&c传送功能在Folia环境下暂不可用！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 校验玩家是否为公会成员
        com.guild.models.GuildMember member = guildService.getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = languageManager.getCoreMessage(player, "guild.home.not-in-guild", "&c您不在任何公会中！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getCoreMessage(player, "guild.home.not-in-guild", "&c您不在任何公会中！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        plugin.getGuildService().getGuildHomeAsync(guild.getId()).thenAccept(location -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (location != null) {
                    startHomeTeleportDelay(player, location);
                } else {
                    String message = languageManager.getCoreMessage(player, "home.not-set", "&c工会家未设置！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void doHomeTeleport(Player player, org.bukkit.Location targetLocation) {
        player.teleport(targetLocation);
        String message = languageManager.getCoreMessage(player, "home.success", "&a已传送到工会家！");
        player.sendMessage(ColorUtils.colorize(message));
    }

    private void startHomeTeleportDelay(Player player, org.bukkit.Location targetLocation) {
        int delay = plugin.getConfigManager().getMainConfig().getInt("guild.home-teleport-delay", 0);
        if (delay <= 0) {
            doHomeTeleport(player, targetLocation);
            return;
        }

        org.bukkit.Location startLocation = player.getLocation().clone();
        org.bukkit.World startWorld = startLocation.getWorld();
        int[] countdown = {delay};
        boolean[] done = {false};
        ScheduledTaskHandle[] handleRef = new ScheduledTaskHandle[1];

        handleRef[0] = com.guild.core.utils.CompatibleScheduler.runTaskTimer(plugin, player, () -> {
            if (!player.isOnline() || done[0]) {
                handleRef[0].cancel();
                return;
            }
            // Folia safety: check world first before distanceSquared to avoid IllegalArgumentException
            if (!startWorld.equals(player.getWorld())
                    || player.getLocation().distanceSquared(startLocation) > 0.5) {
                done[0] = true;
                String cancelled = languageManager.getCoreMessage(player, "home.teleport-cancelled",
                    "&c传送已取消（请不要移动）！");
                player.sendMessage(ColorUtils.colorize(cancelled));
                handleRef[0].cancel();
                return;
            }
            if (countdown[0] <= 0) {
                done[0] = true;
                handleRef[0].cancel();
                doHomeTeleport(player, targetLocation);
            } else {
                String msg = languageManager.getCoreMessage(player, "home.teleporting",
                    "&a正在传送 &e{seconds} &a秒", "{seconds}", String.valueOf(countdown[0]));
                com.guild.util.NotifyUtils.sendActionBar(plugin, player, msg);
                countdown[0]--;
            }
        }, 0L, 20L);
    }
    
    private void handleRelation(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.relation.usage", "&c用法: /guild relation <list|create|delete|accept|reject>");
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
                    String message = languageManager.getCoreMessage(player, "guild.relation.create.usage", "&c用法: /guild relation create <公会名称> <关系类型>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                String targetGuildName = args[2];
                String relationType = args.length >= 4 ? args[3] : "alliance";
                handleRelationCreate(player, targetGuildName, relationType);
                break;
            case "delete":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.delete.usage", "&c用法: /guild relation delete <公会名称>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                targetGuildName = args[2];
                handleRelationDelete(player, targetGuildName);
                break;
            case "accept":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.accept.usage", "&c用法: /guild relation accept <公会名称>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                targetGuildName = args[2];
                handleRelationAccept(player, targetGuildName);
                break;
            case "reject":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.reject.usage", "&c用法: /guild relation reject <公会名称>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                targetGuildName = args[2];
                handleRelationReject(player, targetGuildName);
                break;
            default:
                String message = languageManager.getCoreMessage(player, "guild.relation.invalid-subcommand", "&c无效的子命令！");
                player.sendMessage(ColorUtils.colorize(message));
                break;
        }
    }
    
    private void handleRelationList(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                List<GuildRelation> relations = plugin.getGuildService().getGuildRelationsAsync(guild.getId()).join();
                if (relations.isEmpty()) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.no-relations", "&c公会没有任何关系！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                String message = languageManager.getCoreMessage(player, "guild.relation.list.title", "&a公会关系列表：");
                player.sendMessage(ColorUtils.colorize(message));
                
                for (GuildRelation relation : relations) {
                    Guild targetGuild = guildService.getGuildById(relation.getOtherGuildId(guild.getId()));
                    if (targetGuild != null) {
                        String relationMessage = languageManager.getCoreMessage(player, "guild.relation.list.item", "&b{0} - &f{1}");
                        relationMessage = relationMessage.replace("{0}", targetGuild.getName());
                        relationMessage = relationMessage.replace("{1}", relation.getType().name());
                        player.sendMessage(ColorUtils.colorize(relationMessage));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.relation.error", "&c获取公会关系时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleRelationCreate(Player player, String targetGuildName, String relationType) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.no-permission", "&c您没有管理公会关系的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.guild-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (targetGuild.getId() == guild.getId()) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.cannot-relate-self", "&c您不能与自己的公会建立关系！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查是否已存在关系
                GuildRelation existingRelation = plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId()).join();
                if (existingRelation != null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.already-exists", "&c与该公会的关系已存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 创建关系
                boolean success = plugin.getGuildService().createGuildRelationAsync(guild.getId(), targetGuild.getId(), guild.getName(), targetGuild.getName(), GuildRelation.RelationType.valueOf(relationType.toUpperCase()), player.getUniqueId(), player.getName()).join();
                
                String message = languageManager.getCoreMessage(player, "guild.relation.create.success", "&a关系请求已发送！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.relation.error", "&c创建公会关系时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleRelationDelete(Player player, String targetGuildName) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.no-permission", "&c您没有管理公会关系的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.guild-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查关系是否存在
                GuildRelation relation = plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId()).join();
                if (relation == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.not-found", "&c与该公会的关系不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 删除关系
                boolean success = plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).join();
                
                String message = languageManager.getCoreMessage(player, "guild.relation.delete.success", "&a关系已删除！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.relation.error", "&c删除公会关系时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleRelationAccept(Player player, String targetGuildName) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.no-permission", "&c您没有管理公会关系的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.guild-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查是否有待处理的关系请求
                GuildRelation relation = plugin.getGuildService().getGuildRelationAsync(targetGuild.getId(), guild.getId()).join();
                if (relation == null || relation.getStatus() != GuildRelation.RelationStatus.PENDING) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.no-pending-request", "&c没有来自该公会的待处理关系请求！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 接受关系请求
                relation.setStatus(GuildRelation.RelationStatus.ACTIVE);
                boolean success = plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE).join();
                
                String message = languageManager.getCoreMessage(player, "guild.relation.accept.success", "&a关系请求已接受！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.relation.error", "&c接受公会关系请求时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleRelationReject(Player player, String targetGuildName) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.no-permission", "&c您没有管理公会关系的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.guild-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查是否有待处理的关系请求
                GuildRelation relation = plugin.getGuildService().getGuildRelationAsync(targetGuild.getId(), guild.getId()).join();
                if (relation == null || relation.getStatus() != GuildRelation.RelationStatus.PENDING) {
                    String message = languageManager.getCoreMessage(player, "guild.relation.no-pending-request", "&c没有来自该公会的待处理关系请求！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 拒绝关系请求
                boolean success = plugin.getGuildService().deleteGuildRelationAsync(relation.getId()).join();
                
                String message = languageManager.getCoreMessage(player, "guild.relation.reject.success", "&a关系请求已拒绝！");
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.relation.error", "&c拒绝公会关系请求时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleEconomy(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.economy.usage", "&c用法: /guild economy <info|deposit|withdraw|transfer>");
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
                    String message = languageManager.getCoreMessage(player, "guild.economy.deposit.usage", "&c用法: /guild economy deposit <金额>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    handleDeposit(player, amount);
                } catch (NumberFormatException e) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
                break;
            case "withdraw":
                if (args.length < 3) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.withdraw.usage", "&c用法: /guild economy withdraw <金额>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    handleWithdraw(player, amount);
                } catch (NumberFormatException e) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
                break;
            case "transfer":
                if (args.length < 4) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.transfer.usage", "&c用法: /guild economy transfer <公会名称> <金额>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                String targetGuildName = args[2];
                try {
                    double amount = Double.parseDouble(args[3]);
                    handleTransfer(player, targetGuildName, amount);
                } catch (NumberFormatException e) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
                break;
            default:
                String message = languageManager.getCoreMessage(player, "guild.economy.invalid-subcommand", "&c无效的子命令！");
                player.sendMessage(ColorUtils.colorize(message));
                break;
        }
    }
    
    private void handleEconomyInfo(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.economy.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                double balance = guild.getBalance();
                
                String message = languageManager.getCoreMessage(player, "guild.economy.info", "&a公会经济信息：\n&b余额: &f{0} 金币");
                message = message.replace("{0}", String.format("%.2f", balance));
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.economy.error", "&c获取公会经济信息时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.deposit.usage", "&c用法: /guild deposit <金额>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            handleDeposit(player, amount);
        } catch (NumberFormatException e) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    private void handleDeposit(Player player, double amount) {
        if (amount <= 0) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.deposit.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!plugin.getEconomyManager().hasBalance(player, amount)) {
                    String message = languageManager.getCoreMessage(player, "guild.deposit.insufficient-funds", "&c您的余额不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 扣除玩家余额
                if (!plugin.getEconomyManager().withdraw(player, amount)) {
                    String message = languageManager.getCoreMessage(player, "guild.deposit.error", "&c存入金币时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 增加公会余额
                boolean success = plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() + amount).join();
                if (success) {
                    // 记录投资
                    plugin.getGuildInvestmentService().recordDeposit(guild.getId(), player.getUniqueId(), player.getName(), amount);
                    // 写入 guild_contributions 表（供 GuildFundsGUI 展示）
                    plugin.getGuildService().addGuildContributionAsync(guild.getId(), player.getUniqueId(),
                            player.getName(), amount,
                            com.guild.models.GuildContribution.ContributionType.DEPOSIT,
                            languageManager.getCoreMessage(player, "deposit.contribution-desc",
                                    "{player}存入{amount}")
                                    .replace("{player}", player.getName())
                                    .replace("{amount}", String.format("%.2f", amount)));
                    // 写入 guild_logs 表（供 GuildLogsGUI 展示，记录真实操作者）
                    plugin.getGuildService().logGuildActionAsync(guild.getId(), guild.getName(),
                            player.getUniqueId().toString(), player.getName(),
                            com.guild.models.GuildLog.LogType.FUND_DEPOSITED,
                            languageManager.getCoreMessage(player, "deposit.log-desc",
                                    "{player}存入了{amount}")
                                    .replace("{player}", player.getName())
                                    .replace("{amount}", String.format("%.2f", amount)),
                            languageManager.getCoreMessage(player, "deposit.log-details",
                                    "金额:{amount}")
                                    .replace("{amount}", String.format("%.2f", amount)));
                    // 分发存款事件给模块
                    plugin.getGuildService().notifyEconomyDeposit(guild.getId(), guild.getName(), player.getUniqueId(), player.getName(), amount);
                }
                
                String message = languageManager.getCoreMessage(player, "guild.deposit.success", "&a已成功存入 {0} 金币到公会账户！");
                message = message.replace("{0}", String.format("%.2f", amount));
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.deposit.error", "&c存入金币时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.withdraw.usage", "&c用法: /guild withdraw <金额>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            handleWithdraw(player, amount);
        } catch (NumberFormatException e) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    private void handleWithdraw(Player player, double amount) {
        if (amount <= 0) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.withdraw.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.withdraw.only-master", "&c只有会长可以从公会账户提现！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (guild.getBalance() < amount) {
                    String message = languageManager.getCoreMessage(player, "guild.withdraw.insufficient-funds", "&c公会账户余额不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 增加玩家余额
                if (!plugin.getEconomyManager().deposit(player, amount)) {
                    String message = languageManager.getCoreMessage(player, "guild.withdraw.error", "&c提现金币时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 减少公会余额
                plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() - amount).join();
                // 记录取款
                plugin.getGuildInvestmentService().recordWithdraw(guild.getId(), player.getUniqueId(), amount);
                // 分发取款事件给模块
                plugin.getGuildService().notifyEconomyWithdraw(guild.getId(), guild.getName(), player.getUniqueId(), player.getName(), amount);
                
                String message = languageManager.getCoreMessage(player, "guild.withdraw.success", "&a已成功从公会账户提现 {0} 金币！");
                message = message.replace("{0}", String.format("%.2f", amount));
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.withdraw.error", "&c提现金币时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleTransfer(Player player, String[] args) {
        if (args.length < 3) {
            String message = languageManager.getCoreMessage(player, "guild.transfer.usage", "&c用法: /guild transfer <玩家名称> <金额>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetName = args[1];
        try {
            double amount = Double.parseDouble(args[2]);
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer == null) {
                String message = languageManager.getCoreMessage(player, "guild.transfer.player-not-found", "&c目标玩家不在线！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            handleTransfer(player, targetPlayer, amount);
        } catch (NumberFormatException e) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    private void handleTransfer(Player player, String targetGuildName, double amount) {
        if (amount <= 0) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild sourceGuild = guildService.getPlayerGuild(player.getUniqueId());
                if (sourceGuild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.isGuildLeader(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.only-master", "&c只有会长可以进行公会间转账！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                Guild targetGuild = guildService.getGuildByName(targetGuildName);
                if (targetGuild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.target-not-found", "&c目标公会不存在！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (sourceGuild.getId() == targetGuild.getId()) {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.same-guild", "&c您不能向自己的公会转账！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (sourceGuild.getBalance() < amount) {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.insufficient-funds", "&c公会账户余额不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 减少源公会余额
                boolean sourceSuccess = plugin.getGuildService().updateGuildBalanceAsync(sourceGuild.getId(), sourceGuild.getBalance() - amount).join();
                
                // 增加目标公会余额
                boolean targetSuccess = plugin.getGuildService().updateGuildBalanceAsync(targetGuild.getId(), targetGuild.getBalance() + amount).join();
                
                String message = languageManager.getCoreMessage(player, "guild.transfer.success", "&a已成功转账 {0} 金币到 {1}！");
                message = message.replace("{0}", String.format("%.2f", amount));
                message = message.replace("{1}", targetGuild.getName());
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.transfer.error", "&c转账时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleTransfer(Player player, Player targetPlayer, double amount) {
        if (amount <= 0) {
            String message = languageManager.getCoreMessage(player, "guild.economy.invalid-amount", "&c无效的金额！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.no-permission", "&c您没有转账的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (guild.getBalance() < amount) {
                    String message = languageManager.getCoreMessage(player, "guild.transfer.insufficient-funds", "&c公会账户余额不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 减少公会余额
                plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() - amount).join();
                
                // 增加目标玩家余额
                if (!plugin.getEconomyManager().deposit(targetPlayer, amount)) {
                    // 如果转账失败，恢复公会余额
                    plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() + amount).join();
                    String message = languageManager.getCoreMessage(player, "guild.transfer.error", "&c转账时发生错误！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                String message = languageManager.getCoreMessage(player, "guild.transfer.success", "&a已成功转账 {0} 金币给 {1}！");
                message = message.replace("{0}", String.format("%.2f", amount));
                message = message.replace("{1}", targetPlayer.getName());
                player.sendMessage(ColorUtils.colorize(message));
                
                // 通知目标玩家
                String targetMessage = languageManager.getCoreMessage(targetPlayer, "guild.transfer.received", "&a您收到了 {0} 金币的转账！");
                targetMessage = targetMessage.replace("{0}", String.format("%.2f", amount));
                targetPlayer.sendMessage(ColorUtils.colorize(targetMessage));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.transfer.error", "&c转账时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleLogs(Player player, String[] args) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.logs.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                if (!guildService.hasGuildPermission(player.getUniqueId())) {
                    String message = languageManager.getCoreMessage(player, "guild.logs.no-permission", "&c您没有查看日志的权限！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 这里应该显示公会日志
                // 暂时简化处理
                String message = languageManager.getCoreMessage(player, "guild.logs.title", "&a公会日志：");
                player.sendMessage(ColorUtils.colorize(message));
                player.sendMessage(ColorUtils.colorize("&b- 日志功能正在开发中..."));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.logs.error", "&c获取公会日志时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handlePlaceholder(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getCoreMessage(player, "guild.placeholder.usage", "&c用法: /guild placeholder <player|guild|rank>");
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
                        String message = languageManager.getCoreMessage(player, "guild.placeholder.player", "&a玩家占位符: &f{0}");
                        message = message.replace("{0}", playerPlaceholder);
                        player.sendMessage(ColorUtils.colorize(message));
                        break;
                    case "guild":
                        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            String message1 = languageManager.getCoreMessage(player, "guild.placeholder.not-in-guild", "&c您不在任何公会中！");
                            player.sendMessage(ColorUtils.colorize(message1));
                            return;
                        }
                        String guildPlaceholder = String.format("{guild_%s}", guild.getName().toLowerCase().replace(" ", "_"));
                        String message2 = languageManager.getCoreMessage(player, "guild.placeholder.guild", "&a公会占位符: &f{0}");
                        message2 = message2.replace("{0}", guildPlaceholder);
                        player.sendMessage(ColorUtils.colorize(message2));
                        break;
                    case "rank":
                        GuildMember member = guildService.getGuildMember(player.getUniqueId());
                        if (member == null) {
                            String message3 = languageManager.getCoreMessage(player, "guild.placeholder.not-in-guild", "&c您不在任何公会中！");
                            player.sendMessage(ColorUtils.colorize(message3));
                            return;
                        }
                        String rankPlaceholder = String.format("{guild_rank_%s}", member.getRole().name().toLowerCase());
                        String message4 = languageManager.getCoreMessage(player, "guild.placeholder.rank", "&a职位占位符: &f{0}");
                        message4 = message4.replace("{0}", rankPlaceholder);
                        player.sendMessage(ColorUtils.colorize(message4));
                        break;
                    default:
                        String message5 = languageManager.getCoreMessage(player, "guild.placeholder.invalid-type", "&c无效的占位符类型！");
                        player.sendMessage(ColorUtils.colorize(message5));
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.placeholder.error", "&c获取占位符时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
        });
    }
    
    private void handleTime(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = guildService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    String message = languageManager.getCoreMessage(player, "guild.time.not-in-guild", "&c您不在任何公会中！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.Duration duration = java.time.Duration.between(guild.getCreatedAt(), now);
                long days = duration.toDays();
                long hours = duration.toHours() % 24;
                
                String message = languageManager.getCoreMessage(player, "guild.time.age", "&a公会创建时间：{0}\n&a公会年龄：&f{1} 天 {2} 小时");
                message = message.replace("{0}", guild.getCreatedAt().toString());
                message = message.replace("{1}", String.valueOf(days));
                message = message.replace("{2}", String.valueOf(hours));
                player.sendMessage(ColorUtils.colorize(message));
            } catch (Exception e) {
                e.printStackTrace();
                String message = languageManager.getCoreMessage(player, "guild.time.error", "&c获取公会时间信息时发生错误！");
                player.sendMessage(ColorUtils.colorize(message));
            }
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
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.chat", "&e/guild chat &7- Toggle guild chat mode &7| &e/guild chat <msg> &7- Send guild message")));
        player.sendMessage(ColorUtils.colorize(languageManager.getCoreMessage(player, "help.help", "&e/guild help &7- Show this help")));
    }
}