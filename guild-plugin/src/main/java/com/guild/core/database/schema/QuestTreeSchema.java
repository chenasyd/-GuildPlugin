package com.guild.core.database.schema;

public final class QuestTreeSchema {

    private QuestTreeSchema() {
    }

    public static void createSqlite(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_quest_tree (
                guild_id INTEGER PRIMARY KEY,
                tree_level INTEGER NOT NULL DEFAULT 1,
                virtual_exp INTEGER NOT NULL DEFAULT 0,
                updated_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_quest_tree_ledger (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                action TEXT NOT NULL,
                amount INTEGER NOT NULL DEFAULT 0,
                vanilla_exp INTEGER NOT NULL DEFAULT 0,
                tree_level_after INTEGER NOT NULL DEFAULT 1,
                reason TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_quest_tree_daily (
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                day_key TEXT NOT NULL,
                withdrawn_vanilla INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (guild_id, player_uuid, day_key),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }

    public static void createMysql(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_quest_tree (
                guild_id INT PRIMARY KEY,
                tree_level INT NOT NULL DEFAULT 1,
                virtual_exp BIGINT NOT NULL DEFAULT 0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_quest_tree_ledger (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                action VARCHAR(16) NOT NULL,
                amount BIGINT NOT NULL DEFAULT 0,
                vanilla_exp INT NOT NULL DEFAULT 0,
                tree_level_after INT NOT NULL DEFAULT 1,
                reason VARCHAR(128),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_quest_tree_daily (
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                day_key VARCHAR(16) NOT NULL,
                withdrawn_vanilla INT NOT NULL DEFAULT 0,
                PRIMARY KEY (guild_id, player_uuid, day_key),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }
}
