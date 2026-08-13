package com.guild.activity;

import com.guild.GuildPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Config for builtin guild activity / hybrid contribution scoring.
 */
public final class ActivitySettings {

    private boolean enabled = true;
    private boolean registerInfoButton = true;
    private double scoreWeightActivity = 2.0;
    private int tickIntervalSeconds = 60;
    private int dailyActiveMinutesThreshold = 5;

    public ActivitySettings(GuildPlugin plugin) {
        reload(plugin);
    }

    public void reload(GuildPlugin plugin) {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        enabled = cfg.getBoolean("guild-activity.enabled", true);
        registerInfoButton = cfg.getBoolean("guild-activity.register-info-button", true);
        scoreWeightActivity = cfg.getDouble("guild-activity.score-weight-activity", 2.0);
        if (scoreWeightActivity < 0) {
            scoreWeightActivity = 0;
        }
        tickIntervalSeconds = Math.max(15, cfg.getInt("guild-activity.tick-interval-seconds", 60));
        dailyActiveMinutesThreshold = Math.max(1, cfg.getInt("guild-activity.daily-active-minutes", 5));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRegisterInfoButton() {
        return registerInfoButton;
    }

    public double getScoreWeightActivity() {
        return scoreWeightActivity;
    }

    public int getTickIntervalSeconds() {
        return tickIntervalSeconds;
    }

    public int getDailyActiveMinutesThreshold() {
        return dailyActiveMinutesThreshold;
    }
}
