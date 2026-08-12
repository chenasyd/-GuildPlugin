package com.guild.war.api;

import com.guild.war.GuildWarService;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GuildWarAPIImpl implements GuildWarAPI {

    private final GuildWarService service;

    public GuildWarAPIImpl(GuildWarService service) {
        this.service = service;
    }

    @Override
    public boolean isAvailable() {
        return service.isEnabled();
    }

    @Override
    public String unavailableReason() {
        return service.unavailableReason();
    }

    @Override
    public Collection<WarMatch> getActiveMatches() {
        return service.getActiveMatches();
    }

    @Override
    public WarMatch getMatch(int id) {
        return service.getMatch(id);
    }

    @Override
    public WarMatch getMatchByPlayer(UUID uuid) {
        return service.getMatchByPlayer(uuid);
    }

    @Override
    public CompletableFuture<WarMatch> challenge(Player player, String targetGuild, String preset,
                                                 VictoryMode mode, Integer maxPerTeam,
                                                 Integer scoreToWin, Integer durationSeconds) {
        return service.challenge(player, targetGuild, preset, mode, maxPerTeam, scoreToWin, durationSeconds);
    }

    @Override
    public CompletableFuture<WarMatch> accept(Player player) {
        return service.accept(player);
    }

    @Override
    public CompletableFuture<Void> deny(Player player) {
        return service.deny(player);
    }

    @Override
    public CompletableFuture<Void> join(Player player) {
        return service.join(player);
    }

    @Override
    public CompletableFuture<Void> leave(Player player) {
        return service.leave(player);
    }

    @Override
    public CompletableFuture<Void> forceEnd(int matchId, String reason) {
        return service.forceEnd(matchId, reason);
    }
}
