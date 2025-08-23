package com.guild.listeners;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.guild.core.utils.CompatibleScheduler;

/**
 * 玩家事件监听器
 */
public class PlayerListener implements Listener {
    
    private final GuildPlugin plugin;
    
    public PlayerListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 玩家加入服务器事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 检查工会战争状态
        checkWarStatus(event.getPlayer());
    }
    
    /**
     * 检查工会战争状态并发送通知
     */
    private void checkWarStatus(org.bukkit.entity.Player player) {
        // 异步检查玩家的工会
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild != null) {
                // 检查工会的所有关系
                plugin.getGuildService().getGuildRelationsAsync(guild.getId()).thenAccept(relations -> {
                    // 确保在主线程中执行
                    CompatibleScheduler.runTask(plugin, () -> {
                        for (com.guild.models.GuildRelation relation : relations) {
                            if (relation.isWar()) {
                                String message = plugin.getConfigManager().getMessagesConfig().getString("relations.war-notification", "&4[工会战争] &c您的工会与 {guild} 处于开战状态！");
                                message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                                player.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                            }
                        }
                    });
                });
            }
        });
    }
    
    /**
     * 玩家离开服务器事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 清理玩家的GUI状态
        GUIManager guiManager = plugin.getGuiManager();
        if (guiManager != null) {
            guiManager.closeGUI(event.getPlayer());
        }
    }
    
    /**
     * 处理聊天输入事件（用于GUI输入模式）
     */
    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        GUIManager guiManager = plugin.getGuiManager();
        
        if (guiManager != null && guiManager.isInInputMode(event.getPlayer())) {
            // 取消事件，防止消息发送到聊天
            event.setCancelled(true);
            
            // 处理输入 - 在主线程中执行
            String input = event.getMessage();
            CompatibleScheduler.runTask(plugin, () -> {
                try {
                    guiManager.handleInput(event.getPlayer(), input);
                } catch (Exception e) {
                    plugin.getLogger().severe("处理GUI输入时发生错误: " + e.getMessage());
                    e.printStackTrace();
                    // 发生错误时清除输入模式
                    guiManager.clearInputMode(event.getPlayer());
                }
            });
        }
    }
}
