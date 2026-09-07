package com.guild.war.command.handler;

import org.bukkit.command.CommandSender;

/** /guildwar 子命令处理器。 */
public interface GuildWarSubCommandHandler {

    void handle(GuildWarCommandContext ctx, CommandSender sender, String[] args);
}
