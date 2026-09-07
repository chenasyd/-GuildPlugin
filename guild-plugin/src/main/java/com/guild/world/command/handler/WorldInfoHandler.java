package com.guild.world.command.handler;

import com.guild.world.model.GuildWorld;
import com.guild.world.util.WorldFiles;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class WorldInfoHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        if (args.length < 2) {
            ctx.sendPrefixed(sender, "world.info.usage", "&c用法: /guildworld info <名称>");
            return;
        }
        String name = ctx.worldService().buildWorldName(args[1]);
        GuildWorld gw = ctx.worldService().getWorld(name);
        if (gw == null) {
            ctx.sendPrefixed(sender, "world.info.not-managed",
                    "&c世界 &f{name} &c不在受管列表中！", "{name}", name);
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ctx.sendPlain(sender, "world.info.title",
                "&6========== 世界信息: {world} ==========", "{world}", gw.getWorldName());
        ctx.sendPlain(sender, "world.info.type", "&e类型: &f{type}", "{type}", String.valueOf(gw.getType()));
        ctx.sendPlain(sender, "world.info.status", "&e状态: &f{status}",
                "{status}", WorldCommandArgs.statusText(gw.getStatus()));
        ctx.sendPlain(sender, "world.info.preset", "&e预设: &f{preset}",
                "{preset}", gw.getPresetName().isEmpty() ? "-" : gw.getPresetName());
        ctx.sendPlain(sender, "world.info.spawn", "&e出生点: &f{spawn}",
                "{spawn}", gw.getSpawn().isEmpty() ? "-" : gw.getSpawn());
        ctx.sendPlain(sender, "world.info.guild", "&e关联公会: &f{guild}",
                "{guild}", gw.getOwnerGuildId().isEmpty() ? "-" : gw.getOwnerGuildId());
        ctx.sendPlain(sender, "world.info.created", "&e创建时间: &f{time}",
                "{time}", fmt.format(new Date(gw.getCreatedAt())));
        ctx.sendPlain(sender, "world.info.last-active", "&e最近活动: &f{time}",
                "{time}", fmt.format(new Date(gw.getLastActiveAt())));
        String loadedState = Bukkit.getWorld(name) != null
                ? ctx.t(sender, "world.loaded.yes", "&a已加载")
                : ctx.t(sender, "world.loaded.no", "&7未加载");
        ctx.sendPlain(sender, "world.info.load-state", "&e加载状态: &f{loaded}", "{loaded}", loadedState);
        File worldDir = WorldFiles.resolveWorldDirectory(name);
        boolean exists = WorldFiles.worldDirectoryExists(name);
        String existsText = exists
                ? ctx.t(sender, "world.info.folder.exists", "&a存在")
                : ctx.t(sender, "world.info.folder.missing", "&c缺失");
        ctx.sendPlain(sender, "world.info.folder", "&e文件夹: &f{exists} &7({path})",
                "{exists}", existsText, "{path}", worldDir.getPath());
        if (WorldFiles.usesPaper26Layout()) {
            ctx.sendPlain(sender, "world.info.paper26-hint",
                    "&7提示: Paper/Folia 26+ 世界位于 <level>/dimensions/<ns>/<name>/");
        }
    }
}
