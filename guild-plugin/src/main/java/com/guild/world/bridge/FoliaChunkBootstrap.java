package com.guild.world.bridge;

import java.util.logging.Level;

/**
 * 世界创建后的区块/出生点初始化（classic 与 paper26 共用部分步骤）。
 */
final class FoliaChunkBootstrap {

    private FoliaChunkBootstrap() {
    }

    static Object tryCreateProgressListener(Object console, Object worldData) {
        try {
            Object factory = FoliaNmsReflection.getField(console, "progressListenerFactory");
            int radius = 11;
            try {
                Object rules = FoliaNmsReflection.invoke(worldData, "getGameRules");
                Object key = FoliaNmsReflection.getStatic(
                        FoliaNmsReflection.clazz("net.minecraft.world.level.GameRules"),
                        "RULE_SPAWN_CHUNK_RADIUS");
                Object value = FoliaNmsReflection.tryInvoke(rules, "getInt", key);
                if (value instanceof Integer i) {
                    radius = i;
                }
            } catch (Exception ignored) {
            }
            return FoliaNmsReflection.invoke(factory, "create", radius);
        } catch (Exception e) {
            return null;
        }
    }

    static Object tryFindSpawnChunkPos(Object serverLevel) {
        try {
            Object chunkSource = FoliaNmsReflection.invoke(serverLevel, "getChunkSource");
            Object randomState = FoliaNmsReflection.invoke(chunkSource, "randomState");
            Object sampler = FoliaNmsReflection.invoke(randomState, "sampler");
            Object spawnPos = FoliaNmsReflection.invoke(sampler, "findSpawnPosition");
            return FoliaNmsReflection.construct(
                    FoliaNmsReflection.clazz("net.minecraft.world.level.ChunkPos"), spawnPos);
        } catch (Exception e) {
            return null;
        }
    }

    static void tryInitWorldClassic(Object console, Object serverLevel,
                                    Object worldData, Object worldGenOptions) {
        try {
            if (FoliaNmsReflection.tryInvokeVoid(console, "initWorld", serverLevel, worldData, worldGenOptions)) {
                return;
            }
            if (FoliaNmsReflection.tryInvokeVoid(console, "initWorld", serverLevel, worldData, worldData, worldGenOptions)) {
                return;
            }
            FoliaNmsReflection.tryInvokeVoid(console, "initWorld", serverLevel, worldData);
        } catch (Exception e) {
            FoliaPlatformProbe.logger().log(Level.FINE, "[World] [Folia] initWorld skipped", e);
        }
    }

    static void trySetSpawnSettings(Object serverLevel) throws Exception {
        if (FoliaNmsReflection.tryInvokeVoid(serverLevel, "setSpawnSettings", true, true)) {
            return;
        }
        if (!FoliaNmsReflection.tryInvokeVoid(serverLevel, "setSpawnSettings", true)) {
            throw new NoSuchMethodException("ServerLevel#setSpawnSettings");
        }
    }

    static void tryPrepareClassic(Object console, Object serverLevel) throws Exception {
        Object listener = null;
        try {
            listener = FoliaNmsReflection.getField(
                    FoliaNmsReflection.getField(FoliaNmsReflection.invoke(serverLevel, "getChunkSource"), "chunkMap"),
                    "progressListener");
        } catch (Exception ignored) {
        }
        if (listener != null && FoliaNmsReflection.tryInvokeVoid(console, "prepareLevels", listener, serverLevel)) {
            return;
        }
        if (FoliaNmsReflection.tryInvokeVoid(console, "prepareLevel", serverLevel)) {
            return;
        }
        FoliaPlatformProbe.logger().warning("[World] [Folia] prepareLevels/prepareLevel skipped");
    }
}
