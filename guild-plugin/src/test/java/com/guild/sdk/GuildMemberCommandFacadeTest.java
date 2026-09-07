package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GuildMemberCommandFacadeTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private GuildPlugin plugin;
    private GuildService guildService;
    private GuildMemberCommandFacade facade;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        guildService = mock(GuildService.class);
        when(plugin.getGuildService()).thenReturn(guildService);
        facade = new GuildMemberCommandFacade(plugin);
    }

    @Test
    void addMember_invalidRole_returnsFalseWithoutCallingService() {
        assertFalse(facade.addMember(1, PLAYER, "Name", "NOT_A_ROLE").join());

        verifyNoInteractions(guildService);
    }

    @Test
    void addMember_validRole_delegatesToService() {
        when(guildService.addGuildMemberAsync(1, PLAYER, "Name", GuildMember.Role.MEMBER))
                .thenReturn(CompletableFuture.completedFuture(true));

        assertTrue(facade.addMember(1, PLAYER, "Name", "member").join());

        verify(guildService).addGuildMemberAsync(1, PLAYER, "Name", GuildMember.Role.MEMBER);
    }

    @Test
    void removeMember_delegatesToDirectRemoval() {
        when(guildService.removeGuildMemberDirectAsync(3, PLAYER))
                .thenReturn(CompletableFuture.completedFuture(true));

        assertTrue(facade.removeMember(3, PLAYER).join());

        verify(guildService).removeGuildMemberDirectAsync(3, PLAYER);
    }

    @Test
    void setMemberRole_delegatesToDirectUpdate() {
        when(guildService.updateMemberRoleDirectAsync(2, PLAYER, "OFFICER"))
                .thenReturn(CompletableFuture.completedFuture(true));

        assertTrue(facade.setMemberRole(2, PLAYER, "OFFICER").join());

        verify(guildService).updateMemberRoleDirectAsync(2, PLAYER, "OFFICER");
    }
}
