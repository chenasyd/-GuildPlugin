package com.guild.module.example.territory;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 基于 WorldGuard API 的公会领地桥接实现。
 * <p>
 * 所有公开方法应在 Bukkit 主线程调用（读写 WG 区域存储）。
 */
public final class WorldGuardTerritoryBridge implements TerritoryBridge {

    private static final String REGION_PREFIX = "guild_";

    private final TerritoryRepository repository;
    private final Logger logger;
    private final TerritorySettings settings;
    private final TerritoryCrossServerSync crossServerSync;

    public WorldGuardTerritoryBridge(TerritoryRepository repository, Logger logger,
                                     TerritorySettings settings,
                                     TerritoryCrossServerSync crossServerSync) {
        this.repository = repository;
        this.logger = logger;
        this.settings = settings;
        this.crossServerSync = crossServerSync;
    }

    @Override
    public boolean isOperational() {
        return WorldGuardProbe.probe().fullyReady();
    }

    @Override
    public Optional<TerritoryRecord> claimTerritory(TerritoryClaimRequest request) {
        RegionManager manager = resolveManager(request.getWorldName());
        if (manager == null) {
            logger.warning("Cannot claim territory: world not loaded or regions disabled: "
                    + request.getWorldName());
            return Optional.empty();
        }

        String regionId = request.regionId();
        if (manager.hasRegion(regionId)) {
            logger.info("Territory claim rejected: region already exists: " + regionId);
            return Optional.empty();
        }

        TerritoryBounds.Normalized bounds = TerritoryBounds.fromRequest(request);
        ProtectedCuboidRegion candidate = new ProtectedCuboidRegion(
                regionId, bounds.minVector(), bounds.maxVector());

        if (hasConflictingRegion(manager, candidate, regionId)) {
            logger.info("Territory claim rejected: overlaps existing region for guild "
                    + request.getGuildId());
            return Optional.empty();
        }

        applyMembership(candidate, request.getLeaderUuid(), request.getMemberUuids());
        TerritoryFlagDefaults.apply(candidate, settings);

        manager.addRegion(candidate);
        try {
            if (!manager.saveChanges()) {
                manager.save();
            }
        } catch (StorageException e) {
            manager.removeRegion(regionId);
            logger.log(Level.SEVERE, "Failed to persist WG region " + regionId, e);
            return Optional.empty();
        }

        TerritoryRecord record = toRecord(request, bounds, System.currentTimeMillis());
        repository.put(record);
        repository.save();
        publishClaim(record);
        return Optional.of(record);
    }

    @Override
    public boolean unclaimTerritory(int guildId, String worldName) {
        RegionManager manager = resolveManager(worldName);
        if (manager == null) {
            return false;
        }

        String regionId = TerritoryRecord.defaultRegionId(guildId);
        Optional<TerritoryRecord> existing = repository.getLocal(guildId, worldName);
        long revision = System.currentTimeMillis();
        if (!manager.hasRegion(regionId)) {
            repository.remove(guildId, worldName);
            repository.save();
            publishUnclaim(guildId, existing, worldName, revision);
            return false;
        }

        manager.removeRegion(regionId);
        try {
            if (!manager.saveChanges()) {
                manager.save();
            }
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to remove WG region " + regionId, e);
            return false;
        }

        repository.remove(guildId, worldName);
        repository.save();
        publishUnclaim(guildId, existing, worldName, revision);
        return true;
    }

    private void publishClaim(TerritoryRecord record) {
        if (crossServerSync != null) {
            crossServerSync.publishClaim(record);
        }
    }

    private void publishUnclaim(int guildId, Optional<TerritoryRecord> existing,
                                String worldName, long revision) {
        if (crossServerSync == null) {
            return;
        }
        String serverId = existing.map(TerritoryRecord::getServerId)
                .filter(id -> id != null && !id.isBlank())
                .orElseGet(repository::getLocalServerId);
        crossServerSync.publishUnclaim(guildId, serverId, worldName, revision);
    }

    @Override
    public void syncMembers(int guildId, String worldName, Collection<UUID> memberUuids, UUID leaderUuid) {
        RegionManager manager = resolveManager(worldName);
        if (manager == null) {
            return;
        }

        ProtectedRegion region = manager.getRegion(TerritoryRecord.defaultRegionId(guildId));
        if (region == null) {
            return;
        }

        applyMembership(region, leaderUuid, memberUuids);
        try {
            if (!manager.saveChanges()) {
                manager.save();
            }
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to sync members for guild " + guildId, e);
        }
    }

    @Override
    public Optional<TerritoryRecord> findTerritory(int guildId, String worldName) {
        Optional<TerritoryRecord> cached = repository.get(guildId, worldName);
        if (cached.isPresent()) {
            return cached;
        }

        RegionManager manager = resolveManager(worldName);
        if (manager == null) {
            return Optional.empty();
        }

        ProtectedRegion region = manager.getRegion(TerritoryRecord.defaultRegionId(guildId));
        if (region == null) {
            return Optional.empty();
        }

        TerritoryRecord record = fromRegion(region, guildId, worldName, null);
        repository.put(record);
        repository.save();
        return Optional.of(record);
    }

    @Override
    public Optional<TerritoryRecord> findTerritoryAt(String worldName, int x, int y, int z) {
        RegionManager manager = resolveManager(worldName);
        if (manager == null) {
            return Optional.empty();
        }

        BlockVector3 point = BlockVector3.at(x, y, z);
        for (ProtectedRegion region : manager.getApplicableRegions(point).getRegions()) {
            if (ProtectedRegion.GLOBAL_REGION.equals(region.getId())) {
                continue;
            }
            Integer guildId = parseGuildId(region.getId());
            if (guildId == null) {
                continue;
            }
            Optional<TerritoryRecord> known = repository.get(guildId, worldName);
            if (known.isPresent()) {
                return known;
            }
            TerritoryRecord record = fromRegion(region, guildId, worldName, null);
            repository.put(record);
            return Optional.of(record);
        }
        return Optional.empty();
    }

    @Override
    public boolean hasRegionForRecord(TerritoryRecord record) {
        if (record == null) {
            return false;
        }
        RegionManager manager = resolveManager(record.getWorldName());
        if (manager == null) {
            return false;
        }
        return manager.hasRegion(resolveRegionId(record));
    }

    @Override
    public TerritoryMaterializeOutcome materializeFromRecord(TerritoryRecord record,
                                                           UUID leaderUuid,
                                                           Collection<UUID> memberUuids) {
        if (record == null) {
            return TerritoryMaterializeOutcome.SKIPPED;
        }

        RegionManager manager = resolveManager(record.getWorldName());
        if (manager == null) {
            return TerritoryMaterializeOutcome.WORLD_NOT_LOADED;
        }

        String regionId = resolveRegionId(record);
        if (manager.hasRegion(regionId)) {
            markMaterialized(record);
            return TerritoryMaterializeOutcome.ALREADY_PRESENT;
        }

        TerritoryBounds.Normalized bounds = TerritoryBounds.fromRecord(record);
        ProtectedCuboidRegion candidate = new ProtectedCuboidRegion(
                regionId, bounds.minVector(), bounds.maxVector());

        if (hasConflictingRegion(manager, candidate, regionId)) {
            logger.warning("Territory materialize rejected: overlap for guild "
                    + record.getGuildId() + " in " + record.getWorldName());
            markFailed(record);
            return TerritoryMaterializeOutcome.CONFLICT;
        }

        applyMembership(candidate, leaderUuid, memberUuids);
        TerritoryFlagDefaults.apply(candidate, settings);

        manager.addRegion(candidate);
        try {
            if (!manager.saveChanges()) {
                manager.save();
            }
        } catch (StorageException e) {
            manager.removeRegion(regionId);
            logger.log(Level.SEVERE, "Failed to materialize WG region " + regionId, e);
            markFailed(record);
            return TerritoryMaterializeOutcome.FAILED;
        }

        markMaterialized(record);
        logger.info("Materialized WG territory for guild " + record.getGuildId()
                + " in " + record.getWorldName() + " (" + regionId + ")");
        return TerritoryMaterializeOutcome.CREATED;
    }

    private void markMaterialized(TerritoryRecord record) {
        if (record.getSyncState() == TerritorySyncState.MATERIALIZED) {
            return;
        }
        long now = System.currentTimeMillis();
        repository.put(record.withSyncState(TerritorySyncState.MATERIALIZED, now));
    }

    private void markFailed(TerritoryRecord record) {
        long now = System.currentTimeMillis();
        repository.put(record.withSyncState(TerritorySyncState.FAILED, now));
    }

    private static String resolveRegionId(TerritoryRecord record) {
        String regionId = record.getRegionId();
        if (regionId == null || regionId.isBlank()) {
            return TerritoryRecord.defaultRegionId(record.getGuildId());
        }
        return regionId;
    }

    static Integer parseGuildId(String regionId) {
        if (regionId == null || !regionId.startsWith(REGION_PREFIX)) {
            return null;
        }
        try {
            return Integer.parseInt(regionId.substring(REGION_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void applyMembership(ProtectedRegion region, UUID leaderUuid, Collection<UUID> memberUuids) {
        DefaultDomain owners = new DefaultDomain();
        if (leaderUuid != null) {
            owners.addPlayer(leaderUuid);
        }
        region.setOwners(owners);

        DefaultDomain members = new DefaultDomain();
        if (memberUuids != null) {
            for (UUID uuid : memberUuids) {
                if (uuid == null || uuid.equals(leaderUuid)) {
                    continue;
                }
                members.addPlayer(uuid);
            }
        }
        region.setMembers(members);
    }

    private static boolean hasConflictingRegion(RegionManager manager, ProtectedRegion candidate, String regionId) {
        ApplicableRegionSet overlapping = manager.getApplicableRegions(candidate);
        for (ProtectedRegion existing : overlapping.getRegions()) {
            if (ProtectedRegion.GLOBAL_REGION.equals(existing.getId())) {
                continue;
            }
            if (regionId.equals(existing.getId())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private TerritoryRecord toRecord(TerritoryClaimRequest request,
                                     TerritoryBounds.Normalized bounds,
                                     long claimedAt) {
        return new TerritoryRecord(
                request.getGuildId(),
                request.getGuildName(),
                request.regionId(),
                request.getServerId().isBlank() ? repository.getLocalServerId() : request.getServerId(),
                request.getWorldName(),
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ(),
                claimedAt,
                TerritorySyncState.MATERIALIZED,
                claimedAt
        );
    }

    private TerritoryRecord fromRegion(ProtectedRegion region, int guildId, String worldName, String guildName) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        long now = System.currentTimeMillis();
        return new TerritoryRecord(
                guildId,
                guildName != null ? guildName : ("guild-" + guildId),
                region.getId(),
                repository.getLocalServerId(),
                worldName,
                min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                max.getBlockX(), max.getBlockY(), max.getBlockZ(),
                now,
                TerritorySyncState.MATERIALIZED,
                now
        );
    }

    private RegionManager resolveManager(String worldName) {
        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            return null;
        }
        World world = BukkitAdapter.adapt(Bukkit.getWorld(worldName));
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(world);
    }
}
