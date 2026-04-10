package com.guild.sdk.event;

/**
 * 工会创建/删除事件处理器
 * <p>
 * 使用示例：
 * <pre>{@code
 * ctx.getApi().onGuildCreate(data -> {
 *     logger.info("new guild: " + data.getGuildName());
 * });
 * }</pre>
 */
@FunctionalInterface
public interface GuildEventHandler {

    /** 处理工会事件 */
    void onEvent(GuildEventData data);

    /**
     * 获取注册此处理器的模块实例（用于模块卸载时自动清理）
     * 默认返回 null，表示无法追踪
     */
    default Object getModuleInstance() {
        return null;
    }
}
