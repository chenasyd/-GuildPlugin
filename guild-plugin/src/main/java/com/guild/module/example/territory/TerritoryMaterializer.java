package com.guild.module.example.territory;

import com.guild.core.module.ModuleContext;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.sdk.data.MemberData;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 启动/世界加载时，将本机 DB 元数据 materialize 为 WorldGuard 区域。
 */
public final class TerritoryMaterializer {

    private final ModuleContext context;
    private final TerritoryRepository repository;
    private final TerritoryBridge bridge;
    private final TerritorySettings settings;
    private final Logger logger;

    public TerritoryMaterializer(ModuleContext context, TerritoryRepository repository,
                                 TerritoryBridge bridge, TerritorySettings settings) {
        this.context = context;
        this.repository = repository;
        this.bridge = bridge;
        this.settings = settings;
        this.logger = context != null ? context.getLogger() : Logger.getAnonymousLogger();
    }

    public void scheduleMaterializeOnLoad() {
        if (!isEnabled()) {
            return;
        }
        CompatibleScheduler.runTask(context.getPlugin(), () -> materializeLocalRecords(null));
    }

    public void scheduleMaterializeForWorld(String worldName) {
        if (!isEnabled() || !settings.isMaterializeOnWorldLoad()) {
            return;
        }
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        CompatibleScheduler.runTask(context.getPlugin(), () -> materializeLocalRecords(worldName));
    }

    /** 单测 / 管理命令：同步 materialize 指定记录。 */
    TerritoryMaterializeOutcome materializeRecordNow(TerritoryRecord record,
                                                     UUID leaderUuid,
                                                     List<UUID> memberUuids) {
        if (!bridge.isOperational() || record == null) {
            return TerritoryMaterializeOutcome.SKIPPED;
        }
        reconcileExistingRegion(record);
        if (!needsMaterialization(record)) {
            return TerritoryMaterializeOutcome.ALREADY_PRESENT;
        }
        return bridge.materializeFromRecord(record, leaderUuid, memberUuids);
    }

    boolean needsMaterialization(TerritoryRecord record) {
        if (record == null || !repository.isLocalRecord(record)) {
            return false;
        }
        if (Bukkit.getWorld(record.getWorldName()) == null) {
            return false;
        }
        if (bridge.hasRegionForRecord(record)) {
            reconcileExistingRegion(record);
            return false;
        }
        if (record.getSyncState() == TerritorySyncState.FAILED && !settings.isMaterializeRetryFailed()) {
            return false;
        }
        return true;
    }

    private void materializeLocalRecords(String worldFilter) {
        if (!isEnabled() || context == null) {
            return;
        }

        Map<Integer, List<TerritoryRecord>> grouped = new LinkedHashMap<>();
        for (TerritoryRecord record : repository.viewLocal().values()) {
            if (worldFilter != null && !worldFilter.equals(record.getWorldName())) {
                continue;
            }
            if (!needsMaterialization(record)) {
                continue;
            }
            grouped.computeIfAbsent(record.getGuildId(), ignored -> new ArrayList<>()).add(record);
        }

        if (grouped.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, List<TerritoryRecord>> entry : grouped.entrySet()) {
            int guildId = entry.getKey();
            List<TerritoryRecord> records = List.copyOf(entry.getValue());
            context.getApi().getGuildMembers(guildId).whenComplete((members, error) -> {
                if (error != null) {
                    logger.log(Level.WARNING,
                            "Territory materialize skipped members for guild " + guildId + ": "
                                    + error.getMessage(), error);
                }
                UUID leaderUuid = TerritoryMemberSync.findLeaderUuid(
                        members != null ? members : List.of());
                List<UUID> memberUuids = toMemberUuids(members);
                CompatibleScheduler.runTask(context.getPlugin(), () -> {
                    for (TerritoryRecord record : records) {
                        if (!needsMaterialization(record)) {
                            continue;
                        }
                        TerritoryMaterializeOutcome outcome = bridge.materializeFromRecord(
                                record, leaderUuid, memberUuids);
                        logger.fine(() -> "Territory materialize guild=" + record.getGuildId()
                                + " world=" + record.getWorldName() + " -> " + outcome);
                    }
                });
            });
        }
    }

    private void reconcileExistingRegion(TerritoryRecord record) {
        if (record.getSyncState() == TerritorySyncState.MATERIALIZED) {
            return;
        }
        if (!bridge.hasRegionForRecord(record)) {
            return;
        }
        long now = System.currentTimeMillis();
        repository.put(record.withSyncState(TerritorySyncState.MATERIALIZED, now));
    }

    private static List<UUID> toMemberUuids(List<MemberData> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<UUID> uuids = new ArrayList<>(members.size());
        for (MemberData member : members) {
            if (member.getPlayerUuid() != null) {
                uuids.add(member.getPlayerUuid());
            }
        }
        return uuids;
    }

    private boolean isEnabled() {
        return settings.isMaterializeOnLoad() && bridge.isOperational();
    }
}
