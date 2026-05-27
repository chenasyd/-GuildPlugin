package com.guild.core.utils;

/**
 * 统一的定时任务句柄，兼容 Spigot 与 Folia。
 */
public interface ScheduledTaskHandle {
    void cancel();

    default boolean isCancelled() {
        return false;
    }
}
