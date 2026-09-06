package com.guild.core.database.schema;

/**
 * 执行 DDL / 迁移语句的回调，由 {@link DatabaseManager} 提供实现。
 */
@FunctionalInterface
public interface SqlUpdater {

    void executeUpdate(String sql);
}
