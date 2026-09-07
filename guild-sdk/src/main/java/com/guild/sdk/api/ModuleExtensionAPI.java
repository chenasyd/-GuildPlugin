package com.guild.sdk.api;

import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.gui.ModuleGUIRegistration;
import com.guild.sdk.placeholder.PlaceholderProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 模块 GUI / 命令 / 占位符注册域 API。
 *
 * @since 1.6.7
 */
public interface ModuleExtensionAPI {

    void registerGUIButton(String guiType, int slot, ItemStack item, String moduleId,
                           GUIExtensionHook.GUIClickAction handler);

    void registerGUIButton(String guiType, int slot, ItemStack item, String moduleId,
                           GUIExtensionHook.GUIClickAction handler,
                           String displayNameKey, String... loreKeys);

    @Deprecated
    void registerCustomGUI(String guiId, ModuleGUIFactory factory);

    void registerCustomGUI(String moduleId, String guiId, ModuleGUIFactory factory);

    void registerCustomGUI(ModuleGUIRegistration registration);

    void unregisterCustomGUI(String guiId);

    void openCustomGUI(String guiId, Player player, Map<String, Object> data);

    void openCustomGUI(String guiId, Player player);

    void registerSubCommand(String parentCommand, String name,
                            ModuleCommandHandler handler, String permission);

    void registerSubCommand(String moduleId, String parentCommand, String name,
                            ModuleCommandHandler handler, String permission);

    void registerPlaceholderProvider(PlaceholderProvider provider);

    void registerPlaceholderProvider(String moduleId, PlaceholderProvider provider);

    void unregisterPlaceholderProvider(String identifier);
}
