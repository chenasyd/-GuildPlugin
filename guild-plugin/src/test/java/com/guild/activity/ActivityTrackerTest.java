package com.guild.activity;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.guild.GuildPlugin;
import com.guild.core.config.ConfigManager;
import com.guild.core.module.ModuleManager;
import com.guild.core.module.ModuleRegistry;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.models.Guild;
import com.guild.services.GuildService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityTrackerTest {

    private static final UUID PLAYER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private ServerMock server;
    private GuildPlugin plugin;
    private ActivityRepository repository;
    private ActivitySettings settings;
    private ActivityTracker tracker;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = mock(GuildPlugin.class);
        repository = mock(ActivityRepository.class);
        ConfigManager configManager = mock(ConfigManager.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("guild-activity.enabled", true);
        cfg.set("guild-activity.tick-interval-seconds", 60);
        cfg.set("guild-activity.daily-active-minutes", 5);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ActivityTrackerTest"));
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getServer()).thenReturn(server);
        stubPluginLoader(plugin);

        settings = new ActivitySettings(plugin);
        tracker = new ActivityTracker(plugin, repository, settings);
    }

    @AfterEach
    void tearDown() {
        if (tracker != null) {
            tracker.stop();
        }
        MockBukkit.unmock();
    }

    static void stubPluginLoader(GuildPlugin plugin) {
        PluginLoader loader = mock(PluginLoader.class);
        when(loader.createRegisteredListeners(any(), any())).thenReturn(Collections.emptyMap());
        when(plugin.getPluginLoader()).thenReturn(loader);
    }

    @Test
    void today_andWeekStart_useIsoDates() {
        assertNotNull(ActivityTracker.today());
        assertNotNull(ActivityTracker.weekStart());
        assertEquals(10, ActivityTracker.today().length());
    }

    @Test
    void loggedInToday_matchesLastLoginDate() {
        MemberActivityRecord record = new MemberActivityRecord(1, PLAYER, "Tester");
        record.setLastLoginDate(ActivityTracker.today());

        assertTrue(ActivityTracker.loggedInToday(record));
        assertFalse(ActivityTracker.loggedInToday(null));
        record.setLastLoginDate("1999-01-01");
        assertFalse(ActivityTracker.loggedInToday(record));
    }

    @Test
    void onJoin_recordsLoginForGuildMember() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER);
        when(player.getName()).thenReturn("Tester");

        GuildService guildService = mock(GuildService.class);
        Guild guild = new Guild("G", "G", "", PLAYER, "Leader");
        guild.setId(1);
        when(plugin.getGuildService()).thenReturn(guildService);
        when(guildService.getPlayerGuild(PLAYER)).thenReturn(guild);
        when(repository.find(1, PLAYER)).thenReturn(Optional.empty());

        tracker.onJoin(new PlayerJoinEvent(player, ""));

        ArgumentCaptor<MemberActivityRecord> captor = ArgumentCaptor.forClass(MemberActivityRecord.class);
        verify(repository, timeout(2000)).upsert(captor.capture());
        MemberActivityRecord saved = captor.getValue();
        assertEquals(ActivityTracker.today(), saved.getLastLoginDate());
        assertEquals("Tester", saved.getPlayerName());
    }

    @Test
    void onQuit_flushesSessionWithoutGuild() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER);
        when(player.getName()).thenReturn("Tester");

        GuildService guildService = mock(GuildService.class);
        when(plugin.getGuildService()).thenReturn(guildService);
        when(guildService.getPlayerGuild(PLAYER)).thenReturn(null);

        tracker.onJoin(new PlayerJoinEvent(player, ""));
        tracker.onQuit(new PlayerQuitEvent(player, ""));

        verify(guildService, timeout(2000).atLeastOnce()).getPlayerGuild(PLAYER);
    }

    @Test
    void startAndStop_registersAndUnregisters() {
        tracker.start();
        tracker.stop();
    }
}
