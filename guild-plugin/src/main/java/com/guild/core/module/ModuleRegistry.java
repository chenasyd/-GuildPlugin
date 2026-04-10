package com.guild.core.module;

import com.guild.core.module.hook.GUIExtensionHook;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块注册表 - 管理所有已加载模块及其注册的扩展
 * <p>
 * 职责：
 * <ul>
 *   <li>维护模块 ID -> 实例 的映射关系</li>
 *   <li>提供按类型查找模块的能力</li>
 *   <li>管理各扩展点的注册中心</li>
 *   <li>支持模块卸载时的批量注销</li>
 * </ul>
 */
public class ModuleRegistry {

    /** 所有已加载的模块实例（moduleId -> module） */
    private final Map<String, GuildModule> modules = new ConcurrentHashMap<>();

    /** 模块状态跟踪（moduleId -> state） */
    private final Map<String, ModuleState> states = new ConcurrentHashMap<>();

    /** GUI 扩展点注册中心 */
    private final GUIExtensionHook guiHook = new GUIExtensionHook();

    /**
     * 注册一个已启用的模块
     */
    public void register(GuildModule module) {
        modules.put(module.getDescriptor().getId(), module);
        states.put(module.getDescriptor().getId(), ModuleState.ACTIVE);
    }

    /**
     * 注销模块（包括该模块的所有扩展注册）
     */
    public void unregister(String moduleId) {
        GuildModule removed = modules.remove(moduleId);
        if (removed != null) {
            states.remove(moduleId);

            // 清理该模块的所有 GUI 扩展注册
            guiHook.unregisterByModule(moduleId);
        }
    }

    /**
     * 获取模块实例
     */
    public GuildModule getModule(String moduleId) {
        return modules.get(moduleId);
    }

    /**
     * 检查模块是否已加载
     */
    public boolean isLoaded(String moduleId) {
        return modules.containsKey(moduleId);
    }

    /**
     * 获取所有已加载模块的 ID 集合
     */
    public Set<String> getModuleIds() {
        return Collections.unmodifiableSet(modules.keySet());
    }

    /**
     * 获取所有已加载模块的只读视图
     */
    public Collection<GuildModule> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    /**
     * 获取已加载模块数量
     */
    public int size() {
        return modules.size();
    }

    /**
     * 更新模块状态
     */
    public void setState(String moduleId, ModuleState state) {
        states.put(moduleId, state);
    }

    /**
     * 获取模块状态
     */
    public ModuleState getState(String moduleId) {
        return states.getOrDefault(moduleId, ModuleState.UNLOADED);
    }

    /**
     * 获取所有模块的状态快照
     */
    public Map<String, ModuleState> getAllStates() {
        return Collections.unmodifiableMap(states);
    }

    // ==================== 扩展点访问 ====================

    /**
     * 获取 GUI 扩展钩子（用于注册 GUI 按钮注入等）
     */
    public GUIExtensionHook getGuiExtensionHook() {
        return guiHook;
    }

    /**
     * 清空所有注册（插件关闭时调用）
     */
    public void clear() {
        modules.clear();
        states.clear();
        guiHook.unregisterAll();
    }
}
