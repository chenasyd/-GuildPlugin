package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.sdk.gui.BedrockFormProvider;
import com.guild.sdk.gui.GUILayoutDefinition;
import com.guild.sdk.gui.ModuleGUIConfig;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.gui.ModuleGUIRegistration;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 模块自定义 GUI 注册表与 GUI 增强查询。
 */
public final class ModuleGuiRegistry {

    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<String, ModuleGUIFactory> customGUIRegistry = new ConcurrentHashMap<>();
    private final Map<String, String> guiFactoryOwners = new ConcurrentHashMap<>();

    public ModuleGuiRegistry(GuildPlugin plugin, Logger logger) {
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

    public void clearModuleRegistrations(String moduleId) {
        guiFactoryOwners.entrySet().removeIf(e -> {
            if (moduleId.equals(e.getValue())) {
                customGUIRegistry.remove(e.getKey());
                return true;
            }
            return false;
        });
    }

    public void clearAll() {
        customGUIRegistry.clear();
        guiFactoryOwners.clear();
    }
}
