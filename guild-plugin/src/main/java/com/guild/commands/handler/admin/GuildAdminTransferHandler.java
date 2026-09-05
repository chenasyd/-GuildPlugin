package com.guild.commands.handler.admin;

import com.guild.core.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GuildAdminTransferHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 3) {
            String usage = ctx.languageManager().getCoreMessage("admin.transfer.usage", "&cUsage: /guildadmin transfer <guild name> <new leader>");
            sender.sendMessage(ColorUtils.colorize(usage));
            return;
        }

        String guildName = args[1];
        String newLeaderName = args[2];
        UUID requesterUuid = sender instanceof Player p ? p.getUniqueId() : null;

        Player newLeader = Bukkit.getPlayer(newLeaderName);
        if (newLeader == null) {
            String notOnline = ctx.languageManager().getCoreMessage("admin.transfer.player-not-online", "&cPlayer {player} is not online!")
                    .replace("{player}", newLeaderName);
            sender.sendMessage(ColorUtils.colorize(notOnline));
            return;
        }

        ctx.guildService().getGuildByNameAsync(guildName).thenAccept(guild -> {
            if (guild == null) {
                String notFound = ctx.languageManager().getCoreMessage("admin.transfer.not-found", "&cGuild {guild} does not exist!")
                        .replace("{guild}", guildName);
                ctx.sendMessage(sender, ColorUtils.colorize(notFound));
                return;
            }

            ctx.guildService().getGuildMemberAsync(guild.getId(), newLeader.getUniqueId()).thenAccept(member -> {
                if (member == null) {
                    String notMember = ctx.languageManager().getCoreMessage("admin.transfer.not-member", "&cPlayer {player} is not a member of this guild!")
                            .replace("{player}", newLeaderName);
                    ctx.sendMessage(sender, ColorUtils.colorize(notMember));
                    return;
                }

                ctx.guildService().transferGuildLeadershipAsync(
                        guild.getId(), newLeader.getUniqueId(), newLeader.getName(), requesterUuid
                ).thenAccept(success -> {
                    if (success) {
                        String successMsg = ctx.languageManager().getCoreMessage("admin.transfer.success", "&aGuild {guild} leadership has been transferred to {player}!")
                                .replace("{guild}", guildName)
                                .replace("{player}", newLeaderName);
                        ctx.sendMessage(sender, ColorUtils.colorize(successMsg));
                    } else {
                        String failed = ctx.languageManager().getCoreMessage("admin.transfer.failed", "&cFailed to transfer guild leadership!");
                        ctx.sendMessage(sender, ColorUtils.colorize(failed));
                    }
                });
            });
        });
    }
}
