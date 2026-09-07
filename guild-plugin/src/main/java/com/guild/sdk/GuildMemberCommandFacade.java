package com.guild.sdk;

import com.guild.GuildPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SDK 成员写操作（跳过权限检查的直接管理路径）。
 */
public final class GuildMemberCommandFacade {

    private final GuildPlugin plugin;

    public GuildMemberCommandFacade(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Boolean> addMember(int guildId, UUID playerUuid, String playerName, String role) {
        try {
            com.guild.models.GuildMember.Role r = com.guild.models.GuildMember.Role.valueOf(role.toUpperCase());
            return plugin.getGuildService().addGuildMemberAsync(guildId, playerUuid, playerName, r);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Boolean> removeMember(int guildId, UUID playerUuid) {
        return plugin.getGuildService().removeGuildMemberDirectAsync(guildId, playerUuid);
    }

    public CompletableFuture<Boolean> setMemberRole(int guildId, UUID playerUuid, String role) {
        return plugin.getGuildService().updateMemberRoleDirectAsync(guildId, playerUuid, role);
    }
}
