package com.guild.world.recovery;

import com.guild.core.utils.DebugLog;
import com.guild.world.GuildWorldService;
import com.guild.world.model.GuildWorld;
import com.guild.world.model.WorldStatus;
import com.guild.world.model.WorldType;
import com.guild.world.registry.WorldJournal;
import com.guild.world.registry.WorldRegistry;
import com.guild.world.util.WorldFiles;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * 启动恢复服务 — 意外恢复逻辑链的核心执行者。
 *
 * <p>触发时机：插件 onEnable 后延迟到服务器 RUNNING 状态执行
 * （Folia 禁止在 STARTUP 阶段加载/创建世界）。
 *
 * <p>处理流程：
 * <ol>
 *   <li>崩溃检测：上次关服是否为干净关服（cleanShutdown）+ journal 是否有未完成操作；</li>
 *   <li>补偿中断操作：DELETE 中断 → 幂等重试删除；CREATE/LOAD/PASTE 中断 → 交给状态分类；</li>
 *   <li>注册表分类：已加载 → READY；文件夹存在未加载 → STALE；文件夹缺失 → 孤儿记录清除；</li>
 *   <li>检测"受管前缀但未注册"的已加载世界（外部/手动创建，仅报告）；</li>
 *   <li>按配置策略自动恢复 STALE（默认关闭，保守：只标记不清除）；</li>
 *   <li>写 recovery-report 报告 + 清空 journal + 保存注册表。</li>
 * </ol>
 */
public class WorldRecoveryService {

    /** 一条待处理的残留世界记录 */
    public record StaleWorld(GuildWorld world, String reason) {
    }

    private final Logger logger;
    private final List<StaleWorld> staleWorlds = new ArrayList<>();
    private final List<String> orphanRecords = new ArrayList<>();
    private final List<String> unregisteredPrefixWorlds = new ArrayList<>();
    private final List<String> interruptedOps = new ArrayList<>();

    private boolean lastShutdownWasClean = true;
    private boolean crashDetected = false;
    private boolean recoveryRan = false;

    public WorldRecoveryService(Logger logger) {
        this.logger = logger;
    }

    /**
     * 记录上次关服的干净状态（由 {@link GuildWorldService#load()} 在启动早期调用）。
     */
    public void recordShutdownState(boolean cleanShutdown) {
        this.lastShutdownWasClean = cleanShutdown;
    }

    /**
     * 执行恢复自检。
     */
    public void runRecovery(GuildWorldService service) {
        WorldRegistry registry = service.getRegistry();
        WorldJournal journal = service.getJournal();

        // 1. 崩溃检测
        List<WorldJournal.PendingOp> pending = journal.pending();
        crashDetected = !lastShutdownWasClean || !pending.isEmpty();
        if (crashDetected) {
            // 有中断操作时始终告警；仅“上次非干净关服但无可恢复项”的噪音进详细日志
            if (!pending.isEmpty()) {
                logger.warning("[World] CRASH DETECTED: previous shutdown was not clean "
                        + "or journal has " + pending.size() + " interrupted operation(s).");
            } else {
                DebugLog.warning(logger, "[World] CRASH DETECTED: previous shutdown was not clean "
                        + "or journal has 0 interrupted operation(s).");
            }
        }

        // 2. 补偿中断操作
        for (WorldJournal.PendingOp p : pending) {
            interruptedOps.add(p.world() + " [" + p.op() + "] x" + p.count());
            switch (p.op()) {
                case DELETE:
                    // 删除被中断：幂等重试删除文件夹（兼容经典 / Paper26 路径）
                    if (WorldFiles.worldDirectoryExists(p.world())
                            && !WorldFiles.deleteWorldDirectory(p.world())) {
                        logger.warning("[World] Recovery: failed to re-delete folder for '" + p.world()
                                + "' at " + WorldFiles.resolveWorldDirectory(p.world()));
                    }
                    registry.remove(p.world());
                    break;
                default:
                    // CREATE/LOAD/UNLOAD/PASTE 中断：交给步骤 3 按文件夹存在性分类
                    break;
            }
        }

        // 3. 注册表状态分类
        for (GuildWorld gw : new ArrayList<>(registry.all())) {
            WorldStatus status = gw.getStatus();
            if (status == WorldStatus.REGISTERED || status == WorldStatus.UNLOADED) {
                continue;
            }
            World loaded = Bukkit.getWorld(gw.getWorldName());
            if (loaded != null) {
                // 服务端已加载（例如 bukkit.yml 配置了该世界）→ 直接恢复 READY
                gw.setStatus(WorldStatus.READY);
                gw.touch();
                DebugLog.info(logger, "[World] Recovery: world '" + gw.getWorldName() + "' already loaded, restored to READY.");
                continue;
            }
            if (WorldFiles.worldDirectoryExists(gw.getWorldName())) {
                // 文件夹存在但未加载 → 上次异常遗留
                gw.setStatus(WorldStatus.STALE);
                gw.touch();
                staleWorlds.add(new StaleWorld(gw, "world folder exists but world is not loaded (crashed leftover) at "
                        + WorldFiles.resolveWorldDirectory(gw.getWorldName()).getPath()));
                if (gw.getType() == WorldType.EDIT) {
                    logger.warning("[World] Recovery: EDIT world '" + gw.getWorldName()
                            + "' left behind — safe to discard via /guildworld restore --delete");
                }
            } else {
                // 文件夹缺失 → 孤儿记录，清除
                orphanRecords.add(gw.getWorldName());
                registry.remove(gw.getWorldName());
                logger.warning("[World] Recovery: removed orphaned record '" + gw.getWorldName()
                        + "' (folder missing).");
            }
        }

        // 4. 检测受管前缀但未注册的已加载世界（仅报告，不处理）
        String prefix = service.getWorldNamePrefix();
        if (prefix != null && !prefix.isEmpty()) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().startsWith(prefix) && !registry.contains(world.getName())) {
                    unregisteredPrefixWorlds.add(world.getName());
                }
            }
        }

        // 5. 按策略自动恢复 STALE（默认关闭；EDIT 世界永不自动恢复）
        if (service.isAutoLoadStale()) {
            for (StaleWorld sw : new ArrayList<>(staleWorlds)) {
                if (sw.world().getType() == WorldType.EDIT) {
                    continue;
                }
                logger.info("[World] Recovery: auto-loading stale world '" + sw.world().getWorldName() + "'");
                service.loadWorld(sw.world().getWorldName()).exceptionally(ex -> {
                    logger.severe("[World] Recovery: auto-load failed for '"
                            + sw.world().getWorldName() + "': " + ex.getMessage());
                    return null;
                });
            }
            staleWorlds.removeIf(sw -> sw.world().getType() != WorldType.EDIT
                    && Bukkit.getWorld(sw.world().getWorldName()) != null);
        }

        // 6. 收尾：保存注册表、清空 journal、写报告
        registry.save();
        journal.clear();
        writeReport(service);
        recoveryRan = true;

        String summary = "[World] Recovery check finished: crash=" + crashDetected
                + ", stale=" + staleWorlds.size()
                + ", orphaned-records=" + orphanRecords.size()
                + ", unregistered=" + unregisteredPrefixWorlds.size();
        boolean actionable = !staleWorlds.isEmpty() || !orphanRecords.isEmpty()
                || !unregisteredPrefixWorlds.isEmpty() || !interruptedOps.isEmpty();
        if (actionable) {
            logger.info(summary);
        } else {
            DebugLog.info(logger, summary);
        }
    }

    /** 未启用自检时直接标记已执行（跳过）。 */
    public void markRan() {
        this.recoveryRan = true;
    }

    private void writeReport(GuildWorldService service) {
        File report = new File(service.getWorldsDir(), "recovery-report.log");
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .append("] World Recovery Report\n");
        sb.append("========================================================\n");
        sb.append("Crash detected: ").append(crashDetected).append('\n');
        sb.append("Interrupted operations: ")
                .append(interruptedOps.isEmpty() ? "none" : String.join(", ", interruptedOps)).append('\n');
        sb.append("Orphaned records removed: ")
                .append(orphanRecords.isEmpty() ? "none" : String.join(", ", orphanRecords)).append('\n');
        sb.append("Stale worlds (need attention):\n");
        if (staleWorlds.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (StaleWorld sw : staleWorlds) {
                sb.append("  - ").append(sw.world().getWorldName())
                        .append(" (").append(sw.world().getType()).append("): ").append(sw.reason()).append('\n');
            }
        }
        sb.append("Unregistered worlds with managed prefix:\n");
        if (unregisteredPrefixWorlds.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (String name : unregisteredPrefixWorlds) {
                sb.append("  - ").append(name).append('\n');
            }
        }
        sb.append("Handling: /guildworld restore --list | --load <name> | --delete <name>\n");
        try (FileWriter fw = new FileWriter(report, false)) {
            fw.write(sb.toString());
        } catch (IOException e) {
            logger.warning("[World] Failed to write recovery report: " + e.getMessage());
        }
    }

    /* ── 查询 ────────────────────────────────────────────── */

    public boolean hasIssues() {
        return !staleWorlds.isEmpty() || !unregisteredPrefixWorlds.isEmpty();
    }

    public boolean isCrashDetected() {
        return crashDetected;
    }

    public boolean hasRan() {
        return recoveryRan;
    }

    public List<StaleWorld> getStaleWorlds() {
        return new ArrayList<>(staleWorlds);
    }

    public List<String> getOrphanRecords() {
        return new ArrayList<>(orphanRecords);
    }

    public List<String> getUnregisteredPrefixWorlds() {
        return new ArrayList<>(unregisteredPrefixWorlds);
    }
}
