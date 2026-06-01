package com.guild.sdk.event;

/**
 * 经济事件处理器 —— 模块监听存款/取款的回调接口。
 */
public interface EconomyEventHandler {
    void onEvent(EconomyEventData data);

    Object getModuleInstance();
}
