package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.DebugLog;
import com.guild.core.utils.QuietLog;
import com.guild.util.NotifyUtils;
import java.util.Arrays;
import org.bukkit.entity.Player;

public class GuildAcceptHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                if (args.length < 2) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.accept.usage", "&cUsage: /guild accept <guild name>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replaceAll("[\"']", "").trim();
        
                SubCommandErrors.guardPlayerFuture(ctx.plugin(), ctx.languageManager(), player,
                        "accept", "guild.accept.error", "&cAn error occurred while accepting the invitation!",
                        ctx.guildService().getPlayerGuildAsync(player.getUniqueId())).thenAccept(existingGuild -> {
                    if (existingGuild != null) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.accept.already-in-guild", "&cYou are already in a guild!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }
            
                    SubCommandErrors.guardPlayerFuture(ctx.plugin(), ctx.languageManager(), player,
                            "accept-guild", "guild.accept.error", "&cAn error occurred while accepting the invitation!",
                            ctx.guildService().getGuildByNameAsync(guildName))
                            .thenAccept(guild -> {
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.accept.guild-not-found", "&cGuild does not exist!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 检查玩家是否有该公会的有效邀请
                        SubCommandErrors.guardPlayerFuture(ctx.plugin(), ctx.languageManager(), player,
                                "accept-invitation", "guild.accept.error", "&cAn error occurred while accepting the invitation!",
                                ctx.guildService().getPendingInvitationAsync(player.getUniqueId(), guild.getId()))
                                .thenAccept(invitation -> {
                            if (invitation == null) {
                                ctx.plugin().getLogger().warning("[Accept-Debug] 玩家 " + player.getName() + " 没有来自 " + guild.getName() + " 的邀请");
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    String message = ctx.languageManager().getCoreMessage(player, "guild.accept.no-invitation", "&cYou don't have an invitation from this guild or it has expired!");
                                    player.sendMessage(ColorUtils.colorize(message));
                                });
                                return;
                            }
                    
                            DebugLog.info(ctx.plugin().getLogger(), "[Accept-Debug] 找到邀请 ID=" + invitation.getId() + " 从 " + invitation.getInviterName() + " 到 " + invitation.getTargetName());
                    
                            // 处理邀请接受
                            SubCommandErrors.guardPlayerFuture(ctx.plugin(), ctx.languageManager(), player,
                                    "accept-process", "guild.accept.error", "&cError joining the guild!",
                                    ctx.guildService().processInvitationDirectAsync(invitation, true))
                                    .thenAccept(success -> {
                                if (success) {
                                    DebugLog.info(ctx.plugin().getLogger(), "[Accept-Debug] 邀请处理成功，玩家 " + player.getName() + " 已加入 " + guild.getName());
                                    QuietLog.system("Player " + player.getName() + " accepted invitation and joined " + guild.getName());
                                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                        String message = ctx.languageManager().getCoreMessage(player, "guild.accept.success", "&aYou have successfully joined the guild!");
                                        player.sendMessage(ColorUtils.colorize(message));
                                    });
                            
                                    // 通知邀请者
                                    NotifyUtils.notifyInviterInvitationProcessed(ctx.plugin(), invitation.getInviterUuid(), 
                                        invitation.getInviterName(), player.getName(), guild, true);
                                } else {
                                    ctx.plugin().getLogger().warning("[Accept-Debug] 邀请处理失败，邀请ID=" + invitation.getId());
                                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                        String message = ctx.languageManager().getCoreMessage(player, "guild.accept.error", "&cError joining the guild!");
                                        player.sendMessage(ColorUtils.colorize(message));
                                    });
                                }
                            });
                        });
                    });
                });

    }

}
