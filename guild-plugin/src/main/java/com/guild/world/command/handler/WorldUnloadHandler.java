package com.guild.world.command.handler;

import org.bukkit.command.CommandSender;

public final class WorldUnloadHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            ctx.sendPrefixed(sender, "world.unload.usage", "&c用法: /guildworld unload <名称>");
            return;
        }
        String name = ctx.worldService().buildWorldName(args[1]);
        ctx.sendPrefixed(sender, "world.unload.working", "&e正在卸载世界 &f{name} &e...", "{name}", name);
        ctx.worldService().unloadWorld(name).thenAccept(v ->
                ctx.sendPrefixed(sender, "world.unload.success", "&a世界 &f{name} &a已卸载。", "{name}", name)
        ).exceptionally(ex -> {
            ctx.sendPrefixed(sender, "world.unload.failed", "&c卸载失败: {error}",
                    "{error}", ctx.resolveError(sender, ex));
            return null;
        });
    }
}
