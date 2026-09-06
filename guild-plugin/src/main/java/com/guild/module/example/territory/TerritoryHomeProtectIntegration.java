package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.sdk.home.HomeProtectIntegration;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 领地模块与 {@code guild.home-protect} 互斥/合并策略。
 */
public final class TerritoryHomeProtectIntegration implements HomeProtectIntegration {

    public static final String MODE_DEFER = "defer";
    public static final String MODE_MERGE = "merge";
    public static final String MODE_OFF = "off";

    private final TerritoryModule module;
    private final ModuleContext context;

    public TerritoryHomeProtectIntegration(TerritoryModule module, ModuleContext context) {
        this.module = module;
        this.context = context;
    }

    static String normalizeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return MODE_DEFER;
        }
        return raw.trim().toLowerCase();
    }

    String mode() {
        return normalizeMode(context.getConfig().getString("home-protect.mode", MODE_DEFER));
    }

    private boolean wgReady() {
        return module.getBridge().isOperational();
    }

    @Override
    public boolean deferAll() {
        return wgReady() && MODE_DEFER.equals(mode());
    }

    @Override
    public boolean skipAt(Player player, Location location) {
        if (!wgReady() || !MODE_MERGE.equals(mode()) || location == null || location.getWorld() == null) {
            return false;
        }
        return module.getBridge().findTerritoryAt(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        ).isPresent();
    }

    @Override
    public boolean skipHomeForGuild(int guildId, String worldName) {
        if (!wgReady() || !MODE_MERGE.equals(mode()) || worldName == null) {
            return false;
        }
        return module.getRepository().get(guildId, worldName).isPresent()
                || module.getBridge().findTerritory(guildId, worldName).isPresent();
    }
}
