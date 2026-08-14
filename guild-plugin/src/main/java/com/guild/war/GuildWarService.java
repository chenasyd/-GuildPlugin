package com.guild.war;

import com.guild.GuildPlugin;
import com.guild.core.language.CoreMsg;
import com.guild.core.language.LocalizedException;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ScheduledTaskHandle;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import com.guild.war.event.WarMatchEndEvent;
import com.guild.war.event.WarMatchStartEvent;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarParticipantSnapshot;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 公会战核心服务：发起 → 接受 → 报名 → 进场倒计时 → 激战 → 结算回收。
 */
public final class GuildWarService {

    private final GuildPlugin plugin;
    private final GuildService guildService;
    private final GuildWorldService worldService;
    private final WarReportRepository reportRepository;
    private WarSettings settings;

    private final Map<Integer, WarMatch> matches = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> guildToMatch = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerToMatch = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledTaskHandle> timers = new ConcurrentHashMap<>();

    public GuildWarService(GuildPlugin plugin, GuildService guildService, GuildWorldService worldService) {
        this.plugin = plugin;
        this.guildService = guildService;
        this.worldService = worldService;
        this.reportRepository = new WarReportRepository(plugin);
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
        return matches.values();
    }

    public WarMatch getMatch(int id) {
        return matches.get(id);
    }

    public WarMatch getMatchByPlayer(UUID uuid) {
        Integer id = playerToMatch.get(uuid);
        return id == null ? null : matches.get(id);
    }

    public WarMatch getMatchByGuild(int guildId) {
        Integer id = guildToMatch.get(guildId);
        return id == null ? null : matches.get(id);
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
        if (countNonEnded() >= settings.maxConcurrent) {
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
                    registerMatch(match);
                    broadcastGuild(own.getId(), "war.broadcast.challenged-own",
                            "&e{player} &7向 &f{guild} &7发起公会战（模式: &a{mode}&7，预设: &a{preset}&7，每队上限: &a{max}&7）",
                            "{player}", player.getName(),
                            "{guild}", target.getName(),
                            "{mode}", mode.langKey(),
                            "{preset}", preset,
                            "{max}", String.valueOf(max));
                    broadcastGuild(target.getId(), "war.broadcast.challenged-target",
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
                cancelTimer(match.id());
                match.setPhase(WarPhase.SIGNUP);
                broadcastMatch(match, "war.broadcast.accepted",
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
                broadcastMatch(match, "war.broadcast.denied",
                        "&c{guild} 拒绝了公会战挑战",
                        "{guild}", guild.getName());
                cleanupMatch(match, false);
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
                broadcastMatch(match, "war.broadcast.cancelled", "&e公会战已被取消");
                cleanupMatch(match, false);
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
            if (playerToMatch.containsKey(player.getUniqueId())) {
                return failed("war.error.already-in-match", "&c你已在一场公会战中");
            }
            if (match.countSide(side) >= match.maxPerTeam()) {
                return failed("war.error.team-full", "&c本队报名已满（{max}）",
                        "{max}", String.valueOf(match.maxPerTeam()));
            }
            WarParticipant p = new WarParticipant(player.getUniqueId(), player.getName(), side);
            match.participants().put(player.getUniqueId(), p);
            playerToMatch.put(player.getUniqueId(), match.id());
            broadcastMatch(match, "war.broadcast.joined",
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
        playerToMatch.remove(player.getUniqueId());
        broadcastMatch(match, "war.broadcast.left-signup",
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
                broadcastMatch(match, "war.broadcast.ready",
                        "&a{guild} &7已准备就绪",
                        "{guild}", guild.getName());
                if (match.isTeamAReady() && match.isTeamBReady() && match.bothTeamsHavePlayers()) {
                    beginPreparing(match);
                }
                return CompletableFuture.completedFuture(null);
            });
        });
    }

    public CompletableFuture<Void> forceEnd(int matchId, String reason) {
        WarMatch match = matches.get(matchId);
        if (match == null) {
            return failed("war.error.match-missing", "&c对局不存在");
        }
        String reasonKey = (reason != null && !reason.isBlank()) ? reason : "war.reason.admin-end";
        endMatch(match, null, reasonKey);
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
        for (WarMatch m : matches.values()) {
            if (m.worldName() != null && m.worldName().equals(worldName)
                    && m.phase() != WarPhase.ENDED
                    && m.phase() != WarPhase.PENDING) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldCancelDamage(Player attacker, Player victim) {
        WarMatch match = getMatchByPlayer(victim.getUniqueId());
        if (match == null) {
            return false;
        }
        WarParticipant va = match.get(victim.getUniqueId());
        if (va == null || !va.isFighting()) {
            return true;
        }
        if (match.phase() != WarPhase.ACTIVE) {
            return true;
        }
        if (attacker == null) {
            return false;
        }
        WarParticipant aa = match.get(attacker.getUniqueId());
        if (aa == null) {
            return true;
        }
        if (!aa.isFighting()) {
            return true;
        }
        if (!settings.friendlyFire && aa.side() == va.side()) {
            return true;
        }
        return false;
    }

    public boolean shouldKeepInventory(Player player) {
        if (!settings.keepInventory) {
            return false;
        }
        WarMatch match = getMatchByPlayer(player.getUniqueId());
        return match != null && (match.phase() == WarPhase.ACTIVE
                || match.phase() == WarPhase.COUNTDOWN
                || match.phase() == WarPhase.PREPARING);
    }

    public void handleKill(Player killer, Player victim) {
        WarMatch match = getMatchByPlayer(victim.getUniqueId());
        if (match == null || match.phase() != WarPhase.ACTIVE) {
            return;
        }
        WarParticipant victimP = match.get(victim.getUniqueId());
        if (victimP == null || !victimP.isFighting()) {
            return;
        }

        if (killer != null) {
            WarParticipant killerP = match.get(killer.getUniqueId());
            if (killerP != null && killerP.side() != victimP.side()) {
                killerP.addKill();
                match.addScore(killerP.side(), 1);
                broadcastMatch(match, "war.broadcast.kill",
                        "&e{killer} &7击杀了 &c{victim} &7| &a{a} {sa} &7: &c{sb} {b}",
                        "{killer}", killer.getName(),
                        "{victim}", victim.getName(),
                        "{a}", match.guildAName(),
                        "{sa}", String.valueOf(match.scoreA()),
                        "{sb}", String.valueOf(match.scoreB()),
                        "{b}", match.guildBName());
            }
        }

        if (match.mode() == VictoryMode.LAST_STANDING) {
            eliminate(match, victimP, victim);
            checkSurviveWin(match);
            return;
        }

        if (match.mode() == VictoryMode.FIRST_TO_SCORE) {
            if (match.scoreA() >= match.scoreToWin()) {
                endMatch(match, match.guildAId(), "war.reason.first-score");
            } else if (match.scoreB() >= match.scoreToWin()) {
                endMatch(match, match.guildBId(), "war.reason.first-score");
            }
        }
    }

    /** 死亡重生点：积分模式回队出生点；淘汰模式去观众点。 */
    public Location resolveRespawn(Player player) {
        WarMatch match = getMatchByPlayer(player.getUniqueId());
        if (match == null || match.worldName() == null) {
            return null;
        }
        if (match.phase() != WarPhase.ACTIVE && match.phase() != WarPhase.COUNTDOWN
                && match.phase() != WarPhase.PREPARING) {
            return null;
        }
        WarParticipant p = match.get(player.getUniqueId());
        if (p == null) {
            return null;
        }
        if (p.isEliminated() || p.isSpectating()) {
            if (!settings.eliminateToSpectator) {
                Location fb = worldService.getFallbackLocation();
                return fb != null ? fb.clone() : null;
            }
            if (match.spectatorSpawn() != null) {
                return match.spectatorSpawn().clone();
            }
            return p.side() == WarTeamSide.A
                    ? (match.spawnA() != null ? match.spawnA().clone() : null)
                    : (match.spawnB() != null ? match.spawnB().clone() : null);
        }
        Location dest = p.side() == WarTeamSide.A ? match.spawnA() : match.spawnB();
        return dest != null ? dest.clone() : null;
    }

    public void afterRespawn(Player player) {
        WarMatch match = getMatchByPlayer(player.getUniqueId());
        if (match == null) {
            return;
        }
        WarParticipant p = match.get(player.getUniqueId());
        if (p == null) {
            return;
        }
        if (p.isEliminated() || p.isSpectating()) {
            CompatibleScheduler.runTask(plugin, player, () -> {
                player.setGameMode(GameMode.SPECTATOR);
            });
        }
    }

    public void handleQuit(Player player) {
        WarMatch match = getMatchByPlayer(player.getUniqueId());
        if (match == null) {
            return;
        }
        WarParticipant p = match.get(player.getUniqueId());
        if (p == null) {
            playerToMatch.remove(player.getUniqueId());
            return;
        }
        if (match.phase() == WarPhase.PENDING || match.phase() == WarPhase.SIGNUP) {
            match.participants().remove(player.getUniqueId());
            playerToMatch.remove(player.getUniqueId());
            broadcastMatch(match, "war.broadcast.offline-removed",
                    "&e{player} &7离线，已移出报名",
                    "{player}", player.getName());
            return;
        }
        if (match.phase() == WarPhase.ACTIVE || match.phase() == WarPhase.COUNTDOWN
                || match.phase() == WarPhase.PREPARING) {
            if (match.mode() == VictoryMode.LAST_STANDING && p.isFighting()) {
                p.setEliminated(true);
                p.setAlive(false);
                broadcastMatch(match, "war.broadcast.quit-eliminate",
                        "&e{player} &7退出，视为淘汰",
                        "{player}", player.getName());
                checkSurviveWin(match);
            }
        }
    }

    /* ── Internals ───────────────────────────────────────── */

    private void scheduleChallengeTimeout(WarMatch match) {
        cancelTimer(match.id());
        ScheduledTaskHandle handle = CompatibleScheduler.runTaskLater(plugin, () -> {
            if (match.phase() == WarPhase.PENDING) {
                broadcastMatch(match, "war.broadcast.challenge-timeout", "&c挑战已超时");
                cleanupMatch(match, false);
            }
        }, settings.challengeTimeoutSeconds * 20L);
        timers.put(match.id(), handle);
    }

    private void scheduleSignupTimeout(WarMatch match) {
        cancelTimer(match.id());
        ScheduledTaskHandle handle = CompatibleScheduler.runTaskLater(plugin, () -> {
            if (match.phase() == WarPhase.SIGNUP) {
                if (!match.bothTeamsHavePlayers()) {
                    broadcastMatch(match, "war.broadcast.signup-timeout",
                            "&c报名超时：双方人数不足，对局取消");
                    cleanupMatch(match, false);
                } else {
                    beginPreparing(match);
                }
            }
        }, settings.signupSeconds * 20L);
        timers.put(match.id(), handle);
    }

    private synchronized void beginPreparing(WarMatch match) {
        if (match.phase() != WarPhase.SIGNUP) {
            return;
        }
        cancelTimer(match.id());
        match.setPhase(WarPhase.PREPARING);
        broadcastMatch(match, "war.broadcast.preparing", "&a报名结束，正在创建战场…");

        String worldKey = "war" + match.id() + "_" + System.currentTimeMillis() % 100000;
        worldService.createArenaFromPreset(worldKey, match.presetName())
                .whenComplete((result, err) -> CompatibleScheduler.runTask(plugin, () -> {
                    if (err != null) {
                        plugin.getLogger().log(Level.SEVERE, "[GuildWar] Failed to create arena", err);
                        String errMsg = err.getMessage() != null ? err.getMessage() : err.toString();
                        broadcastMatch(match, "war.broadcast.arena-fail",
                                "&c创建战场失败: {error}",
                                "{error}", errMsg);
                        cleanupMatch(match, false);
                        return;
                    }
                    match.setWorldName(result.world().getWorldName());
                    match.setSpawnA(result.spawns().spawnA());
                    match.setSpawnB(result.spawns().spawnB());
                    match.setSpectatorSpawn(result.spawns().spectator());
                    teleportParticipants(match).thenRun(() -> startCountdown(match));
                }));
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

    private void startCountdown(WarMatch match) {
        match.setPhase(WarPhase.COUNTDOWN);
        final int[] left = {settings.countdownSeconds};
        cancelTimer(match.id());
        ScheduledTaskHandle handle = CompatibleScheduler.runTaskTimer(plugin, () -> {
            if (match.phase() != WarPhase.COUNTDOWN) {
                cancelTimer(match.id());
                return;
            }
            if (left[0] <= 0) {
                cancelTimer(match.id());
                match.setPhase(WarPhase.ACTIVE);
                match.setStartedAt(System.currentTimeMillis());
                broadcastMatch(match, "war.broadcast.fight", "&c&l开战！");
                Bukkit.getPluginManager().callEvent(new WarMatchStartEvent(match));
                scheduleMatchDuration(match);
                return;
            }
            if (left[0] <= 5 || left[0] % 5 == 0) {
                broadcastMatch(match, "war.broadcast.countdown",
                        "&e开战倒计时: &c{seconds}",
                        "{seconds}", String.valueOf(left[0]));
            }
            left[0]--;
        }, 0L, 20L);
        timers.put(match.id(), handle);
    }

    private void scheduleMatchDuration(WarMatch match) {
        if (match.mode() == VictoryMode.FIRST_TO_SCORE) {
            return; // 无时限
        }
        cancelTimer(match.id());
        ScheduledTaskHandle handle = CompatibleScheduler.runTaskLater(plugin, () -> {
            if (match.phase() != WarPhase.ACTIVE) {
                return;
            }
            if (match.mode() == VictoryMode.TIMED_SCORE) {
                resolveTimedScore(match);
            } else {
                resolveSurviveTimeout(match);
            }
        }, match.durationSeconds() * 20L);
        timers.put(match.id(), handle);
    }

    private void resolveTimedScore(WarMatch match) {
        if (match.scoreA() > match.scoreB()) {
            endMatch(match, match.guildAId(), "war.reason.timed-win");
        } else if (match.scoreB() > match.scoreA()) {
            endMatch(match, match.guildBId(), "war.reason.timed-win");
        } else {
            endMatch(match, null, "war.reason.timed-draw");
        }
    }

    private void resolveSurviveTimeout(WarMatch match) {
        int a = match.aliveCount(WarTeamSide.A);
        int b = match.aliveCount(WarTeamSide.B);
        if (a > b) {
            endMatch(match, match.guildAId(), "war.reason.survive-alive");
        } else if (b > a) {
            endMatch(match, match.guildBId(), "war.reason.survive-alive");
        } else if (match.scoreA() > match.scoreB()) {
            endMatch(match, match.guildAId(), "war.reason.survive-kills");
        } else if (match.scoreB() > match.scoreA()) {
            endMatch(match, match.guildBId(), "war.reason.survive-kills");
        } else {
            endMatch(match, null, "war.reason.survive-draw");
        }
    }

    private void checkSurviveWin(WarMatch match) {
        if (match.phase() != WarPhase.ACTIVE || match.mode() != VictoryMode.LAST_STANDING) {
            return;
        }
        int a = match.aliveCount(WarTeamSide.A);
        int b = match.aliveCount(WarTeamSide.B);
        if (a == 0 && b == 0) {
            endMatch(match, null, "war.reason.both-eliminated");
        } else if (a == 0) {
            endMatch(match, match.guildBId(), "war.reason.wipe");
        } else if (b == 0) {
            endMatch(match, match.guildAId(), "war.reason.wipe");
        }
    }

    private void eliminate(WarMatch match, WarParticipant p, Player player) {
        p.setEliminated(true);
        p.setAlive(false);
        p.setSpectating(true);
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!settings.eliminateToSpectator) {
            CompatibleScheduler.runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    worldService.teleportToFallbackWorld(player);
                    msg(player, "war.eliminate.fallback", "&7你已被淘汰，已送回安全点");
                }
            }, 1L);
        } else {
            msg(player, "war.eliminate.spectator", "&7你已被淘汰，重生后进入旁观");
        }
    }

    private synchronized void endMatch(WarMatch match, Integer winnerGuildId, String reasonKey) {
        if (match.phase() == WarPhase.ENDED) {
            return;
        }
        cancelTimer(match.id());
        match.setPhase(WarPhase.ENDED);
        match.setWinnerGuildId(winnerGuildId);
        match.setEndReason(reasonKey);

        WarReportSnapshot snapshot = WarReportSnapshot.fromMatch(match, settings.seasonId);
        try {
            Bukkit.getPluginManager().callEvent(new WarMatchEndEvent(snapshot));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GuildWar] WarMatchEndEvent listener error", e);
        }
        reportRepository.saveAsync(snapshot).whenComplete((saved, err) -> {
            if (err != null) {
                plugin.getLogger().log(Level.WARNING, "[GuildWar] Report save failed", err);
            }
            if (settings.broadcastReport) {
                CompatibleScheduler.runTask(plugin, () -> broadcastReportLines(saved != null ? saved : snapshot));
            }
        });

        String winnerPh = winnerGuildId == null
                ? "war.draw"
                : (winnerGuildId == match.guildAId() ? match.guildAName() : match.guildBName());
        broadcastMatch(match, "war.broadcast.ended",
                "&6对局结束！&e{winner} &7（{reason}） | 比分 &a{sa} &7: &c{sb}",
                "{winner}", winnerPh,
                "{reason}", reasonKey != null ? reasonKey : "",
                "{score}", String.valueOf(match.scoreToWin()),
                "{sa}", String.valueOf(match.scoreA()),
                "{sb}", String.valueOf(match.scoreB()));

        // 先送回，再销毁世界
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
            playerToMatch.remove(p.uuid());
        }
        CompletableFuture.allOf(tps.toArray(new CompletableFuture[0]))
                .whenComplete((v, e) -> CompatibleScheduler.runTaskLater(plugin, () -> {
                    destroyArena(match);
                    unregisterMatch(match);
                }, 40L));
    }

    private void broadcastReportLines(WarReportSnapshot snap) {
        String id = snap.reportId() != null ? String.valueOf(snap.reportId()) : String.valueOf(snap.runtimeMatchId());
        String header = CoreMsg.rawDefault(plugin, "war.report.broadcast",
                "&6[战报 #{id}] &f{a} &a{sa} &7: &c{sb} &f{b} &7→ &e{winner}",
                "{id}", id,
                "{a}", snap.guildAName(),
                "{b}", snap.guildBName(),
                "{sa}", String.valueOf(snap.scoreA()),
                "{sb}", String.valueOf(snap.scoreB()),
                "{winner}", snap.winnerName());
        Bukkit.broadcastMessage(ColorUtils.colorize(header));
        for (WarParticipantSnapshot p : snap.participants()) {
            if (p.kills() <= 0) {
                continue;
            }
            String line = CoreMsg.rawDefault(plugin, "war.report.kill-line",
                    "&7  · &f{player} &7kills: &e{kills}",
                    "{player}", p.name(),
                    "{kills}", String.valueOf(p.kills()));
            Bukkit.broadcastMessage(ColorUtils.colorize(line));
        }
    }

    private void destroyArena(WarMatch match) {
        String world = match.worldName();
        if (world == null) {
            return;
        }
        String policy = worldService.getPostMatchPolicy();
        // reset 本期等同 destroy（下次开战会重新 create+paste）
        worldService.deleteWorld(world, true).whenComplete((v, err) -> {
            if (err != null) {
                plugin.getLogger().log(Level.WARNING, "[GuildWar] Failed to delete arena " + world
                        + " (policy=" + policy + ")", err);
            }
        });
    }

    private void cleanupMatch(WarMatch match, boolean destroyWorld) {
        cancelTimer(match.id());
        match.setPhase(WarPhase.ENDED);
        for (UUID uuid : new ArrayList<>(match.participants().keySet())) {
            playerToMatch.remove(uuid);
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

    private void registerMatch(WarMatch match) {
        matches.put(match.id(), match);
        guildToMatch.put(match.guildAId(), match.id());
        guildToMatch.put(match.guildBId(), match.id());
    }

    private void unregisterMatch(WarMatch match) {
        matches.remove(match.id());
        guildToMatch.remove(match.guildAId(), match.id());
        guildToMatch.remove(match.guildBId(), match.id());
        cancelTimer(match.id());
    }

    private void cancelTimer(int matchId) {
        ScheduledTaskHandle h = timers.remove(matchId);
        if (h != null) {
            h.cancel();
        }
    }

    private int countNonEnded() {
        int n = 0;
        for (WarMatch m : matches.values()) {
            if (m.phase() != WarPhase.ENDED) {
                n++;
            }
        }
        return n;
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
        broadcastGuild(match.guildAId(), key, def, ph);
        broadcastGuild(match.guildBId(), key, def, ph);
    }

    private void broadcastGuild(int guildId, String key, String def, String... ph) {
        guildService.getGuildMembersAsync(guildId).thenAccept(members -> {
            if (members == null) {
                return;
            }
            CompatibleScheduler.runTask(plugin, () -> {
                for (GuildMember m : members) {
                    Player p = Bukkit.getPlayer(m.getPlayerUuid());
                    if (p != null && p.isOnline()) {
                        String[] localizedPh = localizePlaceholders(p, ph);
                        String prefix = CoreMsg.raw(plugin, p, "war.prefix", "&c[公会战] &r");
                        String body = CoreMsg.raw(plugin, p, key, def, localizedPh);
                        p.sendMessage(ColorUtils.colorize(prefix + body));
                    }
                }
            });
        });
    }

    /** Resolve placeholder values that are lang keys (war.* / world.*) per recipient. */
    private String[] localizePlaceholders(Player player, String... ph) {
        if (ph == null || ph.length == 0) {
            return ph != null ? ph : new String[0];
        }
        String[] out = Arrays.copyOf(ph, ph.length);
        for (int i = 0; i + 1 < out.length; i += 2) {
            String v = out[i + 1];
            if (v != null && (v.startsWith("war.") || v.startsWith("world."))) {
                // Pass full ph so nested keys like war.reason.first-score get {score}
                out[i + 1] = CoreMsg.raw(plugin, player, v, defaultForKey(v), ph);
            }
        }
        return out;
    }

    private static String defaultForKey(String key) {
        return switch (key) {
            case "war.mode.first" -> "积分先到";
            case "war.mode.timed" -> "限时积分";
            case "war.mode.survive" -> "最终存活";
            case "war.draw" -> "平局";
            case "war.reason.admin-end" -> "管理员强制结束";
            case "war.reason.first-score" -> "先达到 {score} 分";
            case "war.reason.timed-win" -> "限时结束，积分更高";
            case "war.reason.timed-draw" -> "限时结束，平局";
            case "war.reason.survive-alive" -> "时间到，存活人数更多";
            case "war.reason.survive-kills" -> "时间到，击杀更多";
            case "war.reason.survive-draw" -> "时间到，平局";
            case "war.reason.both-eliminated" -> "双方全灭";
            case "war.reason.wipe" -> "歼灭对方";
            case "war.reason.plugin-shutdown" -> "插件关闭";
            default -> key;
        };
    }

    private void msg(Player player, String key, String def, String... ph) {
        String prefix = CoreMsg.raw(plugin, player, "war.prefix", "&c[公会战] &r");
        String body = CoreMsg.raw(plugin, player, key, def, ph);
        player.sendMessage(ColorUtils.colorize(prefix + body));
    }

    public void shutdown() {
        for (WarMatch match : new ArrayList<>(matches.values())) {
            try {
                if (match.phase() != WarPhase.ENDED) {
                    broadcastMatch(match, "war.broadcast.plugin-shutdown", "&c插件关闭，对局中止");
                    endMatch(match, null, "war.reason.plugin-shutdown");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[GuildWar] shutdown match " + match.id(), e);
            }
        }
        timers.values().forEach(ScheduledTaskHandle::cancel);
        timers.clear();
    }
}
