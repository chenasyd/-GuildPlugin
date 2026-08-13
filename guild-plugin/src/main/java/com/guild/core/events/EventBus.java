package com.guild.core.events;

import com.guild.core.utils.QuietLog;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * 事件总线 - 统一管理插件内部事件
 */
public class EventBus {
    
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<SubscriptionRecord>> moduleSubscriptions = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(EventBus.class.getName());
    
    /**
     * 模块订阅记录（用于按模块批量取消订阅）
     */
    private static class SubscriptionRecord {
        final Class<?> eventType;
        final Consumer<?> handler;
        SubscriptionRecord(Class<?> eventType, Consumer<?> handler) {
            this.eventType = eventType;
            this.handler = handler;
        }
    }
    
    /**
     * 注册事件监听器
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        QuietLog.system("Registered event listener: " + eventType.getSimpleName());
    }
    
    /**
     * 注册事件监听器（带模块归属追踪）。
     * 当调用 unsubscribeByModule(moduleId) 时，该模块注册的所有监听器将被自动移除。
     */
    public <T> void subscribe(String moduleId, Class<T> eventType, Consumer<T> listener) {
        subscribe(eventType, listener);
        moduleSubscriptions.computeIfAbsent(moduleId, k -> new CopyOnWriteArrayList<>())
            .add(new SubscriptionRecord(eventType, listener));
    }
    
    /**
     * 取消注册事件监听器
     */
    @SuppressWarnings("unchecked")
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            QuietLog.system("Unregistered event listener: " + eventType.getSimpleName());
        }
    }
    
    /**
     * 移除指定模块注册的所有事件监听器（模块卸载时调用）
     */
    public void unsubscribeByModule(String moduleId) {
        List<SubscriptionRecord> records = moduleSubscriptions.remove(moduleId);
        if (records == null) return;
        for (SubscriptionRecord record : records) {
            CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(record.eventType);
            if (eventListeners != null) {
                eventListeners.remove(record.handler);
            }
        }
        QuietLog.system("Removed all event subscriptions for module: " + moduleId);
    }
    
    /**
     * 发布事件
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<?> listener : eventListeners) {
                try {
                    ((Consumer<T>) listener).accept(event);
                } catch (Exception e) {
                    logger.severe("Event listener execution failed: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 异步发布事件
     */
    public <T> void publishAsync(T event) {
        new Thread(() -> publish(event)).start();
    }
    
    /**
     * 清除所有监听器
     */
    public void clear() {
        listeners.clear();
        moduleSubscriptions.clear();
        QuietLog.system("Cleared all event listeners");
    }
    
    /**
     * 获取监听器数量
     */
    public int getListenerCount(Class<?> eventType) {
        CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
    
    /**
     * 获取总监听器数量
     */
    public int getTotalListenerCount() {
        return listeners.values().stream().mapToInt(CopyOnWriteArrayList::size).sum();
    }
}
