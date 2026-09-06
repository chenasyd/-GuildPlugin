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

    /** 当前世界是否允许声明领地。 */
    public boolean isWorldAllowed(String worldName) {
        return getWorldClaimPolicy().isAllowed(worldName);
    }

    public TerritoryWorldClaimPolicy getWorldClaimPolicy() {
        return TerritoryWorldClaimPolicy.from(context.getConfig());
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

    public boolean isCrossServerEnabled() {
        if (context == null) {
            return false;
        }
        return context.getConfig().getBoolean("cross-server.enabled", true);
    }

    /** 非空时覆盖随机 server-id。 */
    public String getConfiguredServerId() {
        if (context == null) {
            return "";
        }
        return context.getConfig().getString("cross-server.server-id", "");
    }

    /** 是否在 claim/unclaim 后通过 Bungee 广播缓存失效（需 cross-server.enabled）。 */
    public boolean isBroadcastEventsEnabled() {
        if (context == null) {
            return false;
        }
        return context.getConfig().getBoolean("cross-server.broadcast-events", true);
    }

    /** 启动时根据 DB 元数据在本机补建缺失的 WG 区域。 */
    public boolean isMaterializeOnLoad() {
        if (context == null) {
            return false;
        }
        return context.getConfig().getBoolean("cross-server.materialize-on-load", true);
    }

    /** 世界延迟加载时再次尝试 materialize。 */
    public boolean isMaterializeOnWorldLoad() {
        if (context == null) {
            return false;
        }
        return context.getConfig().getBoolean("cross-server.materialize-on-world-load", true);
    }

    /** 是否重试上次标记为 FAILED 的 materialize。 */
    public boolean isMaterializeRetryFailed() {
        if (context == null) {
            return false;
        }
        return context.getConfig().getBoolean("cross-server.materialize-retry-failed", true);
    }

    /** SDK EventBus 事件总开关。 */
    public boolean isEventsEnabled() {
        if (context == null) {
            return true;
        }
        return context.getConfig().getBoolean("events.enabled", true);
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
