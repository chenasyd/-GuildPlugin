package com.guild.war;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.language.LocalizedException;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import com.guild.war.event.WarMatchStartEvent;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
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
 * 公会战核心服务：发起 → 接受 → 报名 → 进场倒计时 → 激战 → 结算回收。
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

    /* ── Challenge / Accept / Deny ───────────────────────── */

    public CompletableFuture<WarMatch> challenge(Player player, String targetGuildQuery,
                                                 String presetOverride, VictoryMode modeOverride,
                                                 Integer maxOverride, Integer scoreOverride, Integer durationOverride) {
        CompletableFuture<WarMatch> future = new CompletableFuture<>();
        if (!isEnabled()) {
            future.completeExceptionally(new LocalizedException(
                    "war.unavailable", "&c公会战不可用: {reason}",
                    "{reason}", unavailableReason()));
            return future;
        }
        if (registry.countNonEnded() >= settings.maxConcurrent) {
            future.completeExceptionally(new LocalizedException(
                    "war.error.max-concurrent", "&c同时进行的公会战已达上限"));
            return future;
        }

        guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(own -> {
            if (own == null) {
                return failed("war.no-guild", "&c你不在任何公会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("war.officer-only.challenge", "&c只有会长或官员可以发起公会战");
                }
                if (getMatchByGuild(own.getId()) != null) {
                    return failed("war.error.guild-busy", "&c本公会已有进行中的公会战");
                }
                return resolveGuild(targetGuildQuery).thenCompose(target -> {
                    if (target == null) {
                        return failed("war.error.target-not-found", "&c找不到目标公会: {query}",
                                "{query}", targetGuildQuery);
                    }
                    if (target.getId() == own.getId()) {
                        return failed("war.error.self", "&c不能向自己的公会宣战");
                    }
                    if (getMatchByGuild(target.getId()) != null) {
                        return failed("war.error.target-busy", "&c对方公会已有进行中的公会战");
                    }
                    String preset = (presetOverride != null && !presetOverride.isBlank())
                            ? presetOverride.trim() : settings.defaultPreset;
                    if (preset == null || preset.isBlank()) {
                        return failed("war.error.no-preset",
                                "&c未指定预设，请使用 --preset 或在 config 设置 guild-war.default-preset");
                    }
                    if (worldService.getPresets().get(preset) == null
                            || !worldService.getPresets().hasSchematicFile(preset)) {
                        return failed("war.error.preset-missing", "&c预设不存在或缺少 schematic: {preset}",
                                "{preset}", preset);
                    }
                    VictoryMode mode = modeOverride != null ? modeOverride : settings.defaultMode;
                    int max = maxOverride != null ? Math.max(1, maxOverride) : settings.maxPerTeam;
                    int score = scoreOverride != null ? Math.max(1, scoreOverride) : settings.scoreToWin;
                    int duration = resolveDuration(mode, durationOverride);

                    WarMatch match = new WarMatch(
                            own.getId(), own.getName(),
                            target.getId(), target.getName(),
                            player.getUniqueId(),
                            preset, mode, max, score, duration
                    );
                    registry.register(match);
                    broadcast.broadcastGuild(own.getId(), "war.broadcast.challenged-own",
                            "&e{player} &7向 &f{guild} &7发起公会战（模式: &a{mode}&7，预设: &a{preset}&7，每队上限: &a{max}&7）",
                            "{player}", player.getName(),
                            "{guild}", target.getName(),
                            "{mode}", mode.langKey(),
                            "{preset}", preset,
                            "{max}", String.valueOf(max));
                    broadcast.broadcastGuild(target.getId(), "war.broadcast.challenged-target",
                            "&e{guild} &7向你们发起公会战！官员请执行 &a/guildwar accept &7或 &c/guildwar deny",
                            "{guild}", own.getName());
                    scheduleChallengeTimeout(match);
                    return CompletableFuture.completedFuture(match);
                });
            });
        }).whenComplete((m, err) -> {
            if (err != null) {
                future.completeExceptionally(LocalizedException.unwrap(err));
            } else {
                future.complete(m);
            }
        });
        return future;
    }

    public CompletableFuture<WarMatch> accept(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("war.no-guild", "&c你不在任何公会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("war.officer-only.accept", "&c只有会长或官员可以接受公会战");
                }
                WarMatch match = getMatchByGuild(guild.getId());
                if (match == null || match.phase() != WarPhase.PENDING) {
                    return failed("war.error.no-pending", "&c没有待接受的公会战邀请");
                }
                if (match.guildBId() != guild.getId()) {
                    return failed("war.error.accept-not-defender", "&c只有被挑战方可以接受");
                }
                scheduler.cancel(match.id());
                match.setPhase(WarPhase.SIGNUP);
                broadcast.broadcastMatch(match, "war.broadcast.accepted",
                        "&a挑战已接受！请双方成员 &e/guildwar join &a报名（{seconds} 秒，或双方官员 /guildwar ready）",
                        "{seconds}", String.valueOf(settings.signupSeconds));
                scheduleSignupTimeout(match);
                return CompletableFuture.completedFuture(match);
            });
        });
    }

    public CompletableFuture<Void> deny(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("war.no-guild", "&c你不在任何公会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("war.officer-only.deny", "&c只有会长或官员可以拒绝");
                }
                WarMatch match = getMatchByGuild(guild.getId());
                if (match == null || match.phase() != WarPhase.PENDING) {
                    return failed("war.error.no-pending-challenge", "&c没有待处理的挑战");
                }
                if (match.guildBId() != guild.getId()) {
                    return failed("war.error.deny-not-defender", "&c只有被挑战方可以拒绝");
                }
                broadcast.broadcastMatch(match, "war.broadcast.denied",
                        "&c{guild} 拒绝了公会战挑战",
                        "{guild}", guild.getName());
                lifecycle.cleanupMatch(match, false);
                return CompletableFuture.completedFuture(null);
            });
        });
    }

    public CompletableFuture<Void> cancel(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("war.no-guild", "&c你不在任何公会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("war.officer-only.cancel", "&c只有会长或官员可以取消");
                }
                WarMatch match = getMatchByGuild(guild.getId());
                if (match == null || match.phase() == WarPhase.ENDED) {
                    return failed("war.error.nothing-to-cancel", "&c没有可取消的公会战");
                }
                if (match.phase() == WarPhase.ACTIVE || match.phase() == WarPhase.COUNTDOWN
                        || match.phase() == WarPhase.PREPARING) {
                    return failed("war.error.cancel-too-late",
                            "&c战斗已开始，无法取消（可用管理员强制结束）");
                }
                broadcast.broadcastMatch(match, "war.broadcast.cancelled", "&e公会战已被取消");
                lifecycle.cleanupMatch(match, false);
                return CompletableFuture.completedFuture(null);
            });
        });
    }

    /* ── Join / Leave / Ready ────────────────────────────── */

    public CompletableFuture<Void> join(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("war.no-guild", "&c你不在任何公会中");
            }
            WarMatch match = getMatchByGuild(guild.getId());
            if (match == null || (match.phase() != WarPhase.SIGNUP && match.phase() != WarPhase.PENDING)) {
                return failed("war.error.no-signup", "&c当前没有可报名的公会战");
            }
            if (match.phase() == WarPhase.PENDING && match.guildAId() != guild.getId()) {
                return failed("war.error.wait-accept", "&c请等待官员接受挑战后再报名");
            }
            WarTeamSide side = match.sideOfGuild(guild.getId());
            if (side == null) {
                return failed("war.error.guild-not-in-match", "&c你的公会不在本场对局中");
            }
            if (registry.isPlayerLinked(player.getUniqueId())) {
                return failed("war.error.already-in-match", "&c你已在一场公会战中");
            }
            if (match.countSide(side) >= match.maxPerTeam()) {
                return failed("war.error.team-full", "&c本队报名已满（{max}）",
                        "{max}", String.valueOf(match.maxPerTeam()));
            }
            WarParticipant p = new WarParticipant(player.getUniqueId(), player.getName(), side);
            match.participants().put(player.getUniqueId(), p);
            registry.linkPlayer(player.getUniqueId(), match.id());
            broadcast.broadcastMatch(match, "war.broadcast.joined",
                    "&a{player} &7加入了 &f{guild} &7（{a} vs {b}）",
                    "{player}", player.getName(),
                    "{guild}", match.guildNameOf(side),
                    "{a}", String.valueOf(match.countSide(WarTeamSide.A)),
                    "{b}", String.valueOf(match.countSide(WarTeamSide.B)));
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> leave(Player player) {
        WarMatch match = getMatchByPlayer(player.getUniqueId());
        if (match == null) {
            return failed("war.error.not-in-war", "&c你不在公会战中");
        }
        if (match.phase() != WarPhase.SIGNUP && match.phase() != WarPhase.PENDING) {
            return failed("war.error.leave-locked", "&c战斗阶段无法退出报名，请等待结束");
        }
        match.participants().remove(player.getUniqueId());
        registry.unlinkPlayer(player.getUniqueId());
        broadcast.broadcastMatch(match, "war.broadcast.left-signup",
                "&e{player} &7退出了报名",
                "{player}", player.getName());
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> ready(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("war.no-guild", "&c你不在任何公会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("war.officer-only.ready", "&c只有会长或官员可以标记准备就绪");
                }
                WarMatch match = getMatchByGuild(guild.getId());
                if (match == null || match.phase() != WarPhase.SIGNUP) {
                    return failed("war.error.not-signup-phase", "&c当前不在报名阶段");
                }
                WarTeamSide side = match.sideOfGuild(guild.getId());
                if (side == WarTeamSide.A) {
                    match.setTeamAReady(true);
                } else {
                    match.setTeamBReady(true);
                }
                broadcast.broadcastMatch(match, "war.broadcast.ready",
                        "&a{guild} &7已准备就绪",
                        "{guild}", guild.getName());
                if (match.isTeamAReady() && match.isTeamBReady() && match.bothTeamsHavePlayers()) {
                    lifecycle.beginPreparing(match, () -> startCountdown(match));
                }
                return CompletableFuture.completedFuture(null);
            });
        });
    }

    public CompletableFuture<Void> forceEnd(int matchId, String reason) {
        WarMatch match = registry.getMatch(matchId);
        if (match == null) {
            return failed("war.error.match-missing", "&c对局不存在");
        }
        String reasonKey = (reason != null && !reason.isBlank()) ? reason : "war.reason.admin-end";
        lifecycle.endMatch(match, null, reasonKey);
        return CompletableFuture.completedFuture(null);
    }

    /* ── Combat hooks ───────────────────────────────────── */

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

    /* ── Internals ───────────────────────────────────────── */

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

    private int resolveDuration(VictoryMode mode, Integer override) {
        if (override != null && override > 0) {
            return override;
        }
        return mode == VictoryMode.LAST_STANDING
                ? settings.surviveDurationSeconds
                : settings.timedDurationSeconds;
    }

    private CompletableFuture<Guild> resolveGuild(String query) {
        return guildService.getGuildByNameAsync(query).thenCompose(byName -> {
            if (byName != null) {
                return CompletableFuture.completedFuture(byName);
            }
            return guildService.getGuildByTagAsync(query);
        });
    }

    private static boolean isOfficerOrLeader(GuildMember member) {
        return member.getRole() == GuildMember.Role.LEADER
                || member.getRole() == GuildMember.Role.OFFICER;
    }

    private static <T> CompletableFuture<T> failed(String key, String def, String... ph) {
        return CompletableFuture.failedFuture(new LocalizedException(key, def, ph));
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
