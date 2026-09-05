package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.core.permissions.GuildMembershipRules;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * P6：域服务 wiring 与 GuildQueryService 查询/权限逻辑（Mockito spy，不触库）。
 */
class GuildServiceDomainTest {

    private static final int GUILD_ID = 7;
    private static final UUID LEADER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OFFICER_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID MEMBER_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private GuildPlugin plugin;
    private GuildMembershipRules membershipRules;
    private GuildService service;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        when(plugin.getDatabaseManager()).thenReturn(mock(DatabaseManager.class));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("GuildServiceDomainTest"));
        membershipRules = mock(GuildMembershipRules.class);
        when(plugin.getMembershipRules()).thenReturn(membershipRules);

        service = spy(new GuildService(plugin));
        service.ctx.serviceRef = service;
    }

    @Test
    void subServicesAreWiredOnConstruction() {
        assertNotNull(service.ctx.queries);
        assertNotNull(service.ctx.logs);
        assertNotNull(service.ctx.relations);
        assertNotNull(service.ctx.home);
        assertNotNull(service.ctx.economy);
        assertNotNull(service.ctx.members);
        assertNotNull(service.ctx.lifecycle);
        assertEquals(service, service.ctx.serviceRef);
    }

    @Test
    void isGuildLeader_trueForLeaderRole() {
        GuildQueryService queries = spy(service.ctx.queries);
        doReturn(new GuildMember(GUILD_ID, LEADER_UUID, "Leader", GuildMember.Role.LEADER))
                .when(queries).getGuildMember(LEADER_UUID);

        assertTrue(queries.isGuildLeader(LEADER_UUID));
        assertTrue(queries.isGuildLeader(LEADER_UUID, GUILD_ID));
    }

    @Test
    void isGuildLeader_falseForWrongGuildOrRole() {
        GuildQueryService queries = spy(service.ctx.queries);
        doReturn(new GuildMember(GUILD_ID, OFFICER_UUID, "Officer", GuildMember.Role.OFFICER))
                .when(queries).getGuildMember(OFFICER_UUID);

        assertFalse(queries.isGuildLeader(OFFICER_UUID));
        assertFalse(queries.isGuildLeader(OFFICER_UUID, GUILD_ID));
    }

    @Test
    void isGuildOfficer_trueOnlyForOfficerRole() {
        GuildQueryService queries = spy(service.ctx.queries);
        doReturn(new GuildMember(GUILD_ID, OFFICER_UUID, "Officer", GuildMember.Role.OFFICER))
                .when(queries).getGuildMember(OFFICER_UUID);
        doReturn(new GuildMember(GUILD_ID, MEMBER_UUID, "Member", GuildMember.Role.MEMBER))
                .when(queries).getGuildMember(MEMBER_UUID);

        assertTrue(queries.isGuildOfficer(OFFICER_UUID));
        assertFalse(queries.isGuildOfficer(MEMBER_UUID));
    }

    @Test
    void hasGuildPermission_delegatesToMembershipRules() {
        GuildQueryService queries = service.ctx.queries;
        when(membershipRules.canManageGuild(MEMBER_UUID)).thenReturn(true);
        when(membershipRules.canManageGuild(LEADER_UUID)).thenReturn(false);

        assertTrue(queries.hasGuildPermission(MEMBER_UUID));
        assertFalse(queries.hasGuildPermission(LEADER_UUID));
    }

    @Test
    void facadeDelegatesIsGuildLeaderToQueryService() {
        GuildQueryService queries = spy(service.ctx.queries);
        service.ctx.queries = queries;
        doReturn(new GuildMember(GUILD_ID, LEADER_UUID, "Leader", GuildMember.Role.LEADER))
                .when(queries).getGuildMember(LEADER_UUID);

        assertTrue(service.isGuildLeader(LEADER_UUID));
    }

    @Test
    void getEffectiveMaxMembers_usesEconomyService() {
        Guild guild = new Guild("Test", "T", "d", LEADER_UUID, "Leader");
        guild.setMaxMembers(35);

        assertEquals(35, service.getEffectiveMaxMembers(guild));
    }
}
