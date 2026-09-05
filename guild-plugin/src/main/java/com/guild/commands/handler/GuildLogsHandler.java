package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public class GuildLogsHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                        if (guild == null) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.logs.not-in-guild", "&cYou are not in any guild!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        if (!ctx.guildService().hasGuildPermission(player.getUniqueId())) {
                            CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                                String message = ctx.languageManager().getCoreMessage(player, "guild.logs.no-permission", "&cYou do not have permission to view logs!");
                                player.sendMessage(ColorUtils.colorize(message));
                            });
                            return;
                        }
                
                        // 这里应该显示公会日志
                        // 暂时简化处理
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.logs.title", "&aGuild Logs:");
                            player.sendMessage(ColorUtils.colorize(message));
                            player.sendMessage(ColorUtils.colorize("&b- 日志功能正在开发中..."));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.logs.error", "&cAn error occurred while fetching guild logs!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                    }
                });

    }

}
