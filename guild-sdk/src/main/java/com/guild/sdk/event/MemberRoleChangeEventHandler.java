package com.guild.sdk.event;

/**
 * 成员角色变更事件处理器。
 */
public interface MemberRoleChangeEventHandler {
    void onEvent(MemberRoleChangeEventData data);

    default Object getModuleInstance() { return null; }
}
