package com.guild.sdk;

/**
 * 模块卸载时的 SDK 资源清理编排。
 */
public final class ModuleLifecycleCleanup {

    private final ModuleEventBus events;
    private final ModuleHomeProtectCoordinator homeProtect;
    private final ModuleExtensionRegistry extensions;

    public ModuleLifecycleCleanup(ModuleEventBus events,
                                  ModuleHomeProtectCoordinator homeProtect,
                                  ModuleExtensionRegistry extensions) {
        this.events = events;
        this.homeProtect = homeProtect;
        this.extensions = extensions;
    }

    /**
     * 清理指定模块在卸载时注册的事件处理器、Home 保护集成与扩展注册项。
     */
    public void clearOnUnload(String moduleId, Object moduleInstance) {
        events.clearModuleHandlers(moduleInstance);
        homeProtect.clearModuleHandlers(moduleInstance);
        extensions.clearModuleRegistrations(moduleId);
    }

    public void clearAll() {
        events.clearAll();
        extensions.clearAll();
    }
}
