package com.guild.core.module;

/**
 * 工会插件扩展模块接口
 * <p>
 * 所有扩展模块必须实现此接口。模块的生命周期由 ModuleManager 管理：
 * <ol>
 *   <li>加载 jar 文件并解析 module.yml</li>
 *   <li>实例化模块主类（通过反射调用无参构造）</li>
 *   <li>调用 {@link #onEnable(ModuleContext)} 完成初始化</li>
 *   <li>运行中（可响应事件、处理请求等）</li>
 *   <li>调用 {@link #onDisable()} 进行清理</li>
 *   <li>卸载 ClassLoader，释放资源</li>
 * </ol>
 *
 * <h3>开发规范：</h3>
 * <ul>
 *   <li>在 {@code onEnable} 中完成所有注册工作（GUI、命令、事件等）</li>
 *   <li>在 {@code onDisable} 中清理所有资源，注销所有注册</li>
 *   <li>不要在构造函数中进行任何初始化操作</li>
 *   <li>所有面向用户的文本必须通过 {@link ModuleContext} 的消息API获取</li>
 * </ul>
 */
public interface GuildModule {

    /**
     * 获取模块描述符（由 ModuleLoader 在加载时自动填充）
     */
    ModuleDescriptor getDescriptor();

    /**
     * 设置模块描述符
     */
    void setDescriptor(ModuleDescriptor descriptor);

    /**
     * 模块启用 - 当模块被加载时调用
     * <p>
     * 在此方法中完成：
     * <ul>
     *   <li>注册 GUI 扩展按钮</li>
     *   <li>注册子命令处理器</li>
     *   <li>注册事件监听</li>
     *   <li>初始化外部 API 连接</li>
     *   <li>读取配置项</li>
     * </ul>
     *
     * @param context 模块上下文，提供 SDK 能力访问入口
     * @throws Exception 初始化失败时抛出，将导致模块进入 ERROR 状态
     */
    void onEnable(ModuleContext context) throws Exception;

    /**
     * 模块禁用 - 当模块被卸载时调用
     * <p>
     * 在此方法中完成：
     * <ul>
     *   <li>注销所有 GUI 注入</li>
     *   <li>注销所有命令处理器</li>
     *   <li>移除所有事件监听</li>
     *   <li>关闭外部连接</li>
     *   <li>释放其他资源</li>
     * </ul>
     * <p>
     * 注意：ModuleManager 会在调用此方法后自动执行额外的清理操作，
     * 但建议在此方法中也做好防御性清理。
     */
    void onDisable();

    /**
     * 获取模块当前状态
     */
    ModuleState getState();
}
