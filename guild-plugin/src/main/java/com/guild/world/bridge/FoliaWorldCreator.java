package com.guild.world.bridge;

import com.guild.GuildPlugin;
import com.guild.core.utils.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Folia 运行时创建/加载世界的反射桥接（对齐 Skyllia WorldNMS）。
 *
 * <h3>为何不能用 {@code Bukkit.getServer().getClass().getClassLoader()}</h3>
 * CraftBukkit 与 NMS 常分属不同 ClassLoader；必须从已加载的
 * {@code CraftWorld#getHandle()}（ServerLevel）取 NMS ClassLoader。
 *
 * <h3>路径</h3>
 * <ul>
 *   <li>26.x → PaperWorldLoader 路径（Skyllia v26）</li>
 *   <li>其余支持版本 → classic LevelStorageAccess 路径（Skyllia v1_20_R4 … v1_21_R7）</li>
 * </ul>
 */
public final class FoliaWorldCreator {

    private static volatile ClassLoader nmsClassLoader;

    private FoliaWorldCreator() {
    }

    private static Logger logger() {
        GuildPlugin plugin = GuildPlugin.getInstance();
        return plugin != null ? plugin.getLogger() : Bukkit.getLogger();
    }

    /* ── 入口 ─────────────────────────────────────────────── */

    public static World createWorld(WorldCreator creator) {
        try {
            return createWorld0(creator);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("Folia world creation failed for '" + creator.name() + "'", cause);
        } catch (Exception e) {
            throw new RuntimeException("Folia world creation failed for '" + creator.name()
                    + "' (NMS signature mismatch, see cause)", e);
        }
    }

    private static World createWorld0(WorldCreator creator) throws Exception {
        ensureNmsClassLoader();
        String mc = ServerUtils.getMinecraftVersion();
        logger().info("[World] [Folia] Creating world '" + creator.name()
                + "' (mc=" + mc + ", seed=" + creator.seed() + ", env=" + creator.environment() + ")");
        if (isPaper26Family(mc)) {
            return createWorldPaper26(creator);
        }
        return createWorldClassic(creator);
    }

    private static boolean isPaper26Family(String mc) {
        return mc != null && (mc.equals("26") || mc.startsWith("26."));
    }

    /* ── classic：1.19.4 / 1.20.4+ / 1.21.x ─────────────────── */

    private static World createWorldClassic(WorldCreator creator) throws Exception {
        Object craftServer = Bukkit.getServer();
        Object console = invoke(craftServer, "getServer");
        assertServerReady(console);

        String name = creator.name();
        ChunkGenerator generator = resolveGenerator(craftServer, creator);
        BiomeProvider biomeProvider = resolveBiomeProvider(craftServer, creator, generator);

        Object actualDimension = levelStemKey(creator.environment());
        Object session = openSession(craftServer, name, actualDimension);

        Object context = dataLoadContext(console);
        Object dataConfiguration = invoke(context, "dataConfiguration");
        Object datapackWorldgen = invoke(context, "datapackWorldgen");
        Object datapackDimensions = invoke(context, "datapackDimensions");

        Object levelStemKey = registryKey("LEVEL_STEM");
        Object stemRegistry = registryOrThrow(datapackDimensions, levelStemKey);

        Object dynamic = readWorldDataTag(session);
        Object worldData;
        Object dimensionsRegistryAccess = datapackDimensions;

        if (dynamic != null) {
            Object lad = invokeStatic(clazz("net.minecraft.world.level.storage.LevelStorageSource"),
                    "getLevelDataAndDimensions",
                    dynamic, dataConfiguration, stemRegistry, datapackWorldgen);
            worldData = invoke(lad, "worldData");
            Object dimsComplete = invoke(lad, "dimensions");
            dimensionsRegistryAccess = invoke(dimsComplete, "dimensionsRegistryAccess");
            stemRegistry = registryOrThrow(dimensionsRegistryAccess, levelStemKey);
        } else {
            Object worldOptions = construct(clazz("net.minecraft.world.level.levelgen.WorldOptions"),
                    creator.seed(), creator.generateStructures(), false);

            Object json = parseJson(creator.generatorSettings());
            Object wdd = construct(
                    clazz("net.minecraft.server.dedicated.DedicatedServerProperties$WorldDimensionData"),
                    json, creator.type().name().toLowerCase(Locale.ROOT));

            // 必须用 datapackWorldgen（含 world_preset）；勿传 datapackDimensions
            Object worldDimensions = invoke(wdd, "create", datapackWorldgen);
            Object complete = invoke(worldDimensions, "bake", stemRegistry);

            Object lifecycle = invoke(complete, "lifecycle");
            Object packLifecycle = tryInvoke(datapackWorldgen, "allRegistriesLifecycle");
            if (packLifecycle != null) {
                Object added = tryInvoke(lifecycle, "add", packLifecycle);
                if (added != null) {
                    lifecycle = added;
                }
            }

            Object gameRules = createGameRules(dataConfiguration);
            Object levelSettings = construct(clazz("net.minecraft.world.level.LevelSettings"),
                    name,
                    resolveGameType(craftServer),
                    creator.hardcore(),
                    enumConstant("net.minecraft.world.Difficulty", "EASY"),
                    false,
                    gameRules,
                    dataConfiguration);

            worldData = construct(clazz("net.minecraft.world.level.storage.PrimaryLevelData"),
                    levelSettings, worldOptions,
                    invoke(complete, "specialWorldProperty"), lifecycle);
            dimensionsRegistryAccess = invoke(complete, "dimensionsRegistryAccess");
            stemRegistry = registryOrThrow(dimensionsRegistryAccess, levelStemKey);
        }

        setField(worldData, "customDimensions", stemRegistry);
        invoke(worldData, "checkName", name);
        invoke(worldData, "setModdedInfo",
                invoke(console, "getServerModName"),
                invoke(invoke(console, "getModdedStatus"), "shouldReportAsModified"));

        Object worldGenOptions = invoke(worldData, "worldGenOptions");
        long obfuscatedSeed = (long) invokeStatic(clazz("net.minecraft.world.level.biome.BiomeManager"),
                "obfuscateSeed", invoke(worldGenOptions, "seed"));
        boolean isDebug = (boolean) invoke(worldData, "isDebugWorld");

        Object stem = registryGet(stemRegistry, actualDimension);
        if (stem == null) {
            throw new IllegalStateException("Missing LevelStem for " + creator.environment());
        }

        NamespacedKey key = creatorKey(creator);
        Object worldKey = createDimensionKey(console, name, key);

        Object executor = requireField(console, "executor");
        Object randomSequences = invoke(invoke(console, "overworld"), "getRandomSequences");
        Object progressListener = tryCreateProgressListener(console, worldData);

        Object serverLevel = newServerLevelClassic(
                console, executor, session, worldData, worldKey, stem,
                progressListener, isDebug, obfuscatedSeed, randomSequences,
                creator.environment(), generator, biomeProvider);

        trySetField(serverLevel, "randomSpawnSelection", tryFindSpawnChunkPos(serverLevel));

        invoke(console, "addLevel", serverLevel);
        tryInitWorldClassic(console, serverLevel, worldData, worldGenOptions);
        trySetSpawnSettings(serverLevel);
        tryPrepareClassic(console, serverLevel);

        invoke(invokeStatic(clazz("io.papermc.paper.threadedregions.RegionizedServer"), "getInstance"),
                "addWorld", serverLevel);
        logger().info("[World] [Folia] classic path registered '" + name + "'");

        return finishWorld(serverLevel);
    }

    private static Object newServerLevelClassic(
            Object console, Object executor, Object session, Object worldData, Object worldKey, Object stem,
            Object progressListener, boolean isDebug, long obfuscatedSeed, Object randomSequences,
            World.Environment env, ChunkGenerator generator, BiomeProvider biomeProvider) throws Exception {
        Class<?> serverLevelClass = clazz("net.minecraft.server.level.ServerLevel");
        List<?> spawners = List.of();

        // 1.21.6+：无 progressListener
        Object level = tryConstruct(serverLevelClass,
                console, executor, session, worldData, worldKey, stem,
                isDebug, obfuscatedSeed, spawners, true, randomSequences,
                env, generator, biomeProvider);
        if (level != null) {
            return level;
        }
        if (progressListener != null) {
            level = tryConstruct(serverLevelClass,
                    console, executor, session, worldData, worldKey, stem, progressListener,
                    isDebug, obfuscatedSeed, spawners, true, randomSequences,
                    env, generator, biomeProvider);
            if (level != null) {
                return level;
            }
        }
        throw new NoSuchMethodException("No matching ServerLevel constructor (classic)");
    }

    /* ── paper26：26.1.x ──────────────────────────────────── */

    private static World createWorldPaper26(WorldCreator creator) throws Exception {
        Object craftServer = Bukkit.getServer();
        Object console = invoke(craftServer, "getServer");
        assertServerReady(console);

        String name = creator.name();
        ChunkGenerator generator = resolveGenerator(craftServer, creator);
        BiomeProvider biomeProvider = resolveBiomeProvider(craftServer, creator, generator);

        Object actualDimension = levelStemKey(creator.environment());
        NamespacedKey key = creatorKey(creator);

        Class<?> paperLoader = clazz("io.papermc.paper.world.PaperWorldLoader");
        Object dimensionKey = invokeStatic(paperLoader, "dimensionKey", key);

        Object context = dataLoadContext(console);
        Object registryAccess = invoke(context, "datapackDimensions");
        Object levelStemRegKey = registryKey("LEVEL_STEM");
        Object contextLevelStemRegistry = registryOrThrow(registryAccess, levelStemRegKey);

        Object storageSource = requireField(console, "storageSource");
        Object fullRegistryAccess = invoke(console, "registryAccess");

        try {
            invokeStatic(clazz("io.papermc.paper.world.migration.WorldFolderMigration"),
                    "migrateApiWorld",
                    storageSource, fullRegistryAccess, name, actualDimension, dimensionKey);
        } catch (Exception e) {
            logger().log(Level.FINE, "[World] [Folia] migrateApiWorld skipped", e);
        }

        Object loadedWorldData = invokeStatic(paperLoader, "loadWorldData", console, dimensionKey, name);
        Object primaryLevelData = invoke(console, "getWorldData");

        Class<?> wgsClass = clazz("net.minecraft.world.level.levelgen.WorldGenSettings");
        Object wgsType = getStatic(wgsClass, "TYPE");
        Object worldGenSettings = readExistingWorldGenSettings(storageSource, dimensionKey, fullRegistryAccess, wgsType);

        if (worldGenSettings == null) {
            boolean bonusChest = false;
            try {
                bonusChest = (boolean) WorldCreator.class.getMethod("bonusChest").invoke(creator);
            } catch (ReflectiveOperationException ignored) {
            }
            Object worldOptions = construct(clazz("net.minecraft.world.level.levelgen.WorldOptions"),
                    creator.seed(), creator.generateStructures(), bonusChest);
            Object wdd = construct(
                    clazz("net.minecraft.server.dedicated.DedicatedServerProperties$WorldDimensionData"),
                    parseJson(creator.generatorSettings()),
                    creator.type().name().toLowerCase(Locale.ROOT));
            Object worldDimensions = invoke(wdd, "create", invoke(context, "datapackWorldgen"));
            Object complete = invoke(worldDimensions, "bake", contextLevelStemRegistry);
            worldGenSettings = construct(wgsClass, worldOptions, worldDimensions);
            registryAccess = invoke(complete, "dimensionsRegistryAccess");
            contextLevelStemRegistry = registryOrThrow(registryAccess, levelStemRegKey);
            tryInvoke(invoke(loadedWorldData, "levelOverrides"), "setHardcore", creator.hardcore());
        }

        long biomeZoomSeed = (long) invokeStatic(clazz("net.minecraft.world.level.biome.BiomeManager"),
                "obfuscateSeed", invoke(invoke(worldGenSettings, "options"), "seed"));

        Object dims = invoke(worldGenSettings, "dimensions");
        Object customStem = unwrapOptional(tryInvoke(dims, "get", actualDimension));
        if (customStem == null) {
            customStem = registryGet(contextLevelStemRegistry, actualDimension);
        }
        if (customStem == null) {
            throw new IllegalStateException("Missing LevelStem for world " + name);
        }

        Object dimPath = invoke(storageSource, "getDimensionPath", dimensionKey);
        Object dataDirName = invoke(getStatic(clazz("net.minecraft.world.level.storage.LevelResource"), "DATA"), "id");
        Object dataPath = invoke(dimPath, "resolve", dataDirName);
        Object savedDataStorage = construct(
                clazz("net.minecraft.world.level.storage.SavedDataStorage"),
                dataPath, invoke(console, "getFixerUpper"), fullRegistryAccess);
        invoke(savedDataStorage, "set", wgsType,
                construct(wgsClass, invoke(worldGenSettings, "options"), invoke(worldGenSettings, "dimensions")));

        Object executor = requireField(console, "executor");
        boolean isDebug = (boolean) invoke(primaryLevelData, "isDebugWorld");
        Object serverLevel = construct(clazz("net.minecraft.server.level.ServerLevel"),
                console, executor, storageSource, worldGenSettings, dimensionKey, customStem,
                isDebug, biomeZoomSeed, List.of(), true, actualDimension,
                creator.environment(), generator, biomeProvider, savedDataStorage, loadedWorldData);

        invoke(console, "addLevel", serverLevel);
        if (!tryInvokeVoid(console, "initWorld", serverLevel, creator)
                && !tryInvokeVoid(console, "initWorld", serverLevel)) {
            logger().warning("[World] [Folia] initWorld not found on 26.x console");
        }
        trySetSpawnSettings(serverLevel);
        if (!tryInvokeVoid(console, "prepareLevel", serverLevel)) {
            tryPrepareClassic(console, serverLevel);
        }
        logger().info("[World] [Folia] paper26 path registered '" + name + "'");
        return finishWorld(serverLevel);
    }

    /* ── 共享步骤 ─────────────────────────────────────────── */

    private static void assertServerReady(Object console) throws Exception {
        Iterable<?> allLevels = (Iterable<?>) tryInvoke(console, "getAllLevels");
        if (allLevels != null && !allLevels.iterator().hasNext()) {
            throw new IllegalStateException("Cannot create additional worlds on STARTUP");
        }
    }

    private static ChunkGenerator resolveGenerator(Object craftServer, WorldCreator creator) throws Exception {
        ChunkGenerator generator = creator.generator();
        if (generator == null) {
            generator = (ChunkGenerator) invoke(craftServer, "getGenerator", creator.name());
        }
        return generator;
    }

    private static BiomeProvider resolveBiomeProvider(Object craftServer, WorldCreator creator,
                                                      ChunkGenerator generator) throws Exception {
        BiomeProvider biomeProvider = creator.biomeProvider();
        if (biomeProvider == null) {
            biomeProvider = (BiomeProvider) tryInvoke(craftServer, "getBiomeProvider", creator.name());
        }
        if (biomeProvider == null && generator != null) {
            biomeProvider = generator.getDefaultBiomeProvider(null);
        }
        return biomeProvider;
    }

    private static Object levelStemKey(World.Environment env) throws Exception {
        Class<?> levelStem = clazz("net.minecraft.world.level.dimension.LevelStem");
        return switch (env) {
            case NORMAL -> getStatic(levelStem, "OVERWORLD");
            case NETHER -> getStatic(levelStem, "NETHER");
            case THE_END -> getStatic(levelStem, "END");
            default -> throw new IllegalArgumentException("Illegal dimension " + env);
        };
    }

    private static Object openSession(Object craftServer, String name, Object actualDimension) throws Exception {
        File container = (File) invoke(craftServer, "getWorldContainer");
        Class<?> storage = clazz("net.minecraft.world.level.storage.LevelStorageSource");
        Object source = invokeStatic(storage, "createDefault", container.toPath());
        return invoke(source, "validateAndCreateAccess", name, actualDimension);
    }

    /**
     * 1.21.11+ 使用 {@code worldLoaderContext}；旧版为 {@code worldLoader}。
     * 必须优先 context，否则 datapackWorldgen 可能缺 world_preset。
     */
    private static Object dataLoadContext(Object console) throws Exception {
        Object ctx = tryGetField(console, "worldLoaderContext");
        if (ctx != null) {
            return ctx;
        }
        ctx = tryGetField(console, "worldLoader");
        if (ctx != null) {
            return ctx;
        }
        throw new NoSuchFieldException(console.getClass().getName() + "#worldLoaderContext/worldLoader");
    }

    private static Object readWorldDataTag(Object session) throws Exception {
        if (!(boolean) invoke(session, "hasWorldData")) {
            return null;
        }
        Object dynamic;
        try {
            dynamic = invoke(session, "getDataTag");
        } catch (InvocationTargetException ex) {
            dynamic = invoke(session, "getDataTagFallback");
            tryInvoke(session, "restoreLevelDataFromOld");
        }
        // 1.21.11 PaperWorldLoader 路径备选
        if (dynamic == null) {
            Object wrapped = tryInvokeStatic(clazz("io.papermc.paper.world.PaperWorldLoader"),
                    "getLevelData", session);
            if (wrapped != null) {
                dynamic = tryInvoke(wrapped, "dataTag");
            }
        }
        if (dynamic != null) {
            Object summary = invoke(session, "getSummary", dynamic);
            if ((boolean) invoke(summary, "requiresManualConversion")
                    || !(boolean) invoke(summary, "isCompatible")) {
                throw new IllegalStateException("World data incompatible or requires manual conversion");
            }
        }
        return dynamic;
    }

    private static Object readExistingWorldGenSettings(Object storageSource, Object dimensionKey,
                                                       Object registryAccess, Object wgsType) throws Exception {
        Object dataResult = tryInvokeStatic(clazz("net.minecraft.world.level.storage.LevelStorageSource"),
                "readExistingSavedData", storageSource, dimensionKey, registryAccess, wgsType);
        if (dataResult == null) {
            return null;
        }
        Object result = tryInvoke(dataResult, "result");
        return unwrapOptional(result);
    }

    private static Object createGameRules(Object dataConfiguration) throws Exception {
        Object enabledFeatures = tryInvoke(dataConfiguration, "enabledFeatures");
        // 1.21.11：net.minecraft.world.level.gamerules.GameRules
        if (enabledFeatures != null) {
            Object rules = tryConstruct(tryClazz("net.minecraft.world.level.gamerules.GameRules"), enabledFeatures);
            if (rules != null) {
                return rules;
            }
            rules = tryConstruct(tryClazz("net.minecraft.world.level.GameRules"), enabledFeatures);
            if (rules != null) {
                return rules;
            }
        }
        Object rules = tryConstruct(tryClazz("net.minecraft.world.level.gamerules.GameRules"));
        if (rules != null) {
            return rules;
        }
        return construct(clazz("net.minecraft.world.level.GameRules"));
    }

    private static Object resolveGameType(Object craftServer) throws Exception {
        try {
            Object mode = invoke(craftServer, "getDefaultGameMode");
            int id = (int) invoke(mode, "getValue");
            Object byId = tryInvokeStatic(clazz("net.minecraft.world.level.GameType"), "byId", id);
            if (byId != null) {
                return byId;
            }
        } catch (Exception ignored) {
        }
        return enumConstant("net.minecraft.world.level.GameType", "SURVIVAL");
    }

    private static Object tryCreateProgressListener(Object console, Object worldData) {
        try {
            Object factory = getField(console, "progressListenerFactory");
            int radius = 11;
            try {
                Object rules = invoke(worldData, "getGameRules");
                Object key = getStatic(clazz("net.minecraft.world.level.GameRules"), "RULE_SPAWN_CHUNK_RADIUS");
                Object value = tryInvoke(rules, "getInt", key);
                if (value instanceof Integer i) {
                    radius = i;
                }
            } catch (Exception ignored) {
            }
            return invoke(factory, "create", radius);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object createDimensionKey(Object console, String name, NamespacedKey key) throws Exception {
        try {
            Object props = invoke(console, "getProperties");
            String levelName = String.valueOf(getField(props, "levelName"));
            if (name.equals(levelName + "_nether")) {
                return getStatic(clazz("net.minecraft.world.level.Level"), "NETHER");
            }
            if (name.equals(levelName + "_the_end")) {
                return getStatic(clazz("net.minecraft.world.level.Level"), "END");
            }
        } catch (Exception ignored) {
        }
        Object id = createResourceId(key.getNamespace(), key.getKey());
        return invokeStatic(clazz("net.minecraft.resources.ResourceKey"),
                "create", registryKey("DIMENSION"), id);
    }

    private static Object createResourceId(String namespace, String path) throws Exception {
        // 1.21.11+：Identifier；旧版：ResourceLocation
        Class<?> identifier = tryClazz("net.minecraft.resources.Identifier");
        if (identifier != null) {
            Object v = tryInvokeStatic(identifier, "fromNamespaceAndPath", namespace, path);
            if (v != null) {
                return v;
            }
            Object c = tryConstruct(identifier, namespace, path);
            if (c != null) {
                return c;
            }
        }
        Class<?> rl = clazz("net.minecraft.resources.ResourceLocation");
        Object v = tryInvokeStatic(rl, "fromNamespaceAndPath", namespace, path);
        if (v != null) {
            return v;
        }
        return construct(rl, namespace, path);
    }

    private static NamespacedKey creatorKey(WorldCreator creator) {
        try {
            Object key = WorldCreator.class.getMethod("key").invoke(creator);
            if (key instanceof NamespacedKey nk) {
                return nk;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return NamespacedKey.minecraft(creator.name().toLowerCase(Locale.ROOT));
    }

    private static Object tryFindSpawnChunkPos(Object serverLevel) {
        try {
            Object chunkSource = invoke(serverLevel, "getChunkSource");
            Object randomState = invoke(chunkSource, "randomState");
            Object sampler = invoke(randomState, "sampler");
            Object spawnPos = invoke(sampler, "findSpawnPosition");
            return construct(clazz("net.minecraft.world.level.ChunkPos"), spawnPos);
        } catch (Exception e) {
            return null;
        }
    }

    private static void tryInitWorldClassic(Object console, Object serverLevel,
                                            Object worldData, Object worldGenOptions) {
        try {
            if (tryInvokeVoid(console, "initWorld", serverLevel, worldData, worldGenOptions)) {
                return;
            }
            if (tryInvokeVoid(console, "initWorld", serverLevel, worldData, worldData, worldGenOptions)) {
                return;
            }
            tryInvokeVoid(console, "initWorld", serverLevel, worldData);
        } catch (Exception e) {
            logger().log(Level.FINE, "[World] [Folia] initWorld skipped", e);
        }
    }

    private static void trySetSpawnSettings(Object serverLevel) throws Exception {
        if (tryInvokeVoid(serverLevel, "setSpawnSettings", true, true)) {
            return;
        }
        if (!tryInvokeVoid(serverLevel, "setSpawnSettings", true)) {
            throw new NoSuchMethodException("ServerLevel#setSpawnSettings");
        }
    }

    private static void tryPrepareClassic(Object console, Object serverLevel) throws Exception {
        Object listener = null;
        try {
            listener = getField(getField(invoke(serverLevel, "getChunkSource"), "chunkMap"), "progressListener");
        } catch (Exception ignored) {
        }
        if (listener != null && tryInvokeVoid(console, "prepareLevels", listener, serverLevel)) {
            return;
        }
        if (tryInvokeVoid(console, "prepareLevel", serverLevel)) {
            return;
        }
        logger().warning("[World] [Folia] prepareLevels/prepareLevel skipped");
    }

    private static World finishWorld(Object serverLevel) throws Exception {
        Object craftWorld = invoke(serverLevel, "getWorld");
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent((World) craftWorld));
        return (World) craftWorld;
    }

    private static Object parseJson(String settings) throws Exception {
        String json = (settings == null || settings.isEmpty()) ? "{}" : settings;
        Object parsed = tryInvokeStatic(clazz("net.minecraft.util.GsonHelper"), "parse", json);
        if (parsed != null) {
            return parsed;
        }
        return invokeStatic(clazz("com.google.gson.GsonHelper"), "parse", json);
    }

    private static Object registryKey(String field) throws Exception {
        return getStatic(clazz("net.minecraft.core.registries.Registries"), field);
    }

    private static Object registryOrThrow(Object registryAccess, Object key) throws Exception {
        Object reg = tryInvoke(registryAccess, "lookupOrThrow", key);
        if (reg != null) {
            return reg;
        }
        return invoke(registryAccess, "registryOrThrow", key);
    }

    private static Object registryGet(Object registry, Object key) throws Exception {
        Object value = tryInvoke(registry, "getValue", key);
        if (value != null) {
            return value;
        }
        return unwrapOptional(tryInvoke(registry, "get", key));
    }

    private static Object unwrapOptional(Object value) {
        if (value instanceof Optional<?> opt) {
            return opt.orElse(null);
        }
        return value;
    }

    /* ── NMS ClassLoader / 反射 ───────────────────────────── */

    /**
     * 从主世界 ServerLevel 取 NMS ClassLoader（CraftServer 的 URLClassLoader 看不到 net.minecraft）。
     */
    private static void ensureNmsClassLoader() throws Exception {
        if (nmsClassLoader != null) {
            return;
        }
        if (Bukkit.getWorlds().isEmpty()) {
            throw new IllegalStateException("No worlds loaded — cannot resolve NMS ClassLoader");
        }
        World world = Bukkit.getWorlds().get(0);
        Object handle = world.getClass().getMethod("getHandle").invoke(world);
        nmsClassLoader = handle.getClass().getClassLoader();
        logger().info("[World] [Folia] NMS ClassLoader=" + nmsClassLoader.getClass().getName());
    }

    private static Class<?> clazz(String name) {
        try {
            return Class.forName(name, true, nmsClassLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("NMS class not found: " + name, e);
        }
    }

    private static Class<?> tryClazz(String name) {
        try {
            return Class.forName(name, true, nmsClassLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Object invoke(Object target, String name, Object... args) throws Exception {
        return invokeMethod(target.getClass(), target, name, args);
    }

    private static Object invokeStatic(Class<?> cls, String name, Object... args) throws Exception {
        return invokeMethod(cls, null, name, args);
    }

    private static Object invokeMethod(Class<?> cls, Object target, String name, Object[] args) throws Exception {
        Method m = findMethod(cls, name, argClasses(args));
        if (m == null) {
            throw new NoSuchMethodException(cls.getName() + "#" + name + Arrays.toString(argClasses(args)));
        }
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Object tryInvoke(Object target, String name, Object... args) throws Exception {
        Method m = findMethod(target.getClass(), name, argClasses(args));
        if (m == null) {
            return null;
        }
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Object tryInvokeStatic(Class<?> cls, String name, Object... args) throws Exception {
        if (cls == null) {
            return null;
        }
        Method m = findMethod(cls, name, argClasses(args));
        if (m == null) {
            return null;
        }
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    private static boolean tryInvokeVoid(Object target, String name, Object... args) throws Exception {
        Method m = findMethod(target.getClass(), name, argClasses(args));
        if (m == null) {
            return false;
        }
        m.setAccessible(true);
        m.invoke(target, args);
        return true;
    }

    private static Object construct(Class<?> cls, Object... args) throws Exception {
        Object o = tryConstruct(cls, args);
        if (o == null) {
            throw new NoSuchMethodException(cls.getName() + Arrays.toString(argClasses(args)));
        }
        return o;
    }

    private static Object tryConstruct(Class<?> cls, Object... args) throws Exception {
        if (cls == null) {
            return null;
        }
        Class<?>[] types = argClasses(args);
        Constructor<?> exact = null;
        Constructor<?> assignable = null;
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            Class<?>[] pts = c.getParameterTypes();
            if (pts.length != types.length) {
                continue;
            }
            boolean isExact = true;
            boolean isAssign = true;
            for (int i = 0; i < pts.length; i++) {
                if (types[i] == null) {
                    continue;
                }
                if (pts[i] != types[i]) {
                    isExact = false;
                }
                if (!matches(pts[i], types[i])) {
                    isAssign = false;
                    break;
                }
            }
            if (isExact) {
                exact = c;
                break;
            }
            if (isAssign && assignable == null) {
                assignable = c;
            }
        }
        Constructor<?> c = exact != null ? exact : assignable;
        if (c == null) {
            return null;
        }
        c.setAccessible(true);
        return c.newInstance(args);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) {
            throw new NoSuchFieldException(target.getClass().getName() + "#" + name);
        }
        return f.get(target);
    }

    private static Object requireField(Object target, String name) throws Exception {
        return getField(target, name);
    }

    private static Object tryGetField(Object target, String name) {
        try {
            Field f = findField(target.getClass(), name);
            return f == null ? null : f.get(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) {
            throw new NoSuchFieldException(target.getClass().getName() + "#" + name);
        }
        f.set(target, value);
    }

    private static void trySetField(Object target, String name, Object value) {
        if (value == null) {
            return;
        }
        try {
            setField(target, name, value);
        } catch (Exception ignored) {
        }
    }

    private static Object getStatic(Class<?> cls, String field) throws Exception {
        Field f = findField(cls, field);
        if (f == null) {
            throw new NoSuchFieldException(cls.getName() + "#" + field);
        }
        return f.get(null);
    }

    private static Field findField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
            try {
                Field f = c.getField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>[] params) {
        Method fallback = null;
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) {
                    continue;
                }
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length != params.length) {
                    continue;
                }
                boolean exact = true;
                boolean assign = true;
                for (int i = 0; i < pts.length; i++) {
                    if (params[i] == null) {
                        continue;
                    }
                    if (pts[i] != params[i]) {
                        exact = false;
                    }
                    if (!matches(pts[i], params[i])) {
                        assign = false;
                        break;
                    }
                }
                if (exact) {
                    return m;
                }
                if (assign && fallback == null) {
                    fallback = m;
                }
            }
        }
        for (Method m : cls.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != params.length) {
                continue;
            }
            Class<?>[] pts = m.getParameterTypes();
            boolean assign = true;
            for (int i = 0; i < pts.length; i++) {
                if (params[i] != null && !matches(pts[i], params[i])) {
                    assign = false;
                    break;
                }
            }
            if (assign) {
                return m;
            }
        }
        return fallback;
    }

    private static Class<?>[] argClasses(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null ? null : args[i].getClass();
        }
        return types;
    }

    private static boolean matches(Class<?> declared, Class<?> actual) {
        if (actual == null) {
            return true;
        }
        if (declared.isAssignableFrom(actual)) {
            return true;
        }
        if (declared == boolean.class && actual == Boolean.class) return true;
        if (declared == int.class && actual == Integer.class) return true;
        if (declared == long.class && actual == Long.class) return true;
        if (declared == float.class && actual == Float.class) return true;
        if (declared == double.class && actual == Double.class) return true;
        if (declared == byte.class && actual == Byte.class) return true;
        if (declared == short.class && actual == Short.class) return true;
        if (declared == char.class && actual == Character.class) return true;
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(String className, String name) {
        return Enum.valueOf((Class<? extends Enum>) clazz(className), name);
    }
}
