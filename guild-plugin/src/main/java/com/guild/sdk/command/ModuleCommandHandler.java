package com.guild.sdk.command;

import org.bukkit.command.CommandSender;

/**
 * 模块命令处理器接口 - 模块可注册自定义子命令
 * <p>
 * 使用示例：
 * <pre>{@code
 * ctx.getApi().registerSubCommand("guild", "blackhistory",
 *     (sender, args) -> {
 *         // 处理 /guild blackhistory <玩家名>
 *     },
 *     "guild.admin"  // 权限节点
 * );
 * }</pre>
 *
 * @param sender 命令发送者（可以是控制台或玩家）
 * @param args 命令参数数组（不含命令名本身和子命令名）
 */
@FunctionalInterface
public interface ModuleCommandHandler {

    /**
     * 处理子命令执行
     *
     * @param sender 命令发送者
     * @param args   参数列表
     */
    void handle(CommandSender sender, String[] args);
}
