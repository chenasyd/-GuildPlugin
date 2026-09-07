package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ServerUtils;
import com.guild.world.bridge.FoliaWorldCreator;
import com.guild.world.generator.VoidWorldGen;
import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldStatus;
import com.guild.world.model.WorldType;
import com.guild.world.registry.WorldJournal;
import com.guild.world.registry.WorldRegistry;
import com.guild.world.util.WorldFiles;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * 受管世界的创建、加载、卸载、删除与优雅关服。
 */
public final class WorldLifecycle {

    private final GuildPlugin plugin;
    private final WorldRegistry registry;
    private final WorldJournal journal;
    private final Supplier<Boolean> enabled;
    private final Supplier<String> disabledMessage;
    private final Function<String, String> buildWorldName;
    private final Consumer<Player> teleportToFallback;
    private final Set<String> loading = ConcurrentHashMap.newKeySet();

    public WorldLifecycle(GuildPlugin plugin,
                          WorldRegistry registry,
                          WorldJournal journal,
                          Supplier<Boolean> enabled,
                          Supplier<String> disabledMessage,
                          Function<String, String> buildWorldName,
                          Consumer<Player> teleportToFallback) {
        this.plugin = plugin;
        this.registry = registry;
        this.journal = journal;
        this.enabled = enabled;
        this.disabledMessage = disabledMessage;
        this.buildWorldName = buildWorldName;
        this.teleportToFallback = teleportToFallback;
    }

    /**
     * 创建虚空世界（纯 Bukkit API，在主线程/全局区域线程执行）。
     */
    public CompletableFuture<GuildWorld> createVoidWorld(String worldName, WorldType type,
                                                       String presetName, String ownerGuildId, Long seed) {
        CompletableFuture<GuildWorld> future = new CompletableFuture<>();
        if (!enabled.get()) {
            future.completeExceptionally(new IllegalStateException(disabledMessage.get()));
            return future;
        }
        String name = buildWorldName.apply(worldName);
        if (name == null || !name.matches("[a-zA-Z0-9_]{1,64}")) {
            future.completeExceptionally(new IllegalArgumentException("Invalid world name: " + worldName));
            return future;
        }
        if (registry.contains(name) || Bukkit.getWorld(name) != null) {
            future.completeExceptionally(new IllegalStateException("World already exists: " + name));
            return future;
        }
        if (!loading.add(name)) {
            future.completeExceptionally(new IllegalStateException("World is already being created: " + name));
            return future;
        }

        GuildWorld gw = new GuildWorld(name);
        gw.setType(type);
        gw.setPresetName(presetName);
        gw.setOwnerGuildId(ownerGuildId);
        gw.setStatus(WorldStatus.LOADING);
        registry.put(gw);
        registry.save();
        journal.begin(WorldJournal.Op.CREATE, name);

        CompatibleScheduler.runTask(plugin, () -> {
            try {
                World world = doCreateWorld(name, seed);
                applyWorldRules(world, type);
                gw.setSpawnLocation(world.getSpawnLocation());
                gw.setStatus(WorldStatus.READY);
                gw.touch();
                registry.save();
                journal.done(WorldJournal.Op.CREATE, name);
                plugin.getLogger().info("[World] Created void world '" + name + "' type=" + type
                        + " preset=" + gw.getPresetName()
                        + " path=" + WorldFiles.resolveWorldDirectory(name).getPath());
                future.complete(gw);
            } catch (Throwable e) {
                gw.setStatus(WorldStatus.ERROR);
                registry.save();
                plugin.getLogger().log(Level.SEVERE, "[World] Failed to create world '" + name + "'", e);
                future.completeExceptionally(e);
            } finally {
                loading.remove(name);
            }
        });
        return future;
    }

    /**
     * 重新加载一个已注册但未加载的世界（用于恢复 STALE 世界）。
     */
    public CompletableFuture<GuildWorld> loadWorld(String name) {
        CompletableFuture<GuildWorld> future = new CompletableFuture<>();
        if (!enabled.get()) {
            future.completeExceptionally(new IllegalStateException(disabledMessage.get()));
            return future;
        }
        GuildWorld gw = registry.get(name);
        if (gw == null) {
            future.completeExceptionally(new IllegalArgumentException("World is not managed: " + name));
            return future;
        }
        if (Bukkit.getWorld(name) != null) {
            gw.setStatus(WorldStatus.READY);
            gw.touch();
            registry.save();
            future.complete(gw);
            return future;
        }
        if (!loading.add(name)) {
            future.completeExceptionally(new IllegalStateException("World is already being loaded: " + name));
            return future;
        }

        journal.begin(WorldJournal.Op.LOAD, name);
        gw.setStatus(WorldStatus.LOADING);
        registry.save();

        CompatibleScheduler.runTask(plugin, () -> {
            try {
                World world = doCreateWorld(name, null);
                applyWorldRules(world, gw.getType());
                gw.setSpawnLocation(world.getSpawnLocation());
                gw.setStatus(WorldStatus.READY);
                gw.touch();
                registry.save();
                journal.done(WorldJournal.Op.LOAD, name);
                plugin.getLogger().info("[World] Loaded world '" + name + "'");
                future.complete(gw);
            } catch (Throwable e) {
                gw.setStatus(WorldStatus.ERROR);
                registry.save();
                plugin.getLogger().log(Level.SEVERE, "[World] Failed to load world '" + name + "'", e);
                future.completeExceptionally(e);
            } finally {
                loading.remove(name);
            }
        });
        return future;
    }

    /**
     * 卸载一个已加载的世界（踢出玩家 → 保存 → 卸载），状态置 UNLOADED。
     */
    public CompletableFuture<Void> unloadWorld(String name) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!enabled.get()) {
            future.completeExceptionally(new IllegalStateException(disabledMessage.get()));
            return future;
        }
        GuildWorld gw = registry.get(name);
        if (gw == null) {
            future.completeExceptionally(new IllegalArgumentException("World is not managed: " + name));
            return future;
        }
        World world = Bukkit.getWorld(name);
        if (world == null) {
            gw.setStatus(WorldStatus.UNLOADED);
            gw.touch();
            registry.save();
            future.complete(null);
            return future;
        }

        journal.begin(WorldJournal.Op.UNLOAD, name);
        gw.setStatus(WorldStatus.UNLOADING);
        registry.save();

        CompatibleScheduler.runTask(plugin, () -> {
            try {
                for (Player player : new ArrayList<>(world.getPlayers())) {
                    teleportToFallback.accept(player);
                }
                if (world.isAutoSave()) {
                    world.save();
                }
                Bukkit.unloadWorld(world, true);
                gw.setStatus(WorldStatus.UNLOADED);
                gw.touch();
                registry.save();
                journal.done(WorldJournal.Op.UNLOAD, name);
                plugin.getLogger().info("[World] Unloaded world '" + name + "'");
                future.complete(null);
            } catch (Throwable e) {
                gw.setStatus(WorldStatus.ERROR);
                registry.save();
                plugin.getLogger().log(Level.SEVERE, "[World] Failed to unload world '" + name + "'", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * 删除一个受管世界（卸载 → 删除文件夹 → 移除注册表记录）。
     */
    public CompletableFuture<Void> deleteWorld(String name, boolean force) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!enabled.get()) {
            future.completeExceptionally(new IllegalStateException(disabledMessage.get()));
            return future;
        }
        GuildWorld gw = registry.get(name);
        if (gw == null) {
            future.completeExceptionally(new IllegalArgumentException("World is not managed: " + name));
            return future;
        }
        World world = Bukkit.getWorld(name);
        if (world != null && !force) {
            future.completeExceptionally(new IllegalStateException(
                    "World is loaded, unload it first or use --force: " + name));
            return future;
        }

        journal.begin(WorldJournal.Op.DELETE, name);
        gw.setStatus(WorldStatus.UNLOADING);
        registry.save();

        Runnable deleteTask = () -> {
            try {
                World loaded = Bukkit.getWorld(name);
                if (loaded != null) {
                    for (Player player : new ArrayList<>(loaded.getPlayers())) {
                        teleportToFallback.accept(player);
                    }
                    if (loaded.isAutoSave()) {
                        loaded.save();
                    }
                    Bukkit.unloadWorld(loaded, true);
                }
            } catch (Throwable e) {
                plugin.getLogger().log(Level.SEVERE, "[World] Failed to unload world '" + name
                        + "' before deletion", e);
            }
            CompatibleScheduler.runTaskAsync(plugin, () -> {
                try {
                    if (WorldFiles.worldDirectoryExists(name) && !WorldFiles.deleteWorldDirectory(name)) {
                        throw new IllegalStateException("Failed to delete world directory: "
                                + WorldFiles.resolveWorldDirectory(name));
                    }
                    registry.remove(name);
                    registry.save();
                    journal.done(WorldJournal.Op.DELETE, name);
                    plugin.getLogger().info("[World] Deleted world '" + name + "'");
                    future.complete(null);
                } catch (Throwable e) {
                    GuildWorld cur = registry.get(name);
                    if (cur != null) {
                        cur.setStatus(WorldStatus.ERROR);
                        registry.save();
                    }
                    plugin.getLogger().log(Level.SEVERE, "[World] Failed to delete world '" + name + "'", e);
                    future.completeExceptionally(e);
                }
            });
        };

        CompatibleScheduler.runTask(plugin, deleteTask);
        return future;
    }

    /**
     * 插件 onDisable 时调用：标记所有活跃世界为 UNLOADED + cleanShutdown。
     */
    public void shutdown() {
        for (GuildWorld gw : new ArrayList<>(registry.all())) {
            WorldStatus status = gw.getStatus();
            if (status == WorldStatus.REGISTERED || status == WorldStatus.UNLOADED) {
                continue;
            }
            World world = Bukkit.getWorld(gw.getWorldName());
            if (world != null) {
                try {
                    for (Player player : new ArrayList<>(world.getPlayers())) {
                        teleportToFallback.accept(player);
                    }
                } catch (Exception ignored) {
                }
                try {
                    world.save();
                } catch (Exception ignored) {
                }
            }
            gw.setStatus(WorldStatus.UNLOADED);
            gw.touch();
        }
        registry.setCleanShutdown(true);
        registry.save();
        journal.clear();
        plugin.getLogger().info("[World] Graceful shutdown complete. "
                + registry.size() + " managed world(s) marked unloaded.");
    }

    boolean isLoading(String name) {
        return loading.contains(name);
    }

    private World doCreateWorld(String name, Long seed) {
        WorldCreator creator = new WorldCreator(name);
        creator.generator(new VoidWorldGen());
        creator.biomeProvider(VoidWorldGen.THE_VOID_BIOME_PROVIDER);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        if (seed != null) {
            creator.seed(seed);
        }
        World world = ServerUtils.isFolia() ? FoliaWorldCreator.createWorld(creator) : Bukkit.createWorld(creator);
        if (world == null) {
            throw new IllegalStateException("createWorld returned null for '" + name + "'");
        }
        return world;
    }

    private void applyWorldRules(World world, WorldType type) {
        switch (type) {
            case EDIT:
                world.setPVP(false);
                world.setAutoSave(false);
                world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                world.setGameRule(GameRule.DO_MOB_LOOT, false);
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                break;
            case BATTLE:
                world.setPVP(true);
                world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
                break;
            default:
                break;
        }
    }
}
