package com.guild.util;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildApplication;
import com.guild.models.GuildInvitation;
import com.guild.models.GuildMember;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 通知工具类 - 处理申请和邀请的实时通知和上线通知
 */
public final class NotifyUtils {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private NotifyUtils() {}

    /**
     * 发送实时通知给工会会长（有新申请时）
     */
    public static void notifyLeaderNewApplication(GuildPlugin plugin, Guild guild, GuildApplication application) {
        // 获取工会会长
        plugin.getGuildService().getGuildMemberAsync(guild.getLeaderUuid()).thenAccept(member -> {
            if (member == null) return;

            Player leader = Bukkit.getPlayer(guild.getLeaderUuid());
            if (leader != null && leader.isOnline()) {
                CompatibleScheduler.runTask(plugin, () -> {
                    String message = plugin.getLanguageManager().getMessage(leader, "notify.new-application",
                        "&6[工会通知] &e玩家 &f{player} &e申请加入 &f{guild}&e！",
                        "{player}", application.getPlayerName(), "{guild}", guild.getName());
                    
                    leader.sendMessage(ColorUtils.colorize("&a"));
                    leader.sendMessage(ColorUtils.colorize(message));
                    leader.sendMessage(ColorUtils.colorize(plugin.getLanguageManager().getMessage(leader, "notify.application-tip",
                        "&7提示: 输入 /guild applications 查看并处理申请")));
                });
            }
        });
    }

    /**
     * 发送邀请给目标玩家（带点击事件）
     */
    public static void sendInviteWithClickableAction(GuildPlugin plugin, Player target, Player inviter, Guild guild, GuildInvitation invitation) {
        String guildName = ColorUtils.colorize(guild.getName());
        
        // 标题
        String title = plugin.getLanguageManager().getMessage(target, "invite.received-title",
            "&6&l═══ 工会邀请 ═══");
        
        // 主消息（公会名称带颜色）
        String mainMsg = plugin.getLanguageManager().getMessage(target, "invite.received",
            "&e{inviter} &7邀请您加入工会: &f{guild}",
            "{inviter}", inviter.getName(), "{guild}", guildName);
        
        // 接受按钮
        String acceptBtn = plugin.getLanguageManager().getMessage(target, "invite.click-accept",
            "&a[点击接受]");
        String acceptHover = plugin.getLanguageManager().getMessage(target, "invite.click-accept-hover",
            "&7点击接受邀请加入 &f{guild}&7", "{guild}", guild.getName());
        
        // 拒绝按钮
        String declineBtn = plugin.getLanguageManager().getMessage(target, "invite.click-decline",
            "&c[点击拒绝]");
        String declineHover = plugin.getLanguageManager().getMessage(target, "invite.click-decline-hover",
            "&7点击拒绝此邀请", "{guild}", guild.getName());
        
        // 过期时间提示
        String expireTime = invitation.getExpiresAt().format(TIME_FORMATTER);
        String expireMsg = plugin.getLanguageManager().getMessage(target, "invite.expire-time",
            "&7邀请将在 &c{time} &7过期", "{time}", expireTime);

        // 构建可点击消息
        CompatibleScheduler.runTask(plugin, () -> {
            target.sendMessage(ColorUtils.colorize(title));
            target.sendMessage(ColorUtils.colorize(mainMsg));
            
            // 接受按钮组件
            TextComponent acceptComponent = new TextComponent(ColorUtils.colorize("  " + acceptBtn));
            acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                "/guild accept \"" + guild.getName() + "\""));
            acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text(ColorUtils.colorize(acceptHover))));
            
            // 分隔符
            TextComponent separator = new TextComponent(ColorUtils.colorize(" &8| "));
            
            // 拒绝按钮组件
            TextComponent declineComponent = new TextComponent(ColorUtils.colorize(declineBtn));
            declineComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                "/guild decline \"" + guild.getName() + "\""));
            declineComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text(ColorUtils.colorize(declineHover))));
            
            target.spigot().sendMessage(acceptComponent);
            target.spigot().sendMessage(separator);
            target.spigot().sendMessage(declineComponent);
            
            target.sendMessage(ColorUtils.colorize(expireMsg));
            target.sendMessage(ColorUtils.colorize("&a"));
            
            // ActionBar 提醒
            sendActionBar(plugin, target, plugin.getLanguageManager().getMessage(target, "invite.actionbar-tip",
                "&e您收到来自 &f{guild} &e的邀请", "{guild}", guild.getName()));
        });
    }

    /**
     * 玩家上线时检查并通知待处理申请和邀请
     */
    public static void notifyOnLogin(GuildPlugin plugin, Player player) {
        // 异步检查
        plugin.getGuildService().getGuildMemberAsync(player.getUniqueId()).thenAccept(member -> {
            if (member == null) {
                // 玩家没有工会，检查是否有待处理邀请
                notifyPendingInvitations(plugin, player);
            } else if (member.getRole() == GuildMember.Role.LEADER) {
                // 玩家是会长，检查是否有待处理申请
                notifyPendingApplications(plugin, player);
            }
        });
    }

    /**
     * 通知会长有未处理的申请
     */
    private static void notifyPendingApplications(GuildPlugin plugin, Player leader) {
        plugin.getGuildService().getGuildMemberAsync(leader.getUniqueId()).thenAccept(member -> {
            if (member == null) return;
            
            plugin.getGuildService().getGuildByIdAsync(member.getGuildId()).thenAccept(guild -> {
                if (guild == null) return;
                
                plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
                    if (applications.isEmpty()) return;
                    
                    CompatibleScheduler.runTask(plugin, () -> {
                        String msg = plugin.getLanguageManager().getMessage(leader, "notify.pending-applications",
                            "&6[工会通知] &e您有 &c{count} &e个待处理的加入申请！",
                            "{count}", String.valueOf(applications.size()));
                        
                        leader.sendMessage(ColorUtils.colorize("&a"));
                        leader.sendMessage(ColorUtils.colorize(msg));
                        leader.sendMessage(ColorUtils.colorize(plugin.getLanguageManager().getMessage(leader, "notify.applications-tip",
                            "&7提示: 输入 /guild applications 查看详情")));
                        
                        // 如果申请超过0个，发送ActionBar
                        if (!applications.isEmpty()) {
                            sendActionBar(plugin, leader, plugin.getLanguageManager().getMessage(leader, 
                                "notify.applications-actionbar", "&e您有 {count} 个待处理申请！", "{count}", String.valueOf(applications.size())));
                        }
                    });
                });
            });
        });
    }

    /**
     * 通知玩家有未处理的邀请
     */
    private static void notifyPendingInvitations(GuildPlugin plugin, Player player) {
        plugin.getGuildService().getPendingInvitationsAsync(player.getUniqueId()).thenAccept(invitations -> {
            if (invitations.isEmpty()) return;
            
            // 在显示邀请前，再次检查玩家是否已经加入公会
            plugin.getGuildService().getGuildMemberAsync(player.getUniqueId()).thenAccept(member -> {
                // 如果玩家已经是工会成员，不显示邀请通知
                if (member != null) {
                    return;
                }
                
                // 过滤掉已过期的邀请
                List<GuildInvitation> validInvitations = invitations.stream()
                    .filter(inv -> !inv.isExpired())
                    .collect(java.util.stream.Collectors.toList());
                
                if (validInvitations.isEmpty()) return;
                
                CompatibleScheduler.runTask(plugin, () -> {
                    String msg = plugin.getLanguageManager().getMessage(player, "notify.pending-invitations",
                        "&6[工会邀请] &e您有 &a{count} &e个待处理的工会邀请！",
                        "{count}", String.valueOf(validInvitations.size()));
                    
                    player.sendMessage(ColorUtils.colorize("&a"));
                    player.sendMessage(ColorUtils.colorize(msg));
                    
                    // 显示每个邀请（带点击事件）
                    for (GuildInvitation invitation : validInvitations) {
                        plugin.getGuildService().getGuildByIdAsync(invitation.getGuildId()).thenAccept(guild -> {
                            if (guild == null) return;
                            
                            CompatibleScheduler.runTask(plugin, () -> {
                                String guildName = ColorUtils.colorize(guild.getName());
                                String inviteMsg = plugin.getLanguageManager().getMessage(player, "notify.invitation-item",
                                    "&7- 来自 &f{guild} &7(由 {inviter})",
                                    "{guild}", guildName, "{inviter}", invitation.getInviterName());
                                
                                player.sendMessage(ColorUtils.colorize("  " + inviteMsg));
                                
                                // 发送可点击的接受/拒绝按钮
                                String acceptBtnText = plugin.getLanguageManager().getMessage(player, "invite.click-accept", "&a[点击接受]");
                                String acceptHoverText = plugin.getLanguageManager().getMessage(player, "invite.click-accept-hover", "&7点击接受邀请加入 &f{guild}&7", "{guild}", guild.getName());
                                String declineBtnText = plugin.getLanguageManager().getMessage(player, "invite.click-decline", "&c[点击拒绝]");
                                String declineHoverText = plugin.getLanguageManager().getMessage(player, "invite.click-decline-hover", "&7点击拒绝此邀请");
                                
                                TextComponent acceptBtn = new TextComponent(ColorUtils.colorize("    " + acceptBtnText));
                                acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                                    "/guild accept \"" + guild.getName() + "\""));
                                acceptBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                    new Text(ColorUtils.colorize(acceptHoverText))));
                                
                                TextComponent declineBtn = new TextComponent(ColorUtils.colorize(" " + declineBtnText));
                                declineBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                                    "/guild decline \"" + guild.getName() + "\""));
                                declineBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                    new Text(ColorUtils.colorize(declineHoverText))));
                                
                                player.spigot().sendMessage(acceptBtn);
                                player.spigot().sendMessage(declineBtn);
                            });
                        });
                    }
                });
            });
        });
    }

    /**
     * 发送 ActionBar 消息
     */
    public static void sendActionBar(GuildPlugin plugin, Player player, String message) {
        try {
            net.md_5.bungee.api.chat.TextComponent actionBar = new net.md_5.bungee.api.chat.TextComponent(ColorUtils.colorize(message));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBar);
        } catch (Exception e) {
            plugin.getLogger().warning("发送ActionBar失败: " + e.getMessage());
        }
    }

    /**
     * 通知申请者申请已被处理
     */
    public static void notifyApplicantApplicationProcessed(GuildPlugin plugin, UUID applicantUuid, 
            String applicantName, Guild guild, GuildApplication.ApplicationStatus status) {
        Player applicant = Bukkit.getPlayer(applicantUuid);
        if (applicant != null && applicant.isOnline()) {
            CompatibleScheduler.runTask(plugin, () -> {
                String guildName = ColorUtils.colorize(guild.getName());
                
                if (status == GuildApplication.ApplicationStatus.APPROVED) {
                    String msg = plugin.getLanguageManager().getMessage(applicant, "notify.application-approved",
                        "&a[工会通知] &f您的加入申请已被 &a批准&f！",
                        "{guild}", guildName);
                    applicant.sendMessage(ColorUtils.colorize("&a"));
                    applicant.sendMessage(ColorUtils.colorize(msg));
                    sendActionBar(plugin, applicant, plugin.getLanguageManager().getMessage(applicant, 
                        "notify.application-approved-actionbar", "&a您已成功加入工会 &f{guild}", "{guild}", guild.getName()));
                } else {
                    String msg = plugin.getLanguageManager().getMessage(applicant, "notify.application-rejected",
                        "&c[工会通知] &f您的加入申请已被 &c拒绝&f！",
                        "{guild}", guildName);
                    applicant.sendMessage(ColorUtils.colorize("&a"));
                    applicant.sendMessage(ColorUtils.colorize(msg));
                }
            });
        }
    }

    /**
     * 通知邀请者邀请已被处理
     *
     * @param targetName 接受/拒绝邀请的玩家名称
     */
    public static void notifyInviterInvitationProcessed(GuildPlugin plugin, UUID inviterUuid, 
            String inviterName, String targetName, Guild guild, boolean accepted) {
        Player inviter = Bukkit.getPlayer(inviterUuid);
        if (inviter != null && inviter.isOnline()) {
            CompatibleScheduler.runTask(plugin, () -> {
                String guildName = ColorUtils.colorize(guild.getName());
                
                if (accepted) {
                    String msg = plugin.getLanguageManager().getMessage(inviter, "notify.invitation-accepted",
                        "&a[工会通知] &f{target} &a已接受&f您的邀请加入了工会！",
                        "{target}", targetName, "{guild}", guildName);
                    inviter.sendMessage(ColorUtils.colorize("&a"));
                    inviter.sendMessage(ColorUtils.colorize(msg));
                } else {
                    String msg = plugin.getLanguageManager().getMessage(inviter, "notify.invitation-declined",
                        "&c[工会通知] &f{target} &c已拒绝&f您的邀请！",
                        "{target}", targetName);
                    inviter.sendMessage(ColorUtils.colorize("&a"));
                    inviter.sendMessage(ColorUtils.colorize(msg));
                }
            });
        }
    }
}
