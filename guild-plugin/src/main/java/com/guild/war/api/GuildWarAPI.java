package com.guild.war.api;

import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarMatch;
import com.guild.war.model.WarReportSnapshot;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 公会战对外 API。
 *
 * <p>通过 {@code GuildPlugin.getGuildWarAPI()} 或 ServiceContainer 获取。
 */
public interface GuildWarAPI {

    boolean isAvailable();

    String unavailableReason();

    Collection<WarMatch> getActiveMatches();

    WarMatch getMatch(int id);

    WarMatch getMatchByPlayer(UUID uuid);

    /** 等同于命令 status：返回玩家当前对局，无则 null。 */
    default WarMatch status(Player player) {
        return player == null ? null : getMatchByPlayer(player.getUniqueId());
    }

    CompletableFuture<WarMatch> challenge(Player player, String targetGuild,
                                          String preset, VictoryMode mode,
                                          Integer maxPerTeam, Integer scoreToWin, Integer durationSeconds);

    CompletableFuture<WarMatch> accept(Player player);

    CompletableFuture<Void> deny(Player player);

    CompletableFuture<Void> join(Player player);

    CompletableFuture<Void> leave(Player player);

    CompletableFuture<Void> ready(Player player);

    CompletableFuture<Void> cancel(Player player);

    CompletableFuture<Void> forceEnd(int matchId, String reason);

    CompletableFuture<List<WarReportSnapshot>> getRecentMatches(int limit);

    CompletableFuture<WarReportSnapshot> getMatchHistory(int reportId);

    CompletableFuture<WarReportSnapshot> getLatestMatchForPlayer(UUID uuid);

    /**
     * Export a persisted report to {@code plugins/GuildPlugin/exports/} as JSON or CSV.
     *
     * @param reportId report primary key
     * @param format   {@code json} or {@code csv}
     * @return absolute path of the written file
     */
    CompletableFuture<java.nio.file.Path> exportReport(int reportId, String format);
}
