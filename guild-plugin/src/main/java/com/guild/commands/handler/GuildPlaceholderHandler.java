package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class GuildPlaceholderHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                if (args.length < 2) {
                    String message = ctx.languageManager().getCoreMessage(player, "guild.placeholder.usage", "&cUsage: /guild placeholder <player|guild|rank>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
        
                String type = args[1].toLowerCase();
        
                CompletableFuture.runAsync(() -> {
                    try {
                        switch (type) {
                            case "player":
                                String playerName = player.getName();
                                String playerPlaceholder = String.format("{guild_player_%s}", playerName.toLowerCase());
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    String message = ctx.languageManager().getCoreMessage(player, "guild.placeholder.player", "&aPlayer placeholder: &f{0}");
                                    String msg = message.replace("{0}", playerPlaceholder);
                                    player.sendMessage(ColorUtils.colorize(msg));
                                });
                                break;
                            case "guild":
                                Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                                if (guild == null) {
                                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                        String message1 = ctx.languageManager().getCoreMessage(player, "guild.placeholder.not-in-guild", "&cYou are not in any guild!");
                                        player.sendMessage(ColorUtils.colorize(message1));
                                    });
                                    return;
                                }
                                String guildPlaceholder = String.format("{guild_%s}", guild.getName().toLowerCase().replace(" ", "_"));
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    String message2 = ctx.languageManager().getCoreMessage(player, "guild.placeholder.guild", "&aGuild placeholder: &f{0}");
                                    String msg2 = message2.replace("{0}", guildPlaceholder);
                                    player.sendMessage(ColorUtils.colorize(msg2));
                                });
                                break;
                            case "rank":
                                GuildMember member = ctx.guildService().getGuildMember(player.getUniqueId());
                                if (member == null) {
                                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                        String message3 = ctx.languageManager().getCoreMessage(player, "guild.placeholder.not-in-guild", "&cYou are not in any guild!");
                                        player.sendMessage(ColorUtils.colorize(message3));
                                    });
                                    return;
                                }
                                String rankPlaceholder = String.format("{guild_rank_%s}", member.getRole().name().toLowerCase());
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    String message4 = ctx.languageManager().getCoreMessage(player, "guild.placeholder.rank", "&aRank placeholder: &f{0}");
                                    String msg4 = message4.replace("{0}", rankPlaceholder);
                                    player.sendMessage(ColorUtils.colorize(msg4));
                                });
                                break;
                            default:
                                CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                    String message5 = ctx.languageManager().getCoreMessage(player, "guild.placeholder.invalid-type", "&cInvalid placeholder type!");
                                    player.sendMessage(ColorUtils.colorize(message5));
                                });
                                break;
                        }
                    } catch (Exception e) {
                        SubCommandErrors.logAndNotifyPlayer(ctx.plugin(), ctx.languageManager(), player,
                                "placeholder", e, "guild.placeholder.error",
                                "&cAn error occurred while getting placeholders!");
                    }
                });

    }

}
