package com.guild.metrics;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * bStats metrics wrapper class
 * Reports plugin usage data to bStats silently in the background
 */
public class GuildMetrics {

    private final JavaPlugin plugin;
    private final Metrics metrics;
    private final int pluginId;

    /**
     * Construct and initialize bStats Metrics silently (no console output)
     *
     * @param plugin   Plugin instance
     * @param pluginId bStats plugin ID (get at https://bstats.org/what-is-my-plugin-id)
     */
    public GuildMetrics(JavaPlugin plugin, int pluginId) {
        this.plugin = plugin;
        this.pluginId = pluginId;

        // Silent initialization - runs in background, no console output
        this.metrics = new Metrics(plugin, pluginId);

        // Register custom charts
        registerCustomCharts();
    }

    /**
     * Register custom statistics charts
     * Add more custom data dimensions as needed
     */
    private void registerCustomCharts() {
        // Report server type (Spigot / Folia)
        metrics.addCustomChart(new SimplePie("server_type", () -> {
            try {
                Class.forName("io.papermc.paper.threadedregions.scheduler.FoliaScheduler");
                return "Folia";
            } catch (ClassNotFoundException e) {
                return "Spigot/Paper";
            }
        }));

        // Report server version
        metrics.addCustomChart(new SimplePie("server_version", () ->
                plugin.getServer().getBukkitVersion()));

        // Report plugin version
        metrics.addCustomChart(new SimplePie("plugin_version", () ->
                plugin.getDescription().getVersion()));
    }

    /**
     * Get bStats plugin ID
     */
    public int getPluginId() {
        return pluginId;
    }
}
