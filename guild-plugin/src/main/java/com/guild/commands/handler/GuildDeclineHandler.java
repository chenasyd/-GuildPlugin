package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.util.NotifyUtils;
import java.util.Arrays;
import org.bukkit.entity.Player;

public class GuildDeclineHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                if (args.length < 2) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.decline.usage", "&cUsage: /guild decline <guild name>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replaceAll("[\"']", "").trim();
        
                SubCommandErrors.guardPlayerFuture(ctx.plugin(), ctx.languageManager(), player,
                        "decline", "guild.decline.error", "&cAn error occurred while declining the invitation!",
                        ctx.guildService().getGuildByNameAsync(guildName)).thenAccept(guild -> {
                    if (guild == null) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.decline.guild-not-found", "&cGuild does not exist!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }
            
                    // 检查玩家是否有该公会的有效邀请
                    ctx.guildService().getPendingInvitationAsync(player.getUniqueId(), guild.getId()).thenAccept(invitation -> {
                        if (invitation == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.decline.no-invitation", "&cYou don't have an invitation from this guild or it has expired!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 处理邀请拒绝
                        SubCommandErrors.guardPlayerFuture(ctx.plugin(), ctx.languageManager(), player,
                                "decline-process", "guild.decline.error", "&cError declining the invitation!",
                                ctx.guildService().processInvitationDirectAsync(invitation, false))
                                .thenAccept(success -> {
                            if (success) {
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    String message = ctx.languageManager().getCoreMessage(player, "guild.decline.success", "&aYou have declined the invitation!");
                                    player.sendMessage(ColorUtils.colorize(message));
                                });
                        
                                // 通知邀请者
                                NotifyUtils.notifyInviterInvitationProcessed(ctx.plugin(), invitation.getInviterUuid(), 
                                    invitation.getInviterName(), player.getName(), guild, false);
                            } else {
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    String message = ctx.languageManager().getCoreMessage(player, "guild.decline.error", "&cError declining the invitation!");
                                    player.sendMessage(ColorUtils.colorize(message));
                                });
                            }
                        });
                    });
                });

    }

}
