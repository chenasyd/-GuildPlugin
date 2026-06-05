package com.guild.update;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.update.UpdateManager.VersionInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Periodic update checker using dual-source detection (GitHub + Modrinth fallback).
 * <p>
 * Runs once on startup and every 24 hours thereafter via self-scheduling async tasks.
 * Delegates version fetching to {@link UpdateManager}.
 */
public class UpdateChecker {

    private static final long CHECK_INTERVAL_TICKS = 20L * 60 * 60 * 24; // 24 hours

    private final JavaPlugin plugin;
    private final UpdateManager updateManager;
    private final Logger logger;

    public UpdateChecker(JavaPlugin plugin, UpdateManager updateManager) {
        this.plugin = plugin;
        this.updateManager = updateManager;
        this.logger = plugin.getLogger();
    }

    /**
     * Start periodic update checking. Runs first check immediately, then every 24 hours.
     */
    public void start() {
        CompatibleScheduler.runTaskAsync(plugin, this::doCheck);
        CompatibleScheduler.runTaskLater(plugin, this::scheduleNext, CHECK_INTERVAL_TICKS);
    }

    private void scheduleNext() {
        CompatibleScheduler.runTaskAsync(plugin, () -> {
            doCheck();
            CompatibleScheduler.runTaskLater(plugin, this::scheduleNext, CHECK_INTERVAL_TICKS);
        });
    }

    /**
     * Perform a single update check and log the result.
     */
    public void doCheck() {
        try {
            String localVersion = plugin.getDescription().getVersion();
            VersionInfo latest = updateManager.checkLatestVersion();

            if (latest == null) {
                logger.warning("[UpdateChecker] Failed to check for updates from any source.");
                return;
            }

            int comparison = UpdateManager.compareVersions(localVersion, latest.version);
            if (comparison < 0) {
                logger.info("[UpdateChecker] New version available: v" + latest.version
                        + " (current: v" + localVersion + ", source: " + latest.source + ")");
                if (!latest.changelog.isEmpty()) {
                    logger.info("[UpdateChecker] Changelog: " + latest.changelog);
                }
            } else {
                logger.info("[UpdateChecker] You are running the latest version (v" + localVersion + ").");
            }

        } catch (Exception e) {
            logger.warning("[UpdateChecker] Update check failed: " + e.getMessage());
        }
    }
}
