package com.guild.war.api;

import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 工会战对外 API。
 *
 * <p>通过 {@code GuildPlugin.getGuildWarAPI()} 或 ServiceContainer 获取。
 */
public interface GuildWarAPI {

    boolean isAvailable();

    String unavailableReason();

    Collection<WarMatch> getActiveMatches();

    WarMatch getMatch(int id);

    WarMatch getMatchByPlayer(java.util.UUID uuid);

    CompletableFuture<WarMatch> challenge(Player player, String targetGuild,
                                          String preset, VictoryMode mode,
                                          Integer maxPerTeam, Integer scoreToWin, Integer durationSeconds);

    CompletableFuture<WarMatch> accept(Player player);

    CompletableFuture<Void> deny(Player player);

    CompletableFuture<Void> join(Player player);

    CompletableFuture<Void> leave(Player player);

    CompletableFuture<Void> forceEnd(int matchId, String reason);
}
