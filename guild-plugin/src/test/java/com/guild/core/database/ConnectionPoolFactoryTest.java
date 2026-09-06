package com.guild.core.database;

import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionPoolFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveDatabaseType_supportsPrefixedConfigKey() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.type", "mysql");
        assertEquals("mysql", ConnectionPoolFactory.resolveDatabaseType(config));
    }

    @Test
    void buildSqliteHikariConfig_setsWalPragmasAndFilePath() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("sqlite.file", "guild-test.db");
        config.set("sqlite.wal-mode", true);
        config.set("sqlite.foreign-keys", true);
        config.set("sqlite.cache-size", 4000);
        config.set("connection-pool.maximum-pool-size", 1);

        HikariConfig hikari = ConnectionPoolFactory.buildSqliteHikariConfig(tempDir.toFile(), config);

        assertTrue(hikari.getJdbcUrl().endsWith("guild-test.db"));
        assertEquals(1, hikari.getMaximumPoolSize());
        String initSql = hikari.getConnectionInitSql();
        assertTrue(initSql.contains("PRAGMA journal_mode=WAL;"));
        assertTrue(initSql.contains("PRAGMA foreign_keys=ON;"));
        assertTrue(initSql.contains("PRAGMA cache_size=4000;"));
    }

    @Test
    void buildMySqlHikariConfig_buildsJdbcUrlAndCredentials() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("mysql.host", "db.example.com");
        config.set("mysql.port", 3307);
        config.set("mysql.database", "guild_prod");
        config.set("mysql.username", "guild_user");
        config.set("mysql.password", "secret");
        config.set("mysql.use-ssl", true);
        config.set("mysql.timezone", "Asia/Shanghai");

        HikariConfig hikari = ConnectionPoolFactory.buildMySqlHikariConfig(config);

        assertTrue(hikari.getJdbcUrl().contains("jdbc:mysql://db.example.com:3307/guild_prod"));
        assertTrue(hikari.getJdbcUrl().contains("useSSL=true"));
        assertTrue(hikari.getJdbcUrl().contains("serverTimezone=Asia/Shanghai"));
        assertEquals("guild_user", hikari.getUsername());
        assertEquals("secret", hikari.getPassword());
    }
}
