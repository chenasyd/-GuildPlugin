package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildApplication;
import com.guild.models.GuildInvitation;
import com.guild.models.GuildRelation;
import com.guild.models.GuildEconomy;
import com.guild.models.GuildContribution;
import com.guild.models.GuildLog;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 公会业务门面：编排鉴权、事件、日志与通知，数据访问委托各域服务。
 */
public class GuildService extends GuildServiceSupport {

    public GuildService(GuildPlugin plugin) {
        super(new GuildServiceContext(plugin));
        ctx.queries = new GuildQueryService(ctx);
        ctx.logs = new GuildLogService(ctx);
        ctx.relations = new GuildRelationService(ctx);
        ctx.home = new GuildHomeService(ctx);
        ctx.economy = new GuildEconomyService(ctx);
        ctx.members = new GuildMemberService(ctx);
        ctx.lifecycle = new GuildLifecycleService(ctx);
        ctx.serviceRef = this;
    }

    // ==================== 生命周期 ====================

    public CompletableFuture<Boolean> createGuildAsync(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        return ctx.lifecycle.createGuildAsync(name, tag, description, leaderUuid, leaderName);
    }

    public boolean createGuild(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        return ctx.lifecycle.createGuild(name, tag, description, leaderUuid, leaderName);
    }

    public CompletableFuture<Boolean> deleteGuildAsync(int guildId, UUID requesterUuid) {
        return ctx.lifecycle.deleteGuildAsync(guildId, requesterUuid);
    }

    public boolean deleteGuild(int guildId, UUID requesterUuid) {
        return ctx.lifecycle.deleteGuild(guildId, requesterUuid);
    }

    public CompletableFuture<Boolean> forceDeleteGuildAsync(int guildId, UUID adminUuid) {
        return ctx.lifecycle.forceDeleteGuildAsync(guildId, adminUuid);
    }

    public CompletableFuture<Boolean> updateGuildAsync(int guildId, String name, String tag, String description, UUID requesterUuid) {
        return ctx.lifecycle.updateGuildAsync(guildId, name, tag, description, requesterUuid);
    }

    public boolean updateGuild(int guildId, String name, String tag, String description, UUID requesterUuid) {
        return ctx.lifecycle.updateGuild(guildId, name, tag, description, requesterUuid);
    }

    public CompletableFuture<Boolean> updateGuildDescriptionAsync(int guildId, String description) {
        return ctx.lifecycle.updateGuildDescriptionAsync(guildId, description);
    }

    // ==================== 成员 ====================

    public CompletableFuture<Boolean> addGuildMemberAsync(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        return ctx.members.addGuildMemberAsync(guildId, playerUuid, playerName, role);
    }

    public boolean addGuildMember(int guildId, UUID playerUuid, String playerName, GuildMember.Role role) {
        return ctx.members.addGuildMember(guildId, playerUuid, playerName, role);
    }

    public CompletableFuture<Boolean> removeGuildMemberAsync(UUID playerUuid, UUID requesterUuid) {
        return ctx.members.removeGuildMemberAsync(playerUuid, requesterUuid);
    }

    public boolean removeGuildMember(UUID playerUuid, UUID requesterUuid) {
        return ctx.members.removeGuildMember(playerUuid, requesterUuid);
    }

    public CompletableFuture<Boolean> updateMemberRoleAsync(UUID playerUuid, GuildMember.Role newRole, UUID requesterUuid) {
        return ctx.members.updateMemberRoleAsync(playerUuid, newRole, requesterUuid);
    }

    public boolean updateMemberRole(UUID playerUuid, GuildMember.Role newRole, UUID requesterUuid) {
        return ctx.members.updateMemberRole(playerUuid, newRole, requesterUuid);
    }

    public CompletableFuture<Boolean> removeGuildMemberDirectAsync(int guildId, UUID playerUuid) {
        return ctx.members.removeGuildMemberDirectAsync(guildId, playerUuid);
    }

    public CompletableFuture<Boolean> updateMemberRoleDirectAsync(int guildId, UUID playerUuid, String roleName) {
        return ctx.members.updateMemberRoleDirectAsync(guildId, playerUuid, roleName);
    }

    public CompletableFuture<Boolean> transferGuildLeadershipAsync(int guildId, UUID newLeaderUuid, String newLeaderName) {
        return ctx.members.transferGuildLeadershipAsync(guildId, newLeaderUuid, newLeaderName);
    }

    public CompletableFuture<Boolean> transferGuildLeadershipAsync(int guildId, UUID newLeaderUuid, String newLeaderName,
                                                                   UUID requesterUuid) {
        return ctx.members.transferGuildLeadershipAsync(guildId, newLeaderUuid, newLeaderName, requesterUuid);
    }

    public CompletableFuture<Boolean> submitApplicationAsync(int guildId, UUID playerUuid, String playerName, String message) {
        return ctx.members.submitApplicationAsync(guildId, playerUuid, playerName, message);
    }

    public boolean submitApplication(int guildId, UUID playerUuid, String playerName, String message) {
        return ctx.members.submitApplication(guildId, playerUuid, playerName, message);
    }

    public CompletableFuture<Boolean> processApplicationAsync(int applicationId, GuildApplication.ApplicationStatus status, UUID processorUuid) {
        return ctx.members.processApplicationAsync(applicationId, status, processorUuid);
    }

    public boolean processApplication(int applicationId, GuildApplication.ApplicationStatus status, UUID processorUuid) {
        return ctx.members.processApplication(applicationId, status, processorUuid);
    }

    public CompletableFuture<Boolean> sendInvitationAsync(int guildId, UUID inviterUuid, String inviterName, UUID targetUuid, String targetName) {
        return ctx.members.sendInvitationAsync(guildId, inviterUuid, inviterName, targetUuid, targetName);
    }

    public boolean sendInvitation(int guildId, UUID inviterUuid, String inviterName, UUID targetUuid, String targetName) {
        return ctx.members.sendInvitation(guildId, inviterUuid, inviterName, targetUuid, targetName);
    }

    public CompletableFuture<Boolean> processInvitationAsync(UUID targetUuid, UUID inviterUuid, boolean accept) {
        return ctx.members.processInvitationAsync(targetUuid, inviterUuid, accept);
    }

    public CompletableFuture<Boolean> processInvitationDirectAsync(GuildInvitation invitation, boolean accept) {
        return ctx.members.processInvitationDirectAsync(invitation, accept);
    }

    public boolean processInvitation(UUID targetUuid, UUID inviterUuid, boolean accept) {
        return ctx.members.processInvitation(targetUuid, inviterUuid, accept);
    }

    public CompletableFuture<Integer> cleanupExpiredInvitationsAsync() {
        return ctx.members.cleanupExpiredInvitationsAsync();
    }

    public CompletableFuture<Integer> cleanupOldProcessedInvitationsAsync(int days) {
        return ctx.members.cleanupOldProcessedInvitationsAsync(days);
    }

    // ==================== 查询 ====================

    public CompletableFuture<Guild> getPlayerGuildAsync(UUID playerUuid) {
        return ctx.queries.getPlayerGuildAsync(playerUuid);
    }

    public Guild getPlayerGuild(UUID playerUuid) {
        return ctx.queries.getPlayerGuild(playerUuid);
    }

    public CompletableFuture<GuildMember> getGuildMemberAsync(UUID playerUuid) {
        return ctx.queries.getGuildMemberAsync(playerUuid);
    }

    public GuildMember getGuildMember(UUID playerUuid) {
        return ctx.queries.getGuildMember(playerUuid);
    }

    public CompletableFuture<Integer> getGuildMemberCountAsync(int guildId) {
        return ctx.queries.getGuildMemberCountAsync(guildId);
    }

    public int getGuildMemberCount(int guildId) {
        return ctx.queries.getGuildMemberCount(guildId);
    }

    public CompletableFuture<List<GuildMember>> getGuildMembersAsync(int guildId) {
        return ctx.queries.getGuildMembersAsync(guildId);
    }

    public List<GuildMember> getGuildMembers(int guildId) {
        return ctx.queries.getGuildMembers(guildId);
    }

    public CompletableFuture<Guild> getGuildByIdAsync(int guildId) {
        return ctx.queries.getGuildByIdAsync(guildId);
    }

    public Guild getGuildById(int guildId) {
        return ctx.queries.getGuildById(guildId);
    }

    public CompletableFuture<Guild> getGuildByNameAsync(String name) {
        return ctx.queries.getGuildByNameAsync(name);
    }

    public Guild getGuildByName(String name) {
        return ctx.queries.getGuildByName(name);
    }

    public CompletableFuture<Guild> getGuildByTagAsync(String tag) {
        return ctx.queries.getGuildByTagAsync(tag);
    }

    public Guild getGuildByTag(String tag) {
        return ctx.queries.getGuildByTag(tag);
    }

    public CompletableFuture<List<Guild>> getAllGuildsAsync() {
        return ctx.queries.getAllGuildsAsync();
    }

    public List<Guild> getAllGuilds() {
        return ctx.queries.getAllGuilds();
    }

    public boolean isGuildLeader(UUID playerUuid) {
        return ctx.queries.isGuildLeader(playerUuid);
    }

    public boolean isGuildLeader(UUID playerUuid, int guildId) {
        return ctx.queries.isGuildLeader(playerUuid, guildId);
    }

    public boolean isGuildOfficer(UUID playerUuid) {
        return ctx.queries.isGuildOfficer(playerUuid);
    }

    public boolean hasGuildPermission(UUID playerUuid) {
        return ctx.queries.hasGuildPermission(playerUuid);
    }

    public CompletableFuture<Boolean> hasPendingApplicationAsync(UUID playerUuid, int guildId) {
        return ctx.queries.hasPendingApplicationAsync(playerUuid, guildId);
    }

    public boolean hasPendingApplication(UUID playerUuid, int guildId) {
        return ctx.queries.hasPendingApplication(playerUuid, guildId);
    }

    public CompletableFuture<List<GuildApplication>> getGuildApplicationsAsync(int guildId) {
        return ctx.queries.getGuildApplicationsAsync(guildId);
    }

    public List<GuildApplication> getGuildApplications(int guildId) {
        return ctx.queries.getGuildApplications(guildId);
    }

    public CompletableFuture<List<GuildApplication>> getPlayerApplicationsAsync(UUID playerUuid) {
        return ctx.queries.getPlayerApplicationsAsync(playerUuid);
    }

    public List<GuildApplication> getPlayerApplications(UUID playerUuid) {
        return ctx.queries.getPlayerApplications(playerUuid);
    }

    public CompletableFuture<GuildApplication> getApplicationByIdAsync(int applicationId) {
        return ctx.queries.getApplicationByIdAsync(applicationId);
    }

    public GuildApplication getApplicationById(int applicationId) {
        return ctx.queries.getApplicationById(applicationId);
    }

    public CompletableFuture<GuildInvitation> getPendingInvitationAsync(UUID targetUuid, UUID inviterUuid) {
        return ctx.queries.getPendingInvitationAsync(targetUuid, inviterUuid);
    }

    public GuildInvitation getPendingInvitation(UUID targetUuid, UUID inviterUuid) {
        return ctx.queries.getPendingInvitation(targetUuid, inviterUuid);
    }

    public CompletableFuture<GuildInvitation> getPendingInvitationAsync(UUID targetUuid, int guildId) {
        return ctx.queries.getPendingInvitationAsync(targetUuid, guildId);
    }

    public GuildInvitation getPendingInvitation(UUID targetUuid, int guildId) {
        return ctx.queries.getPendingInvitation(targetUuid, guildId);
    }

    public CompletableFuture<List<GuildApplication>> getPendingApplicationsAsync(int guildId) {
        return ctx.queries.getPendingApplicationsAsync(guildId);
    }

    public CompletableFuture<List<GuildApplication>> getApplicationHistoryAsync(int guildId) {
        return ctx.queries.getApplicationHistoryAsync(guildId);
    }

    public CompletableFuture<List<GuildInvitation>> getPendingInvitationsAsync(UUID playerUuid) {
        return ctx.queries.getPendingInvitationsAsync(playerUuid);
    }

    public CompletableFuture<GuildMember> getGuildMemberAsync(int guildId, UUID playerUuid) {
        return ctx.queries.getGuildMemberAsync(guildId, playerUuid);
    }

    public CompletableFuture<GuildRelation> getGuildRelationAsync(int guild1Id, int guild2Id) {
        return ctx.queries.getGuildRelationAsync(guild1Id, guild2Id);
    }

    public CompletableFuture<List<GuildRelation>> getGuildRelationsAsync(int guildId) {
        return ctx.queries.getGuildRelationsAsync(guildId);
    }

    public CompletableFuture<GuildRelation> getGuildRelationByIdAsync(int relationId) {
        return ctx.queries.getGuildRelationByIdAsync(relationId);
    }

    // ==================== 公会家 ====================

    public CompletableFuture<Boolean> setGuildHomeAsync(int guildId, Location location, UUID requesterUuid) {
        return ctx.home.setGuildHomeAsync(guildId, location, requesterUuid);
    }

    public boolean setGuildHome(int guildId, Location location, UUID requesterUuid) {
        return ctx.home.setGuildHome(guildId, location, requesterUuid);
    }

    public CompletableFuture<Location> getGuildHomeAsync(int guildId) {
        return ctx.home.getGuildHomeAsync(guildId);
    }

    public Location getGuildHome(int guildId) {
        return ctx.home.getGuildHome(guildId);
    }

    // ==================== 关系 ====================

    public CompletableFuture<Boolean> createGuildRelationAsync(int guild1Id, int guild2Id, String guild1Name, String guild2Name,
                                                              GuildRelation.RelationType type, UUID initiatorUuid, String initiatorName) {
        return ctx.relations.createGuildRelationAsync(guild1Id, guild2Id, guild1Name, guild2Name, type, initiatorUuid, initiatorName);
    }

    public CompletableFuture<Boolean> updateGuildRelationStatusAsync(int relationId, GuildRelation.RelationStatus status) {
        return ctx.relations.updateGuildRelationStatusAsync(relationId, status);
    }

    public CompletableFuture<Boolean> deleteGuildRelationAsync(int relationId) {
        return ctx.relations.deleteGuildRelationAsync(relationId);
    }

    // ==================== 经济 ====================

    public CompletableFuture<Boolean> initializeGuildEconomyAsync(int guildId) {
        return ctx.economy.initializeGuildEconomyAsync(guildId);
    }

    public CompletableFuture<GuildEconomy> getGuildEconomyAsync(int guildId) {
        return ctx.economy.getGuildEconomyAsync(guildId);
    }

    public CompletableFuture<Boolean> updateGuildEconomyAsync(int guildId, double balance, int level, double experience, double maxExperience, int maxMembers) {
        return ctx.economy.updateGuildEconomyAsync(guildId, balance, level, experience, maxExperience, maxMembers);
    }

    public CompletableFuture<Boolean> addGuildContributionAsync(int guildId, UUID playerUuid, String playerName,
                                                                  double amount, GuildContribution.ContributionType type, String description) {
        return ctx.economy.addGuildContributionAsync(guildId, playerUuid, playerName, amount, type, description);
    }

    public CompletableFuture<List<GuildContribution>> getGuildContributionsAsync(int guildId) {
        return ctx.economy.getGuildContributionsAsync(guildId);
    }

    public CompletableFuture<List<GuildContribution>> getPlayerContributionsAsync(UUID playerUuid) {
        return ctx.economy.getPlayerContributionsAsync(playerUuid);
    }

    public CompletableFuture<Map<UUID, Double>> getGuildContributionNetByPlayerAsync(int guildId) {
        return ctx.economy.getGuildContributionNetByPlayerAsync(guildId);
    }

    public CompletableFuture<List<GuildContribution>> getGuildContributionTotalsAsync(int guildId) {
        return ctx.economy.getGuildContributionTotalsAsync(guildId);
    }

    public CompletableFuture<Boolean> updateGuildBalanceAsync(int guildId, double balance) {
        return ctx.economy.updateGuildBalanceAsync(guildId, balance);
    }

    public CompletableFuture<Boolean> updateGuildBalanceByAdminAsync(int guildId, double balance,
                                                                      UUID adminUuid, String adminName) {
        return ctx.economy.updateGuildBalanceByAdminAsync(guildId, balance, adminUuid, adminName);
    }

    public CompletableFuture<Boolean> updateGuildBalanceAsync(int guildId, double balance,
                                                               String operatorUuid, String operatorName) {
        return ctx.economy.updateGuildBalanceAsync(guildId, balance, operatorUuid, operatorName);
    }

    public CompletableFuture<Boolean> updateGuildLevelAsync(int guildId, int level) {
        return ctx.economy.updateGuildLevelAsync(guildId, level);
    }

    public CompletableFuture<Boolean> updateGuildMaxMembersAsync(int guildId, int maxMembers) {
        return ctx.economy.updateGuildMaxMembersAsync(guildId, maxMembers);
    }

    public CompletableFuture<Boolean> updateGuildFrozenStatusAsync(int guildId, boolean frozen) {
        return ctx.economy.updateGuildFrozenStatusAsync(guildId, frozen);
    }

    public CompletableFuture<Boolean> updateGuildFrozenStatusAsync(int guildId, boolean frozen, UUID operatorUuid) {
        return ctx.economy.updateGuildFrozenStatusAsync(guildId, frozen, operatorUuid);
    }

    public int getEffectiveMaxMembers(Guild guild) {
        return ctx.economy.getEffectiveMaxMembers(guild);
    }

    public CompletableFuture<Boolean> isGuildFullAsync(int guildId) {
        return ctx.economy.isGuildFullAsync(guildId);
    }

    // ==================== 日志 ====================

    public CompletableFuture<Boolean> logGuildActionAsync(int guildId, String guildName, String playerUuid,
                                                          String playerName, GuildLog.LogType logType,
                                                          String description, String details) {
        return ctx.logs.logGuildActionAsync(guildId, guildName, playerUuid, playerName, logType, description, details);
    }

    public boolean logGuildAction(int guildId, String guildName, String playerUuid, String playerName,
                                  GuildLog.LogType logType, String description, String details) {
        return ctx.logs.logGuildAction(guildId, guildName, playerUuid, playerName, logType, description, details);
    }

    public CompletableFuture<List<GuildLog>> getGuildLogsAsync(int guildId, int limit, int offset) {
        return ctx.logs.getGuildLogsAsync(guildId, limit, offset);
    }

    public List<GuildLog> getGuildLogs(int guildId, int limit, int offset) {
        return ctx.logs.getGuildLogs(guildId, limit, offset);
    }

    public CompletableFuture<Integer> getGuildLogsCountAsync(int guildId) {
        return ctx.logs.getGuildLogsCountAsync(guildId);
    }

    public int getGuildLogsCount(int guildId) {
        return ctx.logs.getGuildLogsCount(guildId);
    }

    public CompletableFuture<Integer> cleanOldLogsAsync(int daysToKeep) {
        return ctx.logs.cleanOldLogsAsync(daysToKeep);
    }

    public int cleanOldLogs(int daysToKeep) {
        return ctx.logs.cleanOldLogs(daysToKeep);
    }
}
