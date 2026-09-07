package com.guild.world.bridge;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;

import java.util.List;
import java.util.Locale;

/** classic LevelStorageAccess 路径（1.19.4 / 1.20.4+ / 1.21.x）。 */
final class FoliaClassicWorldCreator {

    private FoliaClassicWorldCreator() {
    }

    static World create(WorldCreator creator) throws Exception {
        Object craftServer = Bukkit.getServer();
        Object console = FoliaNmsReflection.invoke(craftServer, "getServer");
        FoliaWorldCreationSupport.assertServerReady(console);

        String name = creator.name();
        ChunkGenerator generator = FoliaWorldCreationSupport.resolveGenerator(craftServer, creator);
        BiomeProvider biomeProvider = FoliaWorldCreationSupport.resolveBiomeProvider(craftServer, creator, generator);

        Object actualDimension = FoliaWorldCreationSupport.levelStemKey(creator.environment());
        Object session = FoliaWorldCreationSupport.openSession(craftServer, name, actualDimension);

        Object context = FoliaWorldCreationSupport.dataLoadContext(console);
        Object dataConfiguration = FoliaNmsReflection.invoke(context, "dataConfiguration");
        Object datapackWorldgen = FoliaNmsReflection.invoke(context, "datapackWorldgen");
        Object datapackDimensions = FoliaNmsReflection.invoke(context, "datapackDimensions");

        Object levelStemKey = FoliaWorldCreationSupport.registryKey("LEVEL_STEM");
        Object stemRegistry = FoliaWorldCreationSupport.registryOrThrow(datapackDimensions, levelStemKey);

        Object dynamic = FoliaWorldCreationSupport.readWorldDataTag(session);
        Object worldData;
        Object dimensionsRegistryAccess = datapackDimensions;

        if (dynamic != null) {
            Object lad = FoliaNmsReflection.invokeStatic(
                    FoliaNmsReflection.clazz("net.minecraft.world.level.storage.LevelStorageSource"),
                    "getLevelDataAndDimensions",
                    dynamic, dataConfiguration, stemRegistry, datapackWorldgen);
            worldData = FoliaNmsReflection.invoke(lad, "worldData");
            Object dimsComplete = FoliaNmsReflection.invoke(lad, "dimensions");
            dimensionsRegistryAccess = FoliaNmsReflection.invoke(dimsComplete, "dimensionsRegistryAccess");
            stemRegistry = FoliaWorldCreationSupport.registryOrThrow(dimensionsRegistryAccess, levelStemKey);
        } else {
            Object worldOptions = FoliaNmsReflection.construct(
                    FoliaNmsReflection.clazz("net.minecraft.world.level.levelgen.WorldOptions"),
                    creator.seed(), creator.generateStructures(), false);

            Object json = FoliaWorldCreationSupport.parseJson(creator.generatorSettings());
            Object wdd = FoliaNmsReflection.construct(
                    FoliaNmsReflection.clazz("net.minecraft.server.dedicated.DedicatedServerProperties$WorldDimensionData"),
                    json, creator.type().name().toLowerCase(Locale.ROOT));

            Object worldDimensions = FoliaNmsReflection.invoke(wdd, "create", datapackWorldgen);
            Object complete = FoliaNmsReflection.invoke(worldDimensions, "bake", stemRegistry);

            Object lifecycle = FoliaNmsReflection.invoke(complete, "lifecycle");
            Object packLifecycle = FoliaNmsReflection.tryInvoke(datapackWorldgen, "allRegistriesLifecycle");
            if (packLifecycle != null) {
                Object added = FoliaNmsReflection.tryInvoke(lifecycle, "add", packLifecycle);
                if (added != null) {
                    lifecycle = added;
                }
            }

            Object gameRules = FoliaWorldCreationSupport.createGameRules(dataConfiguration);
            Object levelSettings = FoliaNmsReflection.construct(
                    FoliaNmsReflection.clazz("net.minecraft.world.level.LevelSettings"),
                    name,
                    FoliaWorldCreationSupport.resolveGameType(craftServer),
                    creator.hardcore(),
                    FoliaNmsReflection.enumConstant("net.minecraft.world.Difficulty", "EASY"),
                    false,
                    gameRules,
                    dataConfiguration);

            worldData = FoliaNmsReflection.construct(
                    FoliaNmsReflection.clazz("net.minecraft.world.level.storage.PrimaryLevelData"),
                    levelSettings, worldOptions,
                    FoliaNmsReflection.invoke(complete, "specialWorldProperty"), lifecycle);
            dimensionsRegistryAccess = FoliaNmsReflection.invoke(complete, "dimensionsRegistryAccess");
            stemRegistry = FoliaWorldCreationSupport.registryOrThrow(dimensionsRegistryAccess, levelStemKey);
        }

        FoliaNmsReflection.setField(worldData, "customDimensions", stemRegistry);
        FoliaNmsReflection.invoke(worldData, "checkName", name);
        FoliaNmsReflection.invoke(worldData, "setModdedInfo",
                FoliaNmsReflection.invoke(console, "getServerModName"),
                FoliaNmsReflection.invoke(FoliaNmsReflection.invoke(console, "getModdedStatus"), "shouldReportAsModified"));

        Object worldGenOptions = FoliaNmsReflection.invoke(worldData, "worldGenOptions");
        long obfuscatedSeed = (long) FoliaNmsReflection.invokeStatic(
                FoliaNmsReflection.clazz("net.minecraft.world.level.biome.BiomeManager"),
                "obfuscateSeed", FoliaNmsReflection.invoke(worldGenOptions, "seed"));
        boolean isDebug = (boolean) FoliaNmsReflection.invoke(worldData, "isDebugWorld");

        Object stem = FoliaWorldCreationSupport.registryGet(stemRegistry, actualDimension);
        if (stem == null) {
            throw new IllegalStateException("Missing LevelStem for " + creator.environment());
        }

        NamespacedKey key = FoliaWorldCreationSupport.creatorKey(creator);
        Object worldKey = FoliaWorldCreationSupport.createDimensionKey(console, name, key);

        Object executor = FoliaNmsReflection.requireField(console, "executor");
        Object randomSequences = FoliaNmsReflection.invoke(FoliaNmsReflection.invoke(console, "overworld"), "getRandomSequences");
        Object progressListener = FoliaChunkBootstrap.tryCreateProgressListener(console, worldData);

        Object serverLevel = newServerLevelClassic(
                console, executor, session, worldData, worldKey, stem,
                progressListener, isDebug, obfuscatedSeed, randomSequences,
                creator.environment(), generator, biomeProvider);

        FoliaNmsReflection.trySetField(serverLevel, "randomSpawnSelection",
                FoliaChunkBootstrap.tryFindSpawnChunkPos(serverLevel));

        FoliaNmsReflection.invoke(console, "addLevel", serverLevel);
        FoliaChunkBootstrap.tryInitWorldClassic(console, serverLevel, worldData, worldGenOptions);
        FoliaChunkBootstrap.trySetSpawnSettings(serverLevel);
        FoliaChunkBootstrap.tryPrepareClassic(console, serverLevel);

        FoliaNmsReflection.invoke(
                FoliaNmsReflection.invokeStatic(
                        FoliaNmsReflection.clazz("io.papermc.paper.threadedregions.RegionizedServer"), "getInstance"),
                "addWorld", serverLevel);
        FoliaPlatformProbe.logger().info("[World] [Folia] classic path registered '" + name + "'");

        return FoliaWorldCreationSupport.finishWorld(serverLevel);
    }

    private static Object newServerLevelClassic(
            Object console, Object executor, Object session, Object worldData, Object worldKey, Object stem,
            Object progressListener, boolean isDebug, long obfuscatedSeed, Object randomSequences,
            World.Environment env, ChunkGenerator generator, BiomeProvider biomeProvider) throws Exception {
        Class<?> serverLevelClass = FoliaNmsReflection.clazz("net.minecraft.server.level.ServerLevel");
        List<?> spawners = List.of();

        Object level = FoliaNmsReflection.tryConstruct(serverLevelClass,
                console, executor, session, worldData, worldKey, stem,
                isDebug, obfuscatedSeed, spawners, true, randomSequences,
                env, generator, biomeProvider);
        if (level != null) {
            return level;
        }
        if (progressListener != null) {
            level = FoliaNmsReflection.tryConstruct(serverLevelClass,
                    console, executor, session, worldData, worldKey, stem, progressListener,
                    isDebug, obfuscatedSeed, spawners, true, randomSequences,
                    env, generator, biomeProvider);
            if (level != null) {
                return level;
            }
        }
        throw new NoSuchMethodException("No matching ServerLevel constructor (classic)");
    }
}
