package com.guild.sdk;

import com.guild.sdk.event.EconomyEventData;
import com.guild.sdk.event.EconomyEventHandler;
import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventData;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.event.MemberRoleChangeEventData;
import com.guild.sdk.event.MemberRoleChangeEventHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 模块 SDK 事件订阅与分发。
 */
public final class ModuleEventBus {

    private final Logger logger;
    private final List<GuildEventHandler> onGuildCreateHandlers = new CopyOnWriteArrayList<>();
    private final List<GuildEventHandler> onGuildDeleteHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberEventHandler> onMemberJoinHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberEventHandler> onMemberLeaveHandlers = new CopyOnWriteArrayList<>();
    private final List<EconomyEventHandler> onEconomyDepositHandlers = new CopyOnWriteArrayList<>();
    private final List<EconomyEventHandler> onEconomyWithdrawHandlers = new CopyOnWriteArrayList<>();
    private final List<MemberRoleChangeEventHandler> onMemberRoleChangeHandlers = new CopyOnWriteArrayList<>();

    public ModuleEventBus(Logger logger) {
        this.logger = logger;
    }

    public void onGuildCreate(GuildEventHandler handler) {
        onGuildCreateHandlers.add(handler);
    }

    public void onGuildDelete(GuildEventHandler handler) {
        onGuildDeleteHandlers.add(handler);
    }

    public void onMemberJoin(MemberEventHandler handler) {
        onMemberJoinHandlers.add(handler);
    }

    public void onMemberLeave(MemberEventHandler handler) {
        onMemberLeaveHandlers.add(handler);
    }

    public void onEconomyDeposit(EconomyEventHandler handler) {
        onEconomyDepositHandlers.add(handler);
    }

    public void onEconomyWithdraw(EconomyEventHandler handler) {
        onEconomyWithdrawHandlers.add(handler);
    }

    public void onMemberRoleChange(MemberRoleChangeEventHandler handler) {
        onMemberRoleChangeHandlers.add(handler);
    }

    public void fireGuildCreate(int guildId, String guildName, String leaderName) {
        dispatch(onGuildCreateHandlers, new GuildEventData(guildId, guildName, leaderName),
                "onGuildCreate", GuildEventHandler::onEvent);
    }

    public void fireGuildDelete(int guildId, String guildName, String leaderName) {
        dispatch(onGuildDeleteHandlers, new GuildEventData(guildId, guildName, leaderName),
                "onGuildDelete", GuildEventHandler::onEvent);
    }

    public void fireMemberJoin(int guildId, String guildName, UUID playerUuid, String playerName) {
        dispatch(onMemberJoinHandlers,
                new MemberEventData(guildId, guildName, playerUuid, playerName, "JOIN"),
                "onMemberJoin", MemberEventHandler::onEvent);
    }

    public void fireMemberLeave(int guildId, String guildName, UUID playerUuid, String playerName, String eventType) {
        dispatch(onMemberLeaveHandlers,
                new MemberEventData(guildId, guildName, playerUuid, playerName, eventType),
                "onMemberLeave", MemberEventHandler::onEvent);
    }

    public void fireEconomyDeposit(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        dispatch(onEconomyDepositHandlers,
                new EconomyEventData(guildId, guildName, playerUuid, playerName, amount, "DEPOSIT"),
                "onEconomyDeposit", EconomyEventHandler::onEvent);
    }

    public void fireEconomyWithdraw(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        dispatch(onEconomyWithdrawHandlers,
                new EconomyEventData(guildId, guildName, playerUuid, playerName, amount, "WITHDRAW"),
                "onEconomyWithdraw", EconomyEventHandler::onEvent);
    }

    public void fireMemberRoleChange(int guildId, String guildName, UUID playerUuid, String playerName,
                                     String oldRole, String newRole) {
        dispatch(onMemberRoleChangeHandlers,
                new MemberRoleChangeEventData(guildId, guildName, playerUuid, playerName, oldRole, newRole),
                "onMemberRoleChange", MemberRoleChangeEventHandler::onEvent);
    }

    private <H, D> void dispatch(List<H> handlers, D data, String eventLabel, BiConsumer<H, D> invoker) {
        if (handlers.isEmpty()) {
            return;
        }
        for (H handler : handlers) {
            try {
                invoker.accept(handler, data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in " + eventLabel + " handler: " + e.getMessage(), e);
            }
        }
    }

    public void clearModuleHandlers(Object moduleInstance) {
        onGuildCreateHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onGuildDeleteHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onMemberJoinHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onMemberLeaveHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onEconomyDepositHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onEconomyWithdrawHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
        onMemberRoleChangeHandlers.removeIf(h -> h.getModuleInstance() == moduleInstance);
    }

    public void clearAll() {
        onGuildCreateHandlers.clear();
        onGuildDeleteHandlers.clear();
        onMemberJoinHandlers.clear();
        onMemberLeaveHandlers.clear();
        onEconomyDepositHandlers.clear();
        onEconomyWithdrawHandlers.clear();
        onMemberRoleChangeHandlers.clear();
    }
}
