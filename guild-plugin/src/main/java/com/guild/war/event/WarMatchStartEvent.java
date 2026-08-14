package com.guild.war.event;

import com.guild.war.model.WarMatch;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** 公会战进入 ACTIVE 时触发（同步、不可取消）。 */
public final class WarMatchStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final WarMatch match;

    public WarMatchStartEvent(WarMatch match) {
        this.match = match;
    }

    public WarMatch getMatch() {
        return match;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
