package com.guild.core.database.schema;

public final class WarehouseSchema {

    private WarehouseSchema() {
    }

    public static void createSqlite(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_warehouse_items (
                guild_id INTEGER NOT NULL,
                slot INTEGER NOT NULL,
                nbt TEXT NOT NULL,
                PRIMARY KEY (guild_id, slot),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_warehouse_role_perms (
                guild_id INTEGER NOT NULL,
                role TEXT NOT NULL,
                can_open INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (guild_id, role),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_warehouse_access_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                action TEXT NOT NULL,
                page INTEGER NOT NULL DEFAULT 1,
                details TEXT,
                created_at TEXT NOT NULL,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }

    public static void createMysql(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_warehouse_items (
                guild_id INT NOT NULL,
                slot INT NOT NULL,
                nbt MEDIUMTEXT NOT NULL,
                PRIMARY KEY (guild_id, slot),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_warehouse_role_perms (
                guild_id INT NOT NULL,
                role VARCHAR(20) NOT NULL,
                can_open BOOLEAN NOT NULL DEFAULT FALSE,
                PRIMARY KEY (guild_id, role),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_warehouse_access_log (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                action VARCHAR(16) NOT NULL,
                page INT NOT NULL DEFAULT 1,
                details TEXT,
                created_at DATETIME NOT NULL,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                INDEX idx_wh_access_guild (guild_id, created_at)
            )
        """);
    }
}
