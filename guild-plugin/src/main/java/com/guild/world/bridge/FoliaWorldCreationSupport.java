package com.guild.world.bridge;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/** 两条创建路径共用的 NMS 辅助步骤。 */
final class FoliaWorldCreationSupport {

    private FoliaWorldCreationSupport() {
    }

    static void assertServerReady(Object console) throws Exception {
        Iterable<?> allLevels = (Iterable<?>) FoliaNmsReflection.tryInvoke(console, "getAllLevels");
        if (allLevels != null && !allLevels.iterator().hasNext()) {
            throw new IllegalStateException("Cannot create additional worlds on STARTUP");
        }
    }

    static ChunkGenerator resolveGenerator(Object craftServer, WorldCreator creator) throws Exception {
        ChunkGenerator generator = creator.generator();
        if (generator == null) {
            generator = (ChunkGenerator) FoliaNmsReflection.invoke(craftServer, "getGenerator", creator.name());
        }
        return generator;
    }

    static BiomeProvider resolveBiomeProvider(Object craftServer, WorldCreator creator,
                                              ChunkGenerator generator) throws Exception {
        BiomeProvider biomeProvider = creator.biomeProvider();
        if (biomeProvider == null) {
            biomeProvider = (BiomeProvider) FoliaNmsReflection.tryInvoke(craftServer, "getBiomeProvider", creator.name());
        }
        if (biomeProvider == null && generator != null) {
            biomeProvider = generator.getDefaultBiomeProvider(null);
        }
        return biomeProvider;
    }

    static Object levelStemKey(World.Environment env) throws Exception {
        Class<?> levelStem = FoliaNmsReflection.clazz("net.minecraft.world.level.dimension.LevelStem");
        return switch (env) {
            case NORMAL -> FoliaNmsReflection.getStatic(levelStem, "OVERWORLD");
            case NETHER -> FoliaNmsReflection.getStatic(levelStem, "NETHER");
            case THE_END -> FoliaNmsReflection.getStatic(levelStem, "END");
            default -> throw new IllegalArgumentException("Illegal dimension " + env);
        };
    }

    static Object openSession(Object craftServer, String name, Object actualDimension) throws Exception {
        File container = (File) FoliaNmsReflection.invoke(craftServer, "getWorldContainer");
        Class<?> storage = FoliaNmsReflection.clazz("net.minecraft.world.level.storage.LevelStorageSource");
        Object source = FoliaNmsReflection.invokeStatic(storage, "createDefault", container.toPath());
        return FoliaNmsReflection.invoke(source, "validateAndCreateAccess", name, actualDimension);
    }

    static Object dataLoadContext(Object console) throws Exception {
        Object ctx = FoliaNmsReflection.tryGetField(console, "worldLoaderContext");
        if (ctx != null) {
            return ctx;
        }
        ctx = FoliaNmsReflection.tryGetField(console, "worldLoader");
        if (ctx != null) {
            return ctx;
        }
        throw new NoSuchFieldException(console.getClass().getName() + "#worldLoaderContext/worldLoader");
    }

    static Object readWorldDataTag(Object session) throws Exception {
        if (!(boolean) FoliaNmsReflection.invoke(session, "hasWorldData")) {
            return null;
        }
        Object dynamic;
        try {
            dynamic = FoliaNmsReflection.invoke(session, "getDataTag");
        } catch (InvocationTargetException ex) {
            dynamic = FoliaNmsReflection.invoke(session, "getDataTagFallback");
            FoliaNmsReflection.tryInvoke(session, "restoreLevelDataFromOld");
        }
        if (dynamic == null) {
            Object wrapped = FoliaNmsReflection.tryInvokeStatic(
                    FoliaNmsReflection.clazz("io.papermc.paper.world.PaperWorldLoader"),
                    "getLevelData", session);
            if (wrapped != null) {
                dynamic = FoliaNmsReflection.tryInvoke(wrapped, "dataTag");
            }
        }
        if (dynamic != null) {
            Object summary = FoliaNmsReflection.invoke(session, "getSummary", dynamic);
            if ((boolean) FoliaNmsReflection.invoke(summary, "requiresManualConversion")
                    || !(boolean) FoliaNmsReflection.invoke(summary, "isCompatible")) {
                throw new IllegalStateException("World data incompatible or requires manual conversion");
            }
        }
        return dynamic;
    }

    static Object readExistingWorldGenSettings(Object storageSource, Object dimensionKey,
                                               Object registryAccess, Object wgsType) throws Exception {
        Object dataResult = FoliaNmsReflection.tryInvokeStatic(
                FoliaNmsReflection.clazz("net.minecraft.world.level.storage.LevelStorageSource"),
                "readExistingSavedData", storageSource, dimensionKey, registryAccess, wgsType);
        if (dataResult == null) {
            return null;
        }
        Object result = FoliaNmsReflection.tryInvoke(dataResult, "result");
        return FoliaNmsReflection.unwrapOptional(result);
    }

    static Object createGameRules(Object dataConfiguration) throws Exception {
        Object enabledFeatures = FoliaNmsReflection.tryInvoke(dataConfiguration, "enabledFeatures");
        if (enabledFeatures != null) {
            Object rules = FoliaNmsReflection.tryConstruct(
                    FoliaNmsReflection.tryClazz("net.minecraft.world.level.gamerules.GameRules"), enabledFeatures);
            if (rules != null) {
                return rules;
            }
            rules = FoliaNmsReflection.tryConstruct(
                    FoliaNmsReflection.tryClazz("net.minecraft.world.level.GameRules"), enabledFeatures);
            if (rules != null) {
                return rules;
            }
        }
        Object rules = FoliaNmsReflection.tryConstruct(
                FoliaNmsReflection.tryClazz("net.minecraft.world.level.gamerules.GameRules"));
        if (rules != null) {
            return rules;
        }
        return FoliaNmsReflection.construct(FoliaNmsReflection.clazz("net.minecraft.world.level.GameRules"));
    }

    static Object resolveGameType(Object craftServer) throws Exception {
        try {
            Object mode = FoliaNmsReflection.invoke(craftServer, "getDefaultGameMode");
            int id = (int) FoliaNmsReflection.invoke(mode, "getValue");
            Object byId = FoliaNmsReflection.tryInvokeStatic(
                    FoliaNmsReflection.clazz("net.minecraft.world.level.GameType"), "byId", id);
            if (byId != null) {
                return byId;
            }
        } catch (Exception ignored) {
        }
        return FoliaNmsReflection.enumConstant("net.minecraft.world.level.GameType", "SURVIVAL");
    }

    static Object createDimensionKey(Object console, String name, NamespacedKey key) throws Exception {
        try {
            Object props = FoliaNmsReflection.invoke(console, "getProperties");
            String levelName = String.valueOf(FoliaNmsReflection.getField(props, "levelName"));
            if (name.equals(levelName + "_nether")) {
                return FoliaNmsReflection.getStatic(FoliaNmsReflection.clazz("net.minecraft.world.level.Level"), "NETHER");
            }
            if (name.equals(levelName + "_the_end")) {
                return FoliaNmsReflection.getStatic(FoliaNmsReflection.clazz("net.minecraft.world.level.Level"), "END");
            }
        } catch (Exception ignored) {
        }
        Object id = createResourceId(key.getNamespace(), key.getKey());
        return FoliaNmsReflection.invokeStatic(FoliaNmsReflection.clazz("net.minecraft.resources.ResourceKey"),
                "create", registryKey("DIMENSION"), id);
    }

    static Object createResourceId(String namespace, String path) throws Exception {
        Class<?> identifier = FoliaNmsReflection.tryClazz("net.minecraft.resources.Identifier");
        if (identifier != null) {
            Object v = FoliaNmsReflection.tryInvokeStatic(identifier, "fromNamespaceAndPath", namespace, path);
            if (v != null) {
                return v;
            }
            Object c = FoliaNmsReflection.tryConstruct(identifier, namespace, path);
            if (c != null) {
                return c;
            }
        }
        Class<?> rl = FoliaNmsReflection.clazz("net.minecraft.resources.ResourceLocation");
        Object v = FoliaNmsReflection.tryInvokeStatic(rl, "fromNamespaceAndPath", namespace, path);
        if (v != null) {
            return v;
        }
        return FoliaNmsReflection.construct(rl, namespace, path);
    }

    static NamespacedKey creatorKey(WorldCreator creator) {
        try {
            Object key = WorldCreator.class.getMethod("key").invoke(creator);
            if (key instanceof NamespacedKey nk) {
                return nk;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return NamespacedKey.minecraft(creator.name().toLowerCase(Locale.ROOT));
    }

    static World finishWorld(Object serverLevel) throws Exception {
        Object craftWorld = FoliaNmsReflection.invoke(serverLevel, "getWorld");
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent((World) craftWorld));
        return (World) craftWorld;
    }

    static Object parseJson(String settings) throws Exception {
        String json = (settings == null || settings.isEmpty()) ? "{}" : settings;
        Object parsed = FoliaNmsReflection.tryInvokeStatic(
                FoliaNmsReflection.clazz("net.minecraft.util.GsonHelper"), "parse", json);
        if (parsed != null) {
            return parsed;
        }
        return FoliaNmsReflection.invokeStatic(FoliaNmsReflection.clazz("com.google.gson.GsonHelper"), "parse", json);
    }

    static Object registryKey(String field) throws Exception {
        return FoliaNmsReflection.getStatic(FoliaNmsReflection.clazz("net.minecraft.core.registries.Registries"), field);
    }

    static Object registryOrThrow(Object registryAccess, Object key) throws Exception {
        Object reg = FoliaNmsReflection.tryInvoke(registryAccess, "lookupOrThrow", key);
        if (reg != null) {
            return reg;
        }
        return FoliaNmsReflection.invoke(registryAccess, "registryOrThrow", key);
    }

    static Object registryGet(Object registry, Object key) throws Exception {
        Object value = FoliaNmsReflection.tryInvoke(registry, "getValue", key);
        if (value != null) {
            return value;
        }
        return FoliaNmsReflection.unwrapOptional(FoliaNmsReflection.tryInvoke(registry, "get", key));
    }
}
