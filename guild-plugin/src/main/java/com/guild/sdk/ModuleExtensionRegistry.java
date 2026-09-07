package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.module.ModuleManager;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 模块 GUI / 命令 / 占位符注册表。
 */
public final class ModuleExtensionRegistry {

    private final GuildPlugin plugin;
    private final Logger logger;

    private final Map<String, PlaceholderProvider> placeholderProviders = new ConcurrentHashMap<>();
    private final Map<String, ModuleGUIFactory> customGUIRegistry = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ModuleCommandHandler>> commandRegistry = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> permissionRegistry = new ConcurrentHashMap<>();
    private final Map<String, String> placeholderOwners = new ConcurrentHashMap<>();
    private final Map<String, String> commandOwners = new ConcurrentHashMap<>();
    private final Map<String, String> guiFactoryOwners = new ConcurrentHashMap<>();

    public ModuleExtensionRegistry(GuildPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void registerGUIButton(String guiType, int slot, ItemStack item,
                                  String moduleId,
                                  GUIExtensionHook.GUIClickAction handler) {
        if (moduleId == null || moduleId.isEmpty()) {
            throw new IllegalArgumentException("moduleId 不能为空");
        }
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        mm.getRegistry().getGuiExtensionHook()
                .registerButton(guiType, slot, item, moduleId, handler);
    }

    public void registerGUIButton(String guiType, int slot, ItemStack item,
                                  String moduleId,
                                  GUIExtensionHook.GUIClickAction handler,
                                  String displayNameKey, String... loreKeys) {
        if (moduleId == null || moduleId.isEmpty()) {
            throw new IllegalArgumentException("moduleId 不能为空");
        }
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        mm.getRegistry().getGuiExtensionHook()
                .registerButton(guiType, slot, item, moduleId, handler, displayNameKey, loreKeys);
    }

    public void registerCustomGUI(String moduleId, String guiId, ModuleGUIFactory factory) {
        if (moduleId == null || moduleId.isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be empty");
        }
        if (guiId == null || guiId.isEmpty()) {
            throw new IllegalArgumentException("guiId cannot be empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("factory cannot be null");
        }
        if (customGUIRegistry.containsKey(guiId)) {
            throw new IllegalArgumentException("guiId already registered: " + guiId);
        }
        customGUIRegistry.put(guiId, factory);
        guiFactoryOwners.put(guiId, moduleId);
    }

    public void unregisterCustomGUI(String guiId) {
        customGUIRegistry.remove(guiId);
        guiFactoryOwners.remove(guiId);
    }

    public void openCustomGUI(String guiId, Player player, Map<String, Object> data) {
        ModuleGUIFactory factory = customGUIRegistry.get(guiId);
        if (factory == null) {
            logger.warning("custom GUI not found: " + guiId);
            return;
        }
        GUI gui = factory.create(player, data != null ? data : Map.of());
        plugin.getGuiManager().pushAndOpen(player, gui);
    }

    public void registerCustomGUI(ModuleGUIRegistration registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration cannot be null");
        }
        String owner = registration.getModuleId();
        if (owner == null || owner.isEmpty()) {
            throw new IllegalArgumentException(
                    "moduleId is required on ModuleGUIRegistration (call .moduleId(...))");
        }
        String guiId = registration.getGuiId();
        if (customGUIRegistry.containsKey(guiId)) {
            throw new IllegalArgumentException("guiId already registered: " + guiId);
        }
        customGUIRegistry.put(guiId, registration.getFactory());
        guiFactoryOwners.put(guiId, owner);
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        mm.getRegistry().registerCustomGUI(registration);
    }

    public BedrockFormProvider getBedrockFormProvider(String guiId) {
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        ModuleGUIRegistration reg = mm.getRegistry().getCustomGUIRegistration(guiId);
        return reg != null ? reg.getBedrockFormProvider() : null;
    }

    public boolean hasModuleImageBinding(String guiId) {
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        ModuleGUIRegistration reg = mm.getRegistry().getCustomGUIRegistration(guiId);
        return reg != null && reg.getImageEntryId() != null;
    }

    public GUILayoutDefinition getModuleGUILayout(String guiId) {
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        ModuleGUIRegistration reg = mm.getRegistry().getCustomGUIRegistration(guiId);
        return reg != null ? reg.getLayout() : null;
    }

    public ModuleGUIConfig getModuleGUIConfig(String guiId) {
        ModuleManager mm = plugin.getServiceContainer().get(ModuleManager.class);
        ModuleGUIRegistration reg = mm.getRegistry().getCustomGUIRegistration(guiId);
        return reg != null ? reg.getConfig() : null;
    }

    public void registerSubCommand(String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        if (parentCommand == null || parentCommand.isEmpty() || name == null || name.isEmpty() || handler == null) {
            throw new IllegalArgumentException("parentCommand, name and handler cannot be null or empty");
        }

        commandRegistry.computeIfAbsent(parentCommand.toLowerCase(), k -> new ConcurrentHashMap<>())
                .put(name.toLowerCase(), handler);

        if (permission != null) {
            permissionRegistry.computeIfAbsent(parentCommand.toLowerCase(), k -> new ConcurrentHashMap<>())
                    .put(name.toLowerCase(), permission);
        }
    }

    public void registerSubCommand(String moduleId, String parentCommand, String name,
                                   ModuleCommandHandler handler,
                                   String permission) {
        registerSubCommand(parentCommand, name, handler, permission);
        commandOwners.put(parentCommand.toLowerCase() + "." + name.toLowerCase(), moduleId);
    }

    public boolean hasSubCommand(String parentCommand, String name) {
        Map<String, ModuleCommandHandler> subCommands = commandRegistry.get(parentCommand.toLowerCase());
        return subCommands != null && subCommands.containsKey(name.toLowerCase());
    }

    public ModuleCommandHandler getSubCommandHandler(String parentCommand, String name) {
        Map<String, ModuleCommandHandler> subCommands = commandRegistry.get(parentCommand.toLowerCase());
        return subCommands != null ? subCommands.get(name.toLowerCase()) : null;
    }

    public String getSubCommandPermission(String parentCommand, String name) {
        Map<String, String> permissions = permissionRegistry.get(parentCommand.toLowerCase());
        return permissions != null ? permissions.get(name.toLowerCase()) : null;
    }

    public List<String> getSubCommands(String parentCommand) {
        Map<String, ModuleCommandHandler> subCommands = commandRegistry.get(parentCommand.toLowerCase());
        return subCommands != null ? new ArrayList<>(subCommands.keySet()) : Collections.emptyList();
    }

    public void registerPlaceholderProvider(PlaceholderProvider provider) {
        if (provider == null || provider.getIdentifier() == null || provider.getIdentifier().trim().isEmpty()) {
            return;
        }
        placeholderProviders.put(provider.getIdentifier().toLowerCase(), provider);
    }

    public void registerPlaceholderProvider(String moduleId, PlaceholderProvider provider) {
        registerPlaceholderProvider(provider);
        if (provider != null && provider.getIdentifier() != null && !provider.getIdentifier().trim().isEmpty()) {
            placeholderOwners.put(provider.getIdentifier().toLowerCase(), moduleId);
        }
    }

    public void unregisterPlaceholderProvider(String identifier) {
        if (identifier == null) {
            return;
        }
        placeholderProviders.remove(identifier.toLowerCase());
    }

    public Map<String, PlaceholderProvider> getPlaceholderProviders() {
        return placeholderProviders;
    }

    public void clearModuleRegistrations(String moduleId) {
        placeholderOwners.entrySet().removeIf(e -> {
            if (moduleId.equals(e.getValue())) {
                placeholderProviders.remove(e.getKey());
                return true;
            }
            return false;
        });

        commandOwners.entrySet().removeIf(e -> {
            if (moduleId.equals(e.getValue())) {
                String[] parts = e.getKey().split("\\.", 2);
                if (parts.length == 2) {
                    Map<String, ModuleCommandHandler> subs = commandRegistry.get(parts[0]);
                    if (subs != null) {
                        subs.remove(parts[1]);
                    }
                    Map<String, String> perms = permissionRegistry.get(parts[0]);
                    if (perms != null) {
                        perms.remove(parts[1]);
                    }
                }
                return true;
            }
            return false;
        });

        guiFactoryOwners.entrySet().removeIf(e -> {
            if (moduleId.equals(e.getValue())) {
                customGUIRegistry.remove(e.getKey());
                return true;
            }
            return false;
        });
    }

    public void clearAll() {
        placeholderProviders.clear();
        customGUIRegistry.clear();
        commandRegistry.clear();
        permissionRegistry.clear();
        placeholderOwners.clear();
        commandOwners.clear();
        guiFactoryOwners.clear();
    }
}
