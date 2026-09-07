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
        if (onGuildCreateHandlers.isEmpty()) {
            return;
        }
        GuildEventData data = new GuildEventData(guildId, guildName, leaderName);
        for (GuildEventHandler handler : onGuildCreateHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onGuildCreate handler: " + e.getMessage(), e);
            }
        }
    }

    public void fireGuildDelete(int guildId, String guildName, String leaderName) {
        if (onGuildDeleteHandlers.isEmpty()) {
            return;
        }
        GuildEventData data = new GuildEventData(guildId, guildName, leaderName);
        for (GuildEventHandler handler : onGuildDeleteHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onGuildDelete handler: " + e.getMessage(), e);
            }
        }
    }

    public void fireMemberJoin(int guildId, String guildName, UUID playerUuid, String playerName) {
        if (onMemberJoinHandlers.isEmpty()) {
            return;
        }
        MemberEventData data = new MemberEventData(guildId, guildName, playerUuid, playerName, "JOIN");
        for (MemberEventHandler handler : onMemberJoinHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onMemberJoin handler: " + e.getMessage(), e);
            }
        }
    }

    public void fireMemberLeave(int guildId, String guildName, UUID playerUuid, String playerName, String eventType) {
        if (onMemberLeaveHandlers.isEmpty()) {
            return;
        }
        MemberEventData data = new MemberEventData(guildId, guildName, playerUuid, playerName, eventType);
        for (MemberEventHandler handler : onMemberLeaveHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onMemberLeave handler: " + e.getMessage(), e);
            }
        }
    }

    public void fireEconomyDeposit(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        if (onEconomyDepositHandlers.isEmpty()) {
            return;
        }
        EconomyEventData data = new EconomyEventData(guildId, guildName, playerUuid, playerName, amount, "DEPOSIT");
        for (EconomyEventHandler handler : onEconomyDepositHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onEconomyDeposit handler: " + e.getMessage(), e);
            }
        }
    }

    public void fireEconomyWithdraw(int guildId, String guildName, UUID playerUuid, String playerName, double amount) {
        if (onEconomyWithdrawHandlers.isEmpty()) {
            return;
        }
        EconomyEventData data = new EconomyEventData(guildId, guildName, playerUuid, playerName, amount, "WITHDRAW");
        for (EconomyEventHandler handler : onEconomyWithdrawHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onEconomyWithdraw handler: " + e.getMessage(), e);
            }
        }
    }

    public void fireMemberRoleChange(int guildId, String guildName, UUID playerUuid, String playerName,
                                     String oldRole, String newRole) {
        if (onMemberRoleChangeHandlers.isEmpty()) {
            return;
        }
        MemberRoleChangeEventData data = new MemberRoleChangeEventData(
                guildId, guildName, playerUuid, playerName, oldRole, newRole);
        for (MemberRoleChangeEventHandler handler : onMemberRoleChangeHandlers) {
            try {
                handler.onEvent(data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onMemberRoleChange handler: " + e.getMessage(), e);
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
