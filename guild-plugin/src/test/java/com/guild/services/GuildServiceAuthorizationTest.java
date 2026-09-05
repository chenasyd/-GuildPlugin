package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.core.permissions.GuildMembershipRules;
import com.guild.core.permissions.PermissionManager;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * GuildService P0 授权路径单测（Mockito spy，不触库）。
 */
class GuildServiceAuthorizationTest {

    private static final int GUILD_ID = 1;
    private static final UUID LEADER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MEMBER_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ADMIN_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID STRANGER_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private GuildPlugin plugin;
    private PermissionManager permissionManager;
    private GuildMembershipRules membershipRules;
    private Server server;
    private GuildService service;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        DatabaseManager databaseManager = mock(DatabaseManager.class);
        permissionManager = mock(PermissionManager.class);
        membershipRules = mock(GuildMembershipRules.class);
        server = mock(Server.class);

        when(plugin.getDatabaseManager()).thenReturn(databaseManager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("GuildServiceAuthorizationTest"));
        when(plugin.getPermissionManager()).thenReturn(permissionManager);
        when(plugin.getMembershipRules()).thenReturn(membershipRules);
        when(plugin.getServer()).thenReturn(server);

        service = spy(new GuildService(plugin));
        service.ctx.serviceRef = service;
    }

    @Test
    void forceDeleteGuildAsync_rejectsNonAdmin() {
        stubNonAdminPlayer(STRANGER_UUID);

        assertFalse(service.forceDeleteGuildAsync(GUILD_ID, STRANGER_UUID).join());
    }

    @Test
    void updateGuildBalanceByAdminAsync_rejectsNonAdmin() {
        stubNonAdminPlayer(STRANGER_UUID);

        assertFalse(service.updateGuildBalanceByAdminAsync(GUILD_ID, 100.0, STRANGER_UUID, "Stranger").join());
    }

    @Test
    void updateGuildFrozenStatusAsync_rejectsNonAdminOperator() {
        stubNonAdminPlayer(STRANGER_UUID);

        assertFalse(service.updateGuildFrozenStatusAsync(GUILD_ID, true, STRANGER_UUID).join());
    }

    @Test
    void transferGuildLeadershipAsync_rejectsUnauthorizedRequester() {
        Guild guild = sampleGuild();
        doReturn(CompletableFuture.completedFuture(guild)).when(service).getGuildByIdAsync(GUILD_ID);
        doReturn(new GuildMember(GUILD_ID, MEMBER_UUID, "Member", GuildMember.Role.MEMBER))
                .when(service).getGuildMember(MEMBER_UUID);

        assertFalse(service.transferGuildLeadershipAsync(
                GUILD_ID, STRANGER_UUID, "Target", MEMBER_UUID).join());
    }

    @Test
    void transferGuildLeadershipAsync_rejectsWhenTargetNotInGuild() {
        Guild guild = sampleGuild();
        Player admin = stubAdminPlayer(ADMIN_UUID);

        doReturn(CompletableFuture.completedFuture(guild)).when(service).getGuildByIdAsync(GUILD_ID);
        doReturn(CompletableFuture.completedFuture(null))
                .when(service).getGuildMemberAsync(GUILD_ID, STRANGER_UUID);
        when(permissionManager.hasPermission(admin, "guild.admin")).thenReturn(true);

        assertFalse(service.transferGuildLeadershipAsync(
                GUILD_ID, STRANGER_UUID, "Target", ADMIN_UUID).join());
    }

    @Test
    void sendInvitationAsync_rejectsInviterWithoutInvitePermission() {
        GuildMember inviter = new GuildMember(GUILD_ID, MEMBER_UUID, "Member", GuildMember.Role.MEMBER);
        doReturn(CompletableFuture.completedFuture(inviter))
                .when(service).getGuildMemberAsync(GUILD_ID, MEMBER_UUID);
        when(membershipRules.canInvite(inviter)).thenReturn(false);

        assertFalse(service.sendInvitationAsync(
                GUILD_ID, MEMBER_UUID, "Member", STRANGER_UUID, "Target").join());
    }

    @Test
    void sendInvitationAsync_rejectsWhenInviterNotInGuild() {
        doReturn(CompletableFuture.completedFuture(null))
                .when(service).getGuildMemberAsync(GUILD_ID, STRANGER_UUID);

        assertFalse(service.sendInvitationAsync(
                GUILD_ID, STRANGER_UUID, "Stranger", MEMBER_UUID, "Target").join());
    }

    @Test
    void isGuildAdmin_trueWhenOnlinePlayerHasPermission() {
        Player admin = stubAdminPlayer(ADMIN_UUID);
        when(permissionManager.hasPermission(admin, "guild.admin")).thenReturn(true);

        assertTrue(service.isGuildAdmin(ADMIN_UUID));
    }

    @Test
    void isGuildAdmin_falseWhenPlayerOffline() {
        when(server.getPlayer(ADMIN_UUID)).thenReturn(null);

        assertFalse(service.isGuildAdmin(ADMIN_UUID));
    }

    @Test
    void isGuildAdmin_falseWhenPlayerLacksPermission() {
        stubNonAdminPlayer(STRANGER_UUID);

        assertFalse(service.isGuildAdmin(STRANGER_UUID));
    }

    private Guild sampleGuild() {
        Guild guild = new Guild("TestGuild", "TG", "desc", LEADER_UUID, "Leader");
        guild.setId(GUILD_ID);
        return guild;
    }

    private Player stubAdminPlayer(UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true);
        when(server.getPlayer(uuid)).thenReturn(player);
        when(permissionManager.hasPermission(player, "guild.admin")).thenReturn(true);
        return player;
    }

    private void stubNonAdminPlayer(UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true);
        when(server.getPlayer(uuid)).thenReturn(player);
        when(permissionManager.hasPermission(eq(player), eq("guild.admin"))).thenReturn(false);
    }
}
