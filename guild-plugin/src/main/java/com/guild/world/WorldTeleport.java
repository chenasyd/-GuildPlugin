package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.world.model.GuildWorld;
import com.guild.world.registry.WorldRegistry;
import com.guildplugin.util.FoliaTeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 受管世界传送：出生点解析、虚空落地平台与安全回退世界。
 */
public final class WorldTeleport {

    private final GuildPlugin plugin;
    private final WorldRegistry registry;
    private final WorldLifecycle lifecycle;
    private final Supplier<Boolean> enabled;
    private final Supplier<String> disabledMessage;
    private final Function<String, String> buildWorldName;
    private final Supplier<String> fallbackWorldName;

    public WorldTeleport(GuildPlugin plugin,
                         WorldRegistry registry,
                         WorldLifecycle lifecycle,
                         Supplier<Boolean> enabled,
                         Supplier<String> disabledMessage,
                         Function<String, String> buildWorldName,
                         Supplier<String> fallbackWorldName) {
        this.plugin = plugin;
        this.registry = registry;
        this.lifecycle = lifecycle;
        this.enabled = enabled;
        this.disabledMessage = disabledMessage;
        this.buildWorldName = buildWorldName;
        this.fallbackWorldName = fallbackWorldName;
    }

    /**
     * 将玩家传送到受管世界出生点（必要时先加载；虚空世界自动铺落地平台）。
     */
    public CompletableFuture<Boolean> teleportToWorld(Player player, String worldName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (!enabled.get()) {
            future.completeExceptionally(new IllegalStateException(disabledMessage.get()));
            return future;
        }
        if (player == null || !player.isOnline()) {
            future.complete(false);
            return future;
        }
        String name = buildWorldName.apply(worldName);
        GuildWorld gw = registry.get(name);
        if (gw == null) {
            future.completeExceptionally(new IllegalArgumentException("World is not managed: " + name));
            return future;
        }

        Runnable afterLoaded = () -> {
            World world = Bukkit.getWorld(name);
            if (world == null) {
                future.completeExceptionally(new IllegalStateException("World not loaded: " + name));
                return;
            }
            Location dest = resolveTeleportLocation(gw, world);
            ensureVoidPlatform(dest).thenCompose(ok ->
                    FoliaTeleportUtils.safeTeleport(plugin, player, dest)
            ).whenComplete((success, err) -> {
                if (err != null) {
                    future.completeExceptionally(err);
                    return;
                }
                if (Boolean.TRUE.equals(success)) {
                    gw.touch();
                    registry.save();
                }
                future.complete(Boolean.TRUE.equals(success));
            });
        };

        if (Bukkit.getWorld(name) != null) {
            CompatibleScheduler.runTask(plugin, afterLoaded);
            return future;
        }

        lifecycle.loadWorld(name).whenComplete((loaded, err) -> {
            if (err != null) {
                future.completeExceptionally(err);
                return;
            }
            CompatibleScheduler.runTask(plugin, afterLoaded);
        });
        return future;
    }

    /**
     * 传送到安全回退世界（配置 {@code world.safety.fallback-world}）。
     */
    public CompletableFuture<Boolean> teleportToFallbackWorld(Player player) {
        Location fallback = fallbackLocation();
        if (fallback == null) {
            return CompletableFuture.completedFuture(false);
        }
        return FoliaTeleportUtils.safeTeleport(plugin, player, fallback);
    }

    /** 卸载/删除/关服时踢出玩家到回退世界（同步发起传送，不等待结果）。 */
    public void teleportToFallback(Player player) {
        Location fallback = fallbackLocation();
        if (fallback != null) {
            FoliaTeleportUtils.safeTeleport(plugin, player, fallback);
        }
    }

    public Location getFallbackLocation() {
        return fallbackLocation();
    }

    Location resolveTeleportLocation(GuildWorld gw, World world) {
        Location spawn = gw.parseSpawnLocation();
        if (spawn != null && spawn.getWorld() != null) {
            return spawn;
        }
        Location fixed = world.getSpawnLocation();
        if (fixed.getBlockY() < world.getMinHeight() + 2) {
            fixed.setY(64);
        }
        return fixed;
    }

    /**
     * 在目标位置下方铺 3x3 平台（仅当脚下为空气时），避免虚空坠落。
     */
    CompletableFuture<Boolean> ensureVoidPlatform(Location dest) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompatibleScheduler.runTask(plugin, dest, () -> {
            try {
                World world = dest.getWorld();
                if (world == null) {
                    future.complete(false);
                    return;
                }
                int baseY = dest.getBlockY() - 1;
                if (baseY < world.getMinHeight()) {
                    baseY = Math.min(63, world.getMaxHeight() - 2);
                    dest.setY(baseY + 1);
                }
                int cx = dest.getBlockX();
                int cz = dest.getBlockZ();
                boolean placed = false;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Block block = world.getBlockAt(cx + dx, baseY, cz + dz);
                        if (block.getType().isAir() || !block.getType().isSolid()) {
                            block.setType(Material.STONE, false);
                            placed = true;
                        }
                    }
                }
                for (int dy = 0; dy <= 1; dy++) {
                    Block air = world.getBlockAt(cx, baseY + 1 + dy, cz);
                    if (!air.getType().isAir() && air.getType().isSolid()) {
                        air.setType(Material.AIR, false);
                    }
                }
                dest.setX(cx + 0.5);
                dest.setY(baseY + 1);
                dest.setZ(cz + 0.5);
                if (placed) {
                    plugin.getLogger().info("[World] Spawn platform ensured at "
                            + world.getName() + " " + cx + "," + baseY + "," + cz);
                }
                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private Location fallbackLocation() {
        World world = Bukkit.getWorld(fallbackWorldName.get());
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        return world == null ? null : world.getSpawnLocation();
    }
}
