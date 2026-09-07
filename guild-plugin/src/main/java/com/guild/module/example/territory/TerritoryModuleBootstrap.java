package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** 领地模块启动状态日志与管理员通知。 */
final class TerritoryModuleBootstrap {

    private TerritoryModuleBootstrap() {
    }

    static void logLoadStatus(ModuleContext context, TerritoryBridge bridge,
                              WorldGuardProbe.Availability availability) {
        if (bridge.isOperational()) {
            context.getLogger().info("WorldGuard territory bridge active.");
        } else {
            context.getLogger().warning("Guild territory module loaded in degraded mode; missing: "
                    + availability.describeMissing()
                    + ". Install WorldGuard + WorldEdit to enable territory features.");
        }
    }

    static void broadcastStatusToAdmins(ModuleContext context, TerritoryBridge bridge,
                                        WorldGuardProbe.Availability availability, TerritoryTexts texts) {
        if (texts == null || context == null) {
            return;
        }
        String message;
        if (bridge.isOperational()) {
            message = texts.format("module.territory.status-ready",
                    "&a领地模块已加载（WorldGuard 可用）");
        } else {
            message = texts.format("module.territory.status-degraded",
                    "&e领地模块已降级加载：缺少 {0}", availability.describeMissing());
        }
        context.getLogger().info(ColorUtils.stripColor(message));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (context.getPlugin().getPermissionManager().hasPermission(player, "guild.territory.admin")) {
                player.sendMessage(message);
            }
        }
    }

    static TerritoryHomeProtectIntegration registerHomeProtectIntegration(
            TerritoryModule module, ModuleContext context,
            TerritorySettings settings, TerritoryHomeProtectIntegration existing) {
        if (context == null || settings == null) {
            return existing;
        }
        String mode = settings.getHomeProtectMode();
        if (TerritoryHomeProtectIntegration.MODE_OFF.equals(mode)) {
            if (existing != null) {
                context.getApi().unregisterHomeProtectIntegration(module);
            }
            return null;
        }

        TerritoryHomeProtectIntegration integration = existing;
        if (integration == null) {
            integration = new TerritoryHomeProtectIntegration(module, context);
            context.getApi().registerHomeProtectIntegration(module, integration);
        }

        if (module.getBridge().isOperational()
                && TerritoryHomeProtectIntegration.MODE_DEFER.equals(mode)) {
            context.getLogger().info("Deferring guild.home-protect to WorldGuard (home-protect.mode=defer).");
        } else if (module.getBridge().isOperational()) {
            context.getLogger().info("Home-protect merge mode active (home-protect.mode=merge).");
        }
        return integration;
    }
}
