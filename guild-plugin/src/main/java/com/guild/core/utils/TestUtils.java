package com.guild.core.utils;

/**
 * 启动兼容性检测（精简控制台输出）。
 */
public class TestUtils {

    private TestUtils() {
    }

    /**
     * 输出一行兼容性摘要（服务器类型 / MC 版本 / Folia / 调度线程）。
     */
    public static void testCompatibility(java.util.logging.Logger logger) {
        if (logger == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("Compatibility: ")
                .append(ServerUtils.getServerType())
                .append(" MC=").append(ServerUtils.getMinecraftVersion())
                .append(" Folia=").append(ServerUtils.isFolia());
        if (ServerUtils.isFolia()) {
            sb.append(" gworld=").append(ServerUtils.isFoliaVersionSupported());
        }
        sb.append(" primaryThread=").append(CompatibleScheduler.isPrimaryThread());
        logger.info(sb.toString());
    }

    /**
     * @deprecated 已合并进 {@link #testCompatibility(java.util.logging.Logger)}，保留以免旧调用报错。
     */
    @Deprecated
    public static void testSchedulerCompatibility(java.util.logging.Logger logger) {
        // no-op：避免与 testCompatibility 重复刷屏
    }
}
