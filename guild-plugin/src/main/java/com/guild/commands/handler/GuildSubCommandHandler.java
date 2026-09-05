package com.guild.commands.handler;

import org.bukkit.entity.Player;

/** 公会子命令处理器：解析参数 → 鉴权 → 调 Service / 开 GUI。 */
public interface GuildSubCommandHandler {

    void handle(GuildCommandContext ctx, Player player, String[] args);
}
