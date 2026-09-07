package com.guild.core.module;

import com.guild.activity.ActivityScoreService;

/**
 * 检测内置 {@code builtin-activity} 是否可用，供示例模块避免双轨 Tracker。
 */
public final class CoreActivityBridge {

    private CoreActivityBridge() {
    }

    public static boolean isCoreActivityEnabled(ModuleContext context) {
        if (context == null) {
            return false;
        }
        try {
            ActivityScoreService core = context.getServiceContainer().get(ActivityScoreService.class);
            return core != null && core.getSettings().isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }
}
