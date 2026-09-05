package com.guild.services;

import com.guild.models.GuildLog;
import com.guild.models.GuildRelation;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class GuildRelationService extends GuildServiceSupport {

    GuildRelationService(GuildServiceContext ctx) {
        super(ctx);
    }

     // ==================== 公会关系系统 ====================
     
     /**
      * 创建公会关系 (异步)
      */
     public CompletableFuture<Boolean> createGuildRelationAsync(int guild1Id, int guild2Id, String guild1Name, String guild2Name,
                                                              GuildRelation.RelationType type, UUID initiatorUuid, String initiatorName) {
         return guardServiceFutureBoolean("createGuildRelation", ctx.repos.relations().insertAsync(guild1Id, guild2Id, guild1Name, guild2Name, type,
                 initiatorUuid, initiatorName, plusDaysString(7)).thenCompose(ok -> {
             if (!Boolean.TRUE.equals(ok)) {
                 return CompletableFuture.completedFuture(false);
             }
             String details = type.name() + " <-> " + guild2Name;
             String detailsPeer = type.name() + " <-> " + guild1Name;
             CompletableFuture<Boolean> a = ctx.serviceRef.logGuildActionAsync(guild1Id, guild1Name,
                     initiatorUuid.toString(), initiatorName,
                     GuildLog.LogType.RELATION_CREATED, "Relation request created", details);
             CompletableFuture<Boolean> b = ctx.serviceRef.logGuildActionAsync(guild2Id, guild2Name,
                     initiatorUuid.toString(), initiatorName,
                     GuildLog.LogType.RELATION_CREATED, "Relation request received", detailsPeer);
             return a.thenCombine(b, (x, y) -> true);
         }));
     }
     
     /**
      * 更新公会关系状态 (异步)
      */
     public CompletableFuture<Boolean> updateGuildRelationStatusAsync(int relationId, GuildRelation.RelationStatus status) {
         return guardServiceFutureBoolean("updateGuildRelationStatus", ctx.serviceRef.getGuildRelationByIdAsync(relationId).thenCompose(relation -> {
             if (relation == null) {
                 return CompletableFuture.completedFuture(false);
             }
             return ctx.repos.relations().updateStatusAsync(relationId, status, nowString()).thenCompose(ok -> {
                 if (!Boolean.TRUE.equals(ok)) {
                     return CompletableFuture.completedFuture(false);
                 }
                 GuildLog.LogType logType = status == GuildRelation.RelationStatus.ACTIVE
                         ? GuildLog.LogType.RELATION_ACCEPTED
                         : GuildLog.LogType.RELATION_REJECTED;
                 String details = relation.getType().name() + " status=" + status.name()
                         + " peers=" + relation.getGuild1Name() + "/" + relation.getGuild2Name();
                 String actor = relation.getInitiatorUuid() != null
                         ? relation.getInitiatorUuid().toString() : "SYSTEM";
                 String actorName = relation.getInitiatorName() != null
                         ? relation.getInitiatorName() : "system";
                 CompletableFuture<Boolean> a = ctx.serviceRef.logGuildActionAsync(relation.getGuild1Id(), relation.getGuild1Name(),
                         actor, actorName, logType, "Relation status updated", details);
                 CompletableFuture<Boolean> b = ctx.serviceRef.logGuildActionAsync(relation.getGuild2Id(), relation.getGuild2Name(),
                         actor, actorName, logType, "Relation status updated", details);
                 return a.thenCombine(b, (x, y) -> true);
             });
         }));
     }
     
     /**
      * 删除公会关系 (异步)
      */
     public CompletableFuture<Boolean> deleteGuildRelationAsync(int relationId) {
         return guardServiceFutureBoolean("deleteGuildRelation", ctx.serviceRef.getGuildRelationByIdAsync(relationId).thenCompose(relation -> {
             if (relation == null) {
                 return CompletableFuture.completedFuture(false);
             }
             return ctx.repos.relations().deleteByIdAsync(relationId).thenCompose(ok -> {
                 if (!Boolean.TRUE.equals(ok)) {
                     return CompletableFuture.completedFuture(false);
                 }
                 String details = relation.getType().name()
                         + " peers=" + relation.getGuild1Name() + "/" + relation.getGuild2Name();
                 String actor = relation.getInitiatorUuid() != null
                         ? relation.getInitiatorUuid().toString() : "SYSTEM";
                 String actorName = relation.getInitiatorName() != null
                         ? relation.getInitiatorName() : "system";
                 CompletableFuture<Boolean> a = ctx.serviceRef.logGuildActionAsync(relation.getGuild1Id(), relation.getGuild1Name(),
                         actor, actorName, GuildLog.LogType.RELATION_DELETED, "Relation deleted", details);
                 CompletableFuture<Boolean> b = ctx.serviceRef.logGuildActionAsync(relation.getGuild2Id(), relation.getGuild2Name(),
                         actor, actorName, GuildLog.LogType.RELATION_DELETED, "Relation deleted", details);
                 return a.thenCombine(b, (x, y) -> true);
             });
         }));
     }

}
