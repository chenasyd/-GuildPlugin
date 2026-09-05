package com.guild.module.example.territory;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * 运行时探测 WorldGuard / WorldEdit 是否可用。
 * <p>
 * 不使用 WG 类引用，保证模块在缺少依赖时仍可加载（软依赖）。
 */
public final class WorldGuardProbe {

    private static final String WORLDGUARD_PLUGIN = "WorldGuard";
    private static final String WORLDEDIT_PLUGIN = "WorldEdit";

    private WorldGuardProbe() {
    }

    public static Availability probe() {
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin(WORLDGUARD_PLUGIN);
        Plugin worldEdit = Bukkit.getPluginManager().getPlugin(WORLDEDIT_PLUGIN);
        boolean wgEnabled = worldGuard != null && worldGuard.isEnabled();
        boolean weEnabled = worldEdit != null && worldEdit.isEnabled();
        return new Availability(wgEnabled, weEnabled);
    }

    public record Availability(boolean worldGuardReady, boolean worldEditReady) {

        public boolean fullyReady() {
            return worldGuardReady && worldEditReady;
        }

        public String describeMissing() {
            if (fullyReady()) {
                return "ready";
            }
            if (!worldGuardReady && !worldEditReady) {
                return "WorldGuard and WorldEdit";
            }
            if (!worldGuardReady) {
                return "WorldGuard";
            }
            return "WorldEdit";
        }
    }
}
