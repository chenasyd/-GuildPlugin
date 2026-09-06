package com.guild.core.database.schema;

public final class TerritorySchema {

    private TerritorySchema() {
    }

    public static void createSqlite(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_territories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                guild_name TEXT NOT NULL,
                server_id TEXT NOT NULL,
                world_name TEXT NOT NULL,
                region_id TEXT NOT NULL,
                min_x INTEGER NOT NULL,
                min_y INTEGER NOT NULL,
                min_z INTEGER NOT NULL,
                max_x INTEGER NOT NULL,
                max_y INTEGER NOT NULL,
                max_z INTEGER NOT NULL,
                claimed_at INTEGER NOT NULL,
                sync_state TEXT NOT NULL DEFAULT 'MATERIALIZED',
                updated_at INTEGER NOT NULL,
                UNIQUE(guild_id, server_id, world_name),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }

    public static void createMysql(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_territories (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                guild_name VARCHAR(50) NOT NULL,
                server_id VARCHAR(64) NOT NULL,
                world_name VARCHAR(128) NOT NULL,
                region_id VARCHAR(64) NOT NULL,
                min_x INT NOT NULL,
                min_y INT NOT NULL,
                min_z INT NOT NULL,
                max_x INT NOT NULL,
                max_y INT NOT NULL,
                max_z INT NOT NULL,
                claimed_at BIGINT NOT NULL,
                sync_state VARCHAR(16) NOT NULL DEFAULT 'MATERIALIZED',
                updated_at BIGINT NOT NULL,
                UNIQUE KEY uk_guild_server_world (guild_id, server_id, world_name),
                KEY idx_territory_guild (guild_id),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }
}
