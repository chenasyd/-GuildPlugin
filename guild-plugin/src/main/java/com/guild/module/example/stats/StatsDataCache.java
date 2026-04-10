package com.guild.module.example.stats;

import com.guild.module.example.stats.model.GuildStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StatsDataCache {
    private final ConcurrentHashMap<Integer, GuildStatistics> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 300000L; // 5 分钟

    public GuildStatistics getCachedStats(int guildId) {
        GuildStatistics stats = cache.get(guildId);
        if (stats != null && !isExpired(stats)) {
            return stats;
        }
        return null;
    }

    public void updateCache(int guildId, GuildStatistics stats) {
        cache.put(guildId, stats);
    }

    public List<GuildStatistics> getAllCachedStats() {
        return cache.values().stream()
            .filter(s -> !isExpired(s))
            .collect(java.util.stream.Collectors.toList());
    }

    public List<GuildStatistics> getAllCachedStatsIncludingExpired() {
        return new ArrayList<>(cache.values());
    }

    public boolean isExpired(GuildStatistics stats) {
        return System.currentTimeMillis() - stats.getLastUpdated() > CACHE_TTL;
    }

    public void invalidate(int guildId) {
        cache.remove(guildId);
    }

    public void clearAll() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
