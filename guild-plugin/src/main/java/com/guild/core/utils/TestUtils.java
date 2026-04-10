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
        logger.info("=== 服务器兼容性测试 ===");
        logger.info("服务器类型: " + ServerUtils.getServerType());
        logger.info("服务器版本: " + ServerUtils.getServerVersion());
        logger.info("是否支持1.21: " + ServerUtils.supportsApiVersion("1.21"));
        logger.info("是否支持1.21.8: " + ServerUtils.supportsApiVersion("1.21.8"));
        logger.info("是否为Folia: " + ServerUtils.isFolia());
        logger.info("是否为Spigot: " + ServerUtils.isSpigot());
        logger.info("=========================");
    }
    
    /**
     * 测试调度器兼容性
     */
    public static void testSchedulerCompatibility(java.util.logging.Logger logger) {
        if (logger == null) {
            return;
        }
        logger.info("=== 调度器兼容性测试 ===");
        logger.info("是否在主线程: " + CompatibleScheduler.isPrimaryThread());
        logger.info("服务器类型: " + ServerUtils.getServerType());
        logger.info("=========================");
    }
}
