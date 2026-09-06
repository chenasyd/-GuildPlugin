package com.guild.war;

import com.guild.GuildPlugin;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.ScheduledTaskHandle;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarPhase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

/**
 * 每场对局单槽定时器：挑战超时、报名超时、开战倒计时、对局时限。
 */
public final class WarMatchScheduler {

    private final GuildPlugin plugin;
    private final Map<Integer, ScheduledTaskHandle> timers = new ConcurrentHashMap<>();

    public WarMatchScheduler(GuildPlugin plugin) {
        this.plugin = plugin;
    }

    public void cancel(int matchId) {
        ScheduledTaskHandle handle = timers.remove(matchId);
        if (handle != null) {
            handle.cancel();
        }
    }

    public void cancelAll() {
        timers.values().forEach(ScheduledTaskHandle::cancel);
        timers.clear();
    }

    public void scheduleChallengeTimeout(WarMatch match, long delayTicks, Runnable onTimeout) {
        scheduleLater(match.id(), () -> {
            if (match.phase() == WarPhase.PENDING) {
                onTimeout.run();
            }
        }, delayTicks);
    }

    public void scheduleSignupTimeout(WarMatch match, long delayTicks, Runnable onTimeout) {
        scheduleLater(match.id(), () -> {
            if (match.phase() == WarPhase.SIGNUP) {
                onTimeout.run();
            }
        }, delayTicks);
    }

    public void startCountdown(WarMatch match, int countdownSeconds,
                               IntConsumer onTick, Runnable onComplete) {
        cancel(match.id());
        match.setPhase(WarPhase.COUNTDOWN);
        final int[] secondsLeft = {countdownSeconds};
        ScheduledTaskHandle handle = CompatibleScheduler.runTaskTimer(plugin, () -> {
            if (match.phase() != WarPhase.COUNTDOWN) {
                cancel(match.id());
                return;
            }
            if (secondsLeft[0] <= 0) {
                cancel(match.id());
                onComplete.run();
                return;
            }
            onTick.accept(secondsLeft[0]);
            secondsLeft[0]--;
        }, 0L, 20L);
        timers.put(match.id(), handle);
    }

    public void scheduleMatchDuration(WarMatch match, long delayTicks, Runnable onExpire) {
        if (match.mode() == VictoryMode.FIRST_TO_SCORE) {
            return;
        }
        scheduleLater(match.id(), () -> {
            if (match.phase() == WarPhase.ACTIVE) {
                onExpire.run();
            }
        }, delayTicks);
    }

    private void scheduleLater(int matchId, Runnable task, long delayTicks) {
        cancel(matchId);
        ScheduledTaskHandle handle = CompatibleScheduler.runTaskLater(plugin, task, delayTicks);
        timers.put(matchId, handle);
    }

    /** 供单测注入 mock 句柄。 */
    void setTimerForTest(int matchId, ScheduledTaskHandle handle) {
        timers.put(matchId, handle);
    }

    int timerCountForTest() {
        return timers.size();
    }
}
