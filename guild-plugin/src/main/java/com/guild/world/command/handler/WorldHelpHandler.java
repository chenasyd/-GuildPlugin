package com.guild.world.command.handler;

import org.bukkit.command.CommandSender;

public final class WorldHelpHandler implements GuildWorldSubCommandHandler {

    @Override
    public void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args) {
        ctx.sendPlain(sender, "world.help.title", "&6========== GuildWorld 多世界管理 ==========");
        ctx.sendPlain(sender, "world.help.create",
                "&e/guildworld create <名称> [--type battle|edit|template] [--preset <预设>] [--guild <ID>] &7- 创建虚空世界");
        ctx.sendPlain(sender, "world.help.list", "&e/guildworld list &7- 列出受管世界");
        ctx.sendPlain(sender, "world.help.info", "&e/guildworld info <名称> &7- 查看世界详情");
        ctx.sendPlain(sender, "world.help.load", "&e/guildworld load <名称> &7- 加载世界");
        ctx.sendPlain(sender, "world.help.unload", "&e/guildworld unload <名称> &7- 卸载世界");
        ctx.sendPlain(sender, "world.help.delete", "&e/guildworld delete <名称> [--force] &7- 删除世界");
        ctx.sendPlain(sender, "world.help.restore",
                "&e/guildworld restore [--run|--list|--load <名称>|--delete <名称>] &7- 恢复管理");
        ctx.sendPlain(sender, "world.help.tp", "&e/guildworld tp <名称> &7- 传送进入受管世界（Folia 安全）");
        ctx.sendPlain(sender, "world.help.edit", "&e/guildworld edit ... &7- 编辑模式（create/tp/save/leave）");
        ctx.sendPlain(sender, "world.help.preset", "&e/guildworld preset ... &7- 预设管理（list/info/delete/bind）");
    }
}
