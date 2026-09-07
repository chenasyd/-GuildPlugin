package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.DebugLog;
import com.guild.core.utils.ServerUtils;
import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldType;
import com.guild.world.preset.PresetService;
import com.guild.world.recovery.WorldRecoveryService;
import com.guild.world.registry.WorldJournal;
import com.guild.world.registry.WorldRegistry;
import com.guild.world.selection.SelectionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
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
    private final WorldPresetOps presetOps;
    private final WorldTeleport teleport;
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
                this::teleportToFallbackForLifecycle);
        this.presetOps = new WorldPresetOps(
                plugin, registry, presets, selections, lifecycle,
                () -> enabled, this::unsupportedMessage, this::schematicSettings);
        this.teleport = new WorldTeleport(
                plugin, registry, lifecycle,
                () -> enabled, this::unsupportedMessage, this::buildWorldName,
                () -> fallbackWorldName);
    }

    /** 供 {@link WorldLifecycle} 回调（lifecycle 先于 {@link #teleport} 字段赋值）。 */
    private void teleportToFallbackForLifecycle(Player player) {
        teleport.teleportToFallback(player);
    }

    private WorldPresetOps.SchematicSettings schematicSettings() {
        return new WorldPresetOps.SchematicSettings(maxSchematicVolume, ignoreAirOnPaste, includeBlockEntities);
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

    public CompletableFuture<Boolean> teleportToWorld(Player player, String worldName) {
        return teleport.teleportToWorld(player, worldName);
    }

    public CompletableFuture<Boolean> teleportToFallbackWorld(Player player) {
        return teleport.teleportToFallbackWorld(player);
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
        return teleport.getFallbackLocation();
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

    public CompletableFuture<PresetService.PresetMeta> savePresetFromSelection(Player editor, String presetName) {
        return presetOps.savePresetFromSelection(editor, presetName);
    }

    public CompletableFuture<Void> pastePreset(World world, Location pasteAt, String presetName) {
        return presetOps.pastePreset(world, pasteAt, presetName);
    }

    /** 粘贴原点 + 预设 A/B/观众出生点（兼容别名，见 {@link WorldPresetOps.ArenaSpawns}）。 */
    public record ArenaSpawns(Location pasteAt, Location spawnA, Location spawnB, Location spectator) {
        static ArenaSpawns from(WorldPresetOps.ArenaSpawns spawns) {
            return new ArenaSpawns(spawns.pasteAt(), spawns.spawnA(), spawns.spawnB(), spawns.spectator());
        }
    }

    public ArenaSpawns resolvePresetSpawns(World world, Location pasteAt, PresetService.PresetMeta meta) {
        return ArenaSpawns.from(presetOps.resolvePresetSpawns(world, pasteAt, meta));
    }

    public CompletableFuture<GuildWorld> createWorldFromPreset(String worldName, String presetName) {
        return presetOps.createWorldFromPreset(worldName, presetName);
    }

    public record ArenaCreateResult(GuildWorld world, ArenaSpawns spawns) {
        static ArenaCreateResult from(WorldPresetOps.ArenaCreateResult result) {
            return new ArenaCreateResult(result.world(), ArenaSpawns.from(result.spawns()));
        }
    }

    public CompletableFuture<ArenaCreateResult> createArenaFromPreset(String worldName, String presetName) {
        return presetOps.createArenaFromPreset(worldName, presetName).thenApply(ArenaCreateResult::from);
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
