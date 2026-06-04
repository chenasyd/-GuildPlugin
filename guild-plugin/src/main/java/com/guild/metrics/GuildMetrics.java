package com.guild.metrics;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * bStats 数据统计封装类
 * 用于向 bStats 上报插件使用数据
 */
public class GuildMetrics {

    private final JavaPlugin plugin;
    private final Metrics metrics;
    private final int pluginId;

    /**
     * 构造并初始化 bStats Metrics
     *
     * @param plugin   插件实例
     * @param pluginId bStats 插件 ID（在 https://bstats.org/what-is-my-plugin-id 获取）
     */
    public GuildMetrics(JavaPlugin plugin, int pluginId) {
        this.plugin = plugin;
        this.pluginId = pluginId;
        Logger logger = plugin.getLogger();

        logger.info("正在初始化 bStats 数据统计 (Plugin ID: " + pluginId + ")...");
        this.metrics = new Metrics(plugin, pluginId);

        // 注册自定义统计图表
        registerCustomCharts();

        logger.info("bStats 数据统计初始化完成");
    }

    /**
     * 注册自定义统计图表
     * 可根据需要添加更多自定义数据维度
     */
    private void registerCustomCharts() {
        // 示例：上报服务器类型（Spigot / Folia）
        metrics.addCustomChart(new SimplePie("server_type", () -> {
            try {
                Class.forName("io.papermc.paper.threadedregions.scheduler.FoliaScheduler");
                return "Folia";
            } catch (ClassNotFoundException e) {
                return "Spigot/Paper";
            }
        }));

        // 示例：上报服务器版本
        metrics.addCustomChart(new SimplePie("server_version", () ->
                plugin.getServer().getMinecraftVersion()));

        // 示例：上报插件版本
        metrics.addCustomChart(new SimplePie("plugin_version", () ->
                plugin.getDescription().getVersion()));
    }

    /**
     * 获取 bStats 插件 ID
     */
    public int getPluginId() {
        return pluginId;
    }
}
