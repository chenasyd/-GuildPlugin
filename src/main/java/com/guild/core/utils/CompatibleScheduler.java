package com.guild.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * 兼容性调度器 - 支持Spigot和Folia
 */
public class CompatibleScheduler {
    
    /**
     * 在主线程执行任务
     */
    public static void runTask(Plugin plugin, Runnable task) {
        if (ServerUtils.isFolia()) {
            try {
                // 使用反射调用Folia的全局区域调度器
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                globalScheduler.getClass().getMethod("run", Plugin.class, java.util.function.Consumer.class)
                    .invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run());
            } catch (Exception e) {
                // 如果Folia API不可用，回退到传统调度器
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * 在指定位置执行任务
     */
    public static void runTask(Plugin plugin, Location location, Runnable task) {
        if (ServerUtils.isFolia()) {
            try {
                // 使用反射调用Folia的区域调度器
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                regionScheduler.getClass().getMethod("run", Plugin.class, Location.class, java.util.function.Consumer.class)
                    .invoke(regionScheduler, plugin, location, (java.util.function.Consumer<Object>) scheduledTask -> task.run());
            } catch (Exception e) {
                // 如果Folia API不可用，回退到传统调度器
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * 在指定实体所在区域执行任务
     */
    public static void runTask(Plugin plugin, Entity entity, Runnable task) {
        if (ServerUtils.isFolia()) {
            try {
                // 使用反射调用Folia的实体调度器
                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                entityScheduler.getClass().getMethod("run", Plugin.class, java.util.function.Consumer.class, Runnable.class)
                    .invoke(entityScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run(), (Runnable) () -> {});
            } catch (Exception e) {
                // 如果Folia API不可用，回退到传统调度器
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * 延迟执行任务
     */
    public static void runTaskLater(Plugin plugin, Runnable task, long delay) {
        if (ServerUtils.isFolia()) {
            try {
                // 使用反射调用Folia的全局区域调度器
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                globalScheduler.getClass().getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, long.class)
                    .invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run(), delay);
            } catch (Exception e) {
                // 如果Folia API不可用，回退到传统调度器
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }
    
    /**
     * 在指定位置延迟执行任务
     */
    public static void runTaskLater(Plugin plugin, Location location, Runnable task, long delay) {
        if (ServerUtils.isFolia()) {
            try {
                // 使用反射调用Folia的区域调度器
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                regionScheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, java.util.function.Consumer.class, long.class)
                    .invoke(regionScheduler, plugin, location, (java.util.function.Consumer<Object>) scheduledTask -> task.run(), delay);
            } catch (Exception e) {
                // 如果Folia API不可用，回退到传统调度器
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }
    
    /**
     * 异步执行任务
     */
    public static void runTaskAsync(Plugin plugin, Runnable task) {
        if (ServerUtils.isFolia()) {
            try {
                // 使用反射调用Folia的异步调度器
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                asyncScheduler.getClass().getMethod("runNow", Plugin.class, java.util.function.Consumer.class)
                    .invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run());
            } catch (Exception e) {
                // 如果Folia API不可用，回退到传统调度器
                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
    
    /**
     * 重复执行任务
     */
    public static void runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (ServerUtils.isFolia()) {
            try {
                // 使用反射调用Folia的全局区域调度器
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                globalScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class)
                    .invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run(), delay, period);
            } catch (Exception e) {
                // 如果Folia API不可用，回退到传统调度器
                Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }
    
    /**
     * 检查是否在主线程
     */
    public static boolean isPrimaryThread() {
        if (ServerUtils.isFolia()) {
            try {
                // 使用反射调用Folia的全局线程检查
                return (Boolean) Bukkit.class.getMethod("isGlobalTickThread").invoke(null);
            } catch (Exception e) {
                // 如果Folia API不可用，回退到传统检查
                return Bukkit.isPrimaryThread();
            }
        } else {
            return Bukkit.isPrimaryThread();
        }
    }
}
