package com.guild.world.command.handler;

import org.bukkit.command.CommandSender;

public final class WorldDeleteHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            ctx.sendPrefixed(sender, "world.delete.usage", "&c用法: /guildworld delete <名称> [--force]");
            return;
        }
        String name = ctx.worldService().buildWorldName(args[1]);
        boolean force = WorldCommandArgs.containsFlag(args, "--force");
        ctx.sendPrefixed(sender, "world.delete.working", "&e正在删除世界 &f{name} &e...", "{name}", name);
        ctx.worldService().deleteWorld(name, force).thenAccept(v ->
                ctx.sendPrefixed(sender, "world.delete.success",
                        "&a世界 &f{name} &a已删除（注册表记录已清除）。", "{name}", name)
        ).exceptionally(ex -> {
            ctx.sendPrefixed(sender, "world.delete.failed", "&c删除失败: {error}",
                    "{error}", ctx.resolveError(sender, ex));
            return null;
        });
    }
}
