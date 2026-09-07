package com.guild.sdk.api;

import com.guild.sdk.event.EconomyEventHandler;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.event.MemberRoleChangeEventHandler;

/**
 * 模块 SDK 事件订阅域 API（不含 {@code fire*} 分发，供核心服务使用）。
 *
 * @since 1.6.7
 */
public interface ModuleEventAPI {

    void onGuildCreate(GuildEventHandler handler);

    void onGuildDelete(GuildEventHandler handler);

    void onMemberJoin(MemberEventHandler handler);

    void onMemberLeave(MemberEventHandler handler);

    void onEconomyDeposit(EconomyEventHandler handler);

    void onEconomyWithdraw(EconomyEventHandler handler);

    void onMemberRoleChange(MemberRoleChangeEventHandler handler);
}
