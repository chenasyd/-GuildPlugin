package com.guild.world;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/** 多世界模块配置快照（支持 {@link GuildWorldService#reloadSettings()} 热更新）。 */
public final class WorldSettings {

    public final String namePrefix;
    public final String fallbackWorldName;
    public final boolean recoveryCheckEnabled;
    public final boolean autoLoadStale;
    public final boolean autoCleanOrphans;
    public final Material wandMaterial;
    public final int maxSchematicVolume;
    public final boolean ignoreAirOnPaste;
    public final boolean includeBlockEntities;
    public final String postMatchPolicy;

    public WorldSettings(FileConfiguration config) {
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
}
