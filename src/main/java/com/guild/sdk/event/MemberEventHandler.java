package com.guild.sdk.event;

/**
 * 成员加入/离开事件处理器
 */
@FunctionalInterface
public interface MemberEventHandler {
    void onEvent(MemberEventData data);
}
