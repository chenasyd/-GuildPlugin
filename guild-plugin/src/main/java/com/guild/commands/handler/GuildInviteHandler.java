package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.util.InviteMessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GuildInviteHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
        if (args.length < 2) {
            String message = ctx.languageManager().getCoreMessage(player, "guild.invite.usage",
                    "&cUsage: /guild invite <player name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!ctx.plugin().getMembershipRules().canInvite(player)) {
            String message = ctx.languageManager().getCoreMessage(player, "general.no-permission",
                    "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer == null) {
            String message = ctx.languageManager().getCoreMessage(player, "guild.invite.player-not-found",
                    "&cPlayer is not online!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = ctx.languageManager().getCoreMessage(player, "guild.invite.self",
                    "&cYou cannot invite yourself!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        SubCommandErrors.runPlayerAsync(ctx.plugin(), ctx.languageManager(), player,
                "invite", "guild.invite.error", "&cAn error occurred while sending invitation!", () -> {
                    Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                    if (guild == null) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.invite.not-in-guild",
                                    "&cYou are not in any guild!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }

                    Guild targetGuild = ctx.guildService().getPlayerGuild(targetPlayer.getUniqueId());
                    if (targetGuild != null) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.invite.already-in-guild",
                                    "&cThat player is already in a guild!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }

                    int memberCount = ctx.guildService().getGuildMemberCount(guild.getId());
                    int maxMembers = guild.getMaxMembers();
                    if (memberCount >= maxMembers) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.invite.full",
                                    "&cGuild is full!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }

                    String inviteMessage = InviteMessageUtils.formatInviteReceived(ctx.plugin(), targetPlayer, player, guild);
                    CompatibleScheduler.runTask(ctx.plugin(), targetPlayer, () -> targetPlayer.sendMessage(inviteMessage));

                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.invite.success",
                                "&aInvitation sent!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                });
    }
}
