package com.guild.sdk.placeholder;

import org.bukkit.entity.Player;

/**
 * 占位符提供者接口 —— 模块实现此接口以注册自定义 PlaceholderAPI 扩展。
 * <p>
 * 格式: {@code %guild_module_<identifier>_<params>_<fallback>%}
 * fallback 部分可选：无公会或无数据时返回 fallback 文本（由框架自动处理后缀截断）。
 */
public interface PlaceholderProvider {
    /** 占位符标识符，用于 %guild_module_{identifier}_...% 路由。 */
    String getIdentifier();

    /**
     * 处理占位符请求。
     * @param player 请求的玩家（可为 null）
     * @param params 占位符参数（不包含 fallback 后缀，框架已截断）
     * @return 替换文本，返回 null 表示不处理
     */
    String onRequest(Player player, String params);
}
