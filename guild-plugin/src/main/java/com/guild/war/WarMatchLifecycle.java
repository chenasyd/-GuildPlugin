package com.guild.war;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.war.event.WarMatchEndEvent;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarReportSnapshot;
import com.guild.war.model.WarTeamSide;
import com.guild.war.report.WarReportRepository;
import com.guild.world.GuildWorldService;
import com.guildplugin.util.FoliaTeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * 对局生命周期：进场准备、正常结算、异常清理与战场回收。
 */
public final class WarMatchLifecycle {

    private final GuildPlugin plugin;
    private final GuildWorldService worldService;
    private final WarMatchRegistry registry;
    private final WarBroadcastHelper broadcast;
    private final WarMatchScheduler scheduler;
    private final WarReportRepository reportRepository;
    private final Supplier<WarSettings> settings;

    public WarMatchLifecycle(GuildPlugin plugin,
                             GuildWorldService worldService,
                             WarMatchRegistry registry,
                             WarBroadcastHelper broadcast,
                             WarMatchScheduler scheduler,
                             WarReportRepository reportRepository,
                             Supplier<WarSettings> settings) {
        this.plugin = plugin;
        this.worldService = worldService;
        this.registry = registry;
        this.broadcast = broadcast;
        this.scheduler = scheduler;
        this.reportRepository = reportRepository;
        this.settings = settings;
    }

    public synchronized void beginPreparing(WarMatch match, Runnable onCountdownStart) {
        if (match.phase() != WarPhase.SIGNUP) {
            return;
        }
        scheduler.cancel(match.id());
        match.setPhase(WarPhase.PREPARING);
        broadcast.broadcastMatch(match, "war.broadcast.preparing", "&a报名结束，正在创建战场…");

        String worldKey = "war" + match.id() + "_" + System.currentTimeMillis() % 100000;
        worldService.createArenaFromPreset(worldKey, match.presetName())
                .whenComplete((result, err) -> CompatibleScheduler.runTask(plugin, () -> {
                    if (err != null) {
                        plugin.getLogger().log(Level.SEVERE, "[GuildWar] Failed to create arena", err);
                        String errMsg = err.getMessage() != null ? err.getMessage() : err.toString();
                        broadcast.broadcastMatch(match, "war.broadcast.arena-fail",
                                "&c创建战场失败: {error}",
                                "{error}", errMsg);
                        cleanupMatch(match, false);
                        return;
                    }
                    match.setWorldName(result.world().getWorldName());
                    match.setSpawnA(result.spawns().spawnA());
                    match.setSpawnB(result.spawns().spawnB());
                    match.setSpectatorSpawn(result.spawns().spectator());
                    teleportParticipants(match).thenRun(onCountdownStart);
                }));
    }

    public synchronized void endMatch(WarMatch match, Integer winnerGuildId, String reasonKey) {
        if (match.phase() == WarPhase.ENDED) {
            return;
        }
        scheduler.cancel(match.id());
        match.setPhase(WarPhase.ENDED);
        match.setWinnerGuildId(winnerGuildId);
        match.setEndReason(reasonKey);

        WarSettings current = settings.get();
        WarReportSnapshot snapshot = WarReportSnapshot.fromMatch(match, current.seasonId);
        try {
            Bukkit.getPluginManager().callEvent(new WarMatchEndEvent(snapshot));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GuildWar] WarMatchEndEvent listener error", e);
        }
        reportRepository.saveAsync(snapshot).whenComplete((saved, err) -> {
            if (err != null) {
                plugin.getLogger().log(Level.WARNING, "[GuildWar] Report save failed", err);
            }
            if (current.broadcastReport) {
                CompatibleScheduler.runTask(plugin, () -> broadcast.broadcastReportLines(saved != null ? saved : snapshot));
            }
        });

        String winnerPh = winnerGuildId == null
                ? "war.draw"
                : (winnerGuildId == match.guildAId() ? match.guildAName() : match.guildBName());
        broadcast.broadcastMatch(match, "war.broadcast.ended",
                "&6对局结束！&e{winner} &7（{reason}） | 比分 &a{sa} &7: &c{sb}",
                "{winner}", winnerPh,
                "{reason}", reasonKey != null ? reasonKey : "",
                "{score}", String.valueOf(match.scoreToWin()),
                "{sa}", String.valueOf(match.scoreA()),
                "{sb}", String.valueOf(match.scoreB()));

        List<CompletableFuture<Boolean>> tps = new ArrayList<>();
        for (WarParticipant p : match.participantList()) {
            Player player = Bukkit.getPlayer(p.uuid());
            if (player != null && player.isOnline()) {
                CompatibleScheduler.runTask(plugin, player, () -> {
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                });
                tps.add(worldService.teleportToFallbackWorld(player));
            }
            registry.unlinkPlayer(p.uuid());
        }
        CompletableFuture.allOf(tps.toArray(new CompletableFuture[0]))
                .whenComplete((v, e) -> CompatibleScheduler.runTaskLater(plugin, () -> {
                    destroyArena(match);
                    unregisterMatch(match);
                }, 40L));
    }

    public void cleanupMatch(WarMatch match, boolean destroyWorld) {
        scheduler.cancel(match.id());
        match.setPhase(WarPhase.ENDED);
        for (UUID uuid : new ArrayList<>(match.participants().keySet())) {
            registry.unlinkPlayer(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && match.worldName() != null
                    && player.getWorld().getName().equals(match.worldName())) {
                worldService.teleportToFallbackWorld(player);
            }
        }
        if (destroyWorld) {
            destroyArena(match);
        }
        unregisterMatch(match);
    }

    private CompletableFuture<Void> teleportParticipants(WarMatch match) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (WarParticipant p : match.participantList()) {
            Player player = Bukkit.getPlayer(p.uuid());
            if (player == null || !player.isOnline()) {
                continue;
            }
            Location dest = p.side() == WarTeamSide.A ? match.spawnA() : match.spawnB();
            if (dest == null) {
                continue;
            }
            CompatibleScheduler.runTask(plugin, player, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setFireTicks(0);
            });
            futures.add(FoliaTeleportUtils.safeTeleport(plugin, player, dest.clone()));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void destroyArena(WarMatch match) {
        String world = match.worldName();
        if (world == null) {
            return;
        }
        String policy = worldService.getPostMatchPolicy();
        worldService.deleteWorld(world, true).whenComplete((v, err) -> {
            if (err != null) {
                plugin.getLogger().log(Level.WARNING, "[GuildWar] Failed to delete arena " + world
                        + " (policy=" + policy + ")", err);
            }
        });
    }

    private void unregisterMatch(WarMatch match) {
        scheduler.cancel(match.id());
        registry.unregister(match);
    }
}
