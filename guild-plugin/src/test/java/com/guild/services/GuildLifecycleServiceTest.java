package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * P6-b：GuildLifecycleService 创建/删除/更新授权路径单测（Mockito spy，不触库）。
 */
class GuildLifecycleServiceTest {

    private static final int GUILD_ID = 1;
    private static final UUID LEADER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MEMBER_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ADMIN_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private GuildPlugin plugin;
    private PermissionManager permissionManager;
    private Server server;
    private GuildService service;
    private GuildLifecycleService lifecycle;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        permissionManager = mock(PermissionManager.class);
        server = mock(Server.class);
        when(plugin.getDatabaseManager()).thenReturn(mock(DatabaseManager.class));
        when(plugin.getLogger()).thenReturn(Logger.getLogger("GuildLifecycleServiceTest"));
        when(plugin.getPermissionManager()).thenReturn(permissionManager);
        when(plugin.getServer()).thenReturn(server);

        service = spy(new GuildService(plugin));
        service.ctx.serviceRef = service;
        lifecycle = service.ctx.lifecycle;
    }

    @Test
    void createGuildAsync_rejectsWhenNameExists() {
        doReturn(CompletableFuture.completedFuture(sampleGuild()))
                .when(service).getGuildByNameAsync("TakenName");

        assertFalse(lifecycle.createGuildAsync(
                "TakenName", "TG", "desc", LEADER_UUID, "Leader").join());
    }

    @Test
    void createGuildAsync_rejectsWhenTagExists() {
        doReturn(CompletableFuture.completedFuture(null))
                .when(service).getGuildByNameAsync("NewGuild");
        doReturn(CompletableFuture.completedFuture(sampleGuild()))
                .when(service).getGuildByTagAsync("TG");

        assertFalse(lifecycle.createGuildAsync(
                "NewGuild", "TG", "desc", LEADER_UUID, "Leader").join());
    }

    @Test
    void deleteGuildAsync_rejectsWhenRequesterNotLeader() {
        Guild guild = sampleGuild();
        GuildMember member = new GuildMember(GUILD_ID, MEMBER_UUID, "Member", GuildMember.Role.MEMBER);
        doReturn(CompletableFuture.completedFuture(guild)).when(service).getGuildByIdAsync(GUILD_ID);
        doReturn(CompletableFuture.completedFuture(member)).when(service).getGuildMemberAsync(MEMBER_UUID);

        assertFalse(lifecycle.deleteGuildAsync(GUILD_ID, MEMBER_UUID).join());
    }

    @Test
    void deleteGuildAsync_rejectsWhenGuildNotFound() {
        doReturn(CompletableFuture.completedFuture(null)).when(service).getGuildByIdAsync(GUILD_ID);

        assertFalse(lifecycle.deleteGuildAsync(GUILD_ID, LEADER_UUID).join());
    }

    @Test
    void updateGuildAsync_rejectsWhenRequesterIsRegularMember() {
        Guild guild = sampleGuild();
        GuildMember member = new GuildMember(GUILD_ID, MEMBER_UUID, "Member", GuildMember.Role.MEMBER);
        doReturn(CompletableFuture.completedFuture(guild)).when(service).getGuildByIdAsync(GUILD_ID);
        doReturn(CompletableFuture.completedFuture(member)).when(service).getGuildMemberAsync(MEMBER_UUID);

        assertFalse(lifecycle.updateGuildAsync(
                GUILD_ID, "Renamed", null, null, MEMBER_UUID).join());
    }

    @Test
    void forceDeleteGuildAsync_rejectsNonAdmin() {
        stubNonAdminPlayer(ADMIN_UUID);

        assertFalse(lifecycle.forceDeleteGuildAsync(GUILD_ID, ADMIN_UUID).join());
    }

    private Guild sampleGuild() {
        Guild guild = new Guild("TestGuild", "TG", "desc", LEADER_UUID, "Leader");
        guild.setId(GUILD_ID);
        return guild;
    }

    private void stubNonAdminPlayer(UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true);
        when(server.getPlayer(uuid)).thenReturn(player);
        when(permissionManager.hasPermission(eq(player), eq("guild.admin"))).thenReturn(false);
    }
}
