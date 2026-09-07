package com.guild.sdk.api;

import com.guild.sdk.home.HomeProtectIntegration;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 与内置 {@code guild.home-protect} 协调的域 API。
 *
 * @since 1.6.7
 */
public interface ModuleHomeProtectAPI {

    void registerHomeProtectIntegration(Object moduleInstance, HomeProtectIntegration integration);

    boolean isHomeProtectFullyDeferred();

    boolean shouldSkipHomeProtectAt(Player player, Location location);

    boolean shouldSkipHomeProtectForGuildHome(int guildId, String worldName);
}
