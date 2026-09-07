package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 公会/成员只读查询（供 {@link GuildPluginAPI} 委托）。
 */
public final class GuildQueryFacade {

    private final GuildPlugin plugin;
    private final GuildDataMapper mapper;

    public GuildQueryFacade(GuildPlugin plugin) {
        this(plugin, new GuildDataMapper(plugin));
    }

    GuildQueryFacade(GuildPlugin plugin, GuildDataMapper mapper) {
        this.plugin = plugin;
        this.mapper = mapper;
    }

    public CompletableFuture<GuildData> getGuildById(int id) {
        return plugin.getGuildService().getGuildByIdAsync(id).thenApply(mapper::convertGuild);
    }

    public CompletableFuture<GuildData> getGuildByName(String name) {
        return plugin.getGuildService().getGuildByNameAsync(name).thenApply(mapper::convertGuild);
    }

    public CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid) {
        return plugin.getGuildService().getPlayerGuildAsync(playerUuid).thenApply(mapper::convertGuild);
    }

    public CompletableFuture<List<GuildData>> getAllGuilds() {
        return plugin.getGuildService().getAllGuildsAsync().thenApply(list ->
                list.stream().map(mapper::convertGuild).filter(g -> g != null).toList());
    }

    public CompletableFuture<List<MemberData>> getGuildMembers(int guildId) {
        return plugin.getGuildService().getGuildMembersAsync(guildId).thenApply(list ->
                list.stream().map(mapper::convertMember).toList());
    }

    public CompletableFuture<List<com.guild.sdk.data.ActivityScoreData>> getMemberActivityScores(int guildId) {
        var service = plugin.getActivityScoreService();
        if (service == null || !service.getSettings().isEnabled()) {
            return CompletableFuture.completedFuture(List.of());
        }
        return service.getGuildScoresAsync(guildId).thenApply(list -> {
            List<com.guild.sdk.data.ActivityScoreData> out = new ArrayList<>(list.size());
            for (com.guild.activity.MemberActivityScore s : list) {
                out.add(new com.guild.sdk.data.ActivityScoreData(
                        s.getPlayerUuid(), s.getPlayerName(),
                        s.getEconomyPts(), s.getActivityPts(), s.getTotalScore(),
                        s.getRank(), s.isOnline()));
            }
            return out;
        });
    }
}
