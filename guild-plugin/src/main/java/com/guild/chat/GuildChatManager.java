package com.guild.chat;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.models.GuildMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公会聊天管理器 — 管理聊天模式切换、离线消息缓存、消息格式化。
 */
public class GuildChatManager {

    private final GuildPlugin plugin;
    private final Set<UUID> guildChatPlayers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Deque<String>> offlineCache = new ConcurrentHashMap<>();
    private int offlineCacheSize = 20;

    // --- 已缓存的格式模板（由 config reload 时更新） ---
    private volatile String chatFormat = "&7[&bGuild&7]&r {role} {player}&f: {message}";
    private volatile String roleLeader = "&c&lLeader";
    private volatile String roleOfficer = "&6Officer";
    private volatile String roleMember  = "&aMember";
    private volatile boolean allowColorCodes = true;

    public GuildChatManager(GuildPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    /** 从 ConfigManager 重新读取配置 */
    public void reloadConfig() {
        var cfg = plugin.getConfigManager().getMainConfig();
        this.chatFormat = cfg.getString("guild-chat.format", chatFormat);
        this.roleLeader  = cfg.getString("guild-chat.role-leader", roleLeader);
        this.roleOfficer = cfg.getString("guild-chat.role-officer", roleOfficer);
        this.roleMember  = cfg.getString("guild-chat.role-member", roleMember);
        this.allowColorCodes = cfg.getBoolean("guild-chat.allow-color-codes", true);
        this.offlineCacheSize = cfg.getInt("guild-chat.offline-cache-size", 20);
    }

    // ==================== 聊天模式切换 ====================

    /** 开启/切换公会聊天模式 */
    public boolean toggleChatMode(Player player) {
        if (guildChatPlayers.contains(player.getUniqueId())) {
            guildChatPlayers.remove(player.getUniqueId());
            return false;
        } else {
            guildChatPlayers.add(player.getUniqueId());
            return true;
        }
    }

    /** 检查玩家是否处于公会聊天模式 */
    public boolean isInGuildChat(UUID uuid) {
        return guildChatPlayers.contains(uuid);
    }

    /** 玩家退出时清理 */
    public void removePlayer(UUID uuid) {
        guildChatPlayers.remove(uuid);
    }

    // ==================== 消息格式化 ====================

    /**
     * 格式化公会聊天消息
     * @param player 发送者
     * @param role   成员角色
     * @param rawMessage 原始消息（可能含 & 颜色代码）
     * @return 格式化后的消息
     */
    public String formatMessage(Player player, GuildMember.Role role, String rawMessage) {
        String rolePrefix = switch (role) {
            case LEADER  -> roleLeader;
            case OFFICER -> roleOfficer;
            case MEMBER  -> roleMember;
        };

        String msg = rawMessage;
        if (!allowColorCodes) {
            msg = stripColorCodes(msg);
        }

        return chatFormat
            .replace("{role}", rolePrefix)
            .replace("{player}", player.getName())
            .replace("{message}", msg);
    }

    /** 去除 & 颜色代码（保留已翻译的 § 代码） */
    private String stripColorCodes(String input) {
        return input.replaceAll("&[0-9a-fA-Fk-oK-OrR]", "");
    }

    // ==================== 离线消息缓存 ====================

    /** 缓存发给离线玩家的公会消息 */
    public void cacheOfflineMessage(UUID targetUuid, String formattedMessage) {
        Deque<String> queue = offlineCache.computeIfAbsent(targetUuid, k -> new ArrayDeque<>(offlineCacheSize));
        synchronized (queue) {
            if (queue.size() >= offlineCacheSize) {
                queue.pollFirst();
            }
            queue.addLast(formattedMessage);
        }
    }

    /** 玩家上线时发送缓存的离线消息 */
    public void deliverOfflineMessages(Player player) {
        Deque<String> queue = offlineCache.remove(player.getUniqueId());
        if (queue == null || queue.isEmpty()) return;

        player.sendMessage(ColorUtils.colorize("&7--- &bGuild &7chat history &7---"));
        for (String msg : queue) {
            player.sendMessage(ColorUtils.colorize(msg));
        }
        player.sendMessage(ColorUtils.colorize("&7--- &7end &7---"));
    }

    // ==================== getters ====================

    public Set<UUID> getGuildChatPlayers() { return guildChatPlayers; }
    public boolean isAllowColorCodes() { return allowColorCodes; }
}
