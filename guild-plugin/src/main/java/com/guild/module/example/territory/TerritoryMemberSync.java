package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.data.MemberData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.event.MemberRoleChangeEventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 监听公会成员变更，将 WG 区域 members/owners 与数据库成员列表保持同步。
 */
public final class TerritoryMemberSync {

    private final ModuleContext context;
    private final TerritoryBridge bridge;
    private final TerritoryRepository repository;
    private final Object moduleInstance;

    public TerritoryMemberSync(ModuleContext context, TerritoryBridge bridge,
                               TerritoryRepository repository, Object moduleInstance) {
        this.context = context;
        this.bridge = bridge;
        this.repository = repository;
        this.moduleInstance = moduleInstance;
    }

    public void register(GuildPluginAPI api) {
        if (!isSyncEnabled()) {
            context.getLogger().info("Territory member sync disabled by config.");
            return;
        }

        api.onMemberJoin(new MemberEventHandler() {
            @Override
            public void onEvent(com.guild.sdk.event.MemberEventData data) {
                scheduleResync(data.getGuildId(), "join");
            }

            @Override
            public Object getModuleInstance() {
                return moduleInstance;
            }
        });

        api.onMemberLeave(new MemberEventHandler() {
            @Override
            public void onEvent(com.guild.sdk.event.MemberEventData data) {
                scheduleResync(data.getGuildId(), "leave:" + data.getEventType());
            }

            @Override
            public Object getModuleInstance() {
                return moduleInstance;
            }
        });

        api.onMemberRoleChange(new MemberRoleChangeEventHandler() {
            @Override
            public void onEvent(com.guild.sdk.event.MemberRoleChangeEventData data) {
                scheduleResync(data.getGuildId(), "role:" + data.getOldRole() + "->" + data.getNewRole());
            }

            @Override
            public Object getModuleInstance() {
                return moduleInstance;
            }
        });

        api.onGuildDelete(new GuildEventHandler() {
            @Override
            public void onEvent(com.guild.sdk.event.GuildEventData data) {
                scheduleUnclaimAll(data.getGuildId(), "guild-delete");
            }

            @Override
            public Object getModuleInstance() {
                return moduleInstance;
            }
        });
    }

    /** 模块加载后可选全量修复（配置 {@code member-sync.repair-on-load}）。 */
    public void repairAllOnLoad() {
        if (!isSyncEnabled() || !bridge.isOperational()) {
            return;
        }
        if (!context.getConfig().getBoolean("member-sync.repair-on-load", false)) {
            return;
        }
        repairSyncAll();
    }

    /** 管理命令：修复指定公会在所有世界的 WG 成员列表。 */
    public void repairSync(int guildId) {
        if (!isSyncEnabled() || !bridge.isOperational() || guildId <= 0) {
            return;
        }
        scheduleResync(guildId, "admin-repair");
    }

    /** 管理命令：修复所有已登记领地的公会；返回触发的公会数量。 */
    public int repairSyncAll() {
        if (!isSyncEnabled() || !bridge.isOperational()) {
            return 0;
        }
        int[] guildIds = repository.viewLocal().values().stream()
                .mapToInt(TerritoryRecord::getGuildId)
                .distinct()
                .toArray();
        for (int guildId : guildIds) {
            scheduleResync(guildId, "admin-repair-all");
        }
        return guildIds.length;
    }

    void scheduleResync(int guildId, String reason) {
        if (!bridge.isOperational()) {
            return;
        }

        List<TerritoryRecord> territories = repository.findByGuildIdLocal(guildId);
        if (territories.isEmpty()) {
            return;
        }

        context.getApi().getGuildMembers(guildId).whenComplete((members, error) -> {
            if (error != null) {
                logger().log(Level.WARNING,
                        "Territory member sync failed to load members for guild " + guildId + " (" + reason + ")",
                        error);
                return;
            }
            if (members == null || members.isEmpty()) {
                logger().fine(() -> "Territory member sync skipped: guild " + guildId + " has no members");
                return;
            }

            UUID leaderUuid = findLeaderUuid(members);
            List<UUID> memberUuids = new ArrayList<>(members.size());
            for (MemberData member : members) {
                if (member.getPlayerUuid() != null) {
                    memberUuids.add(member.getPlayerUuid());
                }
            }

            CompatibleScheduler.runTask(context.getPlugin(), () -> {
                for (TerritoryRecord territory : territories) {
                    bridge.syncMembers(guildId, territory.getWorldName(), memberUuids, leaderUuid);
                }
                logger().fine(() -> "Territory member sync applied for guild " + guildId
                        + " (" + reason + ", worlds=" + territories.size() + ")");
            });
        });
    }

    private void scheduleUnclaimAll(int guildId, String reason) {
        if (!bridge.isOperational()) {
            repository.removeAllForGuild(guildId);
            repository.save();
            return;
        }

        List<TerritoryRecord> territories = new ArrayList<>(repository.findByGuildId(guildId));
        if (territories.isEmpty()) {
            return;
        }

        CompatibleScheduler.runTask(context.getPlugin(), () -> {
            for (TerritoryRecord territory : territories) {
                if (repository.isLocalRecord(territory)) {
                    bridge.unclaimTerritory(guildId, territory.getWorldName());
                }
            }
            repository.removeAllForGuild(guildId);
            logger().info("Territory unclaimed for dissolved guild " + guildId + " (" + reason + ")");
        });
    }

    static UUID findLeaderUuid(List<MemberData> members) {
        for (MemberData member : members) {
            if (member.getPlayerUuid() != null && "LEADER".equalsIgnoreCase(member.getRole())) {
                return member.getPlayerUuid();
            }
        }
        for (MemberData member : members) {
            if (member.getPlayerUuid() != null) {
                return member.getPlayerUuid();
            }
        }
        return null;
    }

    private boolean isSyncEnabled() {
        return context.getConfig().getBoolean("member-sync.enabled", true);
    }

    private Logger logger() {
        return context.getLogger();
    }
}
