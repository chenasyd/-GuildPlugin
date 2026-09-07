package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.DebugLog;
import com.guild.core.utils.ServerUtils;
import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldStatus;
import com.guild.world.model.WorldType;
import com.guild.world.preset.PresetService;
import com.guild.world.recovery.WorldRecoveryService;
import com.guild.world.registry.WorldJournal;
import com.guild.world.registry.WorldRegistry;
import com.guild.world.schematic.SchematicCodec;
import com.guild.world.schematic.SchematicData;
import com.guild.world.schematic.SchematicExporter;
import com.guild.world.schematic.SchematicPaster;
import com.guild.world.schematic.Vec3i;
import com.guild.world.selection.SelectionManager;
import com.guildplugin.util.FoliaTeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * 多世界管理系统核心服务（虚空世界 + 意外恢复）。
 *
 * <p>职责：
 * <ul>
 *   <li>虚空世界创建/加载/卸载/删除（Paper/Spigot：纯 Bukkit API；Folia：NMS 反射桥接，受版本支持列表门控）；</li>
 *   <li>世界注册表（worlds.yml）与意图日志（journal.log）持久化；</li>
 *   <li>启动自检：崩溃检测 + 残留世界恢复（委托 {@link WorldRecoveryService}）；</li>
 *   <li>正常关服优雅卸载（标记 cleanShutdown）。</li>
 * </ul>
 *
 * <p>Folia 门控：仅当运行在 Folia 且 Minecraft 版本不在
 * {@link ServerUtils#FOLIA_SUPPORTED_VERSIONS} 时 {@link #isEnabled()} 为 false；
 * 此时 create/load/unload/delete 与启动恢复均跳过，shutdown 注册表标记仍照常执行。
 *
 * <p>意外恢复原则：
 * <ol>
 *   <li>每个操作<b>先写 journal 再执行</b>，完成后追加 DONE；</li>
 *   <li>启动时（RUNNING 状态后）检查 journal 残留与注册表状态，分类恢复/清理；</li>
 *   <li>恢复策略保守：只标记不清除，绝不自动删除数据。</li>
 * </ol>
 */
public class GuildWorldService {

    private static final long RECOVERY_DELAY_TICKS = 100L;

    private final GuildPlugin plugin;
    private final File worldsDir;
    private final WorldRegistry registry;
    private final WorldJournal journal;
    private final WorldRecoveryService recovery;
    private final PresetService presets;
    private final SelectionManager selections = new SelectionManager();
    private final WorldLifecycle lifecycle;
    /** 自动触发恢复自检的一次性去重标志（玩家加入 / 延迟兜底二选一）。 */
    private final AtomicBoolean recoveryTriggered = new AtomicBoolean(false);

    /**
     * 多世界功能是否启用。仅当「Folia 且版本不在支持列表」时为 false；
     * Paper/Spigot 走纯 Bukkit API，与版本无关，始终启用。
     */
    private final boolean enabled;

    // 配置项（支持 /guildadmin reload 热更新；enabled 为能力开关，不随配置变）
    private String namePrefix;
    private String fallbackWorldName;
    private boolean recoveryCheckEnabled;
    private boolean autoLoadStale;
    private boolean autoCleanOrphans;
    private Material wandMaterial;
    private int maxSchematicVolume;
    private boolean ignoreAirOnPaste;
    private boolean includeBlockEntities;
    private String postMatchPolicy;

    public GuildWorldService(GuildPlugin plugin) {
        this.plugin = plugin;
        this.worldsDir = new File(plugin.getDataFolder(), "worlds");
        if (!worldsDir.exists() && !worldsDir.mkdirs()) {
            plugin.getLogger().warning("[World] Failed to create worlds directory: " + worldsDir);
        }
        this.registry = new WorldRegistry(worldsDir, plugin.getLogger());
        this.journal = new WorldJournal(worldsDir, plugin.getLogger());
        this.recovery = new WorldRecoveryService(plugin.getLogger());
        this.presets = new PresetService(worldsDir, plugin.getLogger());
        // Folia 需 NMS 桥接，仅支持列表内版本；非 Folia 用 Bukkit.createWorld，始终可用
        this.enabled = !ServerUtils.isFolia() || ServerUtils.isFoliaVersionSupported();
        reloadSettings();
        this.lifecycle = new WorldLifecycle(
                plugin, registry, journal,
                () -> enabled, this::unsupportedMessage, this::buildWorldName,
                this::teleportToFallback);
    }

    /**
     * 从 ConfigManager 重新读取世界相关配置（name-prefix / wand / schematic / post-match 等）。
     */
    public void reloadSettings() {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        this.namePrefix = config.getString("world.name-prefix", "gw_");
        this.fallbackWorldName = config.getString("world.safety.fallback-world", "world");
        this.recoveryCheckEnabled = config.getBoolean("world.recovery.check-on-startup", true);
        this.autoLoadStale = config.getBoolean("world.recovery.auto-load-stale", false);
        this.autoCleanOrphans = config.getBoolean("world.recovery.auto-clean-orphans", true);
        this.wandMaterial = parseMaterial(config.getString("world.edit.wand-material", "WOODEN_AXE"), Material.WOODEN_AXE);
        this.maxSchematicVolume = Math.max(1000, config.getInt("world.schematic.max-volume", 2_000_000));
        this.ignoreAirOnPaste = config.getBoolean("world.schematic.ignore-air", true);
        this.includeBlockEntities = config.getBoolean("world.schematic.include-block-entities", true);
        this.postMatchPolicy = config.getString("world.arena.post-match", "destroy");
    }

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /* ── 生命周期 ───────────────────────────────────────── */

    /**
     * 插件 onEnable 时调用：加载注册表 + 标记本次运行开始（cleanShutdown=false）。
     */
    public void load() {
        registry.load();
        boolean wasClean = registry.isCleanShutdown();
        if (!wasClean) {
            DebugLog.warning(plugin.getLogger(),
                    "[World] Previous shutdown was NOT clean. Startup recovery check will run.");
        }
        registry.setCleanShutdown(false);
        registry.save();
        recovery.recordShutdownState(wasClean);
        DebugLog.info(plugin.getLogger(), "[World] Registry loaded: " + registry.size() + " managed world(s).");
    }

    /**
     * 调度启动恢复自检。采用"玩家加入优先 + 延迟兜底"双触发：
     * <ul>
     *   <li>玩家加入必然发生在服务器 RUNNING 之后，此时加载/创建世界被允许且可靠（Folia 下尤其关键）；</li>
     *   <li>延迟任务保证服务器无人加入时也能自检。</li>
     * </ul>
     * 两者由 {@link #recoveryTriggered} 去重，先到先执行。
     */
    public void scheduleRecovery() {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                // 玩家加入事件在玩家区域线程触发（Folia），恢复逻辑必须调度到全局线程
                CompatibleScheduler.runTask(plugin, () -> runRecoveryOnce("player-join"));
            }
        }, plugin);

        CompatibleScheduler.runTaskLater(plugin, () -> runRecoveryOnce("scheduled"), RECOVERY_DELAY_TICKS);
    }

    /**
     * 一次性自动恢复入口（玩家加入 / 延迟兜底，先到先触发）。
     */
    private void runRecoveryOnce(String source) {
        if (!recoveryTriggered.compareAndSet(false, true)) {
            return;
        }
        DebugLog.info(plugin.getLogger(), "[World] Startup recovery check triggered by " + source + ".");
        runRecovery();
    }

    /**
     * 执行启动恢复自检（崩溃检测 + 残留世界分类处理）。
     */
    public void runRecovery() {
        if (!enabled) {
            plugin.getLogger().warning("[World] Skipping startup recovery: " + unsupportedMessage());
            recovery.markRan();
            return;
        }
        if (!recoveryCheckEnabled) {
            DebugLog.info(plugin.getLogger(), "[World] Recovery check disabled by config, skipping.");
            recovery.markRan();
            return;
        }
        try {
            recovery.runRecovery(this);
        } catch (Throwable e) {
            plugin.getLogger().log(Level.SEVERE, "[World] Recovery check failed", e);
        }
    }

    /**
     * 插件 onDisable 时调用：优雅卸载所有受管世界 + 标记 cleanShutdown。
     */
    public void shutdown() {
        lifecycle.shutdown();
    }

    /* ── 虚空世界创建 ────────────────────────────────────── */

    public CompletableFuture<GuildWorld> createVoidWorld(String worldName, WorldType type,
                                                         String presetName, String ownerGuildId, Long seed) {
        return lifecycle.createVoidWorld(worldName, type, presetName, ownerGuildId, seed);
    }

    /* ── 加载 / 卸载 / 删除 ──────────────────────────────── */

    public CompletableFuture<GuildWorld> loadWorld(String name) {
        return lifecycle.loadWorld(name);
    }

    public CompletableFuture<Void> unloadWorld(String name) {
        return lifecycle.unloadWorld(name);
    }

    public CompletableFuture<Void> deleteWorld(String name, boolean force) {
        return lifecycle.deleteWorld(name, force);
    }

    /* ── 传送（Folia 安全）────────────────────────────────── */

    /**
     * 将玩家传送到受管世界出生点（必要时先加载；虚空世界自动铺落地平台）。
     */
    public CompletableFuture<Boolean> teleportToWorld(Player player, String worldName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (!enabled) {
            future.completeExceptionally(new IllegalStateException(unsupportedMessage()));
            return future;
        }
        if (player == null || !player.isOnline()) {
            future.complete(false);
            return future;
        }
        String name = buildWorldName(worldName);
        GuildWorld gw = registry.get(name);
        if (gw == null) {
            future.completeExceptionally(new IllegalArgumentException("World is not managed: " + name));
            return future;
        }

        Runnable afterLoaded = () -> {
            World world = Bukkit.getWorld(name);
            if (world == null) {
                future.completeExceptionally(new IllegalStateException("World not loaded: " + name));
                return;
            }
            Location dest = resolveTeleportLocation(gw, world);
            ensureVoidPlatform(dest).thenCompose(ok ->
                    FoliaTeleportUtils.safeTeleport(plugin, player, dest)
            ).whenComplete((success, err) -> {
                if (err != null) {
                    future.completeExceptionally(err);
                    return;
                }
                if (Boolean.TRUE.equals(success)) {
                    gw.touch();
                    registry.save();
                }
                future.complete(Boolean.TRUE.equals(success));
            });
        };

        if (Bukkit.getWorld(name) != null) {
            CompatibleScheduler.runTask(plugin, afterLoaded);
            return future;
        }

        loadWorld(name).whenComplete((loaded, err) -> {
            if (err != null) {
                future.completeExceptionally(err);
                return;
            }
            CompatibleScheduler.runTask(plugin, afterLoaded);
        });
        return future;
    }

    /**
     * 传送到安全回退世界（配置 {@code world.safety.fallback-world}）。
     */
    public CompletableFuture<Boolean> teleportToFallbackWorld(Player player) {
        Location fallback = fallbackLocation();
        if (fallback == null) {
            return CompletableFuture.completedFuture(false);
        }
        return FoliaTeleportUtils.safeTeleport(plugin, player, fallback);
    }

    private Location resolveTeleportLocation(GuildWorld gw, World world) {
        Location spawn = gw.parseSpawnLocation();
        if (spawn != null && spawn.getWorld() != null) {
            return spawn;
        }
        Location fixed = world.getSpawnLocation();
        // 虚空世界默认出生点可能在半空，抬高到平台上方
        if (fixed.getBlockY() < world.getMinHeight() + 2) {
            fixed.setY(64);
        }
        return fixed;
    }

    /**
     * 在目标位置下方铺 3x3 平台（仅当脚下为空气时），避免虚空坠落。
     * 方块修改在目标区域线程执行（Folia）。
     */
    private CompletableFuture<Boolean> ensureVoidPlatform(Location dest) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompatibleScheduler.runTask(plugin, dest, () -> {
            try {
                World world = dest.getWorld();
                if (world == null) {
                    future.complete(false);
                    return;
                }
                int baseY = dest.getBlockY() - 1;
                if (baseY < world.getMinHeight()) {
                    baseY = Math.min(63, world.getMaxHeight() - 2);
                    dest.setY(baseY + 1);
                }
                int cx = dest.getBlockX();
                int cz = dest.getBlockZ();
                boolean placed = false;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Block block = world.getBlockAt(cx + dx, baseY, cz + dz);
                        if (block.getType().isAir() || !block.getType().isSolid()) {
                            block.setType(Material.STONE, false);
                            placed = true;
                        }
                    }
                }
                // 清理脚下到头上的障碍，保证站立空间
                for (int dy = 0; dy <= 1; dy++) {
                    Block air = world.getBlockAt(cx, baseY + 1 + dy, cz);
                    if (!air.getType().isAir() && air.getType().isSolid()) {
                        air.setType(Material.AIR, false);
                    }
                }
                dest.setX(cx + 0.5);
                dest.setY(baseY + 1);
                dest.setZ(cz + 0.5);
                if (placed) {
                    plugin.getLogger().info("[World] Spawn platform ensured at "
                            + world.getName() + " " + cx + "," + baseY + "," + cz);
                }
                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /* ── 内部工具 ────────────────────────────────────────── */

    private void teleportToFallback(Player player) {
        Location fallback = fallbackLocation();
        if (fallback != null) {
            FoliaTeleportUtils.safeTeleport(plugin, player, fallback);
        }
    }

    private Location fallbackLocation() {
        World world = Bukkit.getWorld(fallbackWorldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        return world == null ? null : world.getSpawnLocation();
    }

    /**
     * 构造受管世界名：自动附加配置的前缀（若输入已含前缀则不重复添加）。
     */
    public String buildWorldName(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.startsWith(namePrefix) ? input : namePrefix + input;
    }

    /* ── 查询与 getter ───────────────────────────────────── */

    /**
     * 多世界功能是否可用（Folia 不支持版本时为 false）。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 禁用原因文案（仅 {@link #isEnabled()} 为 false 时有意义）。
     */
    public String unsupportedMessage() {
        return CoreMsg.rawDefault(plugin, "world.disabled.folia-unsupported",
                "&c当前 Folia 版本 ({version}) 不支持 gworld",
                "{version}", ServerUtils.getMinecraftVersion());
    }

    public GuildWorld getWorld(String name) {
        return registry.get(name);
    }

    public Collection<GuildWorld> getWorlds() {
        return registry.all();
    }

    public boolean isManaged(String name) {
        return registry.contains(name);
    }

    public WorldRegistry getRegistry() {
        return registry;
    }

    public WorldJournal getJournal() {
        return journal;
    }

    public WorldRecoveryService getRecovery() {
        return recovery;
    }

    public PresetService getPresets() {
        return presets;
    }

    public File getWorldsDir() {
        return worldsDir;
    }

    public String getWorldNamePrefix() {
        return namePrefix;
    }

    public Location getFallbackLocation() {
        return fallbackLocation();
    }

    public SelectionManager getSelections() {
        return selections;
    }

    public Material getWandMaterial() {
        return wandMaterial;
    }

    public String getPostMatchPolicy() {
        return postMatchPolicy;
    }

    /* ── Schematic / Preset ───────────────────────────────── */

    /**
     * 从玩家选区导出 schematic 并写入预设（含 A/B/观众锚点相对 origin 的偏移）。
     */
    public CompletableFuture<PresetService.PresetMeta> savePresetFromSelection(Player editor, String presetName) {
        CompletableFuture<PresetService.PresetMeta> future = new CompletableFuture<>();
        if (!enabled) {
            future.completeExceptionally(new IllegalStateException(unsupportedMessage()));
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

        // origin：优先 spawnA，否则玩家当前位置（相对选区最小角）
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

        SchematicExporter.exportAsync(plugin, world,
                        p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                        p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(),
                        originRel, includeBlockEntities, maxSchematicVolume, plugin.getLogger())
                .whenComplete((data, err) -> {
                    if (err != null) {
                        // 方块实体失败时降级重试仅方块
                        if (includeBlockEntities) {
                            plugin.getLogger().warning("[World] Export with tiles failed, retrying blocks-only: "
                                    + err.getMessage());
                            SchematicExporter.exportAsync(plugin, world,
                                            p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                                            p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(),
                                            originRel, false, maxSchematicVolume, plugin.getLogger())
                                    .whenComplete((data2, err2) -> {
                                        if (err2 != null) {
                                            future.completeExceptionally(err2);
                                        } else {
                                            finishSavePreset(editor, presetName, session, data2, minX, minY, minZ, originAbs, future);
                                        }
                                    });
                        } else {
                            future.completeExceptionally(err);
                        }
                        return;
                    }
                    finishSavePreset(editor, presetName, session, data, minX, minY, minZ, originAbs, future);
                });
        return future;
    }

    private void finishSavePreset(Player editor, String presetName, SelectionManager.Session session,
                                  SchematicData data, int minX, int minY, int minZ, Location originAbs,
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

    public CompletableFuture<Void> pastePreset(World world, Location pasteAt, String presetName) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!enabled) {
            future.completeExceptionally(new IllegalStateException(unsupportedMessage()));
            return future;
        }
        PresetService.PresetMeta meta = presets.get(presetName);
        if (meta == null || !presets.hasSchematicFile(presetName)) {
            future.completeExceptionally(new IllegalArgumentException("Preset schematic not found: " + presetName));
            return future;
        }
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            try {
                SchematicData data = SchematicCodec.read(presets.gwsFile(presetName).toPath());
                Location at = pasteAt.clone();
                at.setWorld(world);
                SchematicPaster.pasteAsync(plugin, world, at, data, ignoreAirOnPaste, includeBlockEntities, plugin.getLogger())
                        .whenComplete((v, err) -> {
                            if (err != null) {
                                future.completeExceptionally(err);
                            } else {
                                applyAnchorsToManagedWorld(world.getName(), meta, at, data);
                                future.complete(null);
                            }
                        });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void applyAnchorsToManagedWorld(String worldName, PresetService.PresetMeta meta,
                                            Location pasteAt, SchematicData data) {
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

    /** 粘贴原点 + 预设 A/B/观众出生点。 */
    public record ArenaSpawns(Location pasteAt, Location spawnA, Location spawnB, Location spectator) {
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
        return createArenaFromPreset(worldName, presetName).thenApply(r -> r.world());
    }

    public record ArenaCreateResult(GuildWorld world, ArenaSpawns spawns) {
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
        return createVoidWorld(worldName, WorldType.BATTLE, presetName, null, null)
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

    public GuildPlugin getPlugin() {
        return plugin;
    }

    public boolean isAutoLoadStale() {
        return autoLoadStale;
    }

    public boolean isAutoCleanOrphans() {
        return autoCleanOrphans;
    }
}
