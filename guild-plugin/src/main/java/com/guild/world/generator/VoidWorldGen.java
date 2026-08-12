package com.guild.world.generator;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Random;

/**
 * 虚空世界生成器 — 纯 Bukkit API，Spigot/Paper/Folia 通用，无版本差异。
 *
 * <p>参考 Skyllia 的 VoidWorldGen 模式：
 * <ul>
 *   <li>{@link #generateNoise} 空实现（不生成任何方块）；</li>
 *   <li>所有 {@code shouldGenerate*} 返回 {@code false}（无地表/基岩/洞穴/装饰/生物/结构）；</li>
 *   <li>{@link #getFixedSpawnLocation} 固定返回 (0.5, 64, 0.5)。</li>
 * </ul>
 */
public class VoidWorldGen extends ChunkGenerator {

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        // 虚空：不生成任何方块
    }

    /**
     * THE_VOID 单一群系 provider 共享实例。
     *
     * <p>创建世界时显式注入 {@link org.bukkit.WorldCreator#biomeProvider}，
     * 保证 Folia 的 NMS 反射创建路径拿到非空 biome provider。
     */
    public static final BiomeProvider THE_VOID_BIOME_PROVIDER = new BiomeProvider() {
        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            return Biome.THE_VOID;
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return List.of(Biome.THE_VOID);
        }
    };

    /**
     * 单一 THE_VOID 生物群系。
     *
     * <p>某些服务端（如 Folia）在创建世界时要求 biome provider 非空，
     * 否则世界初始化阶段可能抛出 NullPointerException。
     */
    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return THE_VOID_BIOME_PROVIDER;
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, 64.0, 0.5);
    }

    /**
     * 防御：旧版 chunk 生成入口。若服务端走此路径，返回空数据而非 null
     * （null 会引发下游 NPE）。虚空世界不生成任何方块。
     */
    @Deprecated
    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        return createChunkData(world);
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return List.of();
    }
}
