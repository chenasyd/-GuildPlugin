package com.guild.core.module.hook;

public interface HookPoint {
    void unregisterByModule(String moduleId);

    void unregisterAll();
}
