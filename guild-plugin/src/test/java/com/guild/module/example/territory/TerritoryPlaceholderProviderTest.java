package com.guild.module.example.territory;

import com.guild.GuildPlugin;
import com.guild.core.module.ModuleContext;
import com.guild.models.Guild;
import com.guild.services.GuildService;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TerritoryPlaceholderProviderTest {

    private TerritoryModule module;
    private ModuleContext context;
    private GuildPlugin plugin;
    private GuildService guildService;
    private Player player;
    private World world;

    private TerritoryRepository repository;
    private TerritoryPlaceholderProvider provider;

    @BeforeEach
    void setUp() {
        module = mock(TerritoryModule.class);
        context = mock(ModuleContext.class);
        plugin = mock(GuildPlugin.class);
        guildService = mock(GuildService.class);
        player = mock(Player.class);
        world = mock(World.class);

        repository = new TerritoryRepository(
                new java.io.File(System.getProperty("java.io.tmpdir")),
                java.util.logging.Logger.getAnonymousLogger());
        when(module.getRepository()).thenReturn(repository);
        when(module.getContext()).thenReturn(context);
        when(module.isWorldGuardReady()).thenReturn(false);
        when(context.getPlugin()).thenReturn(plugin);
        when(plugin.getGuildService()).thenReturn(guildService);
        when(player.getUniqueId()).thenReturn(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");

        provider = new TerritoryPlaceholderProvider(module);
    }

    @Test
    void server_returnsLocalServerId() {
        assertEquals("local-test", provider.onRequest(player, "server"));
    }

    @Test
    void has_returnsTrueWhenLocalTerritoryExists() {
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn(5);
        when(guildService.getPlayerGuild(player.getUniqueId())).thenReturn(guild);

        long now = System.currentTimeMillis();
        repository.put(new TerritoryRecord(
                5, "G5", TerritoryRecord.defaultRegionId(5), "local-test", "world",
                0, 0, 0, 3, 3, 3, now, TerritorySyncState.MATERIALIZED, now));

        assertEquals("True", provider.onRequest(player, "has"));
        assertEquals("guild_5", provider.onRequest(player, "region"));
        assertEquals("materialized", provider.onRequest(player, "sync"));
        assertEquals("64", provider.onRequest(player, "volume"));
    }

    @Test
    void count_withoutGuild_returnsZero() {
        when(guildService.getPlayerGuild(player.getUniqueId())).thenReturn(null);
        assertEquals("0", provider.onRequest(player, "count"));
        assertEquals("False", provider.onRequest(player, "has"));
    }

    @Test
    void count_localAndTotal() {
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn(9);
        when(guildService.getPlayerGuild(player.getUniqueId())).thenReturn(guild);

        long now = System.currentTimeMillis();
        repository.put(new TerritoryRecord(
                9, "G9", TerritoryRecord.defaultRegionId(9), "local-test", "world",
                0, 0, 0, 1, 1, 1, now, TerritorySyncState.MATERIALIZED, now));
        repository.put(new TerritoryRecord(
                9, "G9", TerritoryRecord.defaultRegionId(9), "remote-srv", "world_nether",
                0, 0, 0, 1, 1, 1, now, TerritorySyncState.MATERIALIZED, now));

        assertEquals("2", provider.onRequest(player, "count"));
        assertEquals("1", provider.onRequest(player, "count_local"));
        assertEquals("True", provider.onRequest(player, "has_any"));
    }
}
