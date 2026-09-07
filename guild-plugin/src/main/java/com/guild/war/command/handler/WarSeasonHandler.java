package com.guild.war.command.handler;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.WarSeasonGUI;
import com.guild.war.season.WarSeasonService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WarSeasonHandler implements GuildWarSubCommandHandler {

    @Override
    public void handle(GuildWarCommandContext ctx, CommandSender sender, String[] args) {
        WarSeasonService seasonService = ctx.plugin().getWarSeasonService();
        if (seasonService == null) {
            ctx.send(sender, "war.season.unavailable", "&c赛季系统未就绪");
            return;
        }
        String seasonId = seasonService.currentSeasonId();
        seasonService.getLeaderboardAsync(seasonId, 27).thenAccept(rows -> {
            CompatibleScheduler.runTask(ctx.plugin(), () -> {
                if (sender instanceof Player player && ctx.plugin().getGuiManager() != null) {
                    ctx.plugin().getGuiManager().openGUI(player,
                            new WarSeasonGUI(ctx.plugin(), player, seasonId, rows));
                } else {
                    ctx.send(sender, "war.season.header",
                            "&6── 赛季 &f{season} &6排行 ──", "{season}", seasonId);
                    int i = 1;
                    for (WarSeasonService.SeasonRow row : rows) {
                        ctx.send(sender, "war.season.row",
                                "&e#{rank} &f{name} &a{w}&7/&c{l}&7/&8{d} &7kills=&e{k}",
                                "{rank}", String.valueOf(i++),
                                "{name}", row.guildName(),
                                "{w}", String.valueOf(row.wins()),
                                "{l}", String.valueOf(row.losses()),
                                "{d}", String.valueOf(row.draws()),
                                "{k}", String.valueOf(row.kills()));
                    }
                    if (rows.isEmpty()) {
                        ctx.send(sender, "war.season.empty", "&7本赛季暂无数据");
                    }
                }
            });
        });
    }
}
