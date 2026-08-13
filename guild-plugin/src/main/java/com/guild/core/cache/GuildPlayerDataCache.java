package com.guild.core.cache;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-TTL cache to cut sync JDBC on PlaceholderAPI / permission / GUI hot paths.
 * <p>
 * Invalidate via {@link #invalidate(UUID)} on membership changes
 * ({@code PermissionManager.updatePlayerPermissions} already does this).
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
    /** Prevent recursive load when GuildService sync wrappers consult this cache. */
    private final ThreadLocal<Boolean> loading = ThreadLocal.withInitial(() -> Boolean.FALSE);

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

    /** Return cached snapshot only if still within TTL; never hits JDBC. */
    public Snapshot getIfPresent(UUID playerUuid) {
        if (playerUuid == null) return null;
        Snapshot cached = cache.get(playerUuid);
        if (cached == null) return null;
        if (System.currentTimeMillis() - cached.loadedAtMs >= ttlMs) return null;
        return cached;
    }

    /**
     * Load-through snapshot. Uses async JDBC once (both guild+member), not sync wrappers,
     * so {@link com.guild.services.GuildService#getPlayerGuild} can safely prefer this cache.
     */
    public Snapshot get(UUID playerUuid) {
        if (playerUuid == null) return empty();
        Snapshot present = getIfPresent(playerUuid);
        if (present != null) {
            maybeRefreshContributionAsync(playerUuid, present);
            maybeRefreshMemberCountAsync(playerUuid, present);
            return present;
        }

        GuildService gs = plugin.getGuildService();
        if (gs == null) return empty();

        if (Boolean.TRUE.equals(loading.get())) {
            // Nested call while already loading — avoid deadlock/recursion.
            return empty();
        }

        long now = System.currentTimeMillis();
        Snapshot prev = cache.get(playerUuid);
        loading.set(Boolean.TRUE);
        try {
            CompletableFuture<Guild> guildFut = gs.getPlayerGuildAsync(playerUuid);
            CompletableFuture<GuildMember> memberFut = gs.getGuildMemberAsync(playerUuid);
            Guild guild = guildFut.join();
            GuildMember member = memberFut.join();
            Snapshot snap = new Snapshot(guild, member,
                    prev != null ? prev.contributionNet : null,
                    prev != null ? prev.memberCount : null,
                    now);
            cache.put(playerUuid, snap);
            maybeRefreshContributionAsync(playerUuid, snap);
            maybeRefreshMemberCountAsync(playerUuid, snap);
            return snap;
        } catch (Exception e) {
            return empty();
        } finally {
            loading.set(Boolean.FALSE);
        }
    }

    public Guild getGuild(UUID playerUuid) {
        return get(playerUuid).guild;
    }

    public GuildMember getMember(UUID playerUuid) {
        return get(playerUuid).member;
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
                if (cur != null) {
                    cache.put(playerUuid, cur.withContribution(net));
                }
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
                if (count == null) return;
                Snapshot cur = cache.get(playerUuid);
                if (cur != null && cur.guild != null && cur.guild.getId() == guildId) {
                    cache.put(playerUuid, cur.withMemberCount(count));
                }
            } finally {
                memberCountLoading.remove(guildId);
            }
        });
    }

    private static Snapshot empty() {
        return new Snapshot(null, null, null, null, 0L);
    }
}
