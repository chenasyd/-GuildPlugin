package com.guild.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * 公会聊天事件 — 允许模块拦截/修改公会聊天消息内容（如敏感词过滤、跨服同步）。
 * 在消息即将广播给公会成员之前触发。
 */
public class GuildChatEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player sender;
    private final int guildId;
    private final String guildName;
    private final Set<UUID> recipients;
    private String message;
    private String format;
    private boolean cancelled;

    public GuildChatEvent(Player sender, int guildId, String guildName,
                          Set<UUID> recipients, String message, String format) {
        super(true); // async
        this.sender = sender;
        this.guildId = guildId;
        this.guildName = guildName;
        this.recipients = recipients;
        this.message = message;
        this.format = format;
    }

    /** 发送者 */
    public Player getSender() { return sender; }

    /** 公会 ID */
    public int getGuildId() { return guildId; }

    /** 公会名称 */
    public String getGuildName() { return guildName; }

    /** 接收者 UUID 集合（可修改） */
    public Set<UUID> getRecipients() { return recipients; }

    /** 消息正文（可被模块修改） */
    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    /** 格式化模板（可被模块修改） */
    public String getFormat() { return format; }

    public void setFormat(String format) { this.format = format; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
