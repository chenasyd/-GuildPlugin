package com.guild.module.example.stats;

import com.guild.core.module.CoreActivityBridge;
import com.guild.core.module.ModuleContext;

/**
 * guild-stats：内置 activity 可用时禁用本地 Tracker，改读 {@code getMemberActivityScores}。
 */
final class StatsActivityBridge {

    private StatsActivityBridge() {
    }

    static void configure(ModuleContext context, ActivityTracker activityTracker) {
        if (CoreActivityBridge.isCoreActivityEnabled(context)) {
            context.getLogger().info("[Stats] Using core ActivityScoreService (local ActivityTracker disabled)");
        } else {
            activityTracker.start();
            context.getLogger().info("[Stats] Core activity unavailable — local ActivityTracker enabled (demo)");
        }
    }
}
