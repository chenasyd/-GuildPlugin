package com.guild.war;

import com.guild.war.model.VictoryMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarSettingsTest {

    @Test
    void constructor_appliesDefaultsWhenConfigEmpty() {
        WarSettings settings = new WarSettings(new YamlConfiguration());
        assertTrue(settings.enabled);
        assertEquals(VictoryMode.FIRST_TO_SCORE, settings.defaultMode);
        assertEquals(5, settings.maxPerTeam);
        assertEquals(60, settings.signupSeconds);
        assertEquals(10, settings.countdownSeconds);
        assertEquals(120, settings.challengeTimeoutSeconds);
        assertEquals(20, settings.scoreToWin);
        assertEquals(3, settings.maxConcurrent);
        assertEquals("default", settings.seasonId);
    }

    @Test
    void constructor_clampsMinimumValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("guild-war.max-per-team", 0);
        config.set("guild-war.signup-seconds", 1);
        config.set("guild-war.countdown-seconds", 1);
        config.set("guild-war.challenge-timeout-seconds", 5);
        config.set("guild-war.timed-duration-seconds", 10);
        config.set("guild-war.survive-duration-seconds", 10);
        config.set("guild-war.max-concurrent", 0);

        WarSettings settings = new WarSettings(config);

        assertEquals(1, settings.maxPerTeam);
        assertEquals(5, settings.signupSeconds);
        assertEquals(3, settings.countdownSeconds);
        assertEquals(15, settings.challengeTimeoutSeconds);
        assertEquals(30, settings.timedDurationSeconds);
        assertEquals(30, settings.surviveDurationSeconds);
        assertEquals(1, settings.maxConcurrent);
    }

    @Test
    void constructor_readsCustomModeAndRewards() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("guild-war.enabled", false);
        config.set("guild-war.default-mode", "survive");
        config.set("guild-war.default-preset", "castle");
        config.set("guild-war.rewards.enabled", true);
        config.set("guild-war.rewards.winner-vault", 100.0);
        config.set("guild-war.season.id", "s2");

        WarSettings settings = new WarSettings(config);

        assertFalse(settings.enabled);
        assertEquals(VictoryMode.LAST_STANDING, settings.defaultMode);
        assertEquals("castle", settings.defaultPreset);
        assertTrue(settings.rewardsEnabled);
        assertEquals(100.0, settings.rewardWinnerVault);
        assertEquals("s2", settings.seasonId);
    }
}
