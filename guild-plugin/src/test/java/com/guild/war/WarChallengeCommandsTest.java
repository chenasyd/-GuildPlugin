package com.guild.war;

import com.guild.core.language.LocalizedException;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarParticipant;
import com.guild.war.model.WarPhase;
import com.guild.war.model.WarTeamSide;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WarChallengeCommandsTest {

    private static final UUID OFFICER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MEMBER_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CHALLENGER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private WarMatchRegistry registry;
    private GuildService guildService;
    private WarMatchLifecycle lifecycle;
    private WarBroadcastHelper broadcast;
    private WarMatchSignupFlow signupFlow;
    private WarChallengeCommands commands;
    private WarSettings settings;

    @BeforeEach
    void setUp() {
        registry = new WarMatchRegistry();
        guildService = mock(GuildService.class);
        lifecycle = mock(WarMatchLifecycle.class);
        broadcast = mock(WarBroadcastHelper.class);
        signupFlow = mock(WarMatchSignupFlow.class);
        settings = new WarSettings(new YamlConfiguration());
        commands = new WarChallengeCommands(
                guildService, null, registry, broadcast, new WarMatchScheduler(null),
                lifecycle, () -> settings, () -> true, () -> "", signupFlow);
    }

    @Test
    void leave_unlinksPlayerFromRegistry() throws Exception {
        WarMatch match = signupMatch();
        match.participants().put(MEMBER_UUID, new WarParticipant(MEMBER_UUID, "Member", WarTeamSide.B));
        registry.linkPlayer(MEMBER_UUID, match.id());
        Player player = mockPlayer(MEMBER_UUID, "Member");

        commands.leave(player).get();

        assertFalse(registry.isPlayerLinked(MEMBER_UUID));
        assertFalse(match.participants().containsKey(MEMBER_UUID));
    }

    @Test
    void join_rejectsWhenTeamFull() {
        WarMatch match = signupMatch();
        fillTeam(match, WarTeamSide.A, match.maxPerTeam());
        Guild guild = guild(10, "GuildA");
        when(guildService.getPlayerGuildAsync(MEMBER_UUID))
                .thenReturn(CompletableFuture.completedFuture(guild));
        Player player = mockPlayer(MEMBER_UUID, "Newbie");

        ExecutionException ex = assertThrows(ExecutionException.class, () -> commands.join(player).get());
        assertTrue(ex.getCause() instanceof LocalizedException);
        assertEquals("war.error.team-full", ((LocalizedException) ex.getCause()).key());
    }

    @Test
    void accept_rejectsWhenNotDefender() {
        WarMatch match = pendingMatch();
        registry.register(match);
        Guild guildA = guild(10, "GuildA");
        GuildMember officer = officer();
        when(guildService.getPlayerGuildAsync(OFFICER_UUID))
                .thenReturn(CompletableFuture.completedFuture(guildA));
        when(guildService.getGuildMemberAsync(OFFICER_UUID))
                .thenReturn(CompletableFuture.completedFuture(officer));
        Player player = mockPlayer(OFFICER_UUID, "OfficerA");

        ExecutionException ex = assertThrows(ExecutionException.class, () -> commands.accept(player).get());
        assertTrue(ex.getCause() instanceof LocalizedException);
        assertEquals("war.error.accept-not-defender", ((LocalizedException) ex.getCause()).key());
    }

    @Test
    void ready_bothTeamsReadyTriggersBeginPreparing() throws Exception {
        WarMatch match = signupMatch();
        match.participants().put(MEMBER_UUID, new WarParticipant(MEMBER_UUID, "B1", WarTeamSide.B));
        match.participants().put(OFFICER_UUID, new WarParticipant(OFFICER_UUID, "A1", WarTeamSide.A));
        Guild guildA = guild(10, "GuildA");
        GuildMember officer = officer();
        when(guildService.getPlayerGuildAsync(OFFICER_UUID))
                .thenReturn(CompletableFuture.completedFuture(guildA));
        when(guildService.getGuildMemberAsync(OFFICER_UUID))
                .thenReturn(CompletableFuture.completedFuture(officer));
        match.setTeamBReady(true);
        Player player = mockPlayer(OFFICER_UUID, "OfficerA");

        commands.ready(player).get();

        verify(signupFlow).beginPreparing(match);
    }

    @Test
    void forceEnd_delegatesToLifecycle() throws Exception {
        WarMatch match = signupMatch();
        registry.register(match);

        commands.forceEnd(match.id(), "custom").get();

        verify(lifecycle).endMatch(match, null, "custom");
    }

    @Test
    void challenge_rejectsWhenDisabled() {
        WarChallengeCommands disabled = new WarChallengeCommands(
                guildService, null, registry, broadcast, new WarMatchScheduler(null),
                lifecycle, () -> settings, () -> false, () -> "disabled", signupFlow);
        Player player = mockPlayer(OFFICER_UUID, "Officer");

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> disabled.challenge(player, "Target", null, null, null, null, null).get());
        assertTrue(ex.getCause() instanceof LocalizedException);
        assertEquals("war.unavailable", ((LocalizedException) ex.getCause()).key());
        verifyNoInteractions(signupFlow);
    }

    private WarMatch pendingMatch() {
        return new WarMatch(10, "GuildA", 20, "GuildB", CHALLENGER, "preset",
                VictoryMode.FIRST_TO_SCORE, 2, 10, 600);
    }

    private WarMatch signupMatch() {
        WarMatch match = pendingMatch();
        match.setPhase(WarPhase.SIGNUP);
        registry.register(match);
        return match;
    }

    private static void fillTeam(WarMatch match, WarTeamSide side, int count) {
        for (int i = 0; i < count; i++) {
            UUID uuid = UUID.nameUUIDFromBytes((side.name() + i).getBytes());
            match.participants().put(uuid, new WarParticipant(uuid, "P" + i, side));
        }
    }

    private static Guild guild(int id, String name) {
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn(id);
        when(guild.getName()).thenReturn(name);
        return guild;
    }

    private static GuildMember officer() {
        GuildMember member = mock(GuildMember.class);
        when(member.getRole()).thenReturn(GuildMember.Role.OFFICER);
        return member;
    }

    private static Player mockPlayer(UUID uuid, String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn(name);
        return player;
    }
}
