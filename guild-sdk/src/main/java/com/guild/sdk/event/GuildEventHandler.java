package com.guild.sdk.event;

@FunctionalInterface
public interface GuildEventHandler {
    void onEvent(GuildEventData data);

    default Object getModuleInstance() {
        return null;
    }
}
