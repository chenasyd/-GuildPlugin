package com.guild.world.command.handler;

import org.bukkit.command.CommandSender;

/** /guildworld 子命令处理器。 */
public interface GuildWorldSubCommandHandler {

    void handle(GuildWorldCommandContext ctx, CommandSender sender, String[] args);
}
