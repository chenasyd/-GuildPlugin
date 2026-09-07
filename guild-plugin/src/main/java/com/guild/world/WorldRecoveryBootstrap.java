package com.guild.world;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.DebugLog;
import com.guild.world.recovery.WorldRecoveryService;
import com.guild.world.registry.WorldRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * 启动时注册表加载与恢复自检调度（玩家加入优先 + 延迟兜底）。
 */
public final class WorldRecoveryBootstrap {

    private static final long RECOVERY_DELAY_TICKS = 100L;

    private final GuildPlugin plugin;
    private final WorldRegistry registry;
    private final WorldRecoveryService recovery;
    private final AtomicBoolean recoveryTriggered = new AtomicBoolean(false);
    private final Supplier<Boolean> enabled;
    private final Supplier<String> disabledMessage;
    private final Supplier<Boolean> recoveryCheckEnabled;

    public WorldRecoveryBootstrap(GuildPlugin plugin,
                                  WorldRegistry registry,
                                  WorldRecoveryService recovery,
                                  Supplier<Boolean> enabled,
                                  Supplier<String> disabledMessage,
                                  Supplier<Boolean> recoveryCheckEnabled) {
        this.plugin = plugin;
        this.registry = registry;
        this.recovery = recovery;
        this.enabled = enabled;
        this.disabledMessage = disabledMessage;
        this.recoveryCheckEnabled = recoveryCheckEnabled;
    }

    /**
     * 插件 onEnable 时调用：加载注册表 + 标记本次运行开始（cleanShutdown=false）。
     */
    public void load() {
        registry.load();
        boolean wasClean = registry.isCleanShutdown();
        if (!wasClean) {
            DebugLog.warning(plugin.getLogger(),
                    "[World] Previous shutdown was NOT clean. Startup recovery check will run.");
        }
        registry.setCleanShutdown(false);
        registry.save();
        recovery.recordShutdownState(wasClean);
        DebugLog.info(plugin.getLogger(), "[World] Registry loaded: " + registry.size() + " managed world(s).");
    }

    /**
     * 调度启动恢复自检；{@link #recoveryTriggered} 保证只执行一次。
     */
    public void scheduleRecovery(GuildWorldService service) {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                CompatibleScheduler.runTask(plugin, () -> runRecoveryOnce(service, "player-join"));
            }
        }, plugin);

        CompatibleScheduler.runTaskLater(plugin, () -> runRecoveryOnce(service, "scheduled"), RECOVERY_DELAY_TICKS);
    }

    /**
     * 执行启动恢复自检（崩溃检测 + 残留世界分类处理）。
     */
    public void runRecovery(GuildWorldService service) {
        if (!enabled.get()) {
            plugin.getLogger().warning("[World] Skipping startup recovery: " + disabledMessage.get());
            recovery.markRan();
            return;
        }
        if (!recoveryCheckEnabled.get()) {
            DebugLog.info(plugin.getLogger(), "[World] Recovery check disabled by config, skipping.");
            recovery.markRan();
            return;
        }
        try {
            recovery.runRecovery(service);
        } catch (Throwable e) {
            plugin.getLogger().log(Level.SEVERE, "[World] Recovery check failed", e);
        }
    }

    private void runRecoveryOnce(GuildWorldService service, String source) {
        if (!recoveryTriggered.compareAndSet(false, true)) {
            return;
        }
        DebugLog.info(plugin.getLogger(), "[World] Startup recovery check triggered by " + source + ".");
        runRecovery(service);
    }
}
