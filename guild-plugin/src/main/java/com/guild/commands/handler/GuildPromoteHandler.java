package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.ConfirmPromoteMemberGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class GuildPromoteHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
        if (args.length < 2) {
            String message = ctx.languageManager().getCoreMessage(player, "guild.promote.usage",
                    "&cUsage: /guild promote <player name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!ctx.plugin().getMembershipRules().canPromote(player)) {
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
                        String message = ctx.languageManager().getCoreMessage(player, "guild.promote.not-in-guild",
                                "&cYou are not in any guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                if (!ctx.plugin().getMembershipRules().canPromote(player)) {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.promote.only-master",
                                "&cOnly the leader can promote members!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetName);
                GuildMember targetMember = ctx.guildService().getGuildMember(targetOfflinePlayer.getUniqueId());
                if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.promote.not-in-guild-target",
                                "&cThat player is not in your guild!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                if (targetMember.getRole() == Role.LEADER) {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.promote.already-master",
                                "&cThat player is already the leader!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                if (targetMember.getRole() == Role.OFFICER) {
                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.promote.already-officer",
                                "&cThat player is already an officer!");
                        player.sendMessage(ColorUtils.colorize(message));
                    });
                    return;
                }

                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                    ConfirmPromoteMemberGUI confirmGui = new ConfirmPromoteMemberGUI(
                            ctx.plugin(), guild, targetMember, player, "GuildCommand");
                    ctx.plugin().getGuiManager().openGUI(player, confirmGui);
                });
            } catch (Exception e) {
                SubCommandErrors.logAndNotifyPlayer(ctx.plugin(), ctx.languageManager(), player,
                        "promote", e, "guild.promote.error",
                        "&cAn error occurred while promoting the player!");
            }
        });
    }
}
