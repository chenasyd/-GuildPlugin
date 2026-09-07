package com.guild.sdk.api;

import com.guild.sdk.data.ActivityScoreData;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 公会与成员只读查询域 API。
 *
 * @since 1.6.7
 */
public interface GuildQueryAPI {

    CompletableFuture<GuildData> getGuildById(int id);

    CompletableFuture<GuildData> getGuildByName(String name);

    CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid);

    CompletableFuture<List<GuildData>> getAllGuilds();

    CompletableFuture<List<MemberData>> getGuildMembers(int guildId);

    CompletableFuture<List<ActivityScoreData>> getMemberActivityScores(int guildId);
}
