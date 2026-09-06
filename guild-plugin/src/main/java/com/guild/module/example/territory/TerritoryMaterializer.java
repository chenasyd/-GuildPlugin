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
    private final TerritoryEventEmitter eventEmitter;
    private final Logger logger;

    public TerritoryMaterializer(ModuleContext context, TerritoryRepository repository,
                                 TerritoryBridge bridge, TerritorySettings settings,
                                 TerritoryEventEmitter eventEmitter) {
        this.context = context;
        this.repository = repository;
        this.bridge = bridge;
        this.settings = settings;
        this.eventEmitter = eventEmitter;
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

    /** 单测 / 管理命令：同步 materialize 指定记录（不受 auto-load 配置限制）。 */
    TerritoryMaterializeOutcome materializeRecordNow(TerritoryRecord record,
                                                     UUID leaderUuid,
                                                     List<UUID> memberUuids) {
        if (!bridge.isOperational() || record == null) {
            return TerritoryMaterializeOutcome.SKIPPED;
        }
        reconcileExistingRegion(record);
        if (bridge.hasRegionForRecord(record)) {
            return TerritoryMaterializeOutcome.ALREADY_PRESENT;
        }
        if (Bukkit.getWorld(record.getWorldName()) == null) {
            return TerritoryMaterializeOutcome.WORLD_NOT_LOADED;
        }
        return bridge.materializeFromRecord(record, leaderUuid, memberUuids);
    }

    private TerritoryMaterializeOutcome materializeAndPublish(TerritoryRecord record,
                                                              UUID leaderUuid,
                                                              List<UUID> memberUuids,
                                                              com.guild.sdk.territory.TerritoryEventSource source) {
        TerritoryMaterializeOutcome outcome = materializeRecordNow(record, leaderUuid, memberUuids);
        publishMaterialized(record, outcome, source);
        return outcome;
    }

    private void publishMaterialized(TerritoryRecord record, TerritoryMaterializeOutcome outcome,
                                     com.guild.sdk.territory.TerritoryEventSource source) {
        if (eventEmitter != null && outcome != null) {
            eventEmitter.fireMaterialized(record, outcome, source);
        }
    }

    /**
     * 管理命令：统计待 materialize 的本机记录数（不执行 WG 写入）。
     */
    public int countAdminTargets(Integer guildIdFilter, String worldFilter, boolean forceFailed) {
        return collectAdminTargets(guildIdFilter, worldFilter, forceFailed).size();
    }

    /**
     * 管理命令：异步拉取成员后 materialize 本机 DB 记录为 WG 区域（不广播跨服）。
     * {@code onComplete} 始终在插件主线程回调。
     */
    public void materializeAdmin(Integer guildIdFilter, String worldFilter, boolean forceFailed,
                                 java.util.function.Consumer<MaterializeSummary> onComplete) {
        if (context == null || !bridge.isOperational()) {
            deliverSummary(onComplete, new MaterializeSummary());
            return;
        }

        List<TerritoryRecord> targets = collectAdminTargets(guildIdFilter, worldFilter, forceFailed);
        if (targets.isEmpty()) {
            deliverSummary(onComplete, new MaterializeSummary());
            return;
        }

        Map<Integer, List<TerritoryRecord>> grouped = new LinkedHashMap<>();
        for (TerritoryRecord record : targets) {
            grouped.computeIfAbsent(record.getGuildId(), ignored -> new ArrayList<>()).add(record);
        }

        MaterializeSummary summary = new MaterializeSummary();
        java.util.concurrent.atomic.AtomicInteger pendingGuilds = new java.util.concurrent.atomic.AtomicInteger(
                grouped.size());

        for (Map.Entry<Integer, List<TerritoryRecord>> entry : grouped.entrySet()) {
            int guildId = entry.getKey();
            List<TerritoryRecord> records = List.copyOf(entry.getValue());
            context.getApi().getGuildMembers(guildId).whenComplete((members, error) -> {
                if (error != null) {
                    logger.log(Level.WARNING,
                            "Territory admin materialize skipped members for guild " + guildId + ": "
                                    + error.getMessage(), error);
                }
                UUID leaderUuid = TerritoryMemberSync.findLeaderUuid(
                        members != null ? members : List.of());
                List<UUID> memberUuids = toMemberUuids(members);
                CompatibleScheduler.runTask(context.getPlugin(), () -> {
                    for (TerritoryRecord record : records) {
                        if (!needsMaterialization(record, forceFailed)) {
                            continue;
                        }
                        summary.record(materializeAndPublish(record, leaderUuid, memberUuids,
                                com.guild.sdk.territory.TerritoryEventSource.ADMIN));
                    }
                    if (pendingGuilds.decrementAndGet() == 0) {
                        deliverSummary(onComplete, summary);
                    }
                });
            });
        }
    }

    /** {@link #materializeAdmin} 结果汇总。 */
    public static final class MaterializeSummary {

        private int attempted;
        private int created;
        private int alreadyPresent;
        private int skipped;
        private int worldNotLoaded;
        private int conflict;
        private int failed;

        void record(TerritoryMaterializeOutcome outcome) {
            attempted++;
            switch (outcome) {
                case CREATED -> created++;
                case ALREADY_PRESENT -> alreadyPresent++;
                case SKIPPED -> skipped++;
                case WORLD_NOT_LOADED -> worldNotLoaded++;
                case CONFLICT -> conflict++;
                case FAILED -> failed++;
                default -> skipped++;
            }
        }

        public int getAttempted() {
            return attempted;
        }

        public int getCreated() {
            return created;
        }

        public int getAlreadyPresent() {
            return alreadyPresent;
        }

        public int getSkipped() {
            return skipped;
        }

        public int getWorldNotLoaded() {
            return worldNotLoaded;
        }

        public int getConflict() {
            return conflict;
        }

        public int getFailed() {
            return failed;
        }
    }

    private void deliverSummary(java.util.function.Consumer<MaterializeSummary> onComplete,
                              MaterializeSummary summary) {
        if (onComplete == null) {
            return;
        }
        if (context != null) {
            CompatibleScheduler.runTask(context.getPlugin(), () -> onComplete.accept(summary));
        } else {
            onComplete.accept(summary);
        }
    }

    private List<TerritoryRecord> collectAdminTargets(Integer guildIdFilter, String worldFilter,
                                                      boolean forceFailed) {
        List<TerritoryRecord> targets = new ArrayList<>();
        for (TerritoryRecord record : repository.viewLocal().values()) {
            if (guildIdFilter != null && record.getGuildId() != guildIdFilter) {
                continue;
            }
            if (worldFilter != null && !worldFilter.equals(record.getWorldName())) {
                continue;
            }
            if (needsMaterialization(record, forceFailed)) {
                targets.add(record);
            }
        }
        return targets;
    }

    boolean needsMaterialization(TerritoryRecord record) {
        return needsMaterialization(record, false);
    }

    boolean needsMaterialization(TerritoryRecord record, boolean forceFailed) {
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
        if (record.getSyncState() == TerritorySyncState.FAILED
                && !settings.isMaterializeRetryFailed()
                && !forceFailed) {
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
                        publishMaterialized(record, outcome,
                                com.guild.sdk.territory.TerritoryEventSource.SYSTEM);
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
