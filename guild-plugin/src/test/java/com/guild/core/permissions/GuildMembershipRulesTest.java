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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link GuildMembershipRules} 与 config.yml 角色矩阵一致，而非硬编码 Role 默认值。
 */
class GuildMembershipRulesTest {

    private static final String DEFAULT_MATRIX = """
            permissions:
              default:
                can-create: true
                can-invite: false
                can-kick: false
                can-promote: false
                can-demote: false
                can-delete: false
              leader:
                can-create: true
                can-invite: true
                can-kick: true
                can-promote: true
                can-demote: true
                can-delete: true
              officer:
                can-create: true
                can-invite: true
                can-kick: true
                can-promote: false
                can-demote: false
                can-delete: false
              member:
                can-create: true
                can-invite: false
                can-kick: false
                can-promote: false
                can-demote: false
                can-delete: false
            """;

    private GuildPlugin plugin;
    private PermissionManager permissionManager;
    private GuildMembershipRules rules;
    private GuildService guildService;
    private Server server;

    private final UUID memberUuid = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID officerUuid = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final UUID leaderUuid = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private final Guild guild = new Guild("Test", "TST", "desc", leaderUuid, "Leader");

    @BeforeEach
    void setUp() {
        guild.setId(1);

        plugin = mock(GuildPlugin.class);
        ConfigManager configManager = mock(ConfigManager.class);
        ServiceContainer serviceContainer = mock(ServiceContainer.class);
        guildService = mock(GuildService.class);
        server = mock(Server.class);

        when(plugin.getLogger()).thenReturn(Logger.getLogger("GuildMembershipRulesTest"));
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getServiceContainer()).thenReturn(serviceContainer);
        when(plugin.getGuildService()).thenReturn(guildService);
        when(plugin.getGuildPlayerDataCache()).thenReturn(null);
        when(serviceContainer.get(GuildService.class)).thenReturn(guildService);
        when(plugin.getServer()).thenReturn(server);

        permissionManager = new PermissionManager(plugin);
        when(plugin.getPermissionManager()).thenReturn(permissionManager);
        rules = new GuildMembershipRules(plugin);
        when(plugin.getMembershipRules()).thenReturn(rules);

        applyConfig(DEFAULT_MATRIX);
    }

    @Test
    void defaultMatrix_memberCannotKickOrManage() {
        Player member = onlinePlayer(memberUuid, "Member");
        stubMembership(memberUuid, GuildMember.Role.MEMBER, "Member");

        assertFalse(rules.canKick(member));
        assertFalse(rules.canInvite(member));
        assertFalse(rules.canManageGuild(member));
        assertFalse(rules.canPromote(member));
        assertFalse(rules.canDeleteGuild(member));
    }

    @Test
    void defaultMatrix_officerCanInviteKickAndManageButNotPromote() {
        Player officer = onlinePlayer(officerUuid, "Officer");
        stubMembership(officerUuid, GuildMember.Role.OFFICER, "Officer");

        assertTrue(rules.canInvite(officer));
        assertTrue(rules.canKick(officer));
        assertTrue(rules.canManageGuild(officer));
        assertFalse(rules.canPromote(officer));
        assertFalse(rules.canDemote(officer));
        assertFalse(rules.canDeleteGuild(officer));
    }

    @Test
    void defaultMatrix_leaderHasFullManagementPermissions() {
        Player leader = onlinePlayer(leaderUuid, "Leader");
        stubMembership(leaderUuid, GuildMember.Role.LEADER, "Leader");

        assertTrue(rules.canInvite(leader));
        assertTrue(rules.canKick(leader));
        assertTrue(rules.canPromote(leader));
        assertTrue(rules.canDemote(leader));
        assertTrue(rules.canDeleteGuild(leader));
        assertTrue(rules.canManageGuild(leader));
    }

    @Test
    void configOverride_memberCanKickWhenEnabled() {
        applyConfig(DEFAULT_MATRIX + """
                permissions:
                  member:
                    can-kick: true
                """);

        Player member = onlinePlayer(memberUuid, "Member");
        stubMembership(memberUuid, GuildMember.Role.MEMBER, "Member");
        GuildMember memberRecord = guildService.getGuildMember(memberUuid);

        assertTrue(rules.canKick(member));
        assertTrue(rules.canManageGuild(member));
        assertFalse(rules.canInvite(member));

        when(server.getPlayer(memberUuid)).thenReturn(null);
        assertTrue(rules.canKick(memberRecord));
    }

    @Test
    void configOverride_memberCanInviteWhenEnabled() {
        applyConfig("""
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
                    can-create: true
                    can-invite: true
                    can-kick: false
                """);

        Player member = onlinePlayer(memberUuid, "Member");
        stubMembership(memberUuid, GuildMember.Role.MEMBER, "Member");

        assertTrue(rules.canInvite(member));
        assertTrue(rules.canManageGuild(member));
        assertFalse(rules.canKick(member));
    }

    @Test
    void reloadFromConfig_updatesPlayerPermissionsWithoutRestart() {
        Player member = onlinePlayer(memberUuid, "Member");
        stubMembership(memberUuid, GuildMember.Role.MEMBER, "Member");
        assertFalse(rules.canKick(member));

        applyConfig(DEFAULT_MATRIX + """
                permissions:
                  member:
                    can-kick: true
                """);

        assertTrue(rules.canKick(member));
        assertTrue(rules.canManageGuild(member));
    }

    @Test
    void offlineUuidCheckUsesRoleMatrixFromConfig() {
        stubMembership(officerUuid, GuildMember.Role.OFFICER, "Officer");
        when(server.getPlayer(officerUuid)).thenReturn(null);

        assertTrue(rules.canManageGuild(officerUuid));

        stubMembership(memberUuid, GuildMember.Role.MEMBER, "Member");
        assertFalse(rules.canManageGuild(memberUuid));

        applyConfig(DEFAULT_MATRIX + """
                permissions:
                  member:
                    can-kick: true
                """);
        assertTrue(rules.canManageGuild(memberUuid));
    }

    @Test
    void onlinePlayerRequiresGuildMembership_offlineMemberUsesRoleMatrix() {
        applyConfig(DEFAULT_MATRIX + """
                permissions:
                  member:
                    can-kick: true
                """);

        Player member = onlinePlayer(memberUuid, "Member");
        GuildMember memberRecord = new GuildMember(guild.getId(), memberUuid, "Member", GuildMember.Role.MEMBER);

        when(guildService.getPlayerGuild(memberUuid)).thenReturn(null);
        when(guildService.getGuildMember(memberUuid)).thenReturn(null);
        permissionManager.updatePlayerPermissions(memberUuid);

        assertFalse(rules.canKick(member));

        when(server.getPlayer(memberUuid)).thenReturn(null);
        assertTrue(rules.canKick(memberRecord));
    }

    @Test
    void guildMemberPromoteDemoteFollowConfigMatrix() {
        GuildMember officerRecord = new GuildMember(guild.getId(), officerUuid, "Officer", GuildMember.Role.OFFICER);
        GuildMember leaderRecord = new GuildMember(guild.getId(), leaderUuid, "Leader", GuildMember.Role.LEADER);

        when(server.getPlayer(officerUuid)).thenReturn(null);
        when(server.getPlayer(leaderUuid)).thenReturn(null);

        assertFalse(rules.canPromote(officerRecord));
        assertFalse(rules.canDemote(officerRecord));
        assertTrue(rules.canPromote(leaderRecord));
        assertTrue(rules.canDemote(leaderRecord));

        applyConfig(DEFAULT_MATRIX + """
                permissions:
                  officer:
                    can-promote: true
                    can-demote: true
                """);

        assertTrue(rules.canPromote(officerRecord));
        assertTrue(rules.canDemote(officerRecord));
    }

    @Test
    void isLeaderOfDelegatesToGuildService() {
        Player leader = onlinePlayer(leaderUuid, "Leader");
        when(guildService.isGuildLeader(leaderUuid, guild.getId())).thenReturn(true);
        when(guildService.isGuildLeader(memberUuid, guild.getId())).thenReturn(false);

        assertTrue(rules.isLeaderOf(leader, guild.getId()));
        assertFalse(rules.isLeaderOf(onlinePlayer(memberUuid, "Member"), guild.getId()));
    }

    private Player onlinePlayer(UUID uuid, String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn(name);
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(server.getPlayer(uuid)).thenReturn(player);
        return player;
    }

    private void stubMembership(UUID uuid, GuildMember.Role role, String name) {
        GuildMember member = new GuildMember(guild.getId(), uuid, name, role);
        when(guildService.getPlayerGuild(uuid)).thenReturn(guild);
        when(guildService.getGuildMember(uuid)).thenReturn(member);
        permissionManager.updatePlayerPermissions(uuid);
    }

    private void applyConfig(String yaml) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new IllegalStateException("Invalid test config yaml", e);
        }
        when(plugin.getConfigManager().getMainConfig()).thenReturn(configuration);
        permissionManager.reloadFromConfig();
    }
}
