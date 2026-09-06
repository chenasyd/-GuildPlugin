package com.guild.core.permissions;

import com.guild.GuildPlugin;
import com.guild.core.ServiceContainer;
import com.guild.core.config.ConfigManager;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionManagerTerritoryTest {

    private static final String MATRIX = """
            permissions:
              default:
                can-create: true
                can-invite: false
                can-kick: false
              leader:
                can-invite: true
                can-kick: true
              officer:
                can-invite: true
                can-kick: true
              member:
                can-invite: false
                can-kick: false
            """;

    private PermissionManager permissionManager;
    private GuildService guildService;

    private final UUID memberUuid = UUID.randomUUID();
    private final UUID officerUuid = UUID.randomUUID();
    private final Guild guild = new Guild("G", "G", "", officerUuid, "Officer");

    @BeforeEach
    void setUp() throws InvalidConfigurationException {
        guild.setId(1);

        GuildPlugin plugin = mock(GuildPlugin.class);
        ConfigManager configManager = mock(ConfigManager.class);
        ServiceContainer container = mock(ServiceContainer.class);
        guildService = mock(GuildService.class);
        Server server = mock(Server.class);

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.loadFromString(MATRIX);
        when(configManager.getMainConfig()).thenReturn(cfg);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getServiceContainer()).thenReturn(container);
        when(container.get(GuildService.class)).thenReturn(guildService);
        when(plugin.getGuildService()).thenReturn(guildService);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPlayer(any(UUID.class))).thenReturn(null);

        permissionManager = new PermissionManager(plugin);
        when(plugin.getPermissionManager()).thenReturn(permissionManager);
        when(plugin.getMembershipRules()).thenAnswer(inv -> new GuildMembershipRules(plugin));
    }

    @Test
    void memberCanViewTerritoryInfoWhenInGuild() {
        Player player = mockPlayer(memberUuid);
        when(guildService.getPlayerGuild(memberUuid)).thenReturn(guild);
        when(guildService.getGuildMember(memberUuid)).thenReturn(
                new GuildMember(guild.getId(), memberUuid, "Member", GuildMember.Role.MEMBER));

        assertTrue(permissionManager.hasPermission(player, "guild.territory.info"));
        assertFalse(permissionManager.hasPermission(player, "guild.territory.claim"));
    }

    @Test
    void officerCanClaimTerritory() {
        Player player = mockPlayer(officerUuid);
        when(guildService.getPlayerGuild(officerUuid)).thenReturn(guild);
        when(guildService.getGuildMember(officerUuid)).thenReturn(
                new GuildMember(guild.getId(), officerUuid, "Officer", GuildMember.Role.OFFICER));

        assertTrue(permissionManager.hasPermission(player, "guild.territory.claim"));
        assertTrue(permissionManager.hasPermission(player, "guild.territory.unclaim"));
    }

    private Player mockPlayer(UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission(anyString())).thenReturn(false);
        return player;
    }
}
