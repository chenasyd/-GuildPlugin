package com.guild.commands.handler.admin;

import org.bukkit.command.CommandSender;

/** 公会管理员子命令处理器。 */
public interface GuildAdminSubCommandHandler {

    void handle(GuildAdminCommandContext ctx, CommandSender sender, String[] args);
}
