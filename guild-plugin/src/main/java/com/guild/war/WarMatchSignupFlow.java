package com.guild.war;

import com.guild.war.model.WarMatch;

/** 挑战/报名阶段与对局编排之间的回调，由 {@link WarMatchOrchestrator} 实现。 */
public interface WarMatchSignupFlow {

    void scheduleChallengeTimeout(WarMatch match);

    void scheduleSignupTimeout(WarMatch match);

    void beginPreparing(WarMatch match);
}
