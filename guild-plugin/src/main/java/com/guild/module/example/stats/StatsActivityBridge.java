package com.guild.module.example.stats;

import com.guild.core.module.ModuleContext;

/**
 * 内置 activity 可用时禁用本地 Tracker，避免与 {@code builtin-activity} 双轨写入。
 */
final class StatsActivityBridge {

    private StatsActivityBridge() {
    }

    static void configure(ModuleContext context, ActivityTracker activityTracker) {
        boolean coreActivity = false;
        try {
            var core = context.getServiceContainer().get(com.guild.activity.ActivityScoreService.class);
            coreActivity = core != null && core.getSettings().isEnabled();
        } catch (Exception ignored) {
            // service may not be registered in odd load orders
        }
        if (coreActivity) {
            context.getLogger().info("[Stats] Using core ActivityScoreService (local ActivityTracker disabled)");
        } else {
            activityTracker.start();
            context.getLogger().info("[Stats] Core activity unavailable — local ActivityTracker enabled (demo)");
        }
    }
}
