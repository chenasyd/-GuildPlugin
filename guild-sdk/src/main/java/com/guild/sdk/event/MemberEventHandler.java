package com.guild.sdk.event;

@FunctionalInterface
public interface MemberEventHandler {
    void onEvent(MemberEventData data);

    default Object getModuleInstance() {
        return null;
    }
}
