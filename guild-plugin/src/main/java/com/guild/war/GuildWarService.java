package com.guild.war;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ScheduledTaskHandle;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import com.guild.world.GuildWorldService;
import com.guildplugin.util.FoliaTeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 工会战核心服务：发起 → 接受 → 报名 → 进场倒计时 → 激战 → 结算回收。
 */
public final class GuildWarService {

    public static final String PREFIX = "&c[工会战] &r";

    private final GuildPlugin plugin;
    private final GuildService guildService;
    private final GuildWorldService worldService;
    private WarSettings settings;

    private final Map<Integer, WarMatch> matches = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> guildToMatch = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerToMatch = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledTaskHandle> timers = new ConcurrentHashMap<>();

    public GuildWarService(GuildPlugin plugin, GuildService guildService, GuildWorldService worldService) {
        this.plugin = plugin;
        this.guildService = guildService;
        this.worldService = worldService;
        reloadSettings();
    }

    public void reloadSettings() {
        this.settings = new WarSettings(plugin.getConfig());
    }

    public WarSettings settings() {
        return settings;
    }

    public boolean isEnabled() {
        return settings.enabled && worldService != null && worldService.isEnabled();
    }

    public String unavailableReason() {
        if (!settings.enabled) {
            return "工会战已在配置中关闭";
        }
        if (worldService == null || !worldService.isEnabled()) {
            return worldService == null ? "世界管理未初始化" : worldService.unsupportedMessage();
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
            future.completeExceptionally(new IllegalStateException(unavailableReason()));
            return future;
        }
        if (countNonEnded() >= settings.maxConcurrent) {
            future.completeExceptionally(new IllegalStateException("同时进行的工会战已达上限"));
            return future;
        }

        guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(own -> {
            if (own == null) {
                return failed("你不在任何工会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("只有会长或官员可以发起工会战");
                }
                if (getMatchByGuild(own.getId()) != null) {
                    return failed("本工会已有进行中的工会战");
                }
                return resolveGuild(targetGuildQuery).thenCompose(target -> {
                    if (target == null) {
                        return failed("找不到目标工会: " + targetGuildQuery);
                    }
                    if (target.getId() == own.getId()) {
                        return failed("不能向自己的工会宣战");
                    }
                    if (getMatchByGuild(target.getId()) != null) {
                        return failed("对方工会已有进行中的工会战");
                    }
                    String preset = (presetOverride != null && !presetOverride.isBlank())
                            ? presetOverride.trim() : settings.defaultPreset;
                    if (preset == null || preset.isBlank()) {
                        return failed("未指定预设，请使用 --preset 或在 config 设置 guild-war.default-preset");
                    }
                    if (worldService.getPresets().get(preset) == null
                            || !worldService.getPresets().hasSchematicFile(preset)) {
                        return failed("预设不存在或缺少 schematic: " + preset);
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
                    broadcastGuild(own.getId(), "&e" + player.getName() + " &7向 &f" + target.getName()
                            + " &7发起工会战（模式: &a" + mode.displayName() + "&7，预设: &a" + preset
                            + "&7，每队上限: &a" + max + "&7）");
                    broadcastGuild(target.getId(), "&e" + own.getName() + " &7向你们发起工会战！官员请执行 &a/guildwar accept &7或 &c/guildwar deny");
                    scheduleChallengeTimeout(match);
                    return CompletableFuture.completedFuture(match);
                });
            });
        }).whenComplete((m, err) -> {
            if (err != null) {
                future.completeExceptionally(unwrap(err));
            } else {
                future.complete(m);
            }
        });
        return future;
    }

    public CompletableFuture<WarMatch> accept(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("你不在任何工会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("只有会长或官员可以接受工会战");
                }
                WarMatch match = getMatchByGuild(guild.getId());
                if (match == null || match.phase() != WarPhase.PENDING) {
                    return failed("没有待接受的工会战邀请");
                }
                if (match.guildBId() != guild.getId()) {
                    return failed("只有被挑战方可以接受");
                }
                cancelTimer(match.id());
                match.setPhase(WarPhase.SIGNUP);
                broadcastMatch(match, "&a挑战已接受！请双方成员 &e/guildwar join &a报名（"
                        + settings.signupSeconds + " 秒，或双方官员 /guildwar ready）");
                scheduleSignupTimeout(match);
                return CompletableFuture.completedFuture(match);
            });
        });
    }

    public CompletableFuture<Void> deny(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("你不在任何工会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("只有会长或官员可以拒绝");
                }
                WarMatch match = getMatchByGuild(guild.getId());
                if (match == null || match.phase() != WarPhase.PENDING) {
                    return failed("没有待处理的挑战");
                }
                if (match.guildBId() != guild.getId()) {
                    return failed("只有被挑战方可以拒绝");
                }
                broadcastMatch(match, "&c" + guild.getName() + " 拒绝了工会战挑战");
                cleanupMatch(match, false);
                return CompletableFuture.completedFuture(null);
            });
        });
    }

    public CompletableFuture<Void> cancel(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("你不在任何工会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("只有会长或官员可以取消");
                }
                WarMatch match = getMatchByGuild(guild.getId());
                if (match == null || match.phase() == WarPhase.ENDED) {
                    return failed("没有可取消的工会战");
                }
                if (match.phase() == WarPhase.ACTIVE || match.phase() == WarPhase.COUNTDOWN
                        || match.phase() == WarPhase.PREPARING) {
                    return failed("战斗已开始，无法取消（可用管理员强制结束）");
                }
                broadcastMatch(match, "&e工会战已被取消");
                cleanupMatch(match, false);
                return CompletableFuture.completedFuture(null);
            });
        });
    }

    /* ── Join / Leave / Ready ────────────────────────────── */

    public CompletableFuture<Void> join(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("你不在任何工会中");
            }
            WarMatch match = getMatchByGuild(guild.getId());
            if (match == null || (match.phase() != WarPhase.SIGNUP && match.phase() != WarPhase.PENDING)) {
                return failed("当前没有可报名的工会战");
            }
            if (match.phase() == WarPhase.PENDING && match.guildAId() != guild.getId()) {
                return failed("请等待官员接受挑战后再报名");
            }
            WarTeamSide side = match.sideOfGuild(guild.getId());
            if (side == null) {
                return failed("你的工会不在本场对局中");
            }
            if (playerToMatch.containsKey(player.getUniqueId())) {
                return failed("你已在一场工会战中");
            }
            if (match.countSide(side) >= match.maxPerTeam()) {
                return failed("本队报名已满（" + match.maxPerTeam() + "）");
            }
            WarParticipant p = new WarParticipant(player.getUniqueId(), player.getName(), side);
            match.participants().put(player.getUniqueId(), p);
            playerToMatch.put(player.getUniqueId(), match.id());
            broadcastMatch(match, "&a" + player.getName() + " &7加入了 &f"
                    + match.guildNameOf(side) + " &7（"
                    + match.countSide(WarTeamSide.A) + " vs " + match.countSide(WarTeamSide.B) + "）");
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> leave(Player player) {
        WarMatch match = getMatchByPlayer(player.getUniqueId());
        if (match == null) {
            return failed("你不在工会战中");
        }
        if (match.phase() != WarPhase.SIGNUP && match.phase() != WarPhase.PENDING) {
            return failed("战斗阶段无法退出报名，请等待结束");
        }
        match.participants().remove(player.getUniqueId());
        playerToMatch.remove(player.getUniqueId());
        broadcastMatch(match, "&e" + player.getName() + " &7退出了报名");
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> ready(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("你不在任何工会中");
            }
            return guildService.getGuildMemberAsync(player.getUniqueId()).thenCompose(member -> {
                if (member == null || !isOfficerOrLeader(member)) {
                    return failed("只有会长或官员可以标记准备就绪");
                }
                WarMatch match = getMatchByGuild(guild.getId());
                if (match == null || match.phase() != WarPhase.SIGNUP) {
                    return failed("当前不在报名阶段");
                }
                WarTeamSide side = match.sideOfGuild(guild.getId());
                if (side == WarTeamSide.A) {
                    match.setTeamAReady(true);
                } else {
                    match.setTeamBReady(true);
                }
                broadcastMatch(match, "&a" + guild.getName() + " &7已准备就绪");
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
            return failed("对局不存在");
        }
        endMatch(match, null, reason != null ? reason : "管理员强制结束");
        return CompletableFuture.completedFuture(null);
    }

    /* ── Combat hooks ───────────────────────────────────── */

    public boolean isInWarWorld(Player player) {
        WarMatch m = getMatchByPlayer(player.getUniqueId());
        return m != null && m.worldName() != null && player.getWorld() != null
                && player.getWorld().getName().equals(m.worldName());
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
                broadcastMatch(match, "&e" + killer.getName() + " &7击杀了 &c" + victim.getName()
                        + " &7| &a" + match.guildAName() + " " + match.scoreA()
                        + " &7: &c" + match.scoreB() + " " + match.guildBName());
            }
        }

        if (match.mode() == VictoryMode.LAST_STANDING) {
            eliminate(match, victimP, victim);
            checkSurviveWin(match);
            return;
        }

        if (match.mode() == VictoryMode.FIRST_TO_SCORE) {
            if (match.scoreA() >= match.scoreToWin()) {
                endMatch(match, match.guildAId(), "先达到 " + match.scoreToWin() + " 分");
            } else if (match.scoreB() >= match.scoreToWin()) {
                endMatch(match, match.guildBId(), "先达到 " + match.scoreToWin() + " 分");
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
            broadcastMatch(match, "&e" + player.getName() + " &7离线，已移出报名");
            return;
        }
        if (match.phase() == WarPhase.ACTIVE || match.phase() == WarPhase.COUNTDOWN
                || match.phase() == WarPhase.PREPARING) {
            if (match.mode() == VictoryMode.LAST_STANDING && p.isFighting()) {
                p.setEliminated(true);
                p.setAlive(false);
                broadcastMatch(match, "&e" + player.getName() + " &7退出，视为淘汰");
                checkSurviveWin(match);
            }
        }
    }

    /* ── Internals ───────────────────────────────────────── */

    private void scheduleChallengeTimeout(WarMatch match) {
        cancelTimer(match.id());
        ScheduledTaskHandle handle = CompatibleScheduler.runTaskLater(plugin, () -> {
            if (match.phase() == WarPhase.PENDING) {
                broadcastMatch(match, "&c挑战已超时");
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
                    broadcastMatch(match, "&c报名超时：双方人数不足，对局取消");
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
        broadcastMatch(match, "&a报名结束，正在创建战场…");

        String worldKey = "war" + match.id() + "_" + System.currentTimeMillis() % 100000;
        worldService.createArenaFromPreset(worldKey, match.presetName())
                .whenComplete((result, err) -> CompatibleScheduler.runTask(plugin, () -> {
                    if (err != null) {
                        plugin.getLogger().log(Level.SEVERE, "[GuildWar] Failed to create arena", err);
                        broadcastMatch(match, "&c创建战场失败: " + err.getMessage());
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
                broadcastMatch(match, "&c&l开战！");
                scheduleMatchDuration(match);
                return;
            }
            if (left[0] <= 5 || left[0] % 5 == 0) {
                broadcastMatch(match, "&e开战倒计时: &c" + left[0]);
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
            endMatch(match, match.guildAId(), "限时结束，积分更高");
        } else if (match.scoreB() > match.scoreA()) {
            endMatch(match, match.guildBId(), "限时结束，积分更高");
        } else {
            endMatch(match, null, "限时结束，平局");
        }
    }

    private void resolveSurviveTimeout(WarMatch match) {
        int a = match.aliveCount(WarTeamSide.A);
        int b = match.aliveCount(WarTeamSide.B);
        if (a > b) {
            endMatch(match, match.guildAId(), "时间到，存活人数更多");
        } else if (b > a) {
            endMatch(match, match.guildBId(), "时间到，存活人数更多");
        } else if (match.scoreA() > match.scoreB()) {
            endMatch(match, match.guildAId(), "时间到，击杀更多");
        } else if (match.scoreB() > match.scoreA()) {
            endMatch(match, match.guildBId(), "时间到，击杀更多");
        } else {
            endMatch(match, null, "时间到，平局");
        }
    }

    private void checkSurviveWin(WarMatch match) {
        if (match.phase() != WarPhase.ACTIVE || match.mode() != VictoryMode.LAST_STANDING) {
            return;
        }
        int a = match.aliveCount(WarTeamSide.A);
        int b = match.aliveCount(WarTeamSide.B);
        if (a == 0 && b == 0) {
            endMatch(match, null, "双方全灭");
        } else if (a == 0) {
            endMatch(match, match.guildBId(), "歼灭对方");
        } else if (b == 0) {
            endMatch(match, match.guildAId(), "歼灭对方");
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
                    msg(player, "&7你已被淘汰，已送回安全点");
                }
            }, 1L);
        } else {
            msg(player, "&7你已被淘汰，重生后进入旁观");
        }
    }

    private synchronized void endMatch(WarMatch match, Integer winnerGuildId, String reason) {
        if (match.phase() == WarPhase.ENDED) {
            return;
        }
        cancelTimer(match.id());
        match.setPhase(WarPhase.ENDED);
        match.setWinnerGuildId(winnerGuildId);
        match.setEndReason(reason);

        String winnerName = winnerGuildId == null ? "平局"
                : (winnerGuildId == match.guildAId() ? match.guildAName() : match.guildBName());
        broadcastMatch(match, "&6对局结束！&e" + winnerName + " &7（" + reason + "） | 比分 &a"
                + match.scoreA() + " &7: &c" + match.scoreB());

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

    private static <T> CompletableFuture<T> failed(String msg) {
        return CompletableFuture.failedFuture(new IllegalStateException(msg));
    }

    private static Throwable unwrap(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            if (c instanceof java.util.concurrent.CompletionException
                    || c instanceof java.util.concurrent.ExecutionException) {
                c = c.getCause();
            } else {
                break;
            }
        }
        return c;
    }

    public void broadcastMatch(WarMatch match, String message) {
        broadcastGuild(match.guildAId(), message);
        broadcastGuild(match.guildBId(), message);
    }

    private void broadcastGuild(int guildId, String message) {
        String colored = ColorUtils.colorize(PREFIX + message);
        guildService.getGuildMembersAsync(guildId).thenAccept(members -> {
            if (members == null) {
                return;
            }
            CompatibleScheduler.runTask(plugin, () -> {
                for (GuildMember m : members) {
                    Player p = Bukkit.getPlayer(m.getPlayerUuid());
                    if (p != null && p.isOnline()) {
                        p.sendMessage(colored);
                    }
                }
            });
        });
    }

    public static void msg(Player player, String message) {
        player.sendMessage(ColorUtils.colorize(PREFIX + message));
    }

    public void shutdown() {
        for (WarMatch match : new ArrayList<>(matches.values())) {
            try {
                if (match.phase() != WarPhase.ENDED) {
                    broadcastMatch(match, "&c插件关闭，对局中止");
                    endMatch(match, null, "插件关闭");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[GuildWar] shutdown match " + match.id(), e);
            }
        }
        timers.values().forEach(ScheduledTaskHandle::cancel);
        timers.clear();
    }
}
