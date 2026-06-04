package com.guild.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * 服务容器 - 管理所有服务的生命周期和依赖注入
 */
public class ServiceContainer {
    
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Map<Class<?>, ServiceLifecycle> lifecycles = new HashMap<>();
    private final Logger logger = Logger.getLogger(ServiceContainer.class.getName());
    
    /**
     * 注册服务
     */
    public <T> void register(Class<T> serviceClass, T service) {
        services.put(serviceClass, service);
        logger.info("Registering service: " + serviceClass.getSimpleName());
    }
    
    /**
     * 注册带生命周期的服务
     */
    public <T> void register(Class<T> serviceClass, T service, ServiceLifecycle lifecycle) {
        services.put(serviceClass, service);
        lifecycles.put(serviceClass, lifecycle);
        logger.info("Registering service: " + serviceClass.getSimpleName() + " (with lifecycle)");
    }
    
    /**
     * 获取服务
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceClass) {
        T service = (T) services.get(serviceClass);
        if (service == null) {
            throw new ServiceNotFoundException("Service not found: " + serviceClass.getName());
        }
        return service;
    }
    
    /**
     * 检查服务是否存在
     */
    public boolean has(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }
    
    /**
     * 启动所有服务
     */
    public CompletableFuture<Void> startAll() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Starting all services...");
            for (Map.Entry<Class<?>, ServiceLifecycle> entry : lifecycles.entrySet()) {
                try {
                    entry.getValue().start();
                    logger.info("Service started successfully: " + entry.getKey().getSimpleName());
                } catch (Exception e) {
                    logger.severe("Service failed to start: " + entry.getKey().getSimpleName() + " - " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 停止所有服务
     */
    public CompletableFuture<Void> stopAll() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Stopping all services...");
            for (Map.Entry<Class<?>, ServiceLifecycle> entry : lifecycles.entrySet()) {
                try {
                    entry.getValue().stop();
                    logger.info("Service stopped successfully: " + entry.getKey().getSimpleName());
                } catch (Exception e) {
                    logger.severe("Service failed to stop: " + entry.getKey().getSimpleName() + " - " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 关闭服务容器
     */
    public void shutdown() {
        try {
            stopAll().get();
            services.clear();
            lifecycles.clear();
            logger.info("Service container has been shut down");
        } catch (Exception e) {
            logger.severe("Error shutting down service container: " + e.getMessage());
        }
    }
    
    /**
     * 服务生命周期接口
     */
    public interface ServiceLifecycle {
        void start() throws Exception;
        void stop() throws Exception;
    }
    
    /**
     * 服务未找到异常
     */
    public static class ServiceNotFoundException extends RuntimeException {
        public ServiceNotFoundException(String message) {
            super(message);
        }
    }
}
