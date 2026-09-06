package com.guild.core.database.schema;

public final class ActivitySchema {

    private ActivitySchema() {
    }

    public static void createSqlite(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_member_activity (
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                online_minutes_today INTEGER NOT NULL DEFAULT 0,
                online_minutes_total INTEGER NOT NULL DEFAULT 0,
                active_days_week INTEGER NOT NULL DEFAULT 0,
                active_day_date TEXT,
                week_start_date TEXT,
                last_login_date TEXT,
                last_seen INTEGER NOT NULL DEFAULT 0,
                today_date TEXT,
                PRIMARY KEY (guild_id, player_uuid),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }

    public static void createMysql(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_member_activity (
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                online_minutes_today INT NOT NULL DEFAULT 0,
                online_minutes_total INT NOT NULL DEFAULT 0,
                active_days_week INT NOT NULL DEFAULT 0,
                active_day_date VARCHAR(16) NULL,
                week_start_date VARCHAR(16) NULL,
                last_login_date VARCHAR(16) NULL,
                last_seen BIGINT NOT NULL DEFAULT 0,
                today_date VARCHAR(16) NULL,
                PRIMARY KEY (guild_id, player_uuid),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }
}
