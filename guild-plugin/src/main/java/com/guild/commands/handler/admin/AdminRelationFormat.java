package com.guild.commands.handler.admin;

import com.guild.core.language.LanguageManager;
import com.guild.models.GuildRelation;

/** 管理员关系命令的本地化文本。 */
public final class AdminRelationFormat {

    private AdminRelationFormat() {
    }

    public static String getRelationStatusText(LanguageManager languageManager, GuildRelation.RelationStatus status) {
        String key = "admin.relation.status.unknown";
        switch (status) {
            case PENDING -> key = "admin.relation.status.pending";
            case ACTIVE -> key = "admin.relation.status.active";
            case EXPIRED -> key = "admin.relation.status.expired";
            case CANCELLED -> key = "admin.relation.status.cancelled";
        }
        return languageManager.getCoreMessage(key, "Unknown");
    }

    public static String getRelationTypeText(LanguageManager languageManager, GuildRelation.RelationType type) {
        String key = "admin.relation.type.unknown";
        switch (type) {
            case ALLY -> key = "admin.relation.type.ally";
            case ENEMY -> key = "admin.relation.type.enemy";
            case WAR -> key = "admin.relation.type.war";
            case TRUCE -> key = "admin.relation.type.truce";
            case NEUTRAL -> key = "admin.relation.type.neutral";
        }
        return languageManager.getCoreMessage(key, "Unknown");
    }
}
