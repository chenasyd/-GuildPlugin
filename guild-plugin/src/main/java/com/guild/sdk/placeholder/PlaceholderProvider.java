package com.guild.sdk.placeholder;

import org.bukkit.entity.Player;

/**
 * 占位符提供者接口。
 */
public interface PlaceholderProvider {
    String getIdentifier();
    String onRequest(Player player, String params);
}
