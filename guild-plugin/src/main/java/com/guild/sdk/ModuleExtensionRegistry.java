package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.gui.BedrockFormProvider;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIConfig;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.gui.ModuleGUIRegistration;
import com.guild.sdk.placeholder.PlaceholderProvider;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 模块 GUI / 命令 / 占位符注册表组合门面。
 */
public final class ModuleExtensionRegistry {

    private final ModuleCommandRegistry commands;
    private final ModulePlaceholderRegistry placeholders;
    private final ModuleGuiRegistry guis;

    public ModuleExtensionRegistry(GuildPlugin plugin, Logger logger) {
        this(new ModuleCommandRegistry(), new ModulePlaceholderRegistry(), new ModuleGuiRegistry(plugin, logger));
    }

    ModuleExtensionRegistry(ModuleCommandRegistry commands,
                            ModulePlaceholderRegistry placeholders,
                            ModuleGuiRegistry guis) {
        this.commands = commands;
        this.placeholders = placeholders;
        this.guis = guis;
    }

    public void registerGUIButton(String guiType, int slot, ItemStack item,
                                  String moduleId,
                                  GUIExtensionHook.GUIClickAction handler) {
        guis.registerGUIButton(guiType, slot, item, moduleId, handler);
    }

    public void registerGUIButton(String guiType, int slot, ItemStack item,
                                  String moduleId,
                                  GUIExtensionHook.GUIClickAction handler,
                                  String displayNameKey, String... loreKeys) {
        guis.registerGUIButton(guiType, slot, item, moduleId, handler, displayNameKey, loreKeys);
    }

    public void registerCustomGUI(String moduleId, String guiId, ModuleGUIFactory factory) {
        guis.registerCustomGUI(moduleId, guiId, factory);
    }

    public void unregisterCustomGUI(String guiId) {
        guis.unregisterCustomGUI(guiId);
    }

    public void openCustomGUI(String guiId, Player player, Map<String, Object> data) {
        guis.openCustomGUI(guiId, player, data);
    }

    public void registerCustomGUI(ModuleGUIRegistration registration) {
        guis.registerCustomGUI(registration);
    }

    public BedrockFormProvider getBedrockFormProvider(String guiId) {
        return guis.getBedrockFormProvider(guiId);
    }

    public boolean hasModuleImageBinding(String guiId) {
        return guis.hasModuleImageBinding(guiId);
    }

    public GUILayoutDefinition getModuleGUILayout(String guiId) {
        return guis.getModuleGUILayout(guiId);
    }

    public ModuleGUIConfig getModuleGUIConfig(String guiId) {
        return guis.getModuleGUIConfig(guiId);
    }

    public void registerSubCommand(String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        commands.registerSubCommand(parentCommand, name, handler, permission);
    }

    public void registerSubCommand(String moduleId, String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        commands.registerSubCommand(moduleId, parentCommand, name, handler, permission);
    }

    public boolean hasSubCommand(String parentCommand, String name) {
        return commands.hasSubCommand(parentCommand, name);
    }

    public ModuleCommandHandler getSubCommandHandler(String parentCommand, String name) {
        return commands.getSubCommandHandler(parentCommand, name);
    }

    public String getSubCommandPermission(String parentCommand, String name) {
        return commands.getSubCommandPermission(parentCommand, name);
    }

    public List<String> getSubCommands(String parentCommand) {
        return commands.getSubCommands(parentCommand);
    }

    public void registerPlaceholderProvider(PlaceholderProvider provider) {
        placeholders.registerPlaceholderProvider(provider);
    }

    public void registerPlaceholderProvider(String moduleId, PlaceholderProvider provider) {
        placeholders.registerPlaceholderProvider(moduleId, provider);
    }

    public void unregisterPlaceholderProvider(String identifier) {
        placeholders.unregisterPlaceholderProvider(identifier);
    }

    public Map<String, PlaceholderProvider> getPlaceholderProviders() {
        return placeholders.getPlaceholderProviders();
    }

    public void clearModuleRegistrations(String moduleId) {
        placeholders.clearModuleRegistrations(moduleId);
        commands.clearModuleRegistrations(moduleId);
        guis.clearModuleRegistrations(moduleId);
    }

    public void clearAll() {
        placeholders.clearAll();
        commands.clearAll();
        guis.clearAll();
    }
}
