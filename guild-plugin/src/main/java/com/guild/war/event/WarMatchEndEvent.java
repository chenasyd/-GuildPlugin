package com.guild.war.event;

import com.guild.war.model.WarReportSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 工会战结算事件（同步、不可取消）。
 * 在删图 / unregister 之前触发；扩展插件应监听此事件发奖或记分。
 */
public final class WarMatchEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final WarReportSnapshot snapshot;

    public WarMatchEndEvent(WarReportSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public WarReportSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
