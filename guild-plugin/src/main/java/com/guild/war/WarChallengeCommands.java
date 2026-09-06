package com.guild.war;

import com.guild.core.language.LocalizedException;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import com.guild.world.GuildWorldService;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 公会战玩家/管理员命令：挑战、接受、报名与取消。
 */
public final class WarChallengeCommands {

    private final GuildService guildService;
    private final GuildWorldService worldService;
    private final WarMatchRegistry registry;
    private final WarBroadcastHelper broadcast;
    private final WarMatchScheduler scheduler;
    private final WarMatchLifecycle lifecycle;
    private final Supplier<WarSettings> settings;
    private final BooleanSupplier enabled;
    private final Supplier<String> unavailableReason;
    private final WarMatchSignupFlow signupFlow;

    public WarChallengeCommands(GuildService guildService,
                                GuildWorldService worldService,
                                WarMatchRegistry registry,
                                WarBroadcastHelper broadcast,
                                WarMatchScheduler scheduler,
                                WarMatchLifecycle lifecycle,
                                Supplier<WarSettings> settings,
                                BooleanSupplier enabled,
                                Supplier<String> unavailableReason,
                                WarMatchSignupFlow signupFlow) {
        this.guildService = guildService;
        this.worldService = worldService;
        this.registry = registry;
        this.broadcast = broadcast;
        this.scheduler = scheduler;
        this.lifecycle = lifecycle;
        this.settings = settings;
        this.enabled = enabled;
        this.unavailableReason = unavailableReason;
        this.signupFlow = signupFlow;
    }

    public CompletableFuture<WarMatch> challenge(Player player, String targetGuildQuery,
                                                 String presetOverride, VictoryMode modeOverride,
                                                 Integer maxOverride, Integer scoreOverride,
                                                 Integer durationOverride) {
        CompletableFuture<WarMatch> future = new CompletableFuture<>();
        if (!enabled.getAsBoolean()) {
            future.completeExceptionally(new LocalizedException(
                    "war.unavailable", "&c公会战不可用: {reason}",
                    "{reason}", unavailableReason.get()));
            return future;
        }
        WarSettings current = settings.get();
        if (registry.countNonEnded() >= current.maxConcurrent) {
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
                if (registry.getByGuild(own.getId()) != null) {
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
                    if (registry.getByGuild(target.getId()) != null) {
                        return failed("war.error.target-busy", "&c对方公会已有进行中的公会战");
                    }
                    String preset = (presetOverride != null && !presetOverride.isBlank())
                            ? presetOverride.trim() : current.defaultPreset;
                    if (preset == null || preset.isBlank()) {
                        return failed("war.error.no-preset",
                                "&c未指定预设，请使用 --preset 或在 config 设置 guild-war.default-preset");
                    }
                    if (worldService.getPresets().get(preset) == null
                            || !worldService.getPresets().hasSchematicFile(preset)) {
                        return failed("war.error.preset-missing", "&c预设不存在或缺少 schematic: {preset}",
                                "{preset}", preset);
                    }
                    VictoryMode mode = modeOverride != null ? modeOverride : current.defaultMode;
                    int max = maxOverride != null ? Math.max(1, maxOverride) : current.maxPerTeam;
                    int score = scoreOverride != null ? Math.max(1, scoreOverride) : current.scoreToWin;
                    int duration = resolveDuration(mode, durationOverride, current);

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
                    signupFlow.scheduleChallengeTimeout(match);
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
                WarMatch match = registry.getByGuild(guild.getId());
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
                        "{seconds}", String.valueOf(settings.get().signupSeconds));
                signupFlow.scheduleSignupTimeout(match);
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
                WarMatch match = registry.getByGuild(guild.getId());
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
                WarMatch match = registry.getByGuild(guild.getId());
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

    public CompletableFuture<Void> join(Player player) {
        return guildService.getPlayerGuildAsync(player.getUniqueId()).thenCompose(guild -> {
            if (guild == null) {
                return failed("war.no-guild", "&c你不在任何公会中");
            }
            WarMatch match = registry.getByGuild(guild.getId());
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
        WarMatch match = registry.getByPlayer(player.getUniqueId());
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
                WarMatch match = registry.getByGuild(guild.getId());
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
                    signupFlow.beginPreparing(match);
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

    private CompletableFuture<Guild> resolveGuild(String query) {
        return guildService.getGuildByNameAsync(query).thenCompose(byName -> {
            if (byName != null) {
                return CompletableFuture.completedFuture(byName);
            }
            return guildService.getGuildByTagAsync(query);
        });
    }

    private static int resolveDuration(VictoryMode mode, Integer override, WarSettings settings) {
        if (override != null && override > 0) {
            return override;
        }
        return mode == VictoryMode.LAST_STANDING
                ? settings.surviveDurationSeconds
                : settings.timedDurationSeconds;
    }

    private static boolean isOfficerOrLeader(GuildMember member) {
        return member.getRole() == GuildMember.Role.LEADER
                || member.getRole() == GuildMember.Role.OFFICER;
    }

    private static <T> CompletableFuture<T> failed(String key, String def, String... ph) {
        return CompletableFuture.failedFuture(new LocalizedException(key, def, ph));
    }
}
