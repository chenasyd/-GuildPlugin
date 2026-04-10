package com.guild.sdk.command;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface ModuleCommandHandler {
    void handle(CommandSender sender, String[] args);
}
