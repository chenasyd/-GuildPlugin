package com.guild.core.cache;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短 TTL 缓存：降低 PlaceholderAPI / 权限热路径上的同步 JDBC 压力。
 * <p>
 * 成员身份变更时应调用 {@link #invalidate(UUID)}（已由 {@code PermissionManager.updatePlayerPermissions} 触发）。
 */
public final class GuildPlayerDataCache {

    public static final class Snapshot {
        public final Guild guild;
        public final GuildMember member;
        public final Double contributionNet;
        public final Integer memberCount;
        public final long loadedAtMs;

        public Snapshot(Guild guild, GuildMember member, Double contributionNet,
                        Integer memberCount, long loadedAtMs) {
            this.guild = guild;
            this.member = member;
            this.contributionNet = contributionNet;
            this.memberCount = memberCount;
            this.loadedAtMs = loadedAtMs;
        }

        public Snapshot withContribution(Double net) {
            return new Snapshot(guild, member, net, memberCount, loadedAtMs);
        }

        public Snapshot withMemberCount(Integer count) {
            return new Snapshot(guild, member, contributionNet, count, loadedAtMs);
        }
    }

    private final GuildPlugin plugin;
    private final long ttlMs;
    private final ConcurrentHashMap<UUID, Snapshot> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> contributionLoading = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Boolean> memberCountLoading = new ConcurrentHashMap<>();

    public GuildPlayerDataCache(GuildPlugin plugin, long ttlMs) {
        this.plugin = plugin;
        this.ttlMs = Math.max(500L, ttlMs);
    }

    public void invalidate(UUID playerUuid) {
        if (playerUuid == null) return;
        cache.remove(playerUuid);
        contributionLoading.remove(playerUuid);
    }

    public void invalidateAll() {
        cache.clear();
        contributionLoading.clear();
        memberCountLoading.clear();
    }

    /**
     * 获取快照；缓存未命中时同步加载 guild+member（单次），贡献/人数异步补齐。
     */
    public Snapshot get(UUID playerUuid) {
        if (playerUuid == null) return empty();
        long now = System.currentTimeMillis();
        Snapshot cached = cache.get(playerUuid);
        if (cached != null && now - cached.loadedAtMs < ttlMs) {
            maybeRefreshContributionAsync(playerUuid, cached);
            maybeRefreshMemberCountAsync(playerUuid, cached);
            return cached;
        }

        GuildService gs = plugin.getGuildService();
        if (gs == null) return empty();

        Guild guild;
        GuildMember member;
        try {
            guild = gs.getPlayerGuild(playerUuid);
            member = gs.getGuildMember(playerUuid);
        } catch (Exception e) {
            return empty();
        }

        Snapshot snap = new Snapshot(guild, member, cached != null ? cached.contributionNet : null,
                cached != null ? cached.memberCount : null, now);
        cache.put(playerUuid, snap);
        maybeRefreshContributionAsync(playerUuid, snap);
        maybeRefreshMemberCountAsync(playerUuid, snap);
        return snap;
    }

    private void maybeRefreshContributionAsync(UUID playerUuid, Snapshot snap) {
        if (snap.guild == null) return;
        if (snap.contributionNet != null && System.currentTimeMillis() - snap.loadedAtMs < ttlMs) {
            return;
        }
        if (contributionLoading.putIfAbsent(playerUuid, Boolean.TRUE) != null) return;

        GuildService gs = plugin.getGuildService();
        if (gs == null) {
            contributionLoading.remove(playerUuid);
            return;
        }
        int guildId = snap.guild.getId();
        gs.getGuildContributionNetByPlayerAsync(guildId).whenComplete((map, err) -> {
            try {
                double net = 0;
                if (map != null) {
                    net = map.getOrDefault(playerUuid, 0.0);
                }
                Snapshot cur = cache.get(playerUuid);
                if (cur == null) {
                    cur = new Snapshot(snap.guild, snap.member, net, snap.memberCount, System.currentTimeMillis());
                } else {
                    cur = cur.withContribution(net);
                }
                cache.put(playerUuid, cur);
            } finally {
                contributionLoading.remove(playerUuid);
            }
        });
    }

    private void maybeRefreshMemberCountAsync(UUID playerUuid, Snapshot snap) {
        if (snap.guild == null) return;
        if (snap.memberCount != null && System.currentTimeMillis() - snap.loadedAtMs < ttlMs) {
            return;
        }
        int guildId = snap.guild.getId();
        if (memberCountLoading.putIfAbsent(guildId, Boolean.TRUE) != null) return;

        GuildService gs = plugin.getGuildService();
        if (gs == null) {
            memberCountLoading.remove(guildId);
            return;
        }
        gs.getGuildMemberCountAsync(guildId).whenComplete((count, err) -> {
            try {
                Snapshot cur = cache.get(playerUuid);
                if (cur == null || cur.guild == null || cur.guild.getId() != guildId) return;
                int c = count != null ? count : 0;
                cache.put(playerUuid, cur.withMemberCount(c));
            } finally {
                memberCountLoading.remove(guildId);
            }
        });
    }

    private static Snapshot empty() {
        return new Snapshot(null, null, null, null, 0L);
    }
}
