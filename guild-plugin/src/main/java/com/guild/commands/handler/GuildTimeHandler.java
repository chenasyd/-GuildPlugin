package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class GuildTimeHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.time.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        java.time.Duration duration = java.time.Duration.between(guild.getCreatedAt(), now);
                        long days = duration.toDays();
                        long hours = duration.toHours() % 24;
                
                        String message = ctx.languageManager().getCoreMessage(player, "guild.time.age", "&aGuild created: {0}\n&aGuild age: &f{1} days {2} hours");
                        message = message.replace("{0}", guild.getCreatedAt().toString());
                        message = message.replace("{1}", String.valueOf(days));
                        message = message.replace("{2}", String.valueOf(hours));
                        String finalMessage = message;
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            player.sendMessage(ColorUtils.colorize(finalMessage));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.time.error", "&cAn error occurred while fetching guild time info!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

}
