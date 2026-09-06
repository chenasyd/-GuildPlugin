package com.guild.core.database.schema;

/** 公会核心表：guilds / members / applications / invites / relations / economy / contributions / logs */
public final class GuildCoreSchema {

    private GuildCoreSchema() {
    }

    public static void createSqlite(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guilds (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                tag TEXT UNIQUE,
                description TEXT,
                leader_uuid TEXT NOT NULL,
                leader_name TEXT NOT NULL,
                home_world TEXT,
                home_x REAL,
                home_y REAL,
                home_z REAL,
                home_yaw REAL,
                home_pitch REAL,
                balance REAL DEFAULT 0.0,
                level INTEGER DEFAULT 1,
                max_members INTEGER DEFAULT 6,
                frozen INTEGER DEFAULT 0,
                peak_level INTEGER DEFAULT 1,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                updated_at TEXT DEFAULT (datetime('now','localtime'))
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                role TEXT DEFAULT 'MEMBER',
                joined_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE(guild_id, player_uuid)
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_applications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                message TEXT,
                status TEXT DEFAULT 'PENDING',
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_invites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                inviter_uuid TEXT NOT NULL,
                inviter_name TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING',
                expires_at TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_relations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild1_id INTEGER NOT NULL,
                guild2_id INTEGER NOT NULL,
                guild1_name TEXT NOT NULL,
                guild2_name TEXT NOT NULL,
                relation_type TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING',
                initiator_uuid TEXT NOT NULL,
                initiator_name TEXT NOT NULL,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                updated_at TEXT DEFAULT (datetime('now','localtime')),
                expires_at TEXT,
                FOREIGN KEY (guild1_id) REFERENCES guilds(id) ON DELETE CASCADE,
                FOREIGN KEY (guild2_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE(guild1_id, guild2_id)
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_economy (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL UNIQUE,
                balance REAL DEFAULT 0.0,
                level INTEGER DEFAULT 1,
                experience REAL DEFAULT 0.0,
                max_experience REAL DEFAULT 5000.0,
                max_members INTEGER DEFAULT 6,
                last_updated TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_contributions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                amount REAL NOT NULL,
                contribution_type TEXT NOT NULL,
                description TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id INTEGER NOT NULL,
                guild_name TEXT NOT NULL,
                player_uuid TEXT NOT NULL,
                player_name TEXT NOT NULL,
                log_type TEXT NOT NULL,
                description TEXT NOT NULL,
                details TEXT,
                created_at TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }

    public static void createMysql(SqlUpdater updater) {
        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guilds (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) UNIQUE NOT NULL,
                tag VARCHAR(10) UNIQUE,
                description TEXT,
                leader_uuid VARCHAR(36) NOT NULL,
                leader_name VARCHAR(16) NOT NULL,
                home_world VARCHAR(100),
                home_x DOUBLE,
                home_y DOUBLE,
                home_z DOUBLE,
                home_yaw FLOAT,
                home_pitch FLOAT,
                balance DOUBLE DEFAULT 0.0,
                level INT DEFAULT 1,
                max_members INT DEFAULT 6,
                frozen BOOLEAN DEFAULT FALSE,
                peak_level INT DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_members (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                role VARCHAR(20) DEFAULT 'MEMBER',
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE KEY unique_guild_player (guild_id, player_uuid)
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_applications (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                message TEXT,
                status VARCHAR(20) DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_invites (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                inviter_uuid VARCHAR(36) NOT NULL,
                inviter_name VARCHAR(16) NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                expires_at TIMESTAMP NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_relations (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild1_id INT NOT NULL,
                guild2_id INT NOT NULL,
                guild1_name VARCHAR(50) NOT NULL,
                guild2_name VARCHAR(50) NOT NULL,
                relation_type VARCHAR(20) NOT NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                initiator_uuid VARCHAR(36) NOT NULL,
                initiator_name VARCHAR(16) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NULL,
                FOREIGN KEY (guild1_id) REFERENCES guilds(id) ON DELETE CASCADE,
                FOREIGN KEY (guild2_id) REFERENCES guilds(id) ON DELETE CASCADE,
                UNIQUE KEY unique_guild_relation (guild1_id, guild2_id)
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_economy (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL UNIQUE,
                balance DOUBLE DEFAULT 0.0,
                level INT DEFAULT 1,
                experience DOUBLE DEFAULT 0.0,
                max_experience DOUBLE DEFAULT 5000.0,
                max_members INT DEFAULT 6,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_contributions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                amount DOUBLE NOT NULL,
                contribution_type VARCHAR(20) NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);

        updater.executeUpdate("""
            CREATE TABLE IF NOT EXISTS guild_logs (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guild_id INT NOT NULL,
                guild_name VARCHAR(50) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                log_type VARCHAR(50) NOT NULL,
                description TEXT NOT NULL,
                details TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
        """);
    }
}
