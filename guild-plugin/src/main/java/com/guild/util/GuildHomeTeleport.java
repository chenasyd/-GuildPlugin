package com.guild.util;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ScheduledTaskHandle;
import com.guildplugin.util.FoliaTeleportUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 工会家传送（Spigot / Paper / Folia 安全）。
 *
 * <p>支持 {@code guild.home-teleport-delay} 倒计时；倒计时在玩家实体线程执行，
 * 最终传送走 {@link FoliaTeleportUtils#safeTeleport}。
 */
public final class GuildHomeTeleport {

    private GuildHomeTeleport() {
    }

    /**
     * @param closeInventory 开始倒计时时是否关闭 GUI
     * @param onSuccess      传送成功后回调（已在玩家实体线程）
     * @param onFailure      传送失败后回调（已在玩家实体线程）；可为 null
     */
    public static void start(GuildPlugin plugin, Player player, Location targetLocation,
                             boolean closeInventory,
                             Runnable onSuccess,
                             Consumer<String> onFailure) {
        if (plugin == null || player == null || !player.isOnline()) {
            return;
        }
        if (targetLocation == null || targetLocation.getWorld() == null) {
            if (onFailure != null) {
                CompatibleScheduler.runTask(plugin, player, () -> onFailure.accept("invalid-location"));
            }
            return;
        }

        int delay = 0;
        try {
            delay = plugin.getConfigManager().getMainConfig().getInt("guild.home-teleport-delay", 0);
        } catch (Exception ignored) {
        }

        if (delay <= 0) {
            executeTeleport(plugin, player, targetLocation, onSuccess, onFailure);
            return;
        }

        if (closeInventory) {
            player.closeInventory();
        }

        Location startLocation = player.getLocation().clone();
        World startWorld = startLocation.getWorld();
        int[] countdown = {delay};
        boolean[] done = {false};
        ScheduledTaskHandle[] handleRef = new ScheduledTaskHandle[1];
        LanguageManager lang = plugin.getLanguageManager();

        handleRef[0] = CompatibleScheduler.runTaskTimer(plugin, player, () -> {
            if (!player.isOnline() || done[0]) {
                if (handleRef[0] != null) {
                    handleRef[0].cancel();
                }
                return;
            }
            if (startWorld == null
                    || !startWorld.equals(player.getWorld())
                    || player.getLocation().distanceSquared(startLocation) > 0.5) {
                done[0] = true;
                if (handleRef[0] != null) {
                    handleRef[0].cancel();
                }
                String cancelled = lang != null
                        ? lang.getCoreMessage(player, "home.teleport-cancelled", "&c传送已取消（请不要移动）！")
                        : "&c传送已取消（请不要移动）！";
                player.sendMessage(ColorUtils.colorize(cancelled));
                return;
            }
            if (countdown[0] <= 0) {
                done[0] = true;
                if (handleRef[0] != null) {
                    handleRef[0].cancel();
                }
                executeTeleport(plugin, player, targetLocation, onSuccess, onFailure);
            } else {
                String msg = lang != null
                        ? lang.getCoreMessage(player, "home.teleporting",
                        "&a正在传送 &e{seconds} &a秒", "{seconds}", String.valueOf(countdown[0]))
                        : "&a正在传送 &e" + countdown[0] + " &a秒";
                NotifyUtils.sendActionBar(plugin, player, msg);
                countdown[0]--;
            }
        }, 0L, 20L);
    }

    public static CompletableFuture<Boolean> teleportNow(GuildPlugin plugin, Player player, Location target) {
        return FoliaTeleportUtils.safeTeleport(plugin, player, target);
    }

    private static void executeTeleport(GuildPlugin plugin, Player player, Location target,
                                        Runnable onSuccess, Consumer<String> onFailure) {
        FoliaTeleportUtils.safeTeleport(plugin, player, target).whenComplete((ok, err) ->
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (Boolean.TRUE.equals(ok) && err == null) {
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    } else if (onFailure != null) {
                        onFailure.accept(err != null ? err.getMessage() : "teleport-failed");
                    }
                }));
    }
}
