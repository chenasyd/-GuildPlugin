package com.guild.listeners;

import com.guild.GuildPlugin;
import com.guild.core.module.ModuleManager;
import com.guild.sdk.GuildPluginAPI;

/**
 * 查询模块注册的 home-protect 协调策略（供 {@link GuildHomeProtectListener} 使用）。
 */
final class HomeProtectIntegrationSupport {

    private HomeProtectIntegrationSupport() {
    }

    static boolean isFullyDeferred(GuildPlugin plugin) {
        GuildPluginAPI api = sharedApi(plugin);
        return api != null && api.isHomeProtectFullyDeferred();
    }

    static boolean shouldSkipAt(GuildPlugin plugin, org.bukkit.entity.Player player, org.bukkit.Location location) {
        GuildPluginAPI api = sharedApi(plugin);
        return api != null && api.shouldSkipHomeProtectAt(player, location);
    }

    static boolean shouldSkipGuildHome(GuildPlugin plugin, int guildId, String worldName) {
        GuildPluginAPI api = sharedApi(plugin);
        return api != null && api.shouldSkipHomeProtectForGuildHome(guildId, worldName);
    }

    private static GuildPluginAPI sharedApi(GuildPlugin plugin) {
        if (plugin == null) {
            return null;
        }
        ModuleManager moduleManager = plugin.getModuleManager();
        return moduleManager != null ? moduleManager.getSharedApi() : null;
    }
}
