package com.guild.services;

import com.guild.models.Guild;
import com.guild.models.GuildApplication;
import com.guild.models.GuildInvitation;
import com.guild.models.GuildMember;
import com.guild.models.GuildRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class GuildQueryService extends GuildServiceSupport {

    GuildQueryService(GuildServiceContext ctx) {
        super(ctx);
    }

    /**
     * 获取玩家公会 (异步)
     */
    public CompletableFuture<Guild> getPlayerGuildAsync(UUID playerUuid) {
        return ctx.repos.guilds().findByPlayerUuidAsync(playerUuid);
    }
    
    /**
     * 获取玩家公会 (同步包装器；优先短 TTL 缓存)
     */
    public Guild getPlayerGuild(UUID playerUuid) {
        try {
            var cache = ctx.plugin.getGuildPlayerDataCache();
            if (cache != null) {
                var hit = cache.getIfPresent(playerUuid);
                if (hit != null) {
                    return hit.guild;
                }
                // Load-through populates guild+member in one round-trip pair.
                return cache.getGuild(playerUuid);
            }
            return getPlayerGuildAsync(playerUuid).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching player guild: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取公会成员 (异步)
     */
    public CompletableFuture<GuildMember> getGuildMemberAsync(UUID playerUuid) {
        return ctx.repos.members().findByPlayerUuidAsync(playerUuid);
    }
    
    /**
     * 获取公会成员 (同步包装器；优先短 TTL 缓存)
     */
    public GuildMember getGuildMember(UUID playerUuid) {
        try {
            var cache = ctx.plugin.getGuildPlayerDataCache();
            if (cache != null) {
                var hit = cache.getIfPresent(playerUuid);
                if (hit != null) {
                    return hit.member;
                }
                return cache.getMember(playerUuid);
            }
            return getGuildMemberAsync(playerUuid).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching guild members: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取公会成员数量 (异步)
     */
    public CompletableFuture<Integer> getGuildMemberCountAsync(int guildId) {
        return ctx.repos.members().countByGuildIdAsync(guildId);
    }
    
    /**
     * 获取公会成员数量 (同步包装器)
     */
    public int getGuildMemberCount(int guildId) {
        try {
            return getGuildMemberCountAsync(guildId).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching guild member count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取公会所有成员 (异步)
     */
    public CompletableFuture<List<GuildMember>> getGuildMembersAsync(int guildId) {
        return ctx.repos.members().findAllByGuildIdAsync(guildId);
    }
    
    /**
     * 获取公会所有成员 (同步包装器)
     */
    public List<GuildMember> getGuildMembers(int guildId) {
        try {
            return getGuildMembersAsync(guildId).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching guild member list: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据ID获取公会 (异步)
     */
    public CompletableFuture<Guild> getGuildByIdAsync(int guildId) {
        return ctx.repos.guilds().findByIdAsync(guildId);
    }
    
    /**
     * 根据ID获取公会 (同步包装器)
     */
    public Guild getGuildById(int guildId) {
        try {
            return getGuildByIdAsync(guildId).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching guild by ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据名称获取公会 (异步)
     */
    public CompletableFuture<Guild> getGuildByNameAsync(String name) {
        return ctx.repos.guilds().findByNameAsync(name);
    }
    
    /**
     * 根据名称获取公会 (同步包装器)
     */
    public Guild getGuildByName(String name) {
        try {
            return getGuildByNameAsync(name).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching guild by name: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据标签获取公会 (异步)
     */
    public CompletableFuture<Guild> getGuildByTagAsync(String tag) {
        return ctx.repos.guilds().findByTagAsync(tag);
    }
    
    /**
     * 根据标签获取公会 (同步包装器)
     */
    public Guild getGuildByTag(String tag) {
        try {
            return getGuildByTagAsync(tag).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching guild by tag: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取所有公会 (异步)
     */
    public CompletableFuture<List<Guild>> getAllGuildsAsync() {
        return ctx.repos.guilds().findAllAsync();
    }
    
    /**
     * 获取所有公会 (同步包装器)
     */
    public List<Guild> getAllGuilds() {
        try {
            return getAllGuildsAsync().get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching all guilds: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查是否为公会会长
     */
    public boolean isGuildLeader(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getRole() == GuildMember.Role.LEADER;
    }
    
    /**
     * 检查是否为指定公会的会长
     */
    public boolean isGuildLeader(UUID playerUuid, int guildId) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getGuildId() == guildId && member.getRole() == GuildMember.Role.LEADER;
    }
    
    /**
     * 检查是否为公会官员
     */
    public boolean isGuildOfficer(UUID playerUuid) {
        GuildMember member = getGuildMember(playerUuid);
        return member != null && member.getRole() == GuildMember.Role.OFFICER;
    }
    
    /**
     * 检查是否有公会管理权限（官员级：config 中 can-invite 或 can-kick）
     */
    public boolean hasGuildPermission(UUID playerUuid) {
        return ctx.plugin.getMembershipRules().canManageGuild(playerUuid);
    }
    /**
     * 检查是否有待处理的申请 (异步)
     */
    public CompletableFuture<Boolean> hasPendingApplicationAsync(UUID playerUuid, int guildId) {
        return ctx.repos.applications().hasPendingAsync(playerUuid, guildId);
    }
    
    /**
     * 检查是否有待处理的申请 (同步包装器)
     */
    public boolean hasPendingApplication(UUID playerUuid, int guildId) {
        try {
            return hasPendingApplicationAsync(playerUuid, guildId).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception checking pending applications: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取公会申请列表 (异步)
     */
    public CompletableFuture<List<GuildApplication>> getGuildApplicationsAsync(int guildId) {
        return ctx.repos.applications().findAllByGuildIdAsync(guildId);
    }
    
    /**
     * 获取公会申请列表 (同步包装器)
     */
    public List<GuildApplication> getGuildApplications(int guildId) {
        try {
            return getGuildApplicationsAsync(guildId).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching guild application list: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取玩家申请列表 (异步)
     */
    public CompletableFuture<List<GuildApplication>> getPlayerApplicationsAsync(UUID playerUuid) {
        return ctx.repos.applications().findAllByPlayerUuidAsync(playerUuid);
    }
    
    /**
     * 获取玩家申请列表 (同步包装器)
     */
    public List<GuildApplication> getPlayerApplications(UUID playerUuid) {
        try {
            return getPlayerApplicationsAsync(playerUuid).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching player application list: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据ID获取申请 (异步)
     */
    public CompletableFuture<GuildApplication> getApplicationByIdAsync(int applicationId) {
        return ctx.repos.applications().findByIdAsync(applicationId);
    }
    
    /**
     * 根据ID获取申请 (同步包装器)
     */
    public GuildApplication getApplicationById(int applicationId) {
        try {
            return getApplicationByIdAsync(applicationId).get();
        } catch (Exception e) {
            ctx.logger.severe("Exception fetching application by ID: " + e.getMessage());
            return null;
        }
    }
     /**
      * 获取待处理邀请 (异步)
      */
     public CompletableFuture<GuildInvitation> getPendingInvitationAsync(UUID targetUuid, UUID inviterUuid) {
         return ctx.repos.invitations().findPendingByTargetAndInviterAsync(targetUuid, inviterUuid, nowString());
     }
     
     /**
      * 获取待处理邀请 (同步包装器)
      */
     public GuildInvitation getPendingInvitation(UUID targetUuid, UUID inviterUuid) {
         try {
             return getPendingInvitationAsync(targetUuid, inviterUuid).get();
         } catch (Exception e) {
             ctx.logger.severe("Exception fetching invitation: " + e.getMessage());
             return null;
         }
     }
     
     /**
      * 获取玩家的待处理邀请 (异步)
      */
    public CompletableFuture<GuildInvitation> getPendingInvitationAsync(UUID targetUuid, int guildId) {
        return ctx.repos.invitations().findPendingByTargetAndGuildAsync(targetUuid, guildId, nowString());
    }
     
     /**
      * 获取玩家的待处理邀请 (同步包装器)
      */
     public GuildInvitation getPendingInvitation(UUID targetUuid, int guildId) {
         try {
             return getPendingInvitationAsync(targetUuid, guildId).get();
         } catch (Exception e) {
             ctx.logger.severe("Exception fetching invitation: " + e.getMessage());
             return null;
         }
     }
     
     /**
      * 获取待处理申请 (异步)
      */
     public CompletableFuture<List<GuildApplication>> getPendingApplicationsAsync(int guildId) {
         return ctx.repos.applications().findPendingByGuildIdAsync(guildId);
     }
     
     /**
      * 获取申请历史 (异步)
      */
     public CompletableFuture<List<GuildApplication>> getApplicationHistoryAsync(int guildId) {
         return ctx.repos.applications().findHistoryByGuildIdAsync(guildId);
     }
    
    /**
     * 获取玩家所有待处理的邀请 (异步)
     */
    public CompletableFuture<List<GuildInvitation>> getPendingInvitationsAsync(UUID playerUuid) {
        return ctx.repos.invitations().findAllPendingByPlayerAsync(playerUuid, nowString());
    }

    /**
     * 获取公会关系 (异步)
     */
    public CompletableFuture<GuildRelation> getGuildRelationAsync(int guild1Id, int guild2Id) {
        return ctx.repos.relations().findByGuildPairAsync(guild1Id, guild2Id);
    }

    /**
     * 获取公会的所有关系 (异步)
     */
    public CompletableFuture<List<GuildRelation>> getGuildRelationsAsync(int guildId) {
        return ctx.repos.relations().findAllByGuildIdAsync(guildId);
    }

    /**
     * 按 ID 获取公会关系 (异步)
     */
    public CompletableFuture<GuildRelation> getGuildRelationByIdAsync(int relationId) {
        return ctx.repos.relations().findByIdAsync(relationId);
    }

    /**
     * 获取公会成员 (异步) - 重载方法，接受guildId参数
     */
    public CompletableFuture<GuildMember> getGuildMemberAsync(int guildId, UUID playerUuid) {
         return ctx.repos.members().findByGuildAndPlayerUuidAsync(guildId, playerUuid);
     }

}
