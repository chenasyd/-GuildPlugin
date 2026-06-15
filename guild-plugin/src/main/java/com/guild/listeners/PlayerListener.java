package com.guild.listeners;

import com.guild.GuildPlugin;
import com.guild.chat.GuildChatManager;
import com.guild.core.gui.GUIManager;
import com.guild.core.language.LanguageManager;
import com.guild.events.GuildChatEvent;
import com.guild.models.GuildMember;
import com.guild.util.NotifyUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家事件监听器
 */
public class PlayerListener implements Listener {
    
    private final GuildPlugin plugin;
    private final LanguageManager languageManager;
    private final GuildChatManager chatManager;
    
    public PlayerListener(GuildPlugin plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.chatManager = plugin.getGuildChatManager();
    }
    
    /**
     * 玩家加入服务器事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 检查工会战争状态
        checkWarStatus(event.getPlayer());
        
        // 检查待处理的申请和邀请通知
        checkPendingNotifications(event.getPlayer());

        // 发送缓存的离线公会聊天消息
        chatManager.deliverOfflineMessages(event.getPlayer());
    }
    
    /**
     * 检查并发送待处理的申请和邀请通知
     */
    private void checkPendingNotifications(org.bukkit.entity.Player player) {
        NotifyUtils.notifyOnLogin(plugin, player);
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
                                String message = languageManager.getCoreMessage(player, "relations.war-notification", "&4[工会战争] &c您的工会与 {guild} 处于开战状态！", "{guild}", relation.getOtherGuildName(guild.getId()));
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
        // 清理聊天模式
        chatManager.removePlayer(event.getPlayer().getUniqueId());
        // 清理玩家的GUI状态
        GUIManager guiManager = plugin.getGuiManager();
        if (guiManager != null) {
            guiManager.closeGUI(event.getPlayer());
        }
    }
    
    /**
     * 处理聊天输入事件（GUI输入 / 公会聊天）
     */
    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GUIManager guiManager = plugin.getGuiManager();
        
        // GUI 输入模式优先处理
        if (guiManager != null && guiManager.isInInputMode(player)) {
            event.setCancelled(true);
            String input = event.getMessage();
            CompatibleScheduler.runTask(plugin, () -> {
                try {
                    guiManager.handleInput(player, input);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error handling GUI input: " + e.getMessage());
                    e.printStackTrace();
                    guiManager.clearInputMode(player);
                }
            });
            return;
        }

        // 公会聊天模式处理
        if (chatManager != null && chatManager.isInGuildChat(player.getUniqueId())) {
            handleGuildChat(event);
        }
    }

    /**
     * 处理公会聊天消息 — 线性异步链获取成员→公会→成员列表，然后广播
     */
    private void handleGuildChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);
        String rawMessage = event.getMessage();

        plugin.getGuildService().getGuildMemberAsync(player.getUniqueId()).thenAccept(member -> {
            if (member == null) {
                String msg = languageManager.getCoreMessage(player, "guild.chat.not-in-guild",
                    "&cYou are not in a guild! Chat mode disabled.");
                player.sendMessage(ColorUtils.colorize(msg));
                chatManager.removePlayer(player.getUniqueId());
                return;
            }
            int guildId = member.getGuildId();
            GuildMember.Role role = member.getRole();

            plugin.getGuildService().getGuildByIdAsync(guildId).thenAccept(guild -> {
                if (guild == null) {
                    chatManager.removePlayer(player.getUniqueId());
                    return;
                }

                String formatted = chatManager.formatMessage(player, role, rawMessage);

                plugin.getGuildService().getGuildMembersAsync(guildId).thenAccept(allMembers -> {
                    // Build online recipients set
                    Set<UUID> onlineUuids = new HashSet<>();
                    for (GuildMember m : allMembers) {
                        Player p = Bukkit.getPlayer(m.getPlayerUuid());
                        if (p != null && p.isOnline()) {
                            onlineUuids.add(m.getPlayerUuid());
                        }
                    }

                    // Fire GuildChatEvent for module hooks
                    GuildChatEvent chatEvent = new GuildChatEvent(
                        player, guild.getId(), guild.getName(),
                        onlineUuids, rawMessage, formatted);
                    Bukkit.getPluginManager().callEvent(chatEvent);

                    if (chatEvent.isCancelled()) return;

                    String finalMessage = ColorUtils.colorize(chatEvent.getFormat());
                    for (UUID uuid : chatEvent.getRecipients()) {
                        Player recipient = Bukkit.getPlayer(uuid);
                        if (recipient != null && recipient.isOnline()) {
                            recipient.sendMessage(finalMessage);
                        }
                    }

                    // Cache for offline members
                    for (GuildMember m : allMembers) {
                        if (!onlineUuids.contains(m.getPlayerUuid())) {
                            chatManager.cacheOfflineMessage(m.getPlayerUuid(), finalMessage);
                        }
                    }
                });
            });
        });
    }
}
