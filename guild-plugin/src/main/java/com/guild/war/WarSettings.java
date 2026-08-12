package com.guild.war;

import com.guild.war.model.VictoryMode;
import org.bukkit.configuration.file.FileConfiguration;

/** 工会战配置快照。 */
public final class WarSettings {

    public final boolean enabled;
    public final String defaultPreset;
    public final int maxPerTeam;
    public final int signupSeconds;
    public final int countdownSeconds;
    public final int challengeTimeoutSeconds;
    public final VictoryMode defaultMode;
    public final int scoreToWin;
    public final int timedDurationSeconds;
    public final int surviveDurationSeconds;
    public final boolean friendlyFire;
    public final boolean keepInventory;
    public final int maxConcurrent;
    public final boolean eliminateToSpectator;
    public final boolean arenaProtect;
    public final boolean broadcastReport;
    public final boolean rewardsEnabled;
    public final double rewardWinnerVault;
    public final double rewardLoserVault;
    public final double rewardWinnerGuildBank;
    public final double rewardContributionPoints;
    public final double rewardContributionPerKill;
    public final String seasonId;

    public WarSettings(FileConfiguration config) {
        this.enabled = config.getBoolean("guild-war.enabled", true);
        this.defaultPreset = config.getString("guild-war.default-preset", "");
        this.maxPerTeam = Math.max(1, config.getInt("guild-war.max-per-team", 5));
        this.signupSeconds = Math.max(5, config.getInt("guild-war.signup-seconds", 60));
        this.countdownSeconds = Math.max(3, config.getInt("guild-war.countdown-seconds", 10));
        this.challengeTimeoutSeconds = Math.max(15, config.getInt("guild-war.challenge-timeout-seconds", 120));
        VictoryMode parsed = VictoryMode.parse(config.getString("guild-war.default-mode", "first"));
        this.defaultMode = parsed != null ? parsed : VictoryMode.FIRST_TO_SCORE;
        this.scoreToWin = Math.max(1, config.getInt("guild-war.score-to-win", 20));
        this.timedDurationSeconds = Math.max(30, config.getInt("guild-war.timed-duration-seconds", 600));
        this.surviveDurationSeconds = Math.max(30, config.getInt("guild-war.survive-duration-seconds", 600));
        this.friendlyFire = config.getBoolean("guild-war.friendly-fire", false);
        this.keepInventory = config.getBoolean("guild-war.keep-inventory", true);
        this.maxConcurrent = Math.max(1, config.getInt("guild-war.max-concurrent", 3));
        this.eliminateToSpectator = config.getBoolean("guild-war.eliminate-to-spectator", true);
        this.arenaProtect = config.getBoolean("guild-war.arena-protect", true);
        this.broadcastReport = config.getBoolean("guild-war.broadcast-report", true);
        this.rewardsEnabled = config.getBoolean("guild-war.rewards.enabled", false);
        this.rewardWinnerVault = config.getDouble("guild-war.rewards.winner-vault", 0);
        this.rewardLoserVault = config.getDouble("guild-war.rewards.loser-vault", 0);
        this.rewardWinnerGuildBank = config.getDouble("guild-war.rewards.winner-guild-bank", 0);
        this.rewardContributionPoints = config.getDouble("guild-war.rewards.contribution-win", 10);
        this.rewardContributionPerKill = config.getDouble("guild-war.rewards.contribution-per-kill", 1);
        this.seasonId = config.getString("guild-war.season.id", "default");
    }
}
