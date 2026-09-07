package com.guild.war.command.handler;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarReportSnapshot;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WarReportHandler implements GuildWarSubCommandHandler {

    @Override
    public void handle(GuildWarCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length >= 2) {
            try {
                int id = Integer.parseInt(args[1]);
                ctx.war().reports().getByReportIdAsync(id).thenAccept(snap ->
                        CompatibleScheduler.runTask(ctx.plugin(),
                                () -> showReport(ctx, sender, snap)));
            } catch (NumberFormatException e) {
                ctx.send(sender, "war.report.bad-id", "&c战报 ID 无效");
            }
            return;
        }
        if (!(sender instanceof Player player)) {
            ctx.send(sender, "war.report.need-id", "&c控制台请使用 /guildwar report <id>");
            return;
        }
        ctx.war().reports().getLatestForPlayerAsync(player.getUniqueId()).thenAccept(snap ->
                CompatibleScheduler.runTask(ctx.plugin(), () -> showReport(ctx, sender, snap)));
    }

    static void showReport(GuildWarCommandContext ctx, CommandSender sender, WarReportSnapshot snap) {
        if (snap == null) {
            ctx.send(sender, "war.report.none", "&7没有找到战报");
            return;
        }
        String id = snap.reportId() != null ? String.valueOf(snap.reportId()) : "?";
        ctx.send(sender, "war.report.header",
                "&6── 战报 #{id} ── &f{a} &a{sa}&7:&c{sb} &f{b} &7→ &e{winner}",
                "{id}", id,
                "{a}", snap.guildAName(),
                "{b}", snap.guildBName(),
                "{sa}", String.valueOf(snap.scoreA()),
                "{sb}", String.valueOf(snap.scoreB()),
                "{winner}", snap.winnerName());
        ctx.send(sender, "war.report.meta",
                "&7模式: &f{mode} &7原因: &f{reason} &7耗时: &f{sec}s",
                "{mode}", snap.mode().name(),
                "{reason}", snap.endReason() != null ? snap.endReason() : "-",
                "{sec}", String.valueOf(snap.durationMs() / 1000));
        for (WarParticipantSnapshot p : snap.participants()) {
            ctx.send(sender, "war.report.player",
                    "&7  {side} &f{name} &7kills=&e{kills}{elim}",
                    "{side}", p.side().name(),
                    "{name}", p.name(),
                    "{kills}", String.valueOf(p.kills()),
                    "{elim}", p.eliminated() ? " &8(out)" : "");
        }
    }
}
