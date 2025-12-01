package com.guild.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Kompatybilny scheduler - obsługuje zarówno Spigot, jak i Folia
 */
public class CompatibleScheduler {

    /**
     * Wykonaj zadanie w głównym wątku
     */
    public static void runTask(Plugin plugin, Runnable task) {
        if (ServerUtils.isFolia()) {
            try {
                // Użyj refleksji, aby wywołać globalny scheduler regionów Folia
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                globalScheduler.getClass().getMethod("run", Plugin.class, java.util.function.Consumer.class)
                    .invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run());
            } catch (Exception e) {
                // Jeśli API Folia jest niedostępne, wróć do tradycyjnego schedulera
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Wykonaj zadanie w określonej lokalizacji
     */
    public static void runTask(Plugin plugin, Location location, Runnable task) {
        if (ServerUtils.isFolia()) {
            try {
                // Użyj refleksji, aby wywołać scheduler regionów Folia
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                regionScheduler.getClass().getMethod("run", Plugin.class, Location.class, java.util.function.Consumer.class)
                    .invoke(regionScheduler, plugin, location, (java.util.function.Consumer<Object>) scheduledTask -> task.run());
            } catch (Exception e) {
                // Jeśli API Folia jest niedostępne, wróć do tradycyjnego schedulera
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Wykonaj zadanie w regionie, w którym znajduje się określony byt
     */
    public static void runTask(Plugin plugin, Entity entity, Runnable task) {
        if (ServerUtils.isFolia()) {
            try {
                // Użyj refleksji, aby wywołać scheduler bytu Folia
                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                entityScheduler.getClass().getMethod("run", Plugin.class, java.util.function.Consumer.class, Runnable.class)
                    .invoke(entityScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run(), (Runnable) () -> {});
            } catch (Exception e) {
                // Jeśli API Folia jest niedostępne, wróć do tradycyjnego schedulera
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Wykonaj zadanie z opóźnieniem
     */
    public static void runTaskLater(Plugin plugin, Runnable task, long delay) {
        if (ServerUtils.isFolia()) {
            try {
                // Użyj refleksji, aby wywołać globalny scheduler regionów Folia
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                globalScheduler.getClass().getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, long.class)
                    .invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run(), delay);
            } catch (Exception e) {
                // Jeśli API Folia jest niedostępne, wróć do tradycyjnego schedulera
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * Wykonaj zadanie z opóźnieniem w określonej lokalizacji
     */
    public static void runTaskLater(Plugin plugin, Location location, Runnable task, long delay) {
        if (ServerUtils.isFolia()) {
            try {
                // Użyj refleksji, aby wywołać scheduler regionów Folia
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                regionScheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, java.util.function.Consumer.class, long.class)
                    .invoke(regionScheduler, plugin, location, (java.util.function.Consumer<Object>) scheduledTask -> task.run(), delay);
            } catch (Exception e) {
                // Jeśli API Folia jest niedostępne, wróć do tradycyjnego schedulera
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * Wykonaj zadanie asynchronicznie
     */
    public static void runTaskAsync(Plugin plugin, Runnable task) {
        if (ServerUtils.isFolia()) {
            try {
                // Użyj refleksji, aby wywołać asynchroniczny scheduler Folia
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                asyncScheduler.getClass().getMethod("runNow", Plugin.class, java.util.function.Consumer.class)
                    .invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run());
            } catch (Exception e) {
                // Jeśli API Folia jest niedostępne, wróć do tradycyjnego schedulera
                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Wykonaj zadanie cyklicznie
     */
    public static void runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (ServerUtils.isFolia()) {
            try {
                // Użyj refleksji, aby wywołać globalny scheduler regionów Folia
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                globalScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class)
                    .invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run(), delay, period);
            } catch (Exception e) {
                // Jeśli API Folia jest niedostępne, wróć do tradycyjnego schedulera
                Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    /**
     * Sprawdź, czy jesteś w głównym wątku
     */
    public static boolean isPrimaryThread() {
        if (ServerUtils.isFolia()) {
            try {
                // Użyj refleksji, aby wywołać globalne sprawdzanie wątków Folia
                return (Boolean) Bukkit.class.getMethod("isGlobalTickThread").invoke(null);
            } catch (Exception e) {
                // Jeśli API Folia jest niedostępne, wróć do tradycyjnego sprawdzania
                return Bukkit.isPrimaryThread();
            }
        } else {
            return Bukkit.isPrimaryThread();
        }
    }
}
