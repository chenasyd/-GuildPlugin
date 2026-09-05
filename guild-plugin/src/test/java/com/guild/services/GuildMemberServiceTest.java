package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.core.permissions.GuildMembershipRules;
import com.guild.core.permissions.PermissionManager;
import com.guild.models.Guild;
import com.guild.models.GuildInvitation;
import com.guild.models.GuildMember;
import org.bukkit.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * P6-b：GuildMemberService 授权与容量路径单测（Mockito spy，不触库）。
 */
class GuildMemberServiceTest {

    private static final int GUILD_ID = 1;
    private static final UUID LEADER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OFFICER_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TARGET_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private GuildPlugin plugin;
    private GuildMembershipRules membershipRules;
    private GuildService service;
    private GuildMemberService members;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        when(plugin.getDatabaseManager()).thenReturn(mock(DatabaseManager.class));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("GuildMemberServiceTest"));
        when(plugin.getPermissionManager()).thenReturn(mock(PermissionManager.class));
        membershipRules = mock(GuildMembershipRules.class);
        when(plugin.getMembershipRules()).thenReturn(membershipRules);
        when(plugin.getServer()).thenReturn(mock(Server.class));

        service = spy(new GuildService(plugin));
        service.ctx.serviceRef = service;
        members = service.ctx.members;
    }

    @Test
    void addGuildMemberAsync_rejectsWhenPlayerAlreadyInGuild() {
        Guild otherGuild = new Guild("Other", "OT", "d", LEADER_UUID, "Leader");
        otherGuild.setId(99);
        doReturn(CompletableFuture.completedFuture(otherGuild)).when(service).getPlayerGuildAsync(TARGET_UUID);

        assertFalse(members.addGuildMemberAsync(
                GUILD_ID, TARGET_UUID, "Target", GuildMember.Role.MEMBER).join());
    }

    @Test
    void addGuildMemberAsync_rejectsWhenGuildFull() {
        Guild guild = sampleGuild();
        guild.setMaxMembers(6);
        doReturn(CompletableFuture.completedFuture(null)).when(service).getPlayerGuildAsync(TARGET_UUID);
        doReturn(CompletableFuture.completedFuture(guild)).when(service).getGuildByIdAsync(GUILD_ID);
        doReturn(CompletableFuture.completedFuture(6)).when(service).getGuildMemberCountAsync(GUILD_ID);

        assertFalse(members.addGuildMemberAsync(
                GUILD_ID, TARGET_UUID, "Target", GuildMember.Role.MEMBER).join());
    }

    @Test
    void removeGuildMemberAsync_rejectsKickLeaderByOfficer() {
        GuildMember leader = new GuildMember(GUILD_ID, LEADER_UUID, "Leader", GuildMember.Role.LEADER);
        GuildMember officer = new GuildMember(GUILD_ID, OFFICER_UUID, "Officer", GuildMember.Role.OFFICER);
        doReturn(CompletableFuture.completedFuture(leader)).when(service).getGuildMemberAsync(LEADER_UUID);
        doReturn(CompletableFuture.completedFuture(officer)).when(service).getGuildMemberAsync(OFFICER_UUID);

        assertFalse(members.removeGuildMemberAsync(LEADER_UUID, OFFICER_UUID).join());
    }

    @Test
    void removeGuildMemberAsync_rejectsWhenRequesterNotInSameGuild() {
        GuildMember target = new GuildMember(GUILD_ID, TARGET_UUID, "Target", GuildMember.Role.MEMBER);
        UUID outsiderUuid = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        GuildMember outsider = new GuildMember(2, outsiderUuid, "Outsider", GuildMember.Role.OFFICER);
        doReturn(CompletableFuture.completedFuture(target)).when(service).getGuildMemberAsync(TARGET_UUID);
        doReturn(CompletableFuture.completedFuture(outsider)).when(service).getGuildMemberAsync(outsiderUuid);

        assertFalse(members.removeGuildMemberAsync(TARGET_UUID, outsiderUuid).join());
    }

    @Test
    void processInvitationDirectAsync_rejectsAcceptWhenGuildFull() {
        GuildInvitation invitation = new GuildInvitation(
                GUILD_ID, LEADER_UUID, "Leader", TARGET_UUID, "Target");
        invitation.setId(10);
        doReturn(CompletableFuture.completedFuture(true)).when(service).isGuildFullAsync(GUILD_ID);

        assertFalse(members.processInvitationDirectAsync(invitation, true).join());
    }

    private Guild sampleGuild() {
        Guild guild = new Guild("TestGuild", "TG", "desc", LEADER_UUID, "Leader");
        guild.setId(GUILD_ID);
        return guild;
    }
}
