package com.guild.war.command.handler;

import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WarStatusHandler implements GuildWarSubCommandHandler {

    @Override
    public void handle(GuildWarCommandContext ctx, CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            WarMatch mine = ctx.war().getMatchByPlayer(player.getUniqueId());
            if (mine != null) {
                printMatch(ctx, sender, mine);
                return;
            }
        }
        var active = ctx.war().getActiveMatches();
        if (active.isEmpty()) {
            ctx.send(sender, "war.status.none", "&7当前没有进行中的公会战");
            return;
        }
        for (WarMatch m : active) {
            printMatch(ctx, sender, m);
        }
    }

    static void printMatch(GuildWarCommandContext ctx, CommandSender sender, WarMatch m) {
        ctx.send(sender, "war.status.title", "&6── 公会战 #{id} ──",
                "{id}", String.valueOf(m.id()));
        ctx.send(sender, "war.status.phase-mode", "&7阶段: &f{phase} &7模式: &f{mode}",
                "{phase}", ctx.phaseName(sender, m.phase()),
                "{mode}", m.mode().displayName(ctx.plugin(), sender));
        ctx.send(sender, "war.status.teams",
                "&a{a} &7({ac}/{max}) &fvs &c{b} &7({bc}/{max})",
                "{a}", m.guildAName(),
                "{ac}", String.valueOf(m.countSide(WarTeamSide.A)),
                "{max}", String.valueOf(m.maxPerTeam()),
                "{b}", m.guildBName(),
                "{bc}", String.valueOf(m.countSide(WarTeamSide.B)));
        ctx.send(sender, "war.status.score",
                "&7比分: &a{sa} &7: &c{sb} &7预设: &f{preset}",
                "{sa}", String.valueOf(m.scoreA()),
                "{sb}", String.valueOf(m.scoreB()),
                "{preset}", m.presetName());
        if (m.phase() == WarPhase.ACTIVE || m.phase() == WarPhase.COUNTDOWN) {
            StringBuilder sb = new StringBuilder();
            for (WarParticipant p : m.participantList()) {
                if (p.isFighting()) {
                    sb.append(p.side() == WarTeamSide.A ? "&a" : "&c").append(p.name()).append(" ");
                }
            }
            ctx.send(sender, "war.status.alive", "&7存活: {list}", "{list}", sb.toString());
        }
    }
}
