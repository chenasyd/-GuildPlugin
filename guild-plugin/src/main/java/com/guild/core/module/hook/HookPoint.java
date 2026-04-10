package com.guild.core.module.hook;

/**
 * 扩展点标记接口
 * <p>
 * 所有扩展点（Hook）都实现此接口。扩展点是模块与核心系统交互的桥梁，
 * 模块通过扩展点注册自己的功能注入（如 GUI 按钮、命令处理器等）。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>每个 Hook 负责一种扩展类型的注册和分发</li>
 *   <li>Hook 必须支持按模块 ID 批量清理（用于模块卸载时）</li>
 *   <li>Hook 内部使用线程安全的数据结构</li>
 * </ul>
 *
 * @see GUIExtensionHook
 */
public interface HookPoint {

    /**
     * 清理指定模块的所有注册项
     * 在模块卸载时由 ModuleRegistry 自动调用
     *
     * @param moduleId 要清理的模块ID
     */
    void unregisterByModule(String moduleId);

    /**
     * 清理所有注册项（插件关闭时调用）
     */
    void unregisterAll();
}
