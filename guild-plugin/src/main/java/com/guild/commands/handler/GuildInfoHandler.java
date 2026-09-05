package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class GuildInfoHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.info.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        String message = ctx.languageManager().getCoreMessage(player, "guild.info.message", "&aGuild Info:\n&bName: &f{0}\n&bLevel: &f{1}\n&bLeader: &f{2}\n&bMembers: &f{3}\n&bCreated: &f{4}");
                        message = message.replace("{0}", guild.getName());
                        message = message.replace("{1}", String.valueOf(guild.getLevel()));
                        message = message.replace("{2}", guild.getLeaderName());
                        message = message.replace("{3}", String.valueOf(ctx.guildService().getGuildMemberCount(guild.getId())));
                        message = message.replace("{4}", guild.getCreatedAt().toString());
                
                        String finalMessage = message;
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            player.sendMessage(ColorUtils.colorize(finalMessage));
                        });
                    } catch (Exception e) {
                        SubCommandErrors.logAndNotifyPlayer(ctx.plugin(), ctx.languageManager(), player,
                                "info", e, "guild.info.error",
                                "&cAn error occurred while fetching guild info!");
                    }
                });

    }

}
