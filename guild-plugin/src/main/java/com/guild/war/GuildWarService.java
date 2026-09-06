package com.guild.war;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.language.LocalizedException;
import com.guild.models.Guild;
import com.guild.services.GuildService;
import com.guild.war.event.WarMatchStartEvent;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import com.guild.war.report.WarReportRepository;
import com.guild.world.GuildWorldService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 公会战核心服务：对外 Facade，委托命令、编排、战斗与生命周期子模块。
 */
public final class GuildWarService {

    private final GuildPlugin plugin;
    private final GuildService guildService;
    private final GuildWorldService worldService;
    private final WarReportRepository reportRepository;
    private final WarMatchRegistry registry = new WarMatchRegistry();
    private final WarBroadcastHelper broadcast;
    private final WarMatchScheduler scheduler;
    private final WarMatchLifecycle lifecycle;
    private final WarCombatRules combat;
    private final WarChallengeCommands commands;
    private WarSettings settings;

    public GuildWarService(GuildPlugin plugin, GuildService guildService, GuildWorldService worldService) {
        this.plugin = plugin;
        this.guildService = guildService;
        this.worldService = worldService;
        this.reportRepository = new WarReportRepository(plugin);
        this.broadcast = new WarBroadcastHelper(plugin, guildService);
        this.scheduler = new WarMatchScheduler(plugin);
        this.lifecycle = new WarMatchLifecycle(
                plugin, worldService, registry, broadcast, scheduler, reportRepository, () -> settings);
        this.combat = new WarCombatRules(
                plugin, worldService, registry, broadcast, lifecycle, () -> settings);
        reloadSettings();
        this.commands = new WarChallengeCommands(
                guildService, worldService, registry, broadcast, scheduler, lifecycle, () -> settings,
                this::isEnabled, this::unavailableReason, signupFlow());
    }

    private WarMatchSignupFlow signupFlow() {
        return new WarMatchSignupFlow() {
            @Override
            public void scheduleChallengeTimeout(WarMatch match) {
                GuildWarService.this.scheduleChallengeTimeout(match);
            }

            @Override
            public void scheduleSignupTimeout(WarMatch match) {
                GuildWarService.this.scheduleSignupTimeout(match);
            }

            @Override
            public void beginPreparing(WarMatch match) {
                lifecycle.beginPreparing(match, () -> GuildWarService.this.startCountdown(match));
            }
        };
    }

    public WarReportRepository reports() {
        return reportRepository;
    }

    public GuildPlugin getPlugin() {
        return plugin;
    }

    public void reloadSettings() {
        this.settings = new WarSettings(plugin.getConfigManager().getMainConfig());
    }

    public WarSettings settings() {
        return settings;
    }

    public boolean isEnabled() {
        return settings.enabled && worldService != null && worldService.isEnabled();
    }

    public String unavailableReason() {
        if (!settings.enabled) {
            return CoreMsg.rawDefault(plugin, "war.disabled.config", "&c公会战已在配置中关闭");
        }
        if (worldService == null) {
            return CoreMsg.rawDefault(plugin, "war.disabled.world-uninit", "&c世界管理未初始化");
        }
        if (!worldService.isEnabled()) {
            return worldService.unsupportedMessage();
        }
        return "";
    }

    public Collection<WarMatch> getActiveMatches() {
        return registry.allMatches();
    }

    public WarMatch getMatch(int id) {
        return registry.getMatch(id);
    }

    public WarMatch getMatchByPlayer(UUID uuid) {
        return registry.getByPlayer(uuid);
    }

    public WarMatch getMatchByGuild(int guildId) {
        return registry.getByGuild(guildId);
    }

    public CompletableFuture<WarMatch> challenge(Player player, String targetGuildQuery,
                                                 String presetOverride, VictoryMode modeOverride,
                                                 Integer maxOverride, Integer scoreOverride,
                                                 Integer durationOverride) {
        return commands.challenge(player, targetGuildQuery, presetOverride, modeOverride,
                maxOverride, scoreOverride, durationOverride);
    }

    public CompletableFuture<WarMatch> accept(Player player) {
        return commands.accept(player);
    }

    public CompletableFuture<Void> deny(Player player) {
        return commands.deny(player);
    }

    public CompletableFuture<Void> cancel(Player player) {
        return commands.cancel(player);
    }

    public CompletableFuture<Void> join(Player player) {
        return commands.join(player);
    }

    public CompletableFuture<Void> leave(Player player) {
        return commands.leave(player);
    }

    public CompletableFuture<Void> ready(Player player) {
        return commands.ready(player);
    }

    public CompletableFuture<Void> forceEnd(int matchId, String reason) {
        return commands.forceEnd(matchId, reason);
    }

    public boolean isInWarWorld(Player player) {
        WarMatch m = getMatchByPlayer(player.getUniqueId());
        return m != null && m.worldName() != null && player.getWorld() != null
                && player.getWorld().getName().equals(m.worldName());
    }

    /** 是否为当前进行中对局的战场世界名。 */
    public boolean isArenaWorld(String worldName) {
        if (worldName == null) {
            return false;
        }
        return registry.isArenaWorld(worldName);
    }

    public boolean shouldCancelDamage(Player attacker, Player victim) {
        return combat.shouldCancelDamage(attacker, victim);
    }

    public boolean shouldKeepInventory(Player player) {
        return combat.shouldKeepInventory(player);
    }

    public void handleKill(Player killer, Player victim) {
        combat.handleKill(killer, victim);
    }

    /** 死亡重生点：积分模式回队出生点；淘汰模式去观众点。 */
    public Location resolveRespawn(Player player) {
        return combat.resolveRespawn(player);
    }

    public void afterRespawn(Player player) {
        combat.afterRespawn(player);
    }

    public void handleQuit(Player player) {
        combat.handleQuit(player);
    }

    private void scheduleChallengeTimeout(WarMatch match) {
        scheduler.scheduleChallengeTimeout(match, settings.challengeTimeoutSeconds * 20L, () -> {
            broadcast.broadcastMatch(match, "war.broadcast.challenge-timeout", "&c挑战已超时");
            lifecycle.cleanupMatch(match, false);
        });
    }

    private void scheduleSignupTimeout(WarMatch match) {
        scheduler.scheduleSignupTimeout(match, settings.signupSeconds * 20L, () -> {
            if (!match.bothTeamsHavePlayers()) {
                broadcast.broadcastMatch(match, "war.broadcast.signup-timeout",
                        "&c报名超时：双方人数不足，对局取消");
                lifecycle.cleanupMatch(match, false);
            } else {
                lifecycle.beginPreparing(match, () -> startCountdown(match));
            }
        });
    }

    private void startCountdown(WarMatch match) {
        scheduler.startCountdown(match, settings.countdownSeconds,
                secondsLeft -> {
                    if (secondsLeft <= 5 || secondsLeft % 5 == 0) {
                        broadcast.broadcastMatch(match, "war.broadcast.countdown",
                                "&e开战倒计时: &c{seconds}",
                                "{seconds}", String.valueOf(secondsLeft));
                    }
                },
                () -> {
                    match.setPhase(WarPhase.ACTIVE);
                    match.setStartedAt(System.currentTimeMillis());
                    broadcast.broadcastMatch(match, "war.broadcast.fight", "&c&l开战！");
                    Bukkit.getPluginManager().callEvent(new WarMatchStartEvent(match));
                    scheduleMatchDuration(match);
                });
    }

    private void scheduleMatchDuration(WarMatch match) {
        scheduler.scheduleMatchDuration(match, match.durationSeconds() * 20L, () -> {
            if (match.mode() == VictoryMode.TIMED_SCORE) {
                resolveTimedScore(match);
            } else {
                resolveSurviveTimeout(match);
            }
        });
    }

    private void resolveTimedScore(WarMatch match) {
        if (match.scoreA() > match.scoreB()) {
            lifecycle.endMatch(match, match.guildAId(), "war.reason.timed-win");
        } else if (match.scoreB() > match.scoreA()) {
            lifecycle.endMatch(match, match.guildBId(), "war.reason.timed-win");
        } else {
            lifecycle.endMatch(match, null, "war.reason.timed-draw");
        }
    }

    private void resolveSurviveTimeout(WarMatch match) {
        int a = match.aliveCount(WarTeamSide.A);
        int b = match.aliveCount(WarTeamSide.B);
        if (a > b) {
            lifecycle.endMatch(match, match.guildAId(), "war.reason.survive-alive");
        } else if (b > a) {
            lifecycle.endMatch(match, match.guildBId(), "war.reason.survive-alive");
        } else if (match.scoreA() > match.scoreB()) {
            lifecycle.endMatch(match, match.guildAId(), "war.reason.survive-kills");
        } else if (match.scoreB() > match.scoreA()) {
            lifecycle.endMatch(match, match.guildBId(), "war.reason.survive-kills");
        } else {
            lifecycle.endMatch(match, null, "war.reason.survive-draw");
        }
    }

    public void broadcastMatch(WarMatch match, String key, String def, String... ph) {
        broadcast.broadcastMatch(match, key, def, ph);
    }

    public void shutdown() {
        for (WarMatch match : new ArrayList<>(registry.allMatches())) {
            try {
                if (match.phase() != WarPhase.ENDED) {
                    broadcast.broadcastMatch(match, "war.broadcast.plugin-shutdown", "&c插件关闭，对局中止");
                    lifecycle.endMatch(match, null, "war.reason.plugin-shutdown");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[GuildWar] shutdown match " + match.id(), e);
            }
        }
        scheduler.cancelAll();
    }
}
