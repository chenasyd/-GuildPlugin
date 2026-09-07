package com.guild.world.bridge;

import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.lang.reflect.InvocationTargetException;

/**
 * Folia 运行时创建/加载世界的反射桥接（对齐 Skyllia WorldNMS）。
 *
 * <p>实现拆分为 {@link FoliaPlatformProbe}、{@link FoliaClassicWorldCreator}、
 * {@link FoliaPaper26WorldCreator} 与 {@link FoliaChunkBootstrap}。
 *
 * @see FoliaNmsReflection
 * @see FoliaWorldCreationSupport
 */
public final class FoliaWorldCreator {

    private FoliaWorldCreator() {
    }

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
        FoliaPlatformProbe.ensureNmsClassLoader();
        String mc = FoliaPlatformProbe.minecraftVersion();
        FoliaPlatformProbe.logger().info("[World] [Folia] Creating world '" + creator.name()
                + "' (mc=" + mc + ", seed=" + creator.seed() + ", env=" + creator.environment() + ")");
        if (FoliaPlatformProbe.isPaper26Family(mc)) {
            return FoliaPaper26WorldCreator.create(creator);
        }
        return FoliaClassicWorldCreator.create(creator);
    }
}
