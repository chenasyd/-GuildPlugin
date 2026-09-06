package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

/**
 * 领地模块运行时配置（从 {@code modules.guild-territory.*} 读取，支持热重载）。
 */
public final class TerritorySettings {

    private final ModuleContext context;

    public TerritorySettings(ModuleContext context) {
        this.context = context;
    }

    public void reload() {
        // 配置通过 ModuleConfigSection 按需读取，无需额外缓存失效。
    }

    public Material getWandMaterial() {
        return TerritoryCommandHandler.parseMaterialPublic(
                context.getConfig().getString("claim.wand-material", "WOODEN_AXE"));
    }

    public int getMaxVolume() {
        return Math.max(1, context.getConfig().getInt("claim.max-volume", 50_000));
    }

    public int getMinVolume() {
        return Math.max(1, context.getConfig().getInt("claim.min-volume", 1));
    }

    /** 声明领地时从公会金库扣除；0 表示免费。 */
    public double getClaimCost() {
        return Math.max(0, context.getConfig().getDouble("claim.cost", 0));
    }

    public int getRegionPriority() {
        return context.getConfig().getInt("region.priority", 10);
    }

    public boolean isWorldAllowed(String worldName) {
        List<String> allowList = context.getConfig().getStringList("claim.allowed-worlds");
        if (allowList != null && !allowList.isEmpty()) {
            return allowList.stream().anyMatch(w -> w.equalsIgnoreCase(worldName));
        }
        List<String> denyList = context.getConfig().getStringList("claim.denied-worlds");
        if (denyList == null || denyList.isEmpty()) {
            return true;
        }
        return denyList.stream().noneMatch(w -> w.equalsIgnoreCase(worldName));
    }

    public String getHomeProtectMode() {
        return TerritoryHomeProtectIntegration.normalizeMode(
                context.getConfig().getString("home-protect.mode",
                        TerritoryHomeProtectIntegration.MODE_DEFER));
    }

    public boolean isMemberSyncEnabled() {
        return context.getConfig().getBoolean("member-sync.enabled", true);
    }

    public boolean isRepairOnLoad() {
        return context.getConfig().getBoolean("member-sync.repair-on-load", false);
    }

    public boolean isGuiEnabled() {
        return context.getConfig().getBoolean("gui.enabled", true);
    }

    public boolean isRegisterSettingsButton() {
        return isGuiEnabled()
                && context.getConfig().getBoolean("gui.register-settings-button", true);
    }

    public boolean isRegisterInfoButton() {
        return isGuiEnabled()
                && context.getConfig().getBoolean("gui.register-info-button", true);
    }

    /** WG StateFlag 配置值：allow / deny / none（不设置）。 */
    public String getFlag(String key, String defaultValue) {
        String raw = context.getConfig().getString("flags." + key, defaultValue);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeFlagValue(String raw, String defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
