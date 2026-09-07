package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldStatus;
import com.guild.world.model.WorldType;
import com.guild.world.preset.PresetService;
import com.guild.world.registry.WorldRegistry;
import com.guild.world.schematic.SchematicCodec;
import com.guild.world.schematic.SchematicData;
import com.guild.world.schematic.SchematicExporter;
import com.guild.world.schematic.SchematicPaster;
import com.guild.world.schematic.Vec3i;
import com.guild.world.selection.SelectionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 预设 schematic 的导出、粘贴与战场实例创建。
 */
public final class WorldPresetOps {

    /** Schematic 粘贴/导出相关配置快照。 */
    public record SchematicSettings(int maxVolume, boolean ignoreAirOnPaste, boolean includeBlockEntities) {
    }

    /** 粘贴原点 + 预设 A/B/观众出生点。 */
    public record ArenaSpawns(Location pasteAt, Location spawnA, Location spawnB, Location spectator) {
    }

    public record ArenaCreateResult(GuildWorld world, ArenaSpawns spawns) {
    }

    private final GuildPlugin plugin;
    private final WorldRegistry registry;
    private final PresetService presets;
    private final SelectionManager selections;
    private final WorldLifecycle lifecycle;
    private final Supplier<Boolean> enabled;
    private final Supplier<String> disabledMessage;
    private final Supplier<SchematicSettings> schematicSettings;

    public WorldPresetOps(GuildPlugin plugin,
                          WorldRegistry registry,
                          PresetService presets,
                          SelectionManager selections,
                          WorldLifecycle lifecycle,
                          Supplier<Boolean> enabled,
                          Supplier<String> disabledMessage,
                          Supplier<SchematicSettings> schematicSettings) {
        this.plugin = plugin;
        this.registry = registry;
        this.presets = presets;
        this.selections = selections;
        this.lifecycle = lifecycle;
        this.enabled = enabled;
        this.disabledMessage = disabledMessage;
        this.schematicSettings = schematicSettings;
    }

    /**
     * 从玩家选区导出 schematic 并写入预设（含 A/B/观众锚点相对 origin 的偏移）。
     */
    public CompletableFuture<PresetService.PresetMeta> savePresetFromSelection(Player editor, String presetName) {
        CompletableFuture<PresetService.PresetMeta> future = new CompletableFuture<>();
        if (!enabled.get()) {
            future.completeExceptionally(new IllegalStateException(disabledMessage.get()));
            return future;
        }
        if (!selections.hasCompleteSelection(editor)) {
            future.completeExceptionally(new IllegalStateException("Incomplete selection (need pos1 & pos2)"));
            return future;
        }
        SelectionManager.Session session = selections.of(editor);
        Location p1 = session.pos1;
        Location p2 = session.pos2;
        World world = p1.getWorld();

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        Location originAbs = session.spawnA != null ? session.spawnA
                : (editor.getWorld().equals(world) ? editor.getLocation() : p1);
        Vec3i originRel = new Vec3i(
                originAbs.getBlockX() - minX,
                originAbs.getBlockY() - minY,
                originAbs.getBlockZ() - minZ
        );

        SchematicSettings settings = schematicSettings.get();
        SchematicExporter.exportAsync(plugin, world,
                        p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                        p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(),
                        originRel, settings.includeBlockEntities(), settings.maxVolume(), plugin.getLogger())
                .whenComplete((data, err) -> {
                    if (err != null) {
                        if (settings.includeBlockEntities()) {
                            plugin.getLogger().warning("[World] Export with tiles failed, retrying blocks-only: "
                                    + err.getMessage());
                            SchematicExporter.exportAsync(plugin, world,
                                            p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                                            p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(),
                                            originRel, false, settings.maxVolume(), plugin.getLogger())
                                    .whenComplete((data2, err2) -> {
                                        if (err2 != null) {
                                            future.completeExceptionally(err2);
                                        } else {
                                            finishSavePreset(editor, presetName, session, data2, originAbs, future);
                                        }
                                    });
                        } else {
                            future.completeExceptionally(err);
                        }
                        return;
                    }
                    finishSavePreset(editor, presetName, session, data, originAbs, future);
                });
        return future;
    }

    public CompletableFuture<Void> pastePreset(World world, Location pasteAt, String presetName) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!enabled.get()) {
            future.completeExceptionally(new IllegalStateException(disabledMessage.get()));
            return future;
        }
        PresetService.PresetMeta meta = presets.get(presetName);
        if (meta == null || !presets.hasSchematicFile(presetName)) {
            future.completeExceptionally(new IllegalArgumentException("Preset schematic not found: " + presetName));
            return future;
        }
        SchematicSettings settings = schematicSettings.get();
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            try {
                SchematicData data = SchematicCodec.read(presets.gwsFile(presetName).toPath());
                Location at = pasteAt.clone();
                at.setWorld(world);
                SchematicPaster.pasteAsync(plugin, world, at, data,
                                settings.ignoreAirOnPaste(), settings.includeBlockEntities(), plugin.getLogger())
                        .whenComplete((v, err) -> {
                            if (err != null) {
                                future.completeExceptionally(err);
                            } else {
                                applyAnchorsToManagedWorld(world.getName(), meta, at);
                                future.complete(null);
                            }
                        });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public ArenaSpawns resolvePresetSpawns(World world, Location pasteAt, PresetService.PresetMeta meta) {
        Location at = pasteAt.clone();
        at.setWorld(world);
        Location a = meta.spawnA() == null ? at.clone()
                : SchematicPaster.offsetToWorld(at, meta.spawnA().dx(), meta.spawnA().dy(), meta.spawnA().dz(),
                meta.spawnA().yaw(), meta.spawnA().pitch());
        Location b = meta.spawnB() == null ? at.clone()
                : SchematicPaster.offsetToWorld(at, meta.spawnB().dx(), meta.spawnB().dy(), meta.spawnB().dz(),
                meta.spawnB().yaw(), meta.spawnB().pitch());
        Location spec = meta.spectator() == null ? null
                : SchematicPaster.offsetToWorld(at, meta.spectator().dx(), meta.spectator().dy(), meta.spectator().dz(),
                meta.spectator().yaw(), meta.spectator().pitch());
        return new ArenaSpawns(at, a, b, spec);
    }

    /**
     * 创建 BATTLE 虚空世界并粘贴预设（paste 默认对齐 0.5,64,0.5）。
     */
    public CompletableFuture<GuildWorld> createWorldFromPreset(String worldName, String presetName) {
        return createArenaFromPreset(worldName, presetName).thenApply(ArenaCreateResult::world);
    }

    /**
     * 创建战场实例并返回出生点（供公会战使用）。
     */
    public CompletableFuture<ArenaCreateResult> createArenaFromPreset(String worldName, String presetName) {
        PresetService.PresetMeta meta = presets.get(presetName);
        if (meta == null || !presets.hasSchematicFile(presetName)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Preset schematic not found: " + presetName));
        }
        return lifecycle.createVoidWorld(worldName, WorldType.BATTLE, presetName, null, null)
                .thenCompose(gw -> {
                    World world = Bukkit.getWorld(gw.getWorldName());
                    if (world == null) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("World missing after create"));
                    }
                    Location pasteAt = new Location(world, 0.5, 64, 0.5);
                    return pastePreset(world, pasteAt, presetName).thenApply(v -> {
                        ArenaSpawns spawns = resolvePresetSpawns(world, pasteAt, meta);
                        gw.setStatus(WorldStatus.BUSY);
                        gw.touch();
                        registry.save();
                        return new ArenaCreateResult(gw, spawns);
                    });
                });
    }

    private void finishSavePreset(Player editor, String presetName, SelectionManager.Session session,
                                  SchematicData data, Location originAbs,
                                  CompletableFuture<PresetService.PresetMeta> future) {
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            try {
                Path gws = presets.gwsFile(presetName).toPath();
                SchematicCodec.write(gws, data);
                PresetService.Anchor a = toAnchor(session.spawnA, originAbs);
                PresetService.Anchor b = toAnchor(session.spawnB, originAbs);
                PresetService.Anchor spec = toAnchor(session.spectator, originAbs);
                String pasteOrigin = originAbs.getWorld().getName() + ","
                        + originAbs.getX() + "," + originAbs.getY() + "," + originAbs.getZ() + ","
                        + originAbs.getYaw() + "," + originAbs.getPitch();
                PresetService.PresetMeta meta = presets.saveMeta(
                        presetName,
                        editor.getWorld().getName(),
                        editor.getUniqueId().toString(),
                        "Schematic saved from selection",
                        true,
                        data.size.dx(), data.size.dy(), data.size.dz(),
                        data.blockEntities == null ? 0 : data.blockEntities.size(),
                        pasteOrigin, a, b, spec
                );
                GuildWorld gw = registry.get(editor.getWorld().getName());
                if (gw != null) {
                    gw.setPresetName(meta.name());
                    gw.touch();
                    registry.save();
                }
                future.complete(meta);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
    }

    private static PresetService.Anchor toAnchor(Location loc, Location originAbs) {
        if (loc == null || originAbs == null || loc.getWorld() == null
                || !loc.getWorld().equals(originAbs.getWorld())) {
            return null;
        }
        return new PresetService.Anchor(
                loc.getX() - originAbs.getX(),
                loc.getY() - originAbs.getY(),
                loc.getZ() - originAbs.getZ(),
                loc.getYaw(),
                loc.getPitch()
        );
    }

    private void applyAnchorsToManagedWorld(String worldName, PresetService.PresetMeta meta, Location pasteAt) {
        GuildWorld gw = registry.get(worldName);
        if (gw == null) {
            return;
        }
        PresetService.Anchor spawn = meta.spawnA() != null ? meta.spawnA()
                : new PresetService.Anchor(0, 0, 0, 0, 0);
        Location worldSpawn = SchematicPaster.offsetToWorld(
                pasteAt, spawn.dx(), spawn.dy(), spawn.dz(), spawn.yaw(), spawn.pitch());
        CompatibleScheduler.runTask(plugin, worldSpawn, () -> {
            gw.setSpawnLocation(worldSpawn);
            gw.setPresetName(meta.name());
            gw.touch();
            registry.save();
            World w = Bukkit.getWorld(worldName);
            if (w != null) {
                w.setSpawnLocation(worldSpawn);
            }
        });
    }
}
