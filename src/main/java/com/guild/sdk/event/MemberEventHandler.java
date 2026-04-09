package com.guild.sdk.event;

/**
 * 成员加入/离开事件处理器
 * <p>
 * 使用示例：
 * <pre>{@code
 * ctx.getApi().onMemberJoin(data -> {
 *     logger.info("member joined: " + data.getPlayerName());
 * });
 * }</pre>
 */
@FunctionalInterface
public interface MemberEventHandler {

    /** 处理成员事件 */
    void onEvent(MemberEventData data);

    /**
     * 获取注册此处理器的模块实例（用于模块卸载时自动清理）
     * 默认返回 null，表示无法追踪
     */
    default Object getModuleInstance() {
        return null;
    }
}
