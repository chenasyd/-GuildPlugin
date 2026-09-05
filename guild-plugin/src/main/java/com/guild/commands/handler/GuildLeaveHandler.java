package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class GuildLeaveHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                CompletableFuture.runAsync(() -> {
                    try {
                        GuildMember member = ctx.guildService().getGuildMember(player.getUniqueId());
                        if (member == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.leave.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (member.getRole() == Role.LEADER) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.leave.cannot-leave-as-master", "&cThe leader cannot leave the guild! Transfer leadership or delete the guild first!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        boolean success = ctx.guildService().removeGuildMember(player.getUniqueId(), player.getUniqueId());
                        if (success) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.leave.success", "&aYou have left the guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                        } else {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.leave.error", "&cAn error occurred while leaving the guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.leave.error", "&cAn error occurred while leaving the guild!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

}
