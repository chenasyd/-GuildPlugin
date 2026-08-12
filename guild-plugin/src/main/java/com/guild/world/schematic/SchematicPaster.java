package com.guild.world.schematic;

import com.guild.core.utils.CompatibleScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * 将 {@link SchematicData} 粘贴到世界（按区块分组，Folia 走区域线程）。
 */
public final class SchematicPaster {

    private SchematicPaster() {
    }

    /**
     * @param pasteAt schematic.origin 对齐到的世界坐标
     * @param ignoreAir 是否跳过空气方块
     */
    public static CompletableFuture<Void> pasteAsync(
            JavaPlugin plugin,
            World world,
            Location pasteAt,
            SchematicData data,
            boolean ignoreAir,
            boolean applyBlockEntities,
            Logger logger
    ) {
        if (world == null || pasteAt == null || data == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid paste args"));
        }

        List<BlockData> palette = new ArrayList<>(data.palette.size());
        for (String state : data.palette) {
            palette.add(Bukkit.createBlockData(state));
        }

        int ox = pasteAt.getBlockX() - data.origin.x();
        int oy = pasteAt.getBlockY() - data.origin.y();
        int oz = pasteAt.getBlockZ() - data.origin.z();
        int dx = data.size.dx();
        int dy = data.size.dy();
        int dz = data.size.dz();

        Map<Long, List<Voxel>> blocksByChunk = new HashMap<>();
        int y = 0, z = 0, x = 0, r = 0, runRemaining = 0, idx = 0;
        while (y < dy) {
            if (runRemaining == 0) {
                if (r >= data.blocks.size()) {
                    break;
                }
                int[] entry = data.blocks.get(r++);
                idx = entry[0];
                runRemaining = entry[1];
            }
            runRemaining--;
            BlockData bd = palette.get(idx);
            boolean air = bd.getMaterial().isAir();
            if (!(ignoreAir && air)) {
                int wx = ox + x;
                int wy = oy + y;
                int wz = oz + z;
                long key = (((long) (wx >> 4)) << 32) ^ ((wz >> 4) & 0xffffffffL);
                blocksByChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(new Voxel(wx, wy, wz, bd));
            }
            if (++x >= dx) {
                x = 0;
                if (++z >= dz) {
                    z = 0;
                    ++y;
                }
            }
        }

        Map<Long, List<SchematicData.BlockEntityData>> besByChunk = new HashMap<>();
        if (applyBlockEntities && data.blockEntities != null) {
            for (SchematicData.BlockEntityData be : data.blockEntities) {
                int wx = ox + be.x;
                int wy = oy + be.y;
                int wz = oz + be.z;
                long key = (((long) (wx >> 4)) << 32) ^ ((wz >> 4) & 0xffffffffL);
                SchematicData.BlockEntityData copy = new SchematicData.BlockEntityData();
                copy.x = wx;
                copy.y = wy;
                copy.z = wz;
                copy.kind = be.kind;
                copy.data = be.data;
                besByChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(copy);
            }
        }

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for (var e : blocksByChunk.entrySet()) {
            final int cx = (int) (e.getKey() >> 32);
            final int cz = (int) e.getKey().longValue();
            final List<Voxel> voxels = e.getValue();
            final List<SchematicData.BlockEntityData> bes = besByChunk.getOrDefault(e.getKey(), List.of());
            CompletableFuture<Void> cf = new CompletableFuture<>();
            tasks.add(cf);
            Location sample = new Location(world, (cx << 4) + 8, pasteAt.getBlockY(), (cz << 4) + 8);
            CompatibleScheduler.runTask(plugin, sample, () -> {
                try {
                    world.getChunkAt(cx, cz);
                    for (Voxel v : voxels) {
                        world.getBlockAt(v.x, v.y, v.z).setBlockData(v.bd, false);
                    }
                    for (SchematicData.BlockEntityData be : bes) {
                        Block block = world.getBlockAt(be.x, be.y, be.z);
                        BlockState state = block.getState();
                        TileStateIO.apply(state, be.data, logger);
                    }
                    cf.complete(null);
                } catch (Throwable t) {
                    cf.completeExceptionally(t);
                }
            });
        }

        if (tasks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).thenRun(() -> {
            if (logger != null) {
                logger.info("[World] Schematic pasted at " + pasteAt.getBlockX() + ","
                        + pasteAt.getBlockY() + "," + pasteAt.getBlockZ()
                        + " chunks=" + blocksByChunk.size());
            }
        });
    }

    /** 由 schematic 相对偏移计算世界坐标（相对 origin）。 */
    public static Location offsetToWorld(Location pasteAt, SchematicData data, double dx, double dy, double dz,
                                        float yaw, float pitch) {
        double wx = pasteAt.getX() + (dx - data.origin.x());
        double wy = pasteAt.getY() + (dy - data.origin.y());
        double wz = pasteAt.getZ() + (dz - data.origin.z());
        return new Location(pasteAt.getWorld(), wx, wy, wz, yaw, pitch);
    }

    private record Voxel(int x, int y, int z, BlockData bd) {
    }
}
