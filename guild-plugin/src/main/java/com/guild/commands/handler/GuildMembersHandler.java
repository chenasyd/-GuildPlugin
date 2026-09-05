package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildMember.Role;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class GuildMembersHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.members.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        List<GuildMember> members = ctx.guildService().getGuildMembers(guild.getId());
                        if (members.isEmpty()) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.members.empty", "&cThere are no members in the guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.members.title", "&aGuild Member List:");
                            player.sendMessage(ColorUtils.colorize(message));
                    
                            for (GuildMember m : members) {
                                String memberMessage = ctx.languageManager().getCoreMessage(player, "guild.members.member", "&b{0} - &f{1}");
                                memberMessage = memberMessage.replace("{0}", m.getPlayerName());
                                memberMessage = memberMessage.replace("{1}", m.getRole() == Role.LEADER ? "会长" : (m.getRole() == Role.OFFICER ? "副会长" : "成员"));
                                player.sendMessage(ColorUtils.colorize(memberMessage));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.members.error", "&cAn error occurred while fetching member list!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

}
