package com.guild.core.module;

public interface GuildModule {
    ModuleDescriptor getDescriptor();

    void setDescriptor(ModuleDescriptor descriptor);

    void onEnable(ModuleContext context) throws Exception;

    void onDisable();

    ModuleState getState();
}
