package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.ConfirmKickMemberGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GuildKickHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
        if (args.length < 2) {
            String message = ctx.languageManager().getCoreMessage(player, "guild.kick.usage",
                    "&cUsage: /guild kick <player name>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        if (!ctx.plugin().getMembershipRules().canKick(player)) {
            String message = ctx.languageManager().getCoreMessage(player, "general.no-permission",
                    "&cYou do not have permission to perform this action!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        String targetName = args[1];

        SubCommandErrors.runPlayerAsync(ctx.plugin(), ctx.languageManager(), player,
                "kick", "guild.kick.error", "&cAn error occurred while kicking the player!", () -> {
                    Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                    if (guild == null) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.kick.not-in-guild",
                                    "&cYou are not in any guild!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }

                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetName);
                    GuildMember targetMember = ctx.guildService().getGuildMember(targetOfflinePlayer.getUniqueId());
                    if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.kick.player-not-in-guild",
                                    "&cThat player is not in your guild!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }

                    if (targetMember.getRole() == Role.LEADER) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.kick.cannot-kick-master",
                                    "&cYou cannot kick the leader!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }

                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        ConfirmKickMemberGUI confirmGui = new ConfirmKickMemberGUI(
                                ctx.plugin(), guild, targetMember, player, "MemberManagementGUI");
                        ctx.plugin().getGuiManager().openGUI(player, confirmGui);
                    });
                });
    }
}
