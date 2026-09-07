package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;

import java.time.LocalDateTime;
import java.util.logging.Level;

/**
 * 核心模型 → SDK DTO 映射。
 */
public final class GuildDataMapper {

    private final GuildPlugin plugin;

    public GuildDataMapper(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public GuildData convertGuild(com.guild.models.Guild guild) {
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

    public MemberData convertMember(com.guild.models.GuildMember member) {
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
