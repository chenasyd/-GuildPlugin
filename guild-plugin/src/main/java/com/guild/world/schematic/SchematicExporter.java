package com.guild.world.schematic;

import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ServerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 从世界选区导出 {@link SchematicData}（Folia 按区块区域线程读取）。
 */
public final class SchematicExporter {

    private SchematicExporter() {
    }

    public static CompletableFuture<SchematicData> exportAsync(
            JavaPlugin plugin,
            World world,
            int x1, int y1, int z1,
            int x2, int y2, int z2,
            Vec3i originRel,
            boolean includeBlockEntities,
            int maxVolume,
            Logger logger
    ) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);
        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;
        long volume = (long) dx * dy * dz;
        if (volume <= 0 || volume > maxVolume) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Selection volume " + volume + " exceeds limit " + maxVolume));
        }

        Vec3i origin = clampOrigin(originRel, dx, dy, dz);

        // 按 chunk 收集原始方块字符串与 tile
        Map<Long, List<Captured>> byChunk = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        int minCx = minX >> 4;
        int maxCx = maxX >> 4;
        int minCz = minZ >> 4;
        int maxCz = maxZ >> 4;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                final int fcx = cx;
                final int fcz = cz;
                CompletableFuture<Void> cf = new CompletableFuture<>();
                tasks.add(cf);
                int sampleX = Math.max(minX, fcx << 4);
                int sampleZ = Math.max(minZ, fcz << 4);
                Location sample = new Location(world, sampleX, minY, sampleZ);
                CompatibleScheduler.runTask(plugin, sample, () -> {
                    try {
                        List<Captured> list = new ArrayList<>();
                        int xStart = Math.max(minX, fcx << 4);
                        int xEnd = Math.min(maxX, (fcx << 4) + 15);
                        int zStart = Math.max(minZ, fcz << 4);
                        int zEnd = Math.min(maxZ, (fcz << 4) + 15);
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = zStart; z <= zEnd; z++) {
                                for (int x = xStart; x <= xEnd; x++) {
                                    Block b = world.getBlockAt(x, y, z);
                                    String state = b.getBlockData().getAsString();
                                    SchematicData.BlockEntityData be = null;
                                    if (includeBlockEntities) {
                                        try {
                                            BlockState bs = b.getState();
                                            if (bs instanceof TileState ts) {
                                                Map<String, Object> data = TileStateIO.serialize(ts, logger);
                                                if (data != null) {
                                                    be = new SchematicData.BlockEntityData();
                                                    be.x = x - minX;
                                                    be.y = y - minY;
                                                    be.z = z - minZ;
                                                    be.kind = b.getType().getKey().toString();
                                                    be.data = data;
                                                }
                                            }
                                        } catch (Throwable ignored) {
                                            // 方块实体失败则仅保留方块
                                        }
                                    }
                                    list.add(new Captured(x - minX, y - minY, z - minZ, state, be));
                                }
                            }
                        }
                        long key = (((long) fcx) << 32) ^ (fcz & 0xffffffffL);
                        byChunk.put(key, list);
                        cf.complete(null);
                    } catch (Throwable t) {
                        cf.completeExceptionally(t);
                    }
                });
            }
        }

        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new))
                .thenApplyAsync(v -> assemble(dx, dy, dz, origin, byChunk, includeBlockEntities, logger));
    }

    private static SchematicData assemble(int dx, int dy, int dz, Vec3i origin,
                                          Map<Long, List<Captured>> byChunk,
                                          boolean includeBlockEntities,
                                          Logger logger) {
        String[][][] grid = new String[dx][dy][dz];
        List<SchematicData.BlockEntityData> bes = new ArrayList<>();
        for (List<Captured> list : byChunk.values()) {
            for (Captured c : list) {
                grid[c.x][c.y][c.z] = c.state;
                if (includeBlockEntities && c.be != null) {
                    bes.add(c.be);
                }
            }
        }

        SchematicData out = new SchematicData();
        out.origin = origin;
        out.size = new Size3i(dx, dy, dz);
        out.palette = new ArrayList<>();
        out.blocks = new ArrayList<>();
        out.blockEntities = bes;

        Map<String, Integer> paletteIndex = new LinkedHashMap<>();
        int currentIndex = -1;
        int run = 0;
        for (int y = 0; y < dy; y++) {
            for (int z = 0; z < dz; z++) {
                for (int x = 0; x < dx; x++) {
                    String state = grid[x][y][z];
                    if (state == null) {
                        state = Material.AIR.createBlockData().getAsString();
                    }
                    int idx = paletteIndex.computeIfAbsent(state, s -> {
                        out.palette.add(s);
                        return out.palette.size() - 1;
                    });
                    if (idx == currentIndex) {
                        run++;
                    } else {
                        if (currentIndex != -1) {
                            out.blocks.add(new int[]{currentIndex, run});
                        }
                        currentIndex = idx;
                        run = 1;
                    }
                }
            }
        }
        if (run > 0) {
            out.blocks.add(new int[]{currentIndex, run});
        }
        if (logger != null) {
            logger.info("[World] Schematic exported: " + dx + "x" + dy + "x" + dz
                    + " palette=" + out.palette.size()
                    + " tiles=" + out.blockEntities.size()
                    + " folia=" + ServerUtils.isFolia());
        }
        return out;
    }

    private static Vec3i clampOrigin(Vec3i originRel, int dx, int dy, int dz) {
        if (originRel == null) {
            return new Vec3i(dx / 2, 0, dz / 2);
        }
        int ox = Math.max(0, Math.min(dx - 1, originRel.x()));
        int oy = Math.max(0, Math.min(dy - 1, originRel.y()));
        int oz = Math.max(0, Math.min(dz - 1, originRel.z()));
        return new Vec3i(ox, oy, oz);
    }

    private record Captured(int x, int y, int z, String state, SchematicData.BlockEntityData be) {
    }
}
