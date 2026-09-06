package com.guild.core.database.schema;

public final class WarSchema {

    private WarSchema() {
    }

    public static void createSqlite(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS war_matches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                runtime_match_id INTEGER NOT NULL,
                guild_a_id INTEGER NOT NULL,
                guild_a_name TEXT NOT NULL,
                guild_b_id INTEGER NOT NULL,
                guild_b_name TEXT NOT NULL,
                winner_guild_id INTEGER,
                mode TEXT NOT NULL,
                score_a INTEGER NOT NULL,
                score_b INTEGER NOT NULL,
                score_to_win INTEGER NOT NULL,
                preset_name TEXT,
                end_reason TEXT,
                started_at INTEGER NOT NULL,
                ended_at INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL,
                season_id TEXT NOT NULL DEFAULT 'default'
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS war_match_players (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                match_report_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                guild_id INTEGER NOT NULL,
                side TEXT NOT NULL,
                kills INTEGER NOT NULL DEFAULT 0,
                eliminated INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (match_report_id) REFERENCES war_matches(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS war_season_stats (
                guild_id INTEGER NOT NULL,
                guild_name TEXT NOT NULL,
                season_id TEXT NOT NULL,
                wins INTEGER NOT NULL DEFAULT 0,
                losses INTEGER NOT NULL DEFAULT 0,
                draws INTEGER NOT NULL DEFAULT 0,
                kills INTEGER NOT NULL DEFAULT 0,
                matches INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (guild_id, season_id)
            )
        """);
    }

    public static void createMysql(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS war_matches (
                id INT AUTO_INCREMENT PRIMARY KEY,
                runtime_match_id INT NOT NULL,
                guild_a_id INT NOT NULL,
                guild_a_name VARCHAR(50) NOT NULL,
                guild_b_id INT NOT NULL,
                guild_b_name VARCHAR(50) NOT NULL,
                winner_guild_id INT NULL,
                mode VARCHAR(32) NOT NULL,
                score_a INT NOT NULL,
                score_b INT NOT NULL,
                score_to_win INT NOT NULL,
                preset_name VARCHAR(64),
                end_reason VARCHAR(128),
                started_at BIGINT NOT NULL,
                ended_at BIGINT NOT NULL,
                duration_ms BIGINT NOT NULL,
                season_id VARCHAR(64) NOT NULL DEFAULT 'default'
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS war_match_players (
                id INT AUTO_INCREMENT PRIMARY KEY,
                match_report_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                guild_id INT NOT NULL,
                side VARCHAR(8) NOT NULL,
                kills INT NOT NULL DEFAULT 0,
                eliminated TINYINT NOT NULL DEFAULT 0,
                FOREIGN KEY (match_report_id) REFERENCES war_matches(id) ON DELETE CASCADE
            )
        """);
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS war_season_stats (
                guild_id INT NOT NULL,
                guild_name VARCHAR(50) NOT NULL,
                season_id VARCHAR(64) NOT NULL,
                wins INT NOT NULL DEFAULT 0,
                losses INT NOT NULL DEFAULT 0,
                draws INT NOT NULL DEFAULT 0,
                kills INT NOT NULL DEFAULT 0,
                matches INT NOT NULL DEFAULT 0,
                PRIMARY KEY (guild_id, season_id)
            )
        """);
    }
}
