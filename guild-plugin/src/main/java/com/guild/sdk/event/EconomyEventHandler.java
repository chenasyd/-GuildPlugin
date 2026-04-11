package com.guild.sdk.event;

public interface EconomyEventHandler {
    void onEvent(EconomyEventData data);
    
    Object getModuleInstance();
}