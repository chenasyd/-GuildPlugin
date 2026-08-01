package com.guild.core.events;

import java.util.function.Consumer;

/**
 * 事件总线 SDK 桩。
 * 运行时由主插件中的同名类提供真实实现。
 */
public class EventBus {

    /** 注册事件监听器 */
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
    }

    /** 注册事件监听器（带模块归属追踪） */
    public <T> void subscribe(String moduleId, Class<T> eventType, Consumer<T> listener) {
    }

    /** 取消注册事件监听器 */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
    }

    /** 移除指定模块注册的所有事件监听器 */
    public void unsubscribeByModule(String moduleId) {
    }

    /** 发布事件 */
    public <T> void publish(T event) {
    }

    /** 异步发布事件 */
    public <T> void publishAsync(T event) {
    }

    /** 清除所有监听器 */
    public void clear() {
    }

    /** 获取监听器数量 */
    public int getListenerCount(Class<?> eventType) {
        return 0;
    }

    /** 获取总监听器数量 */
    public int getTotalListenerCount() {
        return 0;
    }
}
