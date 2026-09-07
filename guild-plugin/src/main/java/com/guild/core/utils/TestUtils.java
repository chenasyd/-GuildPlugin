package com.guild.core.utils;

/**
 * 启动兼容性检测（精简控制台输出）。
 */
public class TestUtils {

    private TestUtils() {
    }

    /**
     * 输出兼容性摘要并在非官方版本上自动声明 best-effort。
     */
    public static void testCompatibility(java.util.logging.Logger logger) {
        if (logger == null) {
            return;
        }
        String mc = ServerUtils.getMinecraftVersion();
        MinecraftVersionSupport.CompatibilityLevel level = MinecraftVersionSupport.resolve(mc);

        StringBuilder sb = new StringBuilder("Compatibility: ")
                .append(ServerUtils.getServerType())
                .append(" MC=").append(mc)
                .append(" tier=").append(level.name())
                .append(" Folia=").append(ServerUtils.isFolia());
        if (ServerUtils.isFolia()) {
            sb.append(" gworld=").append(ServerUtils.isFoliaVersionSupported());
        }
        sb.append(" primaryThread=").append(CompatibleScheduler.isPrimaryThread());
        logger.info(sb.toString());
        logger.info(MinecraftVersionSupport.describe(level, mc));

        if (level == MinecraftVersionSupport.CompatibilityLevel.BEST_EFFORT) {
            logger.warning("[Compatibility] Non-official MC version " + mc
                    + " — best-effort mode enabled. Official targets: "
                    + MinecraftVersionSupport.formatOfficialVersionList());
        }
    }

    /**
     * @deprecated 已合并进 {@link #testCompatibility(java.util.logging.Logger)}，保留以免旧调用报错。
     */
    @Deprecated
    public static void testSchedulerCompatibility(java.util.logging.Logger logger) {
        // no-op：避免与 testCompatibility 重复刷屏
    }
}
