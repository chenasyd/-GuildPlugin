package com.guild.core.module.config;

import com.guild.sdk.gui.ModuleGUIConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Compile-time stub for DefaultModuleGUIConfig.
 * Runtime implementation is provided by guild-plugin.
 * <p>
 * Modules can instantiate this in their registration:
 * <pre>{@code
 * .config(new DefaultModuleGUIConfig(moduleId, plugin))
 * }</pre>
 */
public class DefaultModuleGUIConfig implements ModuleGUIConfig {

    public DefaultModuleGUIConfig(String moduleId, Object plugin) {
    }

    public void reload() {
    }

    @Override
    public ItemStack getDisplayItem(String key, ItemStack fallback, Player player) {
        return fallback;
    }

    @Override
    public String getDisplayText(String key, String fallback) {
        return fallback;
    }

    @Override
    public boolean getDisplayFlag(String key, boolean fallback) {
        return fallback;
    }
}
