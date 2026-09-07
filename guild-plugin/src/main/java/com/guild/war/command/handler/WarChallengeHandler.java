package com.guild.war.command.handler;

import com.guild.war.model.VictoryMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WarChallengeHandler implements GuildWarSubCommandHandler {

    @Override
    public void handle(GuildWarCommandContext ctx, CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ctx.send(sender, "war.player-only", "&c仅玩家可用");
            return;
        }
        if (args.length < 2) {
            ctx.send(sender, "war.challenge.usage",
                    "&c用法: /guildwar challenge <公会名|标签> [--preset x] [--mode first|timed|survive] [--max N] [--score N] [--time SEC]");
            return;
        }
        String target = args[1];
        String preset = GuildWarCommandContext.flag(args, "--preset");
        VictoryMode mode = VictoryMode.parse(GuildWarCommandContext.flag(args, "--mode"));
        Integer max = GuildWarCommandContext.intFlag(args, "--max");
        Integer score = GuildWarCommandContext.intFlag(args, "--score");
        Integer time = GuildWarCommandContext.intFlag(args, "--time");

        ctx.war().challenge(player, target, preset, mode, max, score, time)
                .whenComplete((match, err) -> {
                    if (err != null) {
                        ctx.reply(player, err, null, null);
                    } else {
                        ctx.send(player, "war.challenge.success",
                                "&a已向 &f{guild} &a发起挑战（#{id}，{mode}）",
                                "{guild}", match.guildBName(),
                                "{id}", String.valueOf(match.id()),
                                "{mode}", match.mode().displayName(ctx.plugin(), player));
                    }
                });
    }
}
