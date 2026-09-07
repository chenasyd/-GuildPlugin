package com.guild.world.command.handler;

import org.bukkit.command.CommandSender;

public final class WorldLoadHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            ctx.sendPrefixed(sender, "world.load.usage", "&c用法: /guildworld load <名称>");
            return;
        }
        String name = ctx.worldService().buildWorldName(args[1]);
        ctx.sendPrefixed(sender, "world.load.working", "&e正在加载世界 &f{name} &e...", "{name}", name);
        ctx.worldService().loadWorld(name).thenAccept(gw ->
                ctx.sendPrefixed(sender, "world.load.success", "&a世界 &f{world} &a加载成功！",
                        "{world}", gw.getWorldName())
        ).exceptionally(ex -> {
            ctx.sendPrefixed(sender, "world.load.failed", "&c加载失败: {error}",
                    "{error}", ctx.resolveError(sender, ex));
            return null;
        });
    }
}
