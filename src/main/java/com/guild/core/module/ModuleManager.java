package com.guild.core.module;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.core.module.exception.ModuleConflictException;
import com.guild.core.module.exception.ModuleDependencyException;
import com.guild.core.module.exception.ModuleLoadException;
import com.guild.core.utils.ColorUtils;
import com.guild.sdk.GuildPluginAPI;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 模块管理器 - 负责模块的生命周期管理和热插拔操作
 */
public class ModuleManager {

    private final GuildPlugin plugin;
    private final Logger logger;
    private final LanguageManager lang;
    private final File modulesDir;
    private final ModuleLoader loader;
    private final ModuleRegistry registry;

    /** 共享 API 实例（所有模块共用，确保事件集中分发） */
    private final GuildPluginAPI sharedApi;

    /** 核心支持的 API 版本号 */
    private static final String CORE_API_VERSION = "1.0.0";

    public ModuleManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.lang = plugin.getLanguageManager();
        this.modulesDir = new File(plugin.getDataFolder(), "modules");
        this.loader = new ModuleLoader(this);
        this.registry = new ModuleRegistry();
        this.sharedApi = new GuildPluginAPI(plugin);

        if (!modulesDir.exists()) {
            modulesDir.mkdirs();
        }
    }

    // ==================== 启动加载 ====================

    /**
     * 插件启动时自动扫描并加载所有模块
     */
    public void loadAllModules() {
        logger.info(ColorUtils.colorize(lang.getMessage("module.system.scanning", "")));

        File[] jarFiles = modulesDir.listFiles((dir, name) ->
                name.endsWith(".jar") && !name.startsWith("."));

        if (jarFiles == null || jarFiles.length == 0) {
            logger.info(ColorUtils.colorize(lang.getMessage("module.system.no-modules", "")));
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (File jarFile : jarFiles) {
            try {
                loadModule(jarFile);
                successCount++;
            } catch (ModuleLoadException e) {
                failCount++;
                logError(e);
            }
        }

        logger.info(ColorUtils.colorize(lang.getIndexedMessage("module.system.load-complete", "",
                String.valueOf(successCount), String.valueOf(failCount))));
    }

    // ==================== 热加载 ====================

    /**
     * 热加载单个模块（同步方法，保证原子性）
     */
    public synchronized GuildModule loadModule(File jarFile) throws ModuleLoadException {
        String fileName = jarFile.getName();

        // 1. 解析模块描述符
        ModuleDescriptor descriptor = loader.parseDescriptor(jarFile);
        String moduleId = descriptor.getId();

        logger.fine(lang.getIndexedMessage("module.system.loading", "", moduleId));

        // 2. 冲突检查
        if (registry.isLoaded(moduleId)) {
            throw new ModuleConflictException(
                    lang.getIndexedMessage("module.error.already-loaded", "", moduleId)
            );
        }

        // 3. 依赖检查
        checkDependencies(descriptor);

        // 4. API 版本兼容性检查
        checkCompatibility(descriptor);

        // 5. 创建独立 ClassLoader 并实例化模块
        GuildModule module = loader.instantiateModule(jarFile, descriptor);

        // 6. 创建模块上下文并启用（使用共享 API 实例）
        ModuleContext context = new ModuleContext(plugin, descriptor, sharedApi);
        registry.setState(moduleId, ModuleState.LOADING);

        try {
            module.onEnable(context);
        } catch (Exception e) {
            registry.setState(moduleId, ModuleState.ERROR);
            loader.unloadClassloader(moduleId);
            throw new ModuleLoadException(
                    lang.getIndexedMessage("module.error.enable-failed", "",
                            moduleId, e.getMessage()), e
            );
        }

        // 7. 注册到注册表
        registry.register(module);
        registry.setState(moduleId, ModuleState.ACTIVE);

        logger.info(ColorUtils.colorize(lang.getIndexedMessage("module.system.loaded-successfully", "",
                descriptor.getName(), descriptor.getVersion(), descriptor.getAuthor())));

        return module;
    }

    // ==================== 热卸载 ====================

    /**
     * 热卸载指定模块（同步方法，保证原子性）
     */
    public synchronized boolean unloadModule(String moduleId) {
        GuildModule module = registry.getModule(moduleId);
        if (module == null) {
            logger.warning(ColorUtils.colorize(lang.getIndexedMessage("module.error.not-loaded", "", moduleId)));
            return false;
        }

        // 检查依赖者
        try {
            checkDependents(moduleId);
        } catch (ModuleConflictException e) {
            logger.warning(ColorUtils.colorize(e.getMessage()));
            return false;
        }

        logger.info(ColorUtils.colorize(lang.getIndexedMessage("module.system.unloading", "", moduleId)));
        registry.setState(moduleId, ModuleState.DISABLING);

        try {
            registry.unregister(moduleId);
            sharedApi.clearModuleHandlers(module);
            module.onDisable();
            loader.unloadClassloader(moduleId);

            logger.info(ColorUtils.colorize(lang.getIndexedMessage("module.system.unloaded-successfully", "", moduleId)));
            return true;

        } catch (Exception e) {
            registry.setState(moduleId, ModuleState.ERROR);
            logger.log(Level.SEVERE,
                    lang.getIndexedMessage("module.error.unload-failed", "", moduleId, e.getMessage()), e);
            return false;
        }
    }

    // ==================== 热重载 ====================

    /**
     * 热重载指定模块
     */
    public synchronized boolean reloadModule(String moduleId) {
        File moduleFile = findModuleFile(moduleId);
        if (moduleFile == null) {
            logger.warning(ColorUtils.colorize(lang.getIndexedMessage("module.error.file-not-found", "", moduleId)));
            return false;
        }

        logger.info(ColorUtils.colorize(lang.getIndexedMessage("module.system.reloading", "", moduleId)));

        boolean unloaded = unloadModule(moduleId);
        if (!unloaded) {
            return false;
        }

        try {
            loadModule(moduleFile);
            logger.info(ColorUtils.colorize(lang.getIndexedMessage("module.system.reloaded-successfully", "", moduleId)));
            return true;
        } catch (ModuleLoadException e) {
            logError(e);
            return false;
        }
    }

    // ==================== 全部卸载 ====================

    /**
     * 卸载所有已加载的模块（插件关闭时调用）
     */
    public synchronized void unloadAllModules() {
        List<String> moduleIds = new java.util.ArrayList<>(registry.getModuleIds());

        for (String moduleId : moduleIds) {
            try {
                unloadModule(moduleId);
            } catch (Exception e) {
                logger.log(Level.WARNING,
                        lang.getIndexedMessage("module.error.unload-failed-during-shutdown",
                                "", moduleId), e);
            }
        }

        loader.unloadAll();
        sharedApi.clearAll();
        logger.info(ColorUtils.colorize(lang.getMessage("module.system.all-unloaded", "")));
    }

    // ==================== 查询方法 ====================

    /** 获取模块注册表 */
    public ModuleRegistry getRegistry() { return registry; }

    /** 获取模块目录 */
    public File getModulesDirectory() { return modulesDir; }

    /** 获取已加载模块数量 */
    public int getLoadedCount() { return registry.size(); }

    /** 获取共享的 API 实例（供核心服务分发事件用） */
    public GuildPluginAPI getSharedApi() { return sharedApi; }

    // ==================== 内部辅助方法 ====================

    private void checkDependencies(ModuleDescriptor descriptor) throws ModuleDependencyException {
        List<String> depends = descriptor.getDepends();
        if (depends.isEmpty()) {
            return;
        }

        List<String> missing = new java.util.ArrayList<>();
        for (String dep : depends) {
            if (!registry.isLoaded(dep)) {
                missing.add(dep);
            }
        }

        if (!missing.isEmpty()) {
            throw new ModuleDependencyException(descriptor.getId(), missing);
        }

        for (String softDep : descriptor.getSoftDepends()) {
            if (!registry.isLoaded(softDep)) {
                logger.warning(ColorUtils.colorize(lang.getIndexedMessage("module.warning.soft-depend-missing",
                        "", softDep, descriptor.getId())));
            }
        }
    }

    private void checkCompatibility(ModuleDescriptor descriptor) throws ModuleLoadException {
        String requiredApi = descriptor.getApiVersion();
        if (requiredApi == null || requiredApi.trim().isEmpty()) {
            logger.warning(ColorUtils.colorize(lang.getIndexedMessage("module.warning.no-api-version",
                    "", descriptor.getId())));
            return;
        }
        // TODO: 可在此处添加更精细的版本比较逻辑
    }

    private void checkDependents(String moduleId) throws ModuleConflictException {
        for (GuildModule other : registry.getAllModules()) {
            if (other.getDescriptor().getDepends().contains(moduleId)) {
                throw new ModuleConflictException(
                        lang.getIndexedMessage("module.error.has-dependents",
                                "", moduleId, other.getDescriptor().getId())
                );
            }
        }
    }

    private File findModuleFile(String moduleId) {
        File[] files = modulesDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return null;

        for (File file : files) {
            try {
                ModuleDescriptor desc = ModuleDescriptor.fromJar(file);
                if (desc.getId().equals(moduleId)) {
                    return file;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void logError(ModuleLoadException e) {
        if (e instanceof ModuleDependencyException) {
            ModuleDependencyException mde = (ModuleDependencyException) e;
            logger.severe(e.getMessage());
            logger.severe("  缺失依赖: " + mde.getMissingDependencies());
        } else {
            logger.log(Level.SEVERE, e.getMessage(), e.getCause());
        }
    }
}
