package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
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
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 多世界管理系统 Facade（虚空世界 + 意外恢复）。
 *
 * <p>实现已拆分至 {@link WorldLifecycle}、{@link WorldPresetOps}、{@link WorldTeleport}、
 * {@link WorldRecoveryBootstrap} 与 {@link WorldSettings}。
 */
public class GuildWorldService {

    private final GuildPlugin plugin;
    private final File worldsDir;
    private final WorldRegistry registry;
    private final WorldJournal journal;
    private final WorldRecoveryService recovery;
    private final PresetService presets;
    private final SelectionManager selections = new SelectionManager();
    private final WorldRecoveryBootstrap recoveryBootstrap;
    private final WorldLifecycle lifecycle;
    private final WorldPresetOps presetOps;
    private final WorldTeleport teleport;

    /** 仅当「Folia 且版本不在支持列表」时为 false。 */
    private final boolean enabled;

    private WorldSettings settings;

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
        this.enabled = !ServerUtils.isFolia() || ServerUtils.isFoliaVersionSupported();
        reloadSettings();
        this.recoveryBootstrap = new WorldRecoveryBootstrap(
                plugin, registry, recovery,
                () -> enabled, this::unsupportedMessage,
                () -> settings.recoveryCheckEnabled);
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
                () -> settings.fallbackWorldName);
    }

    private void teleportToFallbackForLifecycle(Player player) {
        teleport.teleportToFallback(player);
    }

    private WorldPresetOps.SchematicSettings schematicSettings() {
        return new WorldPresetOps.SchematicSettings(
                settings.maxSchematicVolume, settings.ignoreAirOnPaste, settings.includeBlockEntities);
    }

    public void reloadSettings() {
        this.settings = new WorldSettings(plugin.getConfigManager().getMainConfig());
    }

    public void load() {
        recoveryBootstrap.load();
    }

    public void scheduleRecovery() {
        recoveryBootstrap.scheduleRecovery(this);
    }

    public void runRecovery() {
        recoveryBootstrap.runRecovery(this);
    }

    public void shutdown() {
        lifecycle.shutdown();
    }

    public CompletableFuture<GuildWorld> createVoidWorld(String worldName, WorldType type,
                                                         String presetName, String ownerGuildId, Long seed) {
        return lifecycle.createVoidWorld(worldName, type, presetName, ownerGuildId, seed);
    }

    public CompletableFuture<GuildWorld> loadWorld(String name) {
        return lifecycle.loadWorld(name);
    }

    public CompletableFuture<Void> unloadWorld(String name) {
        return lifecycle.unloadWorld(name);
    }

    public CompletableFuture<Void> deleteWorld(String name, boolean force) {
        return lifecycle.deleteWorld(name, force);
    }

    public CompletableFuture<Boolean> teleportToWorld(Player player, String worldName) {
        return teleport.teleportToWorld(player, worldName);
    }

    public CompletableFuture<Boolean> teleportToFallbackWorld(Player player) {
        return teleport.teleportToFallbackWorld(player);
    }

    public String buildWorldName(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.startsWith(settings.namePrefix) ? input : settings.namePrefix + input;
    }

    public boolean isEnabled() {
        return enabled;
    }

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
        return settings.namePrefix;
    }

    public Location getFallbackLocation() {
        return teleport.getFallbackLocation();
    }

    public SelectionManager getSelections() {
        return selections;
    }

    public Material getWandMaterial() {
        return settings.wandMaterial;
    }

    public String getPostMatchPolicy() {
        return settings.postMatchPolicy;
    }

    public CompletableFuture<PresetService.PresetMeta> savePresetFromSelection(Player editor, String presetName) {
        return presetOps.savePresetFromSelection(editor, presetName);
    }

    public CompletableFuture<Void> pastePreset(World world, Location pasteAt, String presetName) {
        return presetOps.pastePreset(world, pasteAt, presetName);
    }

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
        return settings.autoLoadStale;
    }

    public boolean isAutoCleanOrphans() {
        return settings.autoCleanOrphans;
    }
}
