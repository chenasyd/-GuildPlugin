package com.guild.commands.handler.admin;

import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import org.bukkit.command.CommandSender;

public class GuildAdminListHandler implements GuildAdminSubCommandHandler {

    @Override
    public void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args) {
        ctx.guildService().getAllGuildsAsync().thenAccept(guilds -> {
            String title = ctx.languageManager().getCoreMessage("admin.list.title", "&6=== Guild List ===");
            ctx.sendMessage(sender, ColorUtils.colorize(title));
            if (guilds.isEmpty()) {
                String empty = ctx.languageManager().getCoreMessage("admin.list.empty", "&cNo guilds available");
                ctx.sendMessage(sender, ColorUtils.colorize(empty));
                return;
            }

            for (Guild guild : guilds) {
                String statusKey = guild.isFrozen() ? "admin.list.status-frozen" : "admin.list.status-normal";
                String status = ctx.languageManager().getCoreMessage(statusKey, guild.isFrozen() ? "&c[冻结]" : "&a[正常]");
                String format = ctx.languageManager().getCoreMessage("admin.list.format",
                        "&e{name} &7- Leader: &f{leader} &7- Level: &f{level} &7{status}");
                String message = format
                        .replace("{name}", guild.getName())
                        .replace("{leader}", guild.getLeaderName())
                        .replace("{level}", String.valueOf(guild.getLevel()))
                        .replace("{status}", status);
                ctx.sendMessage(sender, ColorUtils.colorize(message));
            }
        });
    }
}
