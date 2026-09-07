package com.guild.war.command.handler;

import com.guild.core.language.CoreMsg;
import com.guild.core.language.LocalizedException;
import com.guild.core.utils.ColorUtils;
import org.bukkit.command.CommandSender;

public final class WarAdminHandler implements GuildWarSubCommandHandler {

    @Override
    public void handle(GuildWarCommandContext ctx, CommandSender sender, String[] args) {
        if (!sender.hasPermission(GuildWarCommandContext.PERM_ADMIN)) {
            ctx.send(sender, "war.admin-permission", "&c需要 guild.war.admin");
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("end")) {
            ctx.send(sender, "war.admin.end.usage", "&c用法: /guildwar admin end <matchId>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ctx.send(sender, "war.admin.end.invalid-id", "&c无效 ID");
            return;
        }
        ctx.war().forceEnd(id, "war.reason.admin-end").whenComplete((v, err) -> {
            if (err != null) {
                String body = LocalizedException.resolveThrowable(ctx.plugin(), sender, err);
                String prefix = CoreMsg.raw(ctx.plugin(), sender, "war.prefix", "&c[公会战] &r");
                sender.sendMessage(ColorUtils.colorize(prefix + body));
            } else {
                ctx.send(sender, "war.admin.end.ok", "&a已结束对局 #{id}",
                        "{id}", String.valueOf(id));
            }
        });
    }
}
