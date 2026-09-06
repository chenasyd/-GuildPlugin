package com.guild.core.database.schema;

import com.guild.core.database.DatabaseManager;

/**
 * 按域顺序创建全部数据表（core → warehouse → activity → war → territory → quest-tree）。
 */
public final class SchemaInitializer {

    private SchemaInitializer() {
    }

    public static void createAll(DatabaseManager.DatabaseType type, SqlUpdater updater) {
        if (type == DatabaseManager.DatabaseType.SQLITE) {
            GuildCoreSchema.createSqlite(updater);
            WarehouseSchema.createSqlite(updater);
            ActivitySchema.createSqlite(updater);
            WarSchema.createSqlite(updater);
            TerritorySchema.createSqlite(updater);
            QuestTreeSchema.createSqlite(updater);
        } else {
            GuildCoreSchema.createMysql(updater);
            WarehouseSchema.createMysql(updater);
            ActivitySchema.createMysql(updater);
            WarSchema.createMysql(updater);
            TerritorySchema.createMysql(updater);
            QuestTreeSchema.createMysql(updater);
        }
    }

    /** 供单测校验空库初始化后的表清单。 */
    public static String[] expectedTableNames() {
        return new String[]{
                "guilds",
                "guild_members",
                "guild_applications",
                "guild_invites",
                "guild_relations",
                "guild_economy",
                "guild_contributions",
                "guild_logs",
                "guild_warehouse_items",
                "guild_warehouse_role_perms",
                "guild_warehouse_access_log",
                "guild_member_activity",
                "war_matches",
                "war_match_players",
                "war_season_stats",
                "guild_territories",
                "guild_quest_tree",
                "guild_quest_tree_ledger",
                "guild_quest_tree_daily"
        };
    }
}
