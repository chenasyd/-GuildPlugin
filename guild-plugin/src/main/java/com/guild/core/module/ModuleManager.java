package com.guild.core.module;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.core.module.exception.ModuleConflictException;
import com.guild.core.module.exception.ModuleDependencyException;
import com.guild.core.module.exception.ModuleLoadException;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.ConsoleLogger;
import com.guild.sdk.GuildPluginAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        ConsoleLogger.info(lang.getMessage("module.system.scanning", ""));

        File[] jarFiles = modulesDir.listFiles((dir, name) ->
                name.endsWith(".jar") && !name.startsWith("."));

        if (jarFiles == null || jarFiles.length == 0) {
            ConsoleLogger.info(lang.getMessage("module.system.no-modules", ""));
            return;
        }

        // Deduplicate: for same moduleId, keep only the JAR with the newest api-version
        DedupResult dedup = deduplicateModules(jarFiles);
        File[] filteredJars = dedup.filteredJars;
        List<DuplicateInfo> skipped = dedup.skippedDuplicates;

        int successCount = 0;
        int failCount = 0;

        for (File jarFile : filteredJars) {
            try {
                loadModule(jarFile);
                successCount++;
            } catch (ModuleLoadException e) {
                failCount++;
                logError(e);
            }
        }

        // Report skipped duplicates
        for (DuplicateInfo dup : skipped) {
            ConsoleLogger.warn(lang.getIndexedMessage("module.system.duplicate-skip", "",
                    dup.moduleId, dup.skippedFileName, dup.skippedApiVersion, dup.keptApiVersion));
        }
        if (!skipped.isEmpty()) {
            ConsoleLogger.warn(lang.getMessage("module.system.duplicate-cleanup-title", ""));
            for (DuplicateInfo dup : skipped) {
                ConsoleLogger.warn(lang.getIndexedMessage("module.system.duplicate-cleanup-item", "",
                        dup.skippedFileName, dup.keptFileName));
            }
        }

        ConsoleLogger.info(lang.getIndexedMessage("module.system.load-complete", "",
                String.valueOf(successCount), String.valueOf(failCount)));
    }

    /**
     * Pre-scan JARs to detect and resolve duplicate module IDs.
     * For modules with the same ID, keep the one with the newest api-version (SDK version).
     *
     * @param jarFiles all JAR files found in the modules directory
     * @return deduplication result with filtered JAR list and skipped duplicate info
     */
    private DedupResult deduplicateModules(File[] jarFiles) {
        // Parse all descriptors, tracking by moduleId
        Map<String, DescriptorEntry> bestByModule = new LinkedHashMap<>();
        List<DuplicateInfo> skipped = new ArrayList<>();
        List<File> unparseable = new ArrayList<>();

        for (File jarFile : jarFiles) {
            ModuleDescriptor desc;
            try {
                desc = loader.parseDescriptor(jarFile);
            } catch (ModuleLoadException e) {
                // Unparseable JAR (not a valid module) — keep it so it fails naturally during load
                unparseable.add(jarFile);
                continue;
            }

            String moduleId = desc.getId();
            DescriptorEntry existing = bestByModule.get(moduleId);

            if (existing == null) {
                bestByModule.put(moduleId, new DescriptorEntry(jarFile, desc));
            } else {
                // Duplicate moduleId — compare api-version, keep the newer one
                int cmp = compareApiVersions(desc.getApiVersion(), existing.descriptor.getApiVersion());
                if (cmp > 0) {
                    // Current is newer → skip the old one, keep current
                    skipped.add(new DuplicateInfo(
                            moduleId,
                            existing.file.getName(),
                            existing.descriptor.getApiVersion(),
                            desc.getApiVersion(),
                            jarFile.getName()));
                    bestByModule.put(moduleId, new DescriptorEntry(jarFile, desc));
                } else {
                    // Existing is newer or same → skip current
                    skipped.add(new DuplicateInfo(
                            moduleId,
                            jarFile.getName(),
                            desc.getApiVersion(),
                            existing.descriptor.getApiVersion(),
                            existing.file.getName()));
                }
            }
        }

        File[] filtered = new File[bestByModule.size() + unparseable.size()];
        int i = 0;
        for (DescriptorEntry entry : bestByModule.values()) {
            filtered[i++] = entry.file;
        }
        for (File f : unparseable) {
            filtered[i++] = f;
        }

        return new DedupResult(filtered, skipped);
    }

    /**
     * Compare two api-version strings using semantic versioning rules.
     *
     * @return negative if v1 &lt; v2, zero if equal, positive if v1 &gt; v2
     */
    private int compareApiVersions(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        int maxLen = Math.max(p1.length, p2.length);
        for (int i = 0; i < maxLen; i++) {
            int n1 = i < p1.length ? parsePart(p1[i]) : 0;
            int n2 = i < p2.length ? parsePart(p2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Holds a JAR file and its parsed descriptor. */
    private static class DescriptorEntry {
        final File file;
        final ModuleDescriptor descriptor;
        DescriptorEntry(File file, ModuleDescriptor descriptor) {
            this.file = file;
            this.descriptor = descriptor;
        }
    }

    /** Info about a skipped duplicate module JAR. */
    private static class DuplicateInfo {
        final String moduleId;
        final String skippedFileName;
        final String skippedApiVersion;
        final String keptApiVersion;
        final String keptFileName;

        DuplicateInfo(String moduleId, String skippedFileName, String skippedApiVersion,
                      String keptApiVersion, String keptFileName) {
            this.moduleId = moduleId;
            this.skippedFileName = skippedFileName;
            this.skippedApiVersion = skippedApiVersion;
            this.keptApiVersion = keptApiVersion;
            this.keptFileName = keptFileName;
        }
    }

    /** Result of deduplication. */
    private static class DedupResult {
        final File[] filteredJars;
        final List<DuplicateInfo> skippedDuplicates;
        DedupResult(File[] filteredJars, List<DuplicateInfo> skippedDuplicates) {
            this.filteredJars = filteredJars;
            this.skippedDuplicates = skippedDuplicates;
        }
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

        ConsoleLogger.info(lang.getIndexedMessage("module.system.loading", "", moduleId));

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

        ConsoleLogger.info(lang.getIndexedMessage("module.system.loaded-successfully", "",
                descriptor.getName(), descriptor.getVersion(), descriptor.getAuthor()));

        return module;
    }

    // ==================== 热卸载 ====================

    /**
     * 热卸载指定模块（同步方法，保证原子性）
     */
    public synchronized boolean unloadModule(String moduleId) {
        GuildModule module = registry.getModule(moduleId);
        if (module == null) {
            ConsoleLogger.warn(lang.getIndexedMessage("module.error.not-loaded", "", moduleId));
            return false;
        }

        // 检查依赖者
        try {
            checkDependents(moduleId);
        } catch (ModuleConflictException e) {
            logger.warning(ColorUtils.stripColor(e.getMessage()));
            return false;
        }

        ConsoleLogger.info(lang.getIndexedMessage("module.system.unloading", "", moduleId));
        registry.setState(moduleId, ModuleState.DISABLING);

        try {
            registry.unregister(moduleId);
            sharedApi.clearModuleHandlers(module);
            module.onDisable();
            loader.unloadClassloader(moduleId);

            ConsoleLogger.info(lang.getIndexedMessage("module.system.unloaded-successfully", "", moduleId));
            return true;

        } catch (Exception e) {
            registry.setState(moduleId, ModuleState.ERROR);
            ConsoleLogger.severe(lang.getIndexedMessage("module.error.unload-failed", "", moduleId, e.getMessage()));
            logger.log(Level.SEVERE, ColorUtils.stripColor(e.getMessage()), e);
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
            ConsoleLogger.warn(lang.getIndexedMessage("module.error.file-not-found", "", moduleId));
            return false;
        }

        ConsoleLogger.info(lang.getIndexedMessage("module.system.reloading", "", moduleId));

        boolean unloaded = unloadModule(moduleId);
        if (!unloaded) {
            return false;
        }

        try {
            loadModule(moduleFile);
            ConsoleLogger.info(lang.getIndexedMessage("module.system.reloaded-successfully", "", moduleId));
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
                        ColorUtils.stripColor(lang.getIndexedMessage("module.error.unload-failed-during-shutdown",
                                "", moduleId)), e);
            }
        }

        loader.unloadAll();
        sharedApi.clearAll();
        ConsoleLogger.info(lang.getMessage("module.system.all-unloaded", ""));
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
                ConsoleLogger.warn(lang.getIndexedMessage("module.warning.soft-depend-missing",
                        "", softDep, descriptor.getId()));
            }
        }
    }

    private void checkCompatibility(ModuleDescriptor descriptor) throws ModuleLoadException {
        String requiredApi = descriptor.getApiVersion();
        if (requiredApi == null || requiredApi.trim().isEmpty()) {
            ConsoleLogger.warn(lang.getIndexedMessage("module.warning.no-api-version",
                    "", descriptor.getId()));
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
            ConsoleLogger.severe(e.getMessage());
            logger.severe("  Missing dependencies: " + mde.getMissingDependencies());
        } else {
            ConsoleLogger.severe(e.getMessage());
            logger.log(Level.SEVERE,
                    ColorUtils.stripColor(e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                    e.getCause());
        }
    }
}
