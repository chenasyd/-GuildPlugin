package com.guild.war.command.handler;

import org.bukkit.command.CommandSender;

public final class WarHelpHandler implements GuildWarSubCommandHandler {

    @Override
    public void handle(GuildWarCommandContext ctx, CommandSender sender, String[] args) {
        ctx.send(sender, "war.help.title", "&6── 公会战帮助 ──");
        ctx.send(sender, "war.help.challenge",
                "&e/guildwar challenge <公会> [--preset] [--mode first|timed|survive] [--max] [--score] [--time]");
        ctx.send(sender, "war.help.accept-deny", "&e/guildwar accept|deny &7- 接受/拒绝挑战（官员）");
        ctx.send(sender, "war.help.join-leave", "&e/guildwar join|leave &7- 报名/退出");
        ctx.send(sender, "war.help.ready", "&e/guildwar ready &7- 报名阶段提前开局（官员，双方都 ready）");
        ctx.send(sender, "war.help.cancel", "&e/guildwar cancel &7- 取消未开战对局（官员）");
        ctx.send(sender, "war.help.status", "&e/guildwar status &7- 查看状态");
        ctx.send(sender, "war.help.report", "&e/guildwar report [id] &7- 查看战报");
        ctx.send(sender, "war.help.season", "&e/guildwar season &7- 本赛季排行");
        if (sender.hasPermission(GuildWarCommandContext.PERM_ADMIN)) {
            ctx.send(sender, "war.help.export", "&e/guildwar export <id> [json|csv] &7- 导出战报到 plugins/.../exports/");
            ctx.send(sender, "war.help.admin", "&e/guildwar admin end <id> &7- 强制结束");
        }
    }
}
