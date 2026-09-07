package com.guild.world.command.handler;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WorldTpHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ctx.sendPrefixed(sender, "world.tp.player-only", "&c传送命令只能由玩家执行。");
            return;
        }
        if (args.length < 2) {
            ctx.sendPrefixed(sender, "world.tp.usage", "&c用法: /guildworld tp <世界名>");
            return;
        }
        ctx.teleportPlayer(player, args[1]);
    }
}
