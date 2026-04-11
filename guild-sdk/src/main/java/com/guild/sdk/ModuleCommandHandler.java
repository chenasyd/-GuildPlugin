package com.guild.sdk;

import org.bukkit.command.CommandSender;

/**
 * 模块命令处理器
 * 用于处理模块注册的子命令
 */
@FunctionalInterface
public interface ModuleCommandHandler {
    /**
     * 处理命令
     * @param sender 命令发送者
     * @param args 命令参数
     */
    void handle(CommandSender sender, String[] args);
}