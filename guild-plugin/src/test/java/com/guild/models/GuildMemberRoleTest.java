package com.guild.models;

import com.guild.GuildPlugin;
import com.guild.core.config.ConfigManager;
import com.guild.core.permissions.PermissionManager;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 config 驱动的 {@link PermissionManager} 角色矩阵（默认回退值）。
 * 业务鉴权请使用 {@link com.guild.core.permissions.GuildMembershipRules}。
 */
class GuildMemberRoleTest {

    private static final String DEFAULT_MATRIX = """
            permissions:
              leader:
                can-invite: true
                can-kick: true
                can-promote: true
                can-demote: true
                can-delete: true
              officer:
                can-invite: true
                can-kick: true
                can-promote: false
                can-demote: false
                can-delete: false
              member:
                can-invite: false
                can-kick: false
                can-promote: false
                can-demote: false
                can-delete: false
            """;

    private PermissionManager permissionManager;

    @BeforeEach
    void setUp() throws InvalidConfigurationException {
        GuildPlugin plugin = mock(GuildPlugin.class);
        ConfigManager configManager = mock(ConfigManager.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.loadFromString(DEFAULT_MATRIX);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMainConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("GuildMemberRoleTest"));
        permissionManager = new PermissionManager(plugin);
    }

    @Test
    void leaderHasFullManagementPermissions() {
        GuildMember.Role leader = GuildMember.Role.LEADER;
        assertTrue(permissionManager.roleCanInvite(leader));
        assertTrue(permissionManager.roleCanKick(leader));
        assertTrue(permissionManager.roleCanPromote(leader));
        assertTrue(permissionManager.roleCanDemote(leader));
        assertTrue(permissionManager.roleCanDeleteGuild(leader));
    }

    @Test
    void officerCanInviteAndKickOnly() {
        GuildMember.Role officer = GuildMember.Role.OFFICER;
        assertTrue(permissionManager.roleCanInvite(officer));
        assertTrue(permissionManager.roleCanKick(officer));
        assertFalse(permissionManager.roleCanPromote(officer));
        assertFalse(permissionManager.roleCanDemote(officer));
        assertFalse(permissionManager.roleCanDeleteGuild(officer));
    }

    @Test
    void memberHasNoManagementPermissions() {
        GuildMember.Role member = GuildMember.Role.MEMBER;
        assertFalse(permissionManager.roleCanInvite(member));
        assertFalse(permissionManager.roleCanKick(member));
        assertFalse(permissionManager.roleCanPromote(member));
        assertFalse(permissionManager.roleCanDemote(member));
        assertFalse(permissionManager.roleCanDeleteGuild(member));
    }
}
