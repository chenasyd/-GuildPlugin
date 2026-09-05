package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.ConfirmDemoteMemberGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class GuildDemoteHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
        if (args.length < 2) {
            String message = ctx.languageManager().getCoreMessage(player, "guild.demote.usage",
                    "&cUsage: /guild demote <player name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!ctx.plugin().getMembershipRules().canDemote(player)) {
            String message = ctx.languageManager().getCoreMessage(player, "general.no-permission",
                    "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String targetName = args[1];

        CompletableFuture.runAsync(() -> {
            try {
                Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.demote.not-in-guild",
                                "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                if (!ctx.plugin().getMembershipRules().canDemote(player)) {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.demote.only-master",
                                "&cOnly the leader can demote members!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetName);
                GuildMember targetMember = ctx.guildService().getGuildMember(targetOfflinePlayer.getUniqueId());
                if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.demote.not-in-guild-target",
                                "&cThat player is not in your guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                if (targetMember.getRole() == Role.LEADER) {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.demote.cannot-demote-master",
                                "&cYou cannot demote the leader!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                if (targetMember.getRole() != Role.OFFICER) {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.demote.not-officer",
                                "&cThat player is not an officer!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                    ConfirmDemoteMemberGUI confirmGui = new ConfirmDemoteMemberGUI(
                            ctx.plugin(), guild, targetMember, player, "GuildCommand");
                    ctx.plugin().getGuiManager().openGUI(player, confirmGui);
                });
            } catch (Exception e) {
                SubCommandErrors.logAndNotifyPlayer(ctx.plugin(), ctx.languageManager(), player,
                        "demote", e, "guild.demote.error",
                        "&cAn error occurred while demoting the player!");
            }
        });
    }
}
