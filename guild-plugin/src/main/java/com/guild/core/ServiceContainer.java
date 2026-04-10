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
        logger.info("注册服务: " + serviceClass.getSimpleName());
    }
    
    /**
     * 注册带生命周期的服务
     */
    public <T> void register(Class<T> serviceClass, T service, ServiceLifecycle lifecycle) {
        services.put(serviceClass, service);
        lifecycles.put(serviceClass, lifecycle);
        logger.info("注册服务: " + serviceClass.getSimpleName() + " (带生命周期)");
    }
    
    /**
     * 获取服务
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceClass) {
        T service = (T) services.get(serviceClass);
        if (service == null) {
            throw new ServiceNotFoundException("服务未找到: " + serviceClass.getName());
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
            logger.info("正在启动所有服务...");
            for (Map.Entry<Class<?>, ServiceLifecycle> entry : lifecycles.entrySet()) {
                try {
                    entry.getValue().start();
                    logger.info("服务启动成功: " + entry.getKey().getSimpleName());
                } catch (Exception e) {
                    logger.severe("服务启动失败: " + entry.getKey().getSimpleName() + " - " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * 停止所有服务
     */
    public CompletableFuture<Void> stopAll() {
        return CompletableFuture.runAsync(() -> {
            logger.info("正在停止所有服务...");
            for (Map.Entry<Class<?>, ServiceLifecycle> entry : lifecycles.entrySet()) {
                try {
                    entry.getValue().stop();
                    logger.info("服务停止成功: " + entry.getKey().getSimpleName());
                } catch (Exception e) {
                    logger.severe("服务停止失败: " + entry.getKey().getSimpleName() + " - " + e.getMessage());
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
            logger.info("服务容器已关闭");
        } catch (Exception e) {
            logger.severe("关闭服务容器时发生错误: " + e.getMessage());
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
