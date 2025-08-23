package com.guild.core.utils;

/**
 * 测试工具类
 */
public class TestUtils {
    
    /**
     * 测试服务器兼容性
     */
    public static void testCompatibility() {
        System.out.println("=== 服务器兼容性测试 ===");
        System.out.println("服务器类型: " + ServerUtils.getServerType());
        System.out.println("服务器版本: " + ServerUtils.getServerVersion());
        System.out.println("是否支持1.21: " + ServerUtils.supportsApiVersion("1.21"));
        System.out.println("是否支持1.21.8: " + ServerUtils.supportsApiVersion("1.21.8"));
        System.out.println("是否为Folia: " + ServerUtils.isFolia());
        System.out.println("是否为Spigot: " + ServerUtils.isSpigot());
        System.out.println("=========================");
    }
    
    /**
     * 测试调度器兼容性
     */
    public static void testSchedulerCompatibility() {
        System.out.println("=== 调度器兼容性测试 ===");
        System.out.println("是否在主线程: " + CompatibleScheduler.isPrimaryThread());
        System.out.println("服务器类型: " + ServerUtils.getServerType());
        System.out.println("=========================");
    }
}
