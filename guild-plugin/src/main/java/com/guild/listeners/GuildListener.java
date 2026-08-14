package com.guild.listeners;

import com.guild.GuildPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

/**
 * 公会事件监听器
 */
public class GuildListener implements Listener {
    
    private final GuildPlugin plugin;
    
    public GuildListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 玩家聊天事件（可以用于公会聊天功能）
     */
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        // 这里可以添加公会聊天功能
        // 比如检测公会前缀、处理公会聊天等
    }
}
