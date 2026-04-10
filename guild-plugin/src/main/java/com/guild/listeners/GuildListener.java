package com.guild.listeners;

import com.guild.GuildPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

/**
 * 工会事件监听器
 */
public class GuildListener implements Listener {
    
    private final GuildPlugin plugin;
    
    public GuildListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 玩家聊天事件（可以用于工会聊天功能）
     */
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        // 这里可以添加工会聊天功能
        // 比如检测工会前缀、处理工会聊天等
    }
}
