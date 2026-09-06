package com.guild.war;

import com.guild.GuildPlugin;
import com.guild.war.event.WarMatchStartEvent;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import org.bukkit.Bukkit;

import java.util.function.Supplier;

/**
 * 对局编排：挑战/报名超时、开战倒计时、对局时限与超时胜负判定。
 */
public final class WarMatchOrchestrator implements WarMatchSignupFlow {

    private final GuildPlugin plugin;
    private final WarMatchScheduler scheduler;
    private final WarBroadcastHelper broadcast;
    private final WarMatchLifecycle lifecycle;
    private final Supplier<WarSettings> settings;

    public WarMatchOrchestrator(GuildPlugin plugin,
                                WarMatchScheduler scheduler,
                                WarBroadcastHelper broadcast,
                                WarMatchLifecycle lifecycle,
                                Supplier<WarSettings> settings) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.broadcast = broadcast;
        this.lifecycle = lifecycle;
        this.settings = settings;
    }

    @Override
    public void scheduleChallengeTimeout(WarMatch match) {
        WarSettings current = settings.get();
        scheduler.scheduleChallengeTimeout(match, current.challengeTimeoutSeconds * 20L, () -> {
            broadcast.broadcastMatch(match, "war.broadcast.challenge-timeout", "&c挑战已超时");
            lifecycle.cleanupMatch(match, false);
        });
    }

    @Override
    public void scheduleSignupTimeout(WarMatch match) {
        WarSettings current = settings.get();
        scheduler.scheduleSignupTimeout(match, current.signupSeconds * 20L, () -> {
            if (!match.bothTeamsHavePlayers()) {
                broadcast.broadcastMatch(match, "war.broadcast.signup-timeout",
                        "&c报名超时：双方人数不足，对局取消");
                lifecycle.cleanupMatch(match, false);
            } else {
                beginPreparing(match);
            }
        });
    }

    @Override
    public void beginPreparing(WarMatch match) {
        lifecycle.beginPreparing(match, () -> startCountdown(match));
    }

    void startCountdown(WarMatch match) {
        WarSettings current = settings.get();
        scheduler.startCountdown(match, current.countdownSeconds,
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

    void resolveTimedScore(WarMatch match) {
        if (match.scoreA() > match.scoreB()) {
            lifecycle.endMatch(match, match.guildAId(), "war.reason.timed-win");
        } else if (match.scoreB() > match.scoreA()) {
            lifecycle.endMatch(match, match.guildBId(), "war.reason.timed-win");
        } else {
            lifecycle.endMatch(match, null, "war.reason.timed-draw");
        }
    }

    void resolveSurviveTimeout(WarMatch match) {
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
}
