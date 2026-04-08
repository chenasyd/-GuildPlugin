package com.guild.sdk.event;

/**
 * 工会创建/删除事件处理器
 */
@FunctionalInterface
public interface GuildEventHandler {
    void onEvent(GuildEventData data);
}
