package com.guild.sdk.home;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 模块可注册此集成，与主插件 {@code GuildHomeProtectListener} 协调，避免与 WorldGuard 等重复保护。
 */
public interface HomeProtectIntegration {

    /** 为 true 时完全禁用内置 home-protect 监听。 */
    default boolean deferAll() {
        return false;
    }

    /** 为 true 时该位置跳过内置保护（由外部系统负责，如 WG 区域）。 */
    default boolean skipAt(Player player, Location location) {
        return false;
    }

    /** 为 true 时该公会在指定世界的 home 半径保护交由外部系统。 */
    default boolean skipHomeForGuild(int guildId, String worldName) {
        return false;
    }
}
