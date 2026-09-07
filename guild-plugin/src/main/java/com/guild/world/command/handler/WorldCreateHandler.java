package com.guild.world.command.handler;

import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldType;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

public final class WorldCreateHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            ctx.sendPrefixed(sender, "world.create.usage",
                    "&c用法: /guildworld create <名称> [--type battle|edit|template] [--preset <预设>] [--guild <公会ID>]");
            return;
        }
        String name = args[1];
        WorldType type = WorldCommandArgs.parseType(WorldCommandArgs.flagValue(args, 2, "--type"));
        String preset = WorldCommandArgs.flagValue(args, 2, "--preset");
        String guildId = WorldCommandArgs.flagValue(args, 2, "--guild");

        ctx.sendPrefixed(sender, "world.create.working", "&e正在创建虚空世界 &f{name} &e...", "{name}", name);
        CompletableFuture<?> create;
        if (preset != null && ctx.worldService().getPresets().hasSchematicFile(preset)) {
            create = ctx.worldService().createWorldFromPreset(name, preset);
        } else {
            create = ctx.worldService().createVoidWorld(name, type, preset, guildId, null);
        }
        create.thenAccept(gwObj -> {
            GuildWorld gw = (GuildWorld) gwObj;
            ctx.sendPrefixed(sender, "world.create.success",
                    "&a虚空世界 &f{world} &a创建成功！类型: &f{type}&a，状态: &f{status}&a，预设: &f{preset}",
                    "{world}", gw.getWorldName(),
                    "{type}", String.valueOf(gw.getType()),
                    "{status}", String.valueOf(gw.getStatus()),
                    "{preset}", preset != null ? preset : "-");
        }).exceptionally(ex -> {
            ctx.sendPrefixed(sender, "world.create.failed", "&c创建失败: {error}",
                    "{error}", ctx.resolveError(sender, ex));
            return null;
        });
    }
}
