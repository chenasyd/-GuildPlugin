package com.guild.sdk.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 公会成员写操作域 API（跳过权限检查的直接管理路径）。
 *
 * @since 1.6.7
 */
public interface GuildMemberAPI {

    CompletableFuture<Boolean> addMember(int guildId, UUID playerUuid, String playerName, String role);

    CompletableFuture<Boolean> removeMember(int guildId, UUID playerUuid);

    CompletableFuture<Boolean> setMemberRole(int guildId, UUID playerUuid, String role);
}
