package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 公会/成员只读查询与 DTO 转换（供 {@link GuildPluginAPI} 委托）。
 */
public final class GuildQueryFacade {

    private final GuildPlugin plugin;

    public GuildQueryFacade(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<GuildData> getGuildById(int id) {
        return plugin.getGuildService().getGuildByIdAsync(id).thenApply(this::convertGuild);
    }

    public CompletableFuture<GuildData> getGuildByName(String name) {
        return plugin.getGuildService().getGuildByNameAsync(name).thenApply(this::convertGuild);
    }

    public CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid) {
        return plugin.getGuildService().getPlayerGuildAsync(playerUuid).thenApply(this::convertGuild);
    }

    public CompletableFuture<List<GuildData>> getAllGuilds() {
        return plugin.getGuildService().getAllGuildsAsync().thenApply(list ->
                list.stream().map(this::convertGuild).filter(g -> g != null).toList());
    }

    public CompletableFuture<List<MemberData>> getGuildMembers(int guildId) {
        return plugin.getGuildService().getGuildMembersAsync(guildId).thenApply(list ->
                list.stream().map(this::convertMember).toList());
    }

    public CompletableFuture<List<com.guild.sdk.data.ActivityScoreData>> getMemberActivityScores(int guildId) {
        var service = plugin.getActivityScoreService();
        if (service == null || !service.getSettings().isEnabled()) {
            return CompletableFuture.completedFuture(List.of());
        }
        return service.getGuildScoresAsync(guildId).thenApply(list -> {
            List<com.guild.sdk.data.ActivityScoreData> out = new ArrayList<>(list.size());
            for (com.guild.activity.MemberActivityScore s : list) {
                out.add(new com.guild.sdk.data.ActivityScoreData(
                        s.getPlayerUuid(), s.getPlayerName(),
                        s.getEconomyPts(), s.getActivityPts(), s.getTotalScore(),
                        s.getRank(), s.isOnline()));
            }
            return out;
        });
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

    GuildData convertGuild(com.guild.models.Guild guild) {
        if (guild == null) {
            return null;
        }
        long createTimeMillis = 0L;
        try {
            LocalDateTime createdAt = guild.getCreatedAt();
            if (createdAt != null) {
                createTimeMillis = createdAt.atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE,
                    "Could not convert guild createTime for guild " + guild.getId() + ": " + e.getMessage());
        }

        int memberCount;
        try {
            memberCount = plugin.getGuildService().getGuildMemberCount(guild.getId());
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE,
                    "Could not load member count for guild " + guild.getId() + ": " + e.getMessage());
            memberCount = 0;
        }

        return new GuildData(
                guild.getId(),
                guild.getName(),
                guild.getLeaderUuid(),
                guild.getLeaderName(),
                guild.getLevel(),
                0L,
                guild.getBalance(),
                memberCount,
                guild.getMaxMembers(),
                guild.getDescription(),
                createTimeMillis,
                null
        );
    }

    MemberData convertMember(com.guild.models.GuildMember member) {
        if (member == null) {
            return null;
        }
        long joinTimeMillis = 0L;
        try {
            LocalDateTime joinedAt = member.getJoinedAt();
            if (joinedAt != null) {
                joinTimeMillis = joinedAt.atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE,
                    "Could not convert member joinTime for " + member.getPlayerUuid() + ": " + e.getMessage());
        }

        double investedBalance = 0.0;
        try {
            com.guild.services.GuildInvestmentService invSvc =
                    plugin.getServiceContainer().get(com.guild.services.GuildInvestmentService.class);
            if (invSvc != null) {
                investedBalance = invSvc.getInvestedBalance(member.getGuildId(), member.getPlayerUuid());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE,
                    "Could not load invested balance for " + member.getPlayerUuid() + ": " + e.getMessage());
        }

        double contribution = 0.0;
        try {
            var cache = plugin.getGuildPlayerDataCache();
            if (cache != null) {
                var snap = cache.get(member.getPlayerUuid());
                if (snap.contributionNet != null) {
                    contribution = snap.contributionNet;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE,
                    "Could not load contribution cache for " + member.getPlayerUuid() + ": " + e.getMessage());
        }

        boolean online = org.bukkit.Bukkit.getPlayer(member.getPlayerUuid()) != null;

        return new MemberData(
                member.getPlayerUuid(),
                member.getPlayerName(),
                member.getRole().name(),
                joinTimeMillis,
                contribution,
                online,
                investedBalance
        );
    }
}
