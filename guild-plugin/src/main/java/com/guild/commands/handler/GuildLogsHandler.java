package com.guild.commands.handler;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import org.bukkit.entity.Player;

public class GuildLogsHandler implements GuildSubCommandHandler {

    @Override
    public void handle(GuildCommandContext ctx, Player player, String[] args) {
        SubCommandErrors.runPlayerAsync(ctx.plugin(), ctx.languageManager(), player,
                "logs", "guild.logs.error", "&cAn error occurred while fetching guild logs!", () -> {
                    Guild guild = ctx.guildService().getPlayerGuild(player.getUniqueId());
                    if (guild == null) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.logs.not-in-guild", "&cYou are not in any guild!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }

                    if (!ctx.plugin().getMembershipRules().canManageGuild(player)) {
                        CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                            String message = ctx.languageManager().getCoreMessage(player, "guild.logs.no-permission", "&cYou do not have permission to view logs!");
                            player.sendMessage(ColorUtils.colorize(message));
                        });
                        return;
                    }

                    CompatibleScheduler.runTask(ctx.plugin(), player, () -> {
                        String message = ctx.languageManager().getCoreMessage(player, "guild.logs.title", "&aGuild Logs:");
                        player.sendMessage(ColorUtils.colorize(message));
                        player.sendMessage(ColorUtils.colorize("&b- 日志功能正在开发中..."));
                    });
                });
    }

}
