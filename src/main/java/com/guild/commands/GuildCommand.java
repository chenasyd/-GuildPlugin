package com.guild.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager; // 新增
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.ConfirmDeleteGuildGUI;
import com.guild.gui.MainGuildGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildRelation;
import com.guild.services.GuildService;
import com.guild.util.InviteMessageUtils;
import com.guildplugin.util.FoliaTeleportUtils;

/**
 * 工会主命令
 */
public class GuildCommand implements CommandExecutor, TabCompleter {
    
    private final GuildPlugin plugin;
    private final LanguageManager languageManager; // 新增字段
    
    public GuildCommand(GuildPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager(); // 初始化
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
                player.sendMessage(ColorUtils.colorize(languageManager.getMessage(player, "general.unknown-command", "&c未知命令！使用 /guild help 查看帮助。")));
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "create", "info", "members", "invite", "kick", "promote", "demote", "accept", "decline", "leave", "delete", "sethome", "home", "relation", "economy", "deposit", "withdraw", "transfer", "logs", "placeholder", "time", "help"
            );
            
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
                case "invite":
                case "kick":
                case "promote":
                case "demote":
                    // 获取在线玩家列表
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String subSubCommand = args[1].toLowerCase();
            
            if (subCommand.equals("relation") && subSubCommand.equals("create")) {
                // 为创建关系提供简单的补全提示
                // 由于异步操作的限制，这里只提供基本的提示
                List<String> suggestions = Arrays.asList("目标工会名称");
                for (String suggestion : suggestions) {
                    if (suggestion.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(suggestion);
                    }
                }
            } else if (subCommand.equals("relation") && (subSubCommand.equals("delete") || subSubCommand.equals("accept") || subSubCommand.equals("reject"))) {
                // 为关系操作提供简单的补全提示
                // 由于异步操作的限制，这里只提供基本的提示
                List<String> suggestions = Arrays.asList("工会名称");
                for (String suggestion : suggestions) {
                    if (suggestion.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(suggestion);
                    }
                }
            } else if (subCommand.equals("transfer")) {
                // 为转账提供简单的补全提示
                // 由于异步操作的限制，这里只提供基本的提示
                List<String> suggestions = Arrays.asList("目标工会名称");
                for (String suggestion : suggestions) {
                    if (suggestion.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(suggestion);
                    }
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String subSubCommand = args[1].toLowerCase();
            
            if (subCommand.equals("relation") && subSubCommand.equals("create")) {
                // 关系类型补全
                List<String> relationTypes = Arrays.asList("ally", "enemy", "war", "truce", "neutral");
                for (String type : relationTypes) {
                    if (type.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(type);
                    }
                }
            } else if (subCommand.equals("deposit") || subCommand.equals("withdraw") || 
                      (subCommand.equals("transfer") && args.length == 4)) {
                // 金额建议（这里只提供一些常用金额）
                List<String> amounts = Arrays.asList("100", "500", "1000", "5000", "10000");
                for (String amount : amounts) {
                    if (amount.startsWith(args[3])) {
                        completions.add(amount);
                    }
                }
            }
        }
        
        return completions;
    }
    
    /**
     * 处理创建工会命令
     */
    private void handleCreate(Player player, String[] args) {
        // 检查权限
        if (!plugin.getPermissionManager().hasPermission(player, "guild.create")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "create.usage", "&e用法: /guild create <工会名称> [标签] [描述]");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String name = args[1];
        String tag = args.length > 2 ? args[2] : null;
        String description = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
        
        // 验证输入
        if (name.length() < 3 || name.length() > 20) {
            String message = languageManager.getMessage(player, "create.name-too-short", "&c工会名称太短！最少需要 3 个字符。");
            player.sendMessage(ColorUtils.colorize(message.replace("{min}", "3")));
            return;
        }
        
        if (tag != null && (tag.length() < 2 || tag.length() > 6)) {
            String message = languageManager.getMessage(player, "create.tag-too-long", "&c工会标签太长！最多只能有 6 个字符。");
            player.sendMessage(ColorUtils.colorize(message.replace("{max}", "6")));
            return;
        }
        
        if (description != null && description.length() > 100) {
            String message = languageManager.getMessage(player, "create.description-too-long", "&c工会描述不能超过100个字符！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查经济系统
        double creationCost = plugin.getConfigManager().getConfig("config.yml").getDouble("guild.creation-cost", 5000.0);
        if (!plugin.getEconomyManager().isVaultAvailable()) {
            String message = languageManager.getMessage(player, "create.economy-not-available", "&c经济系统不可用，无法创建工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!plugin.getEconomyManager().hasBalance(player, creationCost)) {
            String message = languageManager.getMessage(player, "create.insufficient-funds", "&c您的余额不足！创建工会需要 &e{amount}！")
                .replace("{amount}", plugin.getEconomyManager().format(creationCost));
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 扣除创建费用
        if (!plugin.getEconomyManager().withdraw(player, creationCost)) {
            String message = languageManager.getMessage(player, "create.payment-failed", "&c扣除创建费用失败！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 创建工会 (异步)
        guildService.createGuildAsync(name, tag, description, player.getUniqueId(), player.getName())
            .thenAcceptAsync(success -> {
                if (success) {
                    // 去除名称中的颜色代码，避免影响提示消息颜色
                    String cleanName = ColorUtils.stripColor(name);
                    String cleanTag = tag != null ? ColorUtils.stripColor(tag) : null;
                    String cleanDescription = description != null ? ColorUtils.stripColor(description) : null;
                    
                    String successMessage = languageManager.getMessage(player, "create.success", "&a工会 {name} 创建成功！");
                    player.sendMessage(ColorUtils.colorize(successMessage.replace("{name}", cleanName)));
                    
                    String costMessage = languageManager.getMessage(player, "create.cost-info", "&e创建费用: {amount}")
                        .replace("{amount}", plugin.getEconomyManager().format(creationCost));
                    player.sendMessage(ColorUtils.colorize(costMessage));
                    
                    String nameMessage = languageManager.getMessage(player, "create.name-info", "&e工会名称: {name}");
                    player.sendMessage(ColorUtils.colorize(nameMessage.replace("{name}", cleanName)));
                    
                    if (cleanTag != null) {
                        String tagMessage = languageManager.getMessage(player, "create.tag-info", "&e工会标签: [{tag}]");
                        player.sendMessage(ColorUtils.colorize(tagMessage.replace("{tag}", cleanTag)));
                    }
                    
                    if (cleanDescription != null) {
                        String descMessage = languageManager.getMessage(player, "create.description-info", "&e工会描述: {description}");
                        player.sendMessage(ColorUtils.colorize(descMessage.replace("{description}", cleanDescription)));
                    }
                } else {
                    // 退款
                    plugin.getEconomyManager().deposit(player, creationCost);
                    String failMessage = languageManager.getMessage(player, "create.failed", "&c工会创建失败！可能的原因：");
                    player.sendMessage(ColorUtils.colorize(failMessage));
                    
                    String reason1 = languageManager.getMessage(player, "create.failed-reason-1", "&c- 工会名称或标签已存在");
                    String reason2 = languageManager.getMessage(player, "create.failed-reason-2", "&c- 您已经加入了其他工会");
                    player.sendMessage(ColorUtils.colorize(reason1));
                    player.sendMessage(ColorUtils.colorize(reason2));
                }
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }
    
    /**
     * 处理工会信息命令
     */
    private void handleInfo(Player player) {
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        int memberCount = guildService.getGuildMemberCount(guild.getId());
        
        String header = languageManager.getMessage(player, "info.title", "&6=== 工会信息 ===");
        player.sendMessage(ColorUtils.colorize(header));
        
        String nameMessage = languageManager.getMessage(player, "info.name", "&e名称: &f{name}");
        player.sendMessage(ColorUtils.colorize(nameMessage.replace("{name}", guild.getName())));
        
        if (guild.getTag() != null && !guild.getTag().isEmpty()) {
            String tagMessage = languageManager.getMessage(player, "info.tag", "&e标签: &f{tag}");
            player.sendMessage(ColorUtils.colorize(tagMessage.replace("{tag}", guild.getTag())));
        }
        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            String descMessage = languageManager.getMessage(player, "info.description", "&e描述: &f{description}");
            player.sendMessage(ColorUtils.colorize(descMessage.replace("{description}", guild.getDescription())));
        }
        
        String leaderMessage = languageManager.getMessage(player, "info.leader", "&e会长: &f{leader}");
        player.sendMessage(ColorUtils.colorize(leaderMessage.replace("{leader}", guild.getLeaderName())));
        
        String membersMessage = languageManager.getMessage(player, "info.members", "&e成员数量: &f{count}/{max}");
        player.sendMessage(ColorUtils.colorize(membersMessage
            .replace("{count}", String.valueOf(memberCount))
            .replace("{max}", String.valueOf(guild.getMaxMembers()))));
        
        String roleMessage = languageManager.getMessage(player, "info.role", "&e您的角色: &f{role}");
        player.sendMessage(ColorUtils.colorize(roleMessage.replace("{role}", member.getRole().getDisplayName())));
        
        // 统一使用 TimeProvider 的现实时间格式
        java.time.format.DateTimeFormatter TF = com.guild.core.time.TimeProvider.FULL_FORMATTER;
        String createdMessage = languageManager.getMessage(player, "info.created", "&e创建时间: &f{date}");
        String createdFormatted = guild.getCreatedAt() != null ? guild.getCreatedAt().format(TF) : "未知";
        player.sendMessage(ColorUtils.colorize(createdMessage.replace("{date}", createdFormatted)));
    }
    
    /**
     * 处理工会成员命令
     */
    private void handleMembers(Player player) {
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        List<GuildMember> members = guildService.getGuildMembers(guild.getId());
        if (members.isEmpty()) {
            String message = languageManager.getMessage(player, "members.no-members", "&c工会中没有成员！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String title = languageManager.getMessage(player, "members.title", "&6=== 工会成员 ===");
        player.sendMessage(ColorUtils.colorize(title));
        
        for (GuildMember member : members) {
            String status = "";
            Player onlinePlayer = Bukkit.getPlayer(member.getPlayerUuid());
            if (onlinePlayer != null) {
                status = "&a[在线]";
            } else {
                status = "&7[离线]";
            }
            
            String memberFormat = languageManager.getMessage(player, "members.member-format", "&e{role} {name} &7- {status}");
            String memberMessage = memberFormat
                .replace("{role}", member.getRole().getDisplayName())
                .replace("{name}", member.getPlayerName())
                .replace("{status}", status);
            player.sendMessage(ColorUtils.colorize(memberMessage));
        }
        
        String totalMessage = languageManager.getMessage(player, "members.total", "&e总计: {count} 人");
        player.sendMessage(ColorUtils.colorize(totalMessage.replace("{count}", String.valueOf(members.size()))));
    }
    
    /**
     * 处理邀请命令
     */
    private void handleInvite(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.invite")) {
            String message = languageManager.getMessage(player, "invite.no-permission", "&c您没有权限邀请玩家！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "invite.usage", "&e用法: /guild invite <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetPlayerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = languageManager.getMessage(player, "general.player-not-found", "&c玩家 {player} 不在线！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查邀请者是否有工会
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查邀请权限（配置驱动）
        if (!plugin.getPermissionManager().canInviteMembers(player)) {
            String message = languageManager.getMessage(player, "invite.no-permission", "&c您没有权限邀请玩家！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查目标玩家是否已有工会
        if (guildService.getPlayerGuild(targetPlayer.getUniqueId()) != null) {
            String message = languageManager.getMessage(player, "invite.already-in-guild", "&c玩家 {player} 已经加入了其他工会！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        // 检查是否邀请自己
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = languageManager.getMessage(player, "invite.cannot-invite-self", "&c您不能邀请自己！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 发送邀请 (异步)
        guildService.sendInvitationAsync(guild.getId(), player.getUniqueId(), player.getName(), targetPlayer.getUniqueId(), targetPlayerName)
            .thenAcceptAsync(success -> {
                if (success) {
                    // 使用统一工具格式化消息
                    player.sendMessage(InviteMessageUtils.formatInviteSent(plugin, player, targetPlayer));
                    targetPlayer.sendMessage(InviteMessageUtils.formatInviteTitle(plugin, targetPlayer));
                    targetPlayer.sendMessage(InviteMessageUtils.formatInviteReceived(plugin, targetPlayer, player, guild));

                    if (guild.getTag() != null && !guild.getTag().isEmpty()) {
                        String tagMessage = languageManager.getMessage(player, "invite.guild-tag", "&e工会标签: [{tag}]");
                        targetPlayer.sendMessage(ColorUtils.colorize(tagMessage.replace("{tag}", guild.getTag())));
                    }

                    String acceptMessage = languageManager.getMessage(player, "invite.accept-command", "&e输入 &a/guild accept {inviter} &e接受邀请");
                    targetPlayer.sendMessage(ColorUtils.colorize(acceptMessage.replace("{inviter}", player.getName())));

                    String declineMessage = languageManager.getMessage(player, "invite.decline-command", "&e输入 &c/guild decline {inviter} &e拒绝邀请");
                    targetPlayer.sendMessage(ColorUtils.colorize(declineMessage.replace("{inviter}", player.getName())));
                } else {
                    String failMessage = languageManager.getMessage(player, "invite.already-invited", "&c{player} 已经收到了邀请！");
                    player.sendMessage(ColorUtils.colorize(failMessage.replace("{player}", targetPlayerName)));
                }
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }
    
    /**
     * 处理踢出命令
     */
    private void handleKick(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.kick")) {
            String message = languageManager.getMessage(player, "kick.no-permission", "&c您没有权限踢出玩家！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "kick.usage", "&e用法: /guild kick <玩家名称>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetPlayerName = args[1];
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查踢出者是否有工会
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查踢人权限（配置驱动）
        if (!plugin.getPermissionManager().canKickMembers(player)) {
            String message = languageManager.getMessage(player, "kick.no-permission", "&c您没有权限踢出玩家！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 查找目标玩家
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = languageManager.getMessage(player, "kick.player-not-found", "&c玩家 {player} 不在线！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        // 检查目标玩家是否在同一工会
        GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            String message = languageManager.getMessage(player, "kick.not-in-guild", "&c玩家 {player} 不在您的工会中！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        // 检查是否踢出自己
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = languageManager.getMessage(player, "kick.cannot-kick-self", "&c您不能踢出自己！使用 /guild leave 离开工会。");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查是否踢出会长
        if (targetMember.getRole() == GuildMember.Role.LEADER) {
            String message = languageManager.getMessage(player, "kick.cannot-kick-leader", "&c您不能踢出工会会长！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 执行踢出操作
        boolean success = guildService.removeGuildMember(targetPlayer.getUniqueId(), player.getUniqueId());
        if (success) {
            String successMessage = languageManager.getMessage(player, "kick.success", "&a已将 {player} 踢出工会！");
            player.sendMessage(ColorUtils.colorize(successMessage.replace("{player}", targetPlayerName)));
            
            String kickedMessage = languageManager.getMessage(player, "kick.kicked", "&c您已被踢出工会 {guild}！");
            targetPlayer.sendMessage(ColorUtils.colorize(kickedMessage.replace("{guild}", guild.getName())));
        } else {
            String failMessage = languageManager.getMessage(player, "kick.failed", "&c踢出玩家失败！");
            player.sendMessage(ColorUtils.colorize(failMessage));
        }
    }
    
    /**
     * 处理离开工会命令
     */
    private void handleLeave(Player player) {
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查玩家是否有工会
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = languageManager.getMessage(player, "leave.member-error", "&c工会成员信息错误！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查是否是会长
        if (member.getRole() == GuildMember.Role.LEADER) {
            String message1 = languageManager.getMessage(player, "leave.leader-cannot-leave", "&c工会会长不能离开工会！");
            String message2 = languageManager.getMessage(player, "leave.use-delete", "&c如果您想解散工会，请使用 /guild delete 命令。");
            player.sendMessage(ColorUtils.colorize(message1));
            player.sendMessage(ColorUtils.colorize(message2));
            return;
        }
        
        // 执行离开操作
        boolean success = guildService.removeGuildMember(player.getUniqueId(), player.getUniqueId());
        if (success) {
            String message = languageManager.getMessage(player, "leave.success-with-guild", "&a您已成功离开工会: {guild}");
            player.sendMessage(ColorUtils.colorize(message.replace("{guild}", guild.getName())));
        } else {
            String message = languageManager.getMessage(player, "leave.failed", "&c离开工会失败！");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    /**
     * 处理删除工会命令（打开确认 GUI）
     */
    private void handleDelete(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = languageManager.getMessage(player, "delete.no-permission", "&c您没有权限删除工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 检查玩家是否有工会
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 检查是否是会长
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = languageManager.getMessage(player, "delete.leader-only", "&c只有工会会长才能删除工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 打开确认删除GUI
        plugin.getGuiManager().openGUI(player, new ConfirmDeleteGuildGUI(plugin, guild, player));
    }

    private void handleDeleteConfirm(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = languageManager.getMessage(player, "delete.no-permission", "&c您没有权限删除工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = languageManager.getMessage(player, "delete.leader-only", "&c只有工会会长才能删除工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        plugin.getGuildService().deleteGuildAsync(guild.getId(), player.getUniqueId()).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String message = languageManager.getMessage(player, "delete.success", "&a工会已被删除！");
                    player.sendMessage(ColorUtils.colorize(message.replace("{guild}", guild.getName())));
                } else {
                    String message = languageManager.getMessage(player, "delete.failed", "&c删除工会失败！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }

    private void handleDeleteCancel(Player player) {
        String message = languageManager.getMessage(player, "delete.canceled", "&a已取消删除工会！");
        player.sendMessage(ColorUtils.colorize(message));
    }

    /**
     * 处理设置工会家命令
     */
    private void handleSetHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.sethome")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查玩家是否有工会
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查是否是会长
        if (!guildService.isGuildLeader(player.getUniqueId())) {
            String message = languageManager.getMessage(player, "sethome.only-leader", "&c只有工会会长才能设置工会家！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 设置工会家 (异步)
        guildService.setGuildHomeAsync(guild.getId(), player.getLocation(), player.getUniqueId())
            .thenAcceptAsync(success -> {
                if (success) {
                    String message = languageManager.getMessage(player, "sethome.success", "&a工会家设置成功！");
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    String message = languageManager.getMessage(player, "sethome.failed", "&c设置工会家失败！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }
    
    /**
     * 处理传送到工会家命令（使用 Folia 兼容传送）
     */
    private void handleHome(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.home")) {
            String message = languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 检查玩家是否有工会
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // 获取工会家位置 (异步)
        guildService.getGuildHomeAsync(guild.getId())
            .thenAcceptAsync(homeLocation -> {
                if (homeLocation == null) {
                    String message = languageManager.getMessage(player, "home.not-set", "&c工会家尚未设置！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                // Folia 兼容安全传送
                FoliaTeleportUtils.safeTeleport(plugin, player, homeLocation);
                String message = languageManager.getMessage(player, "home.success", "&a已传送到工会家！");
                player.sendMessage(ColorUtils.colorize(message));
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }

    /**
     * 处理提升成员命令
     */
    private void handlePromote(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.promote")) {
            String message = languageManager.getMessage(player, "permissions.promote.no-permission", "&c您没有权限提升玩家！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "permissions.promote.usage", "&e用法: /guild promote <玩家>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetPlayerName = args[1];
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查提升者是否有工会
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 已通过节点校验，按配置驱动，不再强制"仅会长"
        
        // 查找目标玩家
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = languageManager.getMessage(player, "permissions.promote.player-not-found", "&c玩家 {player} 不在线！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        // 检查目标玩家是否在同一工会
        GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            String message = languageManager.getMessage(player, "permissions.promote.not-in-guild", "&c玩家 {player} 不在您的工会中！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        // 检查是否提升自己
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = languageManager.getMessage(player, "permissions.promote.cannot-promote-self", "&c您不能提升自己！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查当前角色
        GuildMember.Role currentRole = targetMember.getRole();
        GuildMember.Role newRole = null;
        
        if (currentRole == GuildMember.Role.MEMBER) {
            newRole = GuildMember.Role.OFFICER;
        } else if (currentRole == GuildMember.Role.OFFICER) {
            String message = languageManager.getMessage(player, "permissions.promote.already-highest", "&c玩家 {player} 已经是最高职位！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        if (newRole != null) {
            // 执行提升操作
            boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), newRole, player.getUniqueId());
            if (success) {
                String successMessage = languageManager.getMessage(player, "permissions.promote.success", "&a已将 {player} 提升为 {role}！");
                player.sendMessage(ColorUtils.colorize(successMessage
                    .replace("{player}", targetPlayerName)
                    .replace("{role}", newRole.getDisplayName())));
                
                String promotedMessage = languageManager.getMessage(player, "permissions.promote.success", "&a您已被提升为 {role}！");
                targetPlayer.sendMessage(ColorUtils.colorize(promotedMessage.replace("{role}", newRole.getDisplayName())));
            } else {
                String failMessage = languageManager.getMessage(player, "permissions.promote.cannot-promote", "&c无法提升该玩家！");
                player.sendMessage(ColorUtils.colorize(failMessage));
            }
        }
    }
    
    /**
     * 处理降级成员命令
     */
    private void handleDemote(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.demote")) {
            String message = languageManager.getMessage(player, "permissions.demote.no-permission", "&c您没有权限降级玩家！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "permissions.demote.usage", "&e用法: /guild demote <玩家>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String targetPlayerName = args[1];
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查降级者是否有工会
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "info.no-guild", "&c您还没有加入任何工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 已通过节点校验，按配置驱动，不再强制"仅会长"
        
        // 查找目标玩家
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = languageManager.getMessage(player, "permissions.demote.player-not-found", "&c玩家 {player} 不在线！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        // 检查目标玩家是否在同一工会
        GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            String message = languageManager.getMessage(player, "permissions.demote.not-in-guild", "&c玩家 {player} 不在您的工会中！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        // 检查是否降级自己
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = languageManager.getMessage(player, "permissions.demote.cannot-demote-self", "&c您不能降级自己！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查是否降级会长
        if (targetMember.getRole() == GuildMember.Role.LEADER) {
            String message = languageManager.getMessage(player, "permissions.demote.cannot-demote-leader", "&c不能降级工会会长！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查当前角色
        GuildMember.Role currentRole = targetMember.getRole();
        GuildMember.Role newRole = null;
        
        if (currentRole == GuildMember.Role.OFFICER) {
            newRole = GuildMember.Role.MEMBER;
        } else if (currentRole == GuildMember.Role.MEMBER) {
            String message = languageManager.getMessage(player, "permissions.demote.already-lowest", "&c玩家 {player} 已经是最低职位！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        
        if (newRole != null) {
            // 执行降级操作
            boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), newRole, player.getUniqueId());
            if (success) {
                String successMessage = languageManager.getMessage(player, "permissions.demote.success", "&a已将 {player} 降级为 {role}！");
                player.sendMessage(ColorUtils.colorize(successMessage
                    .replace("{player}", targetPlayerName)
                    .replace("{role}", newRole.getDisplayName())));
                
                String demotedMessage = languageManager.getMessage(player, "permissions.demote.success", "&a您已被降级为 {role}！");
                targetPlayer.sendMessage(ColorUtils.colorize(demotedMessage.replace("{role}", newRole.getDisplayName())));
            } else {
                String failMessage = languageManager.getMessage(player, "permissions.demote.cannot-demote", "&c无法降级该玩家！");
                player.sendMessage(ColorUtils.colorize(failMessage));
            }
        }
    }
    
    /**
     * 处理接受邀请命令
     */
    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "invite.accept-command", "&e输入 &a/guild accept {inviter} &e接受邀请");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String inviterName = args[1];
        Player inviter = Bukkit.getPlayer(inviterName);
        if (inviter == null) {
            String message = languageManager.getMessage(player, "general.player-not-found", "&c玩家 {player} 不在线！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", inviterName)));
            return;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 先检查邀请是否存在
        guildService.getPendingInvitationAsync(player.getUniqueId(), inviter.getUniqueId()).thenAccept(invitation -> {
            if (invitation == null) {
                String failMessage = languageManager.getMessage(player, "invite.expired", "&c工会邀请已过期或不存在！");
                CompatibleScheduler.runTask(plugin, () -> player.sendMessage(ColorUtils.colorize(failMessage)));
                return;
            }
            
            // 获取工会信息
            guildService.getGuildByIdAsync(invitation.getGuildId()).thenAccept(guild -> {
                if (guild == null) {
                    String failMessage = languageManager.getMessage(player, "invite.expired", "&c工会邀请已过期或工会不存在！");
                    CompatibleScheduler.runTask(plugin, () -> player.sendMessage(ColorUtils.colorize(failMessage)));
                    return;
                }
                
                // 处理邀请
                guildService.processInvitationAsync(player.getUniqueId(), inviter.getUniqueId(), true).thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (success) {
                            // 去除工会名称中的颜色代码
                            String cleanGuildName = ColorUtils.stripColor(guild.getName());
                            String successMessage = languageManager.getMessage(player, "invite.accepted", "&a您已接受 {guild} 的邀请！")
                                .replace("{guild}", cleanGuildName);
                            player.sendMessage(ColorUtils.colorize(successMessage));
                            
                            String inviterMessage = languageManager.getMessage(player, "invite.accepted-by-inviter", "&a{player} 已接受您的邀请！");
                            if (inviter.isOnline()) {
                                inviter.sendMessage(ColorUtils.colorize(inviterMessage.replace("{player}", player.getName())));
                            }
                        } else {
                            String failMessage = languageManager.getMessage(player, "invite.expired", "&c工会邀请已过期！");
                            player.sendMessage(ColorUtils.colorize(failMessage));
                        }
                    });
                });
            });
        });
    }
    
    /**
     * 处理拒绝邀请命令
     */
    private void handleDecline(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "invite.decline-command", "&e输入 &c/guild decline {inviter} &e拒绝邀请");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String inviterName = args[1];
        Player inviter = Bukkit.getPlayer(inviterName);
        if (inviter == null) {
            String message = languageManager.getMessage(player, "general.player-not-found", "&c玩家 {player} 不在线！");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", inviterName)));
            return;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = languageManager.getMessage(player, "general.service-error", "&c工会服务未初始化！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 先检查邀请是否存在
        guildService.getPendingInvitationAsync(player.getUniqueId(), inviter.getUniqueId()).thenAccept(invitation -> {
            if (invitation == null) {
                String failMessage = languageManager.getMessage(player, "invite.expired", "&c工会邀请已过期或不存在！");
                CompatibleScheduler.runTask(plugin, () -> player.sendMessage(ColorUtils.colorize(failMessage)));
                return;
            }
            
            // 获取工会信息
            guildService.getGuildByIdAsync(invitation.getGuildId()).thenAccept(guild -> {
                if (guild == null) {
                    String failMessage = languageManager.getMessage(player, "invite.expired", "&c工会邀请已过期或工会不存在！");
                    CompatibleScheduler.runTask(plugin, () -> player.sendMessage(ColorUtils.colorize(failMessage)));
                    return;
                }
                
                // 处理邀请
                guildService.processInvitationAsync(player.getUniqueId(), inviter.getUniqueId(), false).thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (success) {
                            // 去除工会名称中的颜色代码
                            String cleanGuildName = ColorUtils.stripColor(guild.getName());
                            String successMessage = languageManager.getMessage(player, "invite.declined", "&c您已拒绝 {guild} 的邀请！")
                                .replace("{guild}", cleanGuildName);
                            player.sendMessage(ColorUtils.colorize(successMessage));
                            
                            String inviterMessage = languageManager.getMessage(player, "invite.declined-by-inviter", "&c{player} 已拒绝您的邀请！");
                            if (inviter.isOnline()) {
                                inviter.sendMessage(ColorUtils.colorize(inviterMessage.replace("{player}", player.getName())));
                            }
                        } else {
                            String failMessage = languageManager.getMessage(player, "invite.expired", "&c工会邀请已过期！");
                            player.sendMessage(ColorUtils.colorize(failMessage));
                        }
                    });
                });
            });
        });
    }
    
    /**
     * 处理帮助命令
     */
    private void handleHelp(Player player) {
        String title = languageManager.getMessage(player, "help.title", "&6=== 工会系统帮助 ===");
        player.sendMessage(ColorUtils.colorize(title));
        
        String mainMenu = languageManager.getMessage(player, "help.main-menu", "&e/guild &7- 打开工会主界面");
        player.sendMessage(ColorUtils.colorize(mainMenu));
        
        String create = languageManager.getMessage(player, "help.create", "&e/guild create <名称> [标签] [描述] &7- 创建工会");
        player.sendMessage(ColorUtils.colorize(create));
        
        String info = languageManager.getMessage(player, "help.info", "&e/guild info &7- 查看工会信息");
        player.sendMessage(ColorUtils.colorize(info));
        
        String members = languageManager.getMessage(player, "help.members", "&e/guild members &7- 查看工会成员");
        player.sendMessage(ColorUtils.colorize(members));
        
        String invite = languageManager.getMessage(player, "help.invite", "&e/guild invite <玩家> &7- 邀请玩家加入工会");
        player.sendMessage(ColorUtils.colorize(invite));
        
        String kick = languageManager.getMessage(player, "help.kick", "&e/guild kick <玩家> &7- 踢出工会成员");
        player.sendMessage(ColorUtils.colorize(kick));
        
        String promote = languageManager.getMessage(player, "help.promote", "&e/guild promote <玩家> &7- 提升工会成员");
        player.sendMessage(ColorUtils.colorize(promote));
        
        String demote = languageManager.getMessage(player, "help.demote", "&e/guild demote <玩家> &7- 降级工会成员");
        player.sendMessage(ColorUtils.colorize(demote));
        
        String accept = languageManager.getMessage(player, "help.accept", "&e/guild accept <邀请者> &7- 接受工会邀请");
        player.sendMessage(ColorUtils.colorize(accept));
        
        String decline = languageManager.getMessage(player, "help.decline", "&e/guild decline <邀请者> &7- 拒绝工会邀请");
        player.sendMessage(ColorUtils.colorize(decline));
        
        String leave = languageManager.getMessage(player, "help.leave", "&e/guild leave &7- 离开工会");
        player.sendMessage(ColorUtils.colorize(leave));
        
        String delete = languageManager.getMessage(player, "help.delete", "&e/guild delete &7- 删除工会");
        player.sendMessage(ColorUtils.colorize(delete));
        
        String sethome = languageManager.getMessage(player, "help.sethome", "&e/guild sethome &7- 设置工会家");
        player.sendMessage(ColorUtils.colorize(sethome));
        
        String home = languageManager.getMessage(player, "help.home", "&e/guild home &7- 传送到工会家");
        player.sendMessage(ColorUtils.colorize(home));
        
        String help = languageManager.getMessage(player, "help.help", "&e/guild help &7- 显示此帮助信息");
        player.sendMessage(ColorUtils.colorize(help));
        
        String relation = languageManager.getMessage(player, "help.relation", "&e/guild relation &7- 管理工会关系");
        player.sendMessage(ColorUtils.colorize(relation));

        String economy = languageManager.getMessage(player, "help.economy", "&e/guild economy &7- 管理工会经济");
        player.sendMessage(ColorUtils.colorize(economy));

        String deposit = languageManager.getMessage(player, "help.deposit", "&e/guild deposit <金额> &7- 向工会存入资金");
        player.sendMessage(ColorUtils.colorize(deposit));

        String withdraw = languageManager.getMessage(player, "help.withdraw", "&e/guild withdraw <金额> &7- 从工会取出资金");
        player.sendMessage(ColorUtils.colorize(withdraw));

        String transfer = languageManager.getMessage(player, "help.transfer", "&e/guild transfer <工会> <金额> &7- 向其他工会转账");
        player.sendMessage(ColorUtils.colorize(transfer));

        String logs = languageManager.getMessage(player, "help.logs", "&e/guild logs &7- 查看工会操作日志");
        player.sendMessage(ColorUtils.colorize(logs));
    }
    
    /**
     * 处理工会关系命令
     */
    private void handleRelation(Player player, String[] args) {
        // 获取玩家工会
        Guild guild = plugin.getGuildService().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = languageManager.getMessage(player, "relation.no-guild", "&c您还没有加入工会！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 检查权限（只有会长可以管理关系）
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = languageManager.getMessage(player, "relation.only-leader", "&c只有工会会长才能管理工会关系！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (args.length == 1) {
            // 显示关系管理帮助
            showRelationHelp(player);
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "list":
                handleRelationList(player, guild);
                break;
            case "create":
                if (args.length < 4) {
                    String message = languageManager.getMessage(player, "relation.create-usage", "&e用法: /guild relation create <目标工会> <关系类型>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationCreate(player, guild, args[2], args[3]);
                break;
            case "delete":
                if (args.length < 3) {
                    String message = languageManager.getMessage(player, "relation.delete-usage", "&e用法: /guild relation delete <目标工会>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationDelete(player, guild, args[2]);
                break;
            case "accept":
                if (args.length < 3) {
                    String message = languageManager.getMessage(player, "relation.accept-usage", "&e用法: /guild relation accept <目标工会>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationAccept(player, guild, args[2]);
                break;
            case "reject":
                if (args.length < 3) {
                    String message = languageManager.getMessage(player, "relation.reject-usage", "&e用法: /guild relation reject <目标工会>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationReject(player, guild, args[2]);
                break;
            default:
                String message = languageManager.getMessage(player, "relation.unknown-subcommand", "&c未知的子命令！使用 /guild relation 查看帮助。");
                player.sendMessage(ColorUtils.colorize(message));
                break;
        }
    }
    
    /**
     * 显示关系管理帮助
     */
    private void showRelationHelp(Player player) {
        String title = languageManager.getMessage(player, "relation.help-title", "&6=== 工会关系管理 ===");
        player.sendMessage(ColorUtils.colorize(title));
        
        String list = languageManager.getMessage(player, "relation.help-list", "&e/guild relation list &7- 查看所有关系");
        player.sendMessage(ColorUtils.colorize(list));
        
        String create = languageManager.getMessage(player, "relation.help-create", "&e/guild relation create <工会> <类型> &7- 创建关系");
        player.sendMessage(ColorUtils.colorize(create));
        
        String delete = languageManager.getMessage(player, "relation.help-delete", "&e/guild relation delete <工会> &7- 删除关系");
        player.sendMessage(ColorUtils.colorize(delete));
        
        String accept = languageManager.getMessage(player, "relation.help-accept", "&e/guild relation accept <工会> &7- 接受关系请求");
        player.sendMessage(ColorUtils.colorize(accept));
        
        String reject = languageManager.getMessage(player, "relation.help-reject", "&e/guild relation reject <工会> &7- 拒绝关系请求");
        player.sendMessage(ColorUtils.colorize(reject));
        
        String types = languageManager.getMessage(player, "relation.help-types", "&7关系类型: &eally(盟友), enemy(敌对), war(开战), truce(停战), neutral(中立)");
        player.sendMessage(ColorUtils.colorize(types));
    }
    
    /**
     * 处理关系列表
     */
    private void handleRelationList(Player player, Guild guild) {
        plugin.getGuildService().getGuildRelationsAsync(guild.getId()).thenAccept(relations -> {
            if (relations == null || relations.isEmpty()) {
                String message = languageManager.getMessage(player, "relation.no-relations", "&7您的工会还没有任何关系。");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            String title = languageManager.getMessage(player, "relation.list-title", "&6=== 工会关系列表 ===");
            player.sendMessage(ColorUtils.colorize(title));
            
            for (GuildRelation relation : relations) {
                String otherGuildName = relation.getOtherGuildName(guild.getId());
                String status = relation.getStatus().name();
                String type = relation.getType().name();
                
                String relationInfo = languageManager.getMessage(player, "relation.list-format", "&e{other_guild} &7- {type} ({status})")
                    .replace("{other_guild}", otherGuildName)
                    .replace("{type}", type)
                    .replace("{status}", status);
                player.sendMessage(ColorUtils.colorize(relationInfo));
            }
        });
    }
    
    /**
     * 处理创建关系
     */
    private void handleRelationCreate(Player player, Guild guild, String targetGuildName, String relationTypeStr) {
        // 验证关系类型
        GuildRelation.RelationType relationType;
        try {
            relationType = GuildRelation.RelationType.valueOf(relationTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            String message = languageManager.getMessage(player, "relation.invalid-type", "&c无效的关系类型！有效类型: ally, enemy, war, truce, neutral");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 获取目标工会
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = languageManager.getMessage(player, "relation.target-not-found", "&c目标工会 {guild} 不存在！")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            if (targetGuild.getId() == guild.getId()) {
                String message = languageManager.getMessage(player, "relation.cannot-relation-self", "&c不能与自己建立关系！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 创建关系
            plugin.getGuildService().createGuildRelationAsync(guild.getId(), targetGuild.getId(), guild.getName(), targetGuild.getName(), relationType, player.getUniqueId(), player.getName())
                .thenAccept(success -> {
                    if (success) {
                        String message = languageManager.getMessage(player, "relation.create-success", "&a已向 {guild} 发送 {type} 关系请求！")
                            .replace("{guild}", targetGuildName)
                            .replace("{type}", relationType.name());
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = languageManager.getMessage(player, "relation.create-failed", "&c创建关系失败！可能已经存在关系。");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }
    
    /**
     * 处理删除关系
     */
    private void handleRelationDelete(Player player, Guild guild, String targetGuildName) {
        // 获取目标工会
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = languageManager.getMessage(player, "relation.target-not-found", "&c目标工会 {guild} 不存在！")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 获取关系然后删除
            plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId())
                .thenCompose(relation -> {
                    if (relation == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getGuildService().deleteGuildRelationAsync(relation.getId());
                })
                .thenAccept(success -> {
                    if (success) {
                        String message = languageManager.getMessage(player, "relation.delete-success", "&a已删除与 {guild} 的关系！")
                            .replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = languageManager.getMessage(player, "relation.delete-failed", "&c删除关系失败！可能关系不存在。");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }
    
    /**
     * 处理接受关系
     */
    private void handleRelationAccept(Player player, Guild guild, String targetGuildName) {
        // 获取目标工会
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = languageManager.getMessage(player, "relation.target-not-found", "&c目标工会 {guild} 不存在！")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 获取关系然后接受
            plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId())
                .thenCompose(relation -> {
                    if (relation == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE);
                })
                .thenAccept(success -> {
                    if (success) {
                        String message = languageManager.getMessage(player, "relation.accept-success", "&a已接受 {guild} 的关系请求！")
                            .replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = languageManager.getMessage(player, "relation.accept-failed", "&c接受关系失败！可能没有待处理的关系请求。");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }
    
    /**
     * 处理拒绝关系
     */
    private void handleRelationReject(Player player, Guild guild, String targetGuildName) {
        // 获取目标工会
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = languageManager.getMessage(player, "relation.target-not-found", "&c目标工会 {guild} 不存在！")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 获取关系然后拒绝
            plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId())
                .thenCompose(relation -> {
                    if (relation == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED);
                })
                .thenAccept(success -> {
                    if (success) {
                        String message = languageManager.getMessage(player, "relation.reject-success", "&c已拒绝 {guild} 的关系请求！")
                            .replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = languageManager.getMessage(player, "relation.reject-failed", "&c拒绝关系失败！可能没有待处理的关系请求。");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }
    
    /**
     * 处理工会经济命令
     */
    private void handleEconomy(Player player, String[] args) {
        // 获取玩家工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                String message = languageManager.getMessage(player, "economy.no-guild", "&c您还没有加入工会！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 显示工会经济信息
            String message = languageManager.getMessage(player, "economy.info", "&6工会经济信息");
            player.sendMessage(ColorUtils.colorize(message));
            
            String balanceMessage = languageManager.getMessage(player, "economy.balance", "&7当前资金: &e{balance}")
                .replace("{balance}", plugin.getEconomyManager().format(guild.getBalance()));
            player.sendMessage(ColorUtils.colorize(balanceMessage));
            
            String levelMessage = languageManager.getMessage(player, "economy.level", "&7当前等级: &e{level}")
                .replace("{level}", String.valueOf(guild.getLevel()));
            player.sendMessage(ColorUtils.colorize(levelMessage));
            
            String maxMembersMessage = languageManager.getMessage(player, "economy.max-members", "&7最大成员: &e{max_members}")
                .replace("{max_members}", String.valueOf(guild.getMaxMembers()));
            player.sendMessage(ColorUtils.colorize(maxMembersMessage));
        });
    }
    
    /**
     * 处理存款命令
     */
    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "deposit.usage", "&c用法: /guild deposit <金额>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            String message = languageManager.getMessage(player, "deposit.invalid-amount", "&c金额格式错误！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (amount <= 0) {
            String message = languageManager.getMessage(player, "deposit.must-be-positive", "&c金额必须大于0！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 获取玩家工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                String message = languageManager.getMessage(player, "economy.no-guild", "&c您还没有加入工会！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 检查玩家余额
            if (!plugin.getEconomyManager().hasBalance(player, amount)) {
                String message = languageManager.getMessage(player, "economy.insufficient-balance", "&c您的余额不足！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 执行存款
            plugin.getEconomyManager().withdraw(player, amount);
            plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() + amount).thenAccept(success -> {
                if (success) {
                    String message = languageManager.getMessage(player, "economy.deposit-success", "&a成功向工会存款 &e{amount}！")
                        .replace("{amount}", plugin.getEconomyManager().format(amount));
                    player.sendMessage(ColorUtils.colorize(message));
                } else {
                    // 退款
                    plugin.getEconomyManager().deposit(player, amount);
                    String message = languageManager.getMessage(player, "economy.deposit-failed", "&c存款失败！");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * 处理取款命令
     */
    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "withdraw.usage", "&c用法: /guild withdraw <金额>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            String message = languageManager.getMessage(player, "withdraw.invalid-amount", "&c金额格式错误！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (amount <= 0) {
            String message = languageManager.getMessage(player, "withdraw.must-be-positive", "&c金额必须大于0！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 获取玩家工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                String message = languageManager.getMessage(player, "economy.no-guild", "&c您还没有加入工会！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 检查工会余额
            if (guild.getBalance() < amount) {
                String message = languageManager.getMessage(player, "economy.guild-insufficient-balance", "&c工会余额不足！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 检查权限（只有会长可以取款）
            plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                    String message = languageManager.getMessage(player, "economy.leader-only", "&c只有工会会长才能取款！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 执行取款
                plugin.getGuildService().updateGuildBalanceAsync(guild.getId(), guild.getBalance() - amount).thenAccept(success -> {
                    if (success) {
                        plugin.getEconomyManager().deposit(player, amount);
                        String message = languageManager.getMessage(player, "economy.withdraw-success", "&a成功从工会取款 &e{amount}！")
                            .replace("{amount}", plugin.getEconomyManager().format(amount));
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = languageManager.getMessage(player, "economy.withdraw-failed", "&c取款失败！");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
        });
    }
    
    /**
     * 处理转账命令
     */
    private void handleTransfer(Player player, String[] args) {
        if (args.length < 3) {
            String message = languageManager.getMessage(player, "transfer.usage", "&c用法: /guild transfer <工会> <金额>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String targetGuildName = args[1];
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            String message = languageManager.getMessage(player, "transfer.invalid-amount", "&c金额格式错误！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (amount <= 0) {
            String message = languageManager.getMessage(player, "transfer.must-be-positive", "&c金额必须大于0！");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // 获取玩家工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(sourceGuild -> {
            if (sourceGuild == null) {
                String message = languageManager.getMessage(player, "economy.no-guild", "&c您还没有加入工会！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 检查权限（只有会长可以转账）
            plugin.getGuildService().getGuildMemberAsync(sourceGuild.getId(), player.getUniqueId()).thenAccept(member -> {
                if (member == null || member.getRole() != GuildMember.Role.LEADER) {
                    String message = languageManager.getMessage(player, "economy.leader-only", "&c只有工会会长才能转账！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 检查工会余额
                if (sourceGuild.getBalance() < amount) {
                    String message = languageManager.getMessage(player, "economy.guild-insufficient-balance", "&c工会余额不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 查找目标工会
                plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
                    if (targetGuild == null) {
                        String message = languageManager.getMessage(player, "economy.target-guild-not-found", "&c目标工会不存在！");
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }
                    
                    // 不能转账给自己
                    if (sourceGuild.getId() == targetGuild.getId()) {
                        String message = languageManager.getMessage(player, "economy.cannot-transfer-to-self", "&c不能转账给自己的工会！");
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }
                    
                    // 执行转账
                    plugin.getGuildService().updateGuildBalanceAsync(sourceGuild.getId(), sourceGuild.getBalance() - amount).thenAccept(success1 -> {
                        if (success1) {
                            plugin.getGuildService().updateGuildBalanceAsync(targetGuild.getId(), targetGuild.getBalance() + amount).thenAccept(success2 -> {
                                if (success2) {
                                    String message = languageManager.getMessage(player, "economy.transfer-success", "&a成功向工会 &e{target} &a转账 &e{amount}！")
                                        .replace("{target}", targetGuildName)
                                        .replace("{amount}", plugin.getEconomyManager().format(amount));
                                    player.sendMessage(ColorUtils.colorize(message));
                                } else {
                                    // 回滚
                                    plugin.getGuildService().updateGuildBalanceAsync(sourceGuild.getId(), sourceGuild.getBalance() + amount);
                                    String message = languageManager.getMessage(player, "economy.transfer-failed", "&c转账失败！");
                                    player.sendMessage(ColorUtils.colorize(message));
                                }
                            });
                        } else {
                            String message = languageManager.getMessage(player, "economy.transfer-failed", "&c转账失败！");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                    });
                });
            });
        });
    }
    
    /**
     * 处理日志查看命令
     */
    private void handleLogs(Player player, String[] args) {
        // 获取玩家工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                String message = languageManager.getMessage(player, "general.no-guild", "&c您还没有加入工会！");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // 检查权限
            plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                if (member == null) {
                    String message = languageManager.getMessage(player, "general.no-permission", "&c权限不足！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // 打开工会日志GUI
                plugin.getGuiManager().openGUI(player, new com.guild.gui.GuildLogsGUI(plugin, guild, player));
            });
        });
    }
    
    /**
     * 处理占位符测试命令
     */
    private void handlePlaceholder(Player player, String[] args) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize(languageManager.getMessage(player, "general.no-permission", "&c您没有权限执行此操作！")));
            return;
        }
        
        if (args.length < 2) {
            String message = languageManager.getMessage(player, "placeholder.usage", "&c用法: /guild placeholder <变量名>");
            player.sendMessage(ColorUtils.colorize(message));
            message = languageManager.getMessage(player, "placeholder.example", "&e示例: /guild placeholder name");
            player.sendMessage(ColorUtils.colorize(message));
            message = languageManager.getMessage(player, "placeholder.available", "&e可用变量: name, tag, description, leader, membercount, role, hasguild, isleader, isofficer");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        String placeholder = "%guild_" + args[1] + "%";
        String result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
        
        player.sendMessage(ColorUtils.colorize("&6=== PlaceholderAPI 测试 ==="));
        player.sendMessage(ColorUtils.colorize("&e变量: &f" + placeholder));
        player.sendMessage(ColorUtils.colorize("&e结果: &f" + result));
        player.sendMessage(ColorUtils.colorize("&6========================"));
    }
    
    /**
     * /guild time：显示现实系统时间与当前世界游戏内时间
     */
    private void handleTime(Player player) {
        String title = languageManager.getMessage(player, "time.title", "&6=== 时间测试 ===");
        String realNow = com.guild.core.time.TimeProvider.nowString();
        // Minecraft 世界时间（白天循环 0-23999 ticks）
        long ticks = player.getWorld().getTime() % 24000L;
        int hours = (int)((ticks / 1000L + 6) % 24); // 0 tick 对应 06:00
        int minutes = (int)((ticks % 1000L) * 60L / 1000L);
        String gameTime = String.format("%02d:%02d", hours, minutes);
        String ticksStr = String.valueOf(ticks);
        player.sendMessage(ColorUtils.colorize(title));
        String message = languageManager.getMessage(player, "time.real-time", "&e现实时间: &f");
        player.sendMessage(ColorUtils.colorize(message + realNow));
        message = languageManager.getMessage(player, "time.game-time", "&e游戏时间: &f");
        player.sendMessage(ColorUtils.colorize(message + gameTime + " &7(" + ticksStr + " ticks)"));
    }
}
