package com.guild.core.utils;

/**
 * 测试工具类
 */
public class TestUtils {
    
    /**
     * 测试服务器兼容性
     */
    public static void testCompatibility(java.util.logging.Logger logger) {
        if (logger == null) {
            return;
        }
        logger.info("=== Server Compatibility Test ===");
        logger.info("Server type: " + ServerUtils.getServerType());
        logger.info("Server version: " + ServerUtils.getServerVersion());
        logger.info("Supports 1.20: " + ServerUtils.supportsApiVersion("1.20"));
        logger.info("Is Folia: " + ServerUtils.isFolia());
        logger.info("Is Spigot: " + ServerUtils.isSpigot());
        logger.info("=========================");
    }
    
    /**
     * 测试调度器兼容性
     */
    public static void testSchedulerCompatibility(java.util.logging.Logger logger) {
        if (logger == null) {
            return;
        }
        logger.info("=== Scheduler Compatibility Test ===");
        logger.info("Is on primary thread: " + CompatibleScheduler.isPrimaryThread());
        logger.info("Server type: " + ServerUtils.getServerType());
        logger.info("=========================");
    }
}
