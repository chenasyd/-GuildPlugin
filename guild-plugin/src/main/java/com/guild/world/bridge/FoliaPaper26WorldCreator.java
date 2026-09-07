package com.guild.world.bridge;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/** PaperWorldLoader 路径（26.x）。 */
final class FoliaPaper26WorldCreator {

    private FoliaPaper26WorldCreator() {
    }

    static World create(WorldCreator creator) throws Exception {
        Object craftServer = Bukkit.getServer();
        Object console = FoliaNmsReflection.invoke(craftServer, "getServer");
        FoliaWorldCreationSupport.assertServerReady(console);

        String name = creator.name();
        ChunkGenerator generator = FoliaWorldCreationSupport.resolveGenerator(craftServer, creator);
        BiomeProvider biomeProvider = FoliaWorldCreationSupport.resolveBiomeProvider(craftServer, creator, generator);

        Object actualDimension = FoliaWorldCreationSupport.levelStemKey(creator.environment());
        NamespacedKey key = FoliaWorldCreationSupport.creatorKey(creator);

        Class<?> paperLoader = FoliaNmsReflection.clazz("io.papermc.paper.world.PaperWorldLoader");
        Object dimensionKey = FoliaNmsReflection.invokeStatic(paperLoader, "dimensionKey", key);

        Object context = FoliaWorldCreationSupport.dataLoadContext(console);
        Object registryAccess = FoliaNmsReflection.invoke(context, "datapackDimensions");
        Object levelStemRegKey = FoliaWorldCreationSupport.registryKey("LEVEL_STEM");
        Object contextLevelStemRegistry = FoliaWorldCreationSupport.registryOrThrow(registryAccess, levelStemRegKey);

        Object storageSource = FoliaNmsReflection.requireField(console, "storageSource");
        Object fullRegistryAccess = FoliaNmsReflection.invoke(console, "registryAccess");

        try {
            FoliaNmsReflection.invokeStatic(
                    FoliaNmsReflection.clazz("io.papermc.paper.world.migration.WorldFolderMigration"),
                    "migrateApiWorld",
                    storageSource, fullRegistryAccess, name, actualDimension, dimensionKey);
        } catch (Exception e) {
            FoliaPlatformProbe.logger().log(Level.FINE, "[World] [Folia] migrateApiWorld skipped", e);
        }

        Object loadedWorldData = FoliaNmsReflection.invokeStatic(paperLoader, "loadWorldData", console, dimensionKey, name);
        Object primaryLevelData = FoliaNmsReflection.invoke(console, "getWorldData");

        Class<?> wgsClass = FoliaNmsReflection.clazz("net.minecraft.world.level.levelgen.WorldGenSettings");
        Object wgsType = FoliaNmsReflection.getStatic(wgsClass, "TYPE");
        Object worldGenSettings = FoliaWorldCreationSupport.readExistingWorldGenSettings(
                storageSource, dimensionKey, fullRegistryAccess, wgsType);

        if (worldGenSettings == null) {
            boolean bonusChest = false;
            try {
                bonusChest = (boolean) WorldCreator.class.getMethod("bonusChest").invoke(creator);
            } catch (ReflectiveOperationException ignored) {
            }
            Object worldOptions = FoliaNmsReflection.construct(
                    FoliaNmsReflection.clazz("net.minecraft.world.level.levelgen.WorldOptions"),
                    creator.seed(), creator.generateStructures(), bonusChest);
            Object wdd = FoliaNmsReflection.construct(
                    FoliaNmsReflection.clazz("net.minecraft.server.dedicated.DedicatedServerProperties$WorldDimensionData"),
                    FoliaWorldCreationSupport.parseJson(creator.generatorSettings()),
                    creator.type().name().toLowerCase(Locale.ROOT));
            Object worldDimensions = FoliaNmsReflection.invoke(wdd, "create", FoliaNmsReflection.invoke(context, "datapackWorldgen"));
            Object complete = FoliaNmsReflection.invoke(worldDimensions, "bake", contextLevelStemRegistry);
            worldGenSettings = FoliaNmsReflection.construct(wgsClass, worldOptions, worldDimensions);
            registryAccess = FoliaNmsReflection.invoke(complete, "dimensionsRegistryAccess");
            contextLevelStemRegistry = FoliaWorldCreationSupport.registryOrThrow(registryAccess, levelStemRegKey);
            FoliaNmsReflection.tryInvoke(FoliaNmsReflection.invoke(loadedWorldData, "levelOverrides"),
                    "setHardcore", creator.hardcore());
        }

        long biomeZoomSeed = (long) FoliaNmsReflection.invokeStatic(
                FoliaNmsReflection.clazz("net.minecraft.world.level.biome.BiomeManager"),
                "obfuscateSeed", FoliaNmsReflection.invoke(FoliaNmsReflection.invoke(worldGenSettings, "options"), "seed"));

        Object dims = FoliaNmsReflection.invoke(worldGenSettings, "dimensions");
        Object customStem = FoliaNmsReflection.unwrapOptional(FoliaNmsReflection.tryInvoke(dims, "get", actualDimension));
        if (customStem == null) {
            customStem = FoliaWorldCreationSupport.registryGet(contextLevelStemRegistry, actualDimension);
        }
        if (customStem == null) {
            throw new IllegalStateException("Missing LevelStem for world " + name);
        }

        Object dimPath = FoliaNmsReflection.invoke(storageSource, "getDimensionPath", dimensionKey);
        Object dataDirName = FoliaNmsReflection.invoke(
                FoliaNmsReflection.getStatic(FoliaNmsReflection.clazz("net.minecraft.world.level.storage.LevelResource"), "DATA"),
                "id");
        Object dataPath = FoliaNmsReflection.invoke(dimPath, "resolve", dataDirName);
        Object savedDataStorage = FoliaNmsReflection.construct(
                FoliaNmsReflection.clazz("net.minecraft.world.level.storage.SavedDataStorage"),
                dataPath, FoliaNmsReflection.invoke(console, "getFixerUpper"), fullRegistryAccess);
        FoliaNmsReflection.invoke(savedDataStorage, "set", wgsType,
                FoliaNmsReflection.construct(wgsClass,
                        FoliaNmsReflection.invoke(worldGenSettings, "options"),
                        FoliaNmsReflection.invoke(worldGenSettings, "dimensions")));

        Object executor = FoliaNmsReflection.requireField(console, "executor");
        boolean isDebug = (boolean) FoliaNmsReflection.invoke(primaryLevelData, "isDebugWorld");
        Object serverLevel = FoliaNmsReflection.construct(
                FoliaNmsReflection.clazz("net.minecraft.server.level.ServerLevel"),
                console, executor, storageSource, worldGenSettings, dimensionKey, customStem,
                isDebug, biomeZoomSeed, List.of(), true, actualDimension,
                creator.environment(), generator, biomeProvider, savedDataStorage, loadedWorldData);

        FoliaNmsReflection.invoke(console, "addLevel", serverLevel);
        if (!FoliaNmsReflection.tryInvokeVoid(console, "initWorld", serverLevel, creator)
                && !FoliaNmsReflection.tryInvokeVoid(console, "initWorld", serverLevel)) {
            FoliaPlatformProbe.logger().warning("[World] [Folia] initWorld not found on 26.x console");
        }
        FoliaChunkBootstrap.trySetSpawnSettings(serverLevel);
        if (!FoliaNmsReflection.tryInvokeVoid(console, "prepareLevel", serverLevel)) {
            FoliaChunkBootstrap.tryPrepareClassic(console, serverLevel);
        }
        FoliaPlatformProbe.logger().info("[World] [Folia] paper26 path registered '" + name + "'");
        return FoliaWorldCreationSupport.finishWorld(serverLevel);
    }
}
