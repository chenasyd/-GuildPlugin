package com.guild.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * 兼容性调度器 - 支持Spigot和Folia
 */
public class CompatibleScheduler {
    
    /**
     * 在主线程执行任务
     */
    public static void runTask(Plugin plugin, Runnable task) {
        // 检查插件是否已启用
        if (plugin != null && !plugin.isEnabled()) {
            return;
        }

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
        // 检查插件是否已启用
        if (plugin != null && !plugin.isEnabled()) {
            return;
        }

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
    public static ScheduledTaskHandle runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        // 检查插件是否已启用
        if (plugin != null && !plugin.isEnabled()) {
            return () -> {};
        }

        if (ServerUtils.isFolia()) {
            try {
                // 使用反射调用Folia的全局区域调度器
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Object scheduledTask = globalScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class)
                    .invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTaskRef -> task.run(), delay, period);

                return new ScheduledTaskHandle() {
                    private volatile boolean cancelled = false;

                    @Override
                    public void cancel() {
                        if (cancelled) {
                            return;
                        }
                        cancelled = true;
                        try {
                            scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
                        } catch (Exception ignored) {
                        }
                    }

                    @Override
                    public boolean isCancelled() {
                        return cancelled;
                    }
                };
            } catch (Exception e) {
                // 如果Folia API不可用，回退到传统调度器
                return new ScheduledTaskHandle() {
                    private final org.bukkit.scheduler.BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
                    private volatile boolean cancelled = false;

                    @Override
                    public void cancel() {
                        if (cancelled) {
                            return;
                        }
                        cancelled = true;
                        bukkitTask.cancel();
                    }

                    @Override
                    public boolean isCancelled() {
                        return cancelled || bukkitTask.isCancelled();
                    }
                };
            }
        } else {
            return new ScheduledTaskHandle() {
                private final org.bukkit.scheduler.BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
                private volatile boolean cancelled = false;

                @Override
                public void cancel() {
                    if (cancelled) {
                        return;
                    }
                    cancelled = true;
                    bukkitTask.cancel();
                }

                @Override
                public boolean isCancelled() {
                    return cancelled || bukkitTask.isCancelled();
                }
            };
        }
    }
    
    /**
     * 在指定实体所在区域重复执行任务 — Folia 使用 entity.getScheduler().runAtFixedRate，
     * 确保对实体的所有操作（获取位置、发送消息、传送）在正确的区域线程内执行。
     */
    public static ScheduledTaskHandle runTaskTimer(Plugin plugin, Entity entity, Runnable task, long delay, long period) {
        if (plugin != null && !plugin.isEnabled()) {
            return () -> {};
        }

        if (ServerUtils.isFolia()) {
            try {
                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                java.util.concurrent.atomic.AtomicReference<Object> taskRef = new java.util.concurrent.atomic.AtomicReference<>();
                Object scheduledTask = entityScheduler.getClass()
                    .getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class, long.class)
                    .invoke(entityScheduler, plugin,
                        (java.util.function.Consumer<Object>) t -> {
                            taskRef.set(t);
                            task.run();
                        },
                        (Runnable) () -> {}, delay, period);
                taskRef.set(scheduledTask);
                return new ScheduledTaskHandle() {
                    private volatile boolean cancelled = false;
                    @Override
                    public void cancel() {
                        if (cancelled) return;
                        cancelled = true;
                        try {
                            taskRef.get().getClass().getMethod("cancel").invoke(taskRef.get());
                        } catch (Exception ignored) {}
                    }
                    @Override
                    public boolean isCancelled() { return cancelled; }
                };
            } catch (Exception e) {
                return new ScheduledTaskHandle() {
                    private final org.bukkit.scheduler.BukkitTask bt = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
                    private volatile boolean cancelled = false;
                    @Override
                    public void cancel() { if (!cancelled) { cancelled = true; bt.cancel(); } }
                    @Override
                    public boolean isCancelled() { return cancelled || bt.isCancelled(); }
                };
            }
        } else {
            return new ScheduledTaskHandle() {
                private final org.bukkit.scheduler.BukkitTask bt = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
                private volatile boolean cancelled = false;
                @Override
                public void cancel() { if (!cancelled) { cancelled = true; bt.cancel(); } }
                @Override
                public boolean isCancelled() { return cancelled || bt.isCancelled(); }
            };
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
