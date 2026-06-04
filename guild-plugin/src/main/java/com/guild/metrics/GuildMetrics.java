package com.guild.metrics;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * bStats metrics wrapper.
 * Pattern follows SimpleClaimSystem's minimal approach — just instantiate Metrics and let bStats
 * handle its own lifecycle (config creation, scheduling, shutdown via JVM exit).
 */
public class GuildMetrics {

    /** The Metrics instance for bStats integration. */
    public Metrics metrics;

    /**
     * Enable bStats metrics for the plugin.
     * Writes a silent bStats config first to suppress HTTP error spam (e.g. 429 when
     * the plugin ID has not yet been registered on bStats.org).
     *
     * @param plugin   the JavaPlugin instance
     * @param pluginId bStats plugin ID (register at https://bstats.org/what-is-my-plugin-id)
     */
    public GuildMetrics(JavaPlugin plugin, int pluginId) {
        // Write silent config before bStats reads it, suppressing HTTP 429 and connection
        // error warnings when the plugin ID is not yet registered on bStats.org
        setupBStatsSilentConfig(plugin);

        // bStats Metrics constructor handles config reading, scheduling, and all lifecycle
        metrics = new Metrics(plugin, pluginId);
        registerCustomCharts(plugin);
    }

    /**
     * Create bStats config.yml with silent logging settings before bStats initializes.
     * bStats reads this file in its constructor; writing it first ensures failed requests
     * are suppressed without interfering with bStats' own config handling.
     */
    private void setupBStatsSilentConfig(JavaPlugin plugin) {
        File bStatsDir = new File(plugin.getDataFolder().getParentFile(), "bStats");
        if (!bStatsDir.exists()) {
            bStatsDir.mkdirs();
        }
        File configFile = new File(bStatsDir, "config.yml");

        // Always write; bStats itself may have created a config with loud defaults
        // on a previous run. Overwriting ensures HTTP 429 and connection errors are
        // suppressed — users can manually edit the file later if needed.
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# bStats configuration\n");
            writer.write("enabled: true\n");
            writer.write("logFailedRequests: false\n");
            writer.write("logSentData: false\n");
            writer.write("logResponseStatusText: false\n");
        } catch (IOException ignored) {
            // bStats will use its own defaults if we can't write the config
        }
    }

    /**
     * Register custom statistics charts to provide richer data on bStats.
     */
    private void registerCustomCharts(JavaPlugin plugin) {
        // Server type (Spigot / Folia)
        metrics.addCustomChart(new SimplePie("server_type", () -> {
            try {
                Class.forName("io.papermc.paper.threadedregions.scheduler.FoliaScheduler");
                return "Folia";
            } catch (ClassNotFoundException e) {
                return "Spigot/Paper";
            }
        }));

        // Server version
        metrics.addCustomChart(new SimplePie("server_version", () ->
                plugin.getServer().getBukkitVersion()));

        // Plugin version
        metrics.addCustomChart(new SimplePie("plugin_version", () ->
                plugin.getDescription().getVersion()));
    }
}
