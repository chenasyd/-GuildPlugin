package com.guild.core.placeholder;

import com.guild.GuildPlugin;
import com.guild.services.GuildService;

/**
 * 占位符管理器 - 管理PlaceholderAPI集成
 */
public class PlaceholderManager {
    
    private final GuildPlugin plugin;
    private GuildService guildService;
    private GuildPlaceholderExpansion placeholderExpansion;
    private boolean placeholderApiAvailable = false;
    
    public PlaceholderManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.guildService = null; // 临时设置为null，避免循环依赖
    }
    
    /**
     * 设置工会服务（在服务容器初始化后调用）
     */
    public void setGuildService(GuildService guildService) {
        this.guildService = guildService;
    }
    
    /**
     * 注册占位符
     */
    public void registerPlaceholders() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                // 创建并注册 PlaceholderExpansion
                placeholderExpansion = new GuildPlaceholderExpansion(plugin, guildService);
                placeholderExpansion.register();
                placeholderApiAvailable = true;
                plugin.getLogger().info("PlaceholderAPI 占位符注册成功");
            } catch (Exception e) {
                plugin.getLogger().warning("PlaceholderAPI 初始化失败: " + e.getMessage());
                placeholderApiAvailable = false;
            }
        } else {
            plugin.getLogger().warning("PlaceholderAPI 未找到，占位符功能将不可用");
            placeholderApiAvailable = false;
        }
    }
    

    
    /**
     * 检查 PlaceholderAPI 是否可用
     */
    public boolean isPlaceholderApiAvailable() {
        return placeholderApiAvailable;
    }
}
