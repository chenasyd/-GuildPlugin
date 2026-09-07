package com.guild.module.example.member.rank;

import com.guild.GuildPlugin;
import com.guild.core.module.ModuleContext;
import com.guild.core.module.ModuleState;
import com.guild.sdk.config.ModuleConfigSection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnlineActivityTrackerTest {

    private static final UUID PLAYER = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private MemberRankModule module;
    private OnlineActivityTracker tracker;

    @BeforeEach
    void setUp() {
        module = mock(MemberRankModule.class);
        ModuleContext context = mock(ModuleContext.class);
        ModuleConfigSection config = mock(ModuleConfigSection.class);

        when(module.getContext()).thenReturn(context);
        when(context.getConfig()).thenReturn(config);
        when(config.getInt(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(config.getLong(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(module.getState()).thenReturn(ModuleState.ACTIVE);

        tracker = new OnlineActivityTracker(module);
    }

    @Test
    void onMove_ignoresSameBlock() throws Exception {
        Player player = mockPlayer();
        Location loc = player.getLocation();

        tracker.onMove(new PlayerMoveEvent(player, loc, loc.clone()));

        assertFalse(lastActionMillis().containsKey(PLAYER));
    }

    @Test
    void onMove_marksPlayerActiveWhenBlockChanges() throws Exception {
        Player player = mockPlayer();
        Location from = player.getLocation();
        Location to = from.clone().add(1, 0, 0);

        tracker.onMove(new PlayerMoveEvent(player, from, to));

        assertTrue(lastActionMillis().containsKey(PLAYER));
    }

    @Test
    void onInteract_ignoresPhysicalAction() throws Exception {
        Player player = mockPlayer();

        tracker.onInteract(new PlayerInteractEvent(player, Action.PHYSICAL, null, null, null));

        assertFalse(lastActionMillis().containsKey(PLAYER));
    }

    @Test
    void onQuit_clearsTrackedState() throws Exception {
        Player player = mockPlayer();
        Location to = player.getLocation().clone().add(1, 0, 0);
        tracker.onMove(new PlayerMoveEvent(player, player.getLocation(), to));
        assertTrue(lastActionMillis().containsKey(PLAYER));

        tracker.onQuit(new PlayerQuitEvent(player, ""));

        assertFalse(lastActionMillis().containsKey(PLAYER));
    }

    @Test
    void onCommand_marksPlayerActive() throws Exception {
        Player player = mockPlayer();
        org.bukkit.Server server = mock(org.bukkit.Server.class);
        when(player.getServer()).thenReturn(server);
        when(server.getOnlinePlayers()).thenReturn(java.util.Collections.emptyList());

        tracker.onCommand(new org.bukkit.event.player.PlayerCommandPreprocessEvent(player, "/help"));

        assertTrue(lastActionMillis().containsKey(PLAYER));
    }

    private Player mockPlayer() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location loc = new Location(world, 0, 64, 0);
        when(player.getUniqueId()).thenReturn(PLAYER);
        when(player.getLocation()).thenReturn(loc);
        return player;
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Long> lastActionMillis() throws Exception {
        Field field = OnlineActivityTracker.class.getDeclaredField("lastActionMillis");
        field.setAccessible(true);
        return (Map<UUID, Long>) field.get(tracker);
    }
}
