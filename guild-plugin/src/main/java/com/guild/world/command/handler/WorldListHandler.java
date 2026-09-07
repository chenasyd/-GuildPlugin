package com.guild.world.command.handler;

import com.guild.world.model.GuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Collection;

public final class WorldListHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        Collection<GuildWorld> worlds = ctx.worldService().getWorlds();
        ctx.sendPlain(sender, "world.list.title",
                "&6========== 受管世界列表 ({count}) ==========",
                "{count}", String.valueOf(worlds.size()));
        if (worlds.isEmpty()) {
            ctx.sendPlain(sender, "world.list.empty",
                    "&7暂无受管世界，使用 &e/guildworld create <名称> &7创建。");
            return;
        }
        for (GuildWorld gw : worlds) {
            boolean loaded = Bukkit.getWorld(gw.getWorldName()) != null;
            String loadedText = loaded
                    ? ctx.t(sender, "world.loaded.yes", "&a已加载")
                    : ctx.t(sender, "world.loaded.no", "&7未加载");
            ctx.sendPlain(sender, "world.list.entry",
                    "&e{world} &7| &f{type} &7| &f{status} &7| {loaded} &7| 预设: &f{preset}",
                    "{world}", gw.getWorldName(),
                    "{type}", String.valueOf(gw.getType()),
                    "{status}", WorldCommandArgs.statusText(gw.getStatus()),
                    "{loaded}", loadedText,
                    "{preset}", gw.getPresetName().isEmpty() ? "&7-" : gw.getPresetName());
        }
    }
}
