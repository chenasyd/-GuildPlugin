package com.guild.activity;

import com.guild.GuildPlugin;
import com.guild.models.GuildMember;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Computes hybrid economy + activity scores for guild members.
 */
public final class ActivityScoreService {

    private final GuildPlugin plugin;
    private final ActivityRepository repository;
    private final ActivitySettings settings;

    public ActivityScoreService(GuildPlugin plugin, ActivityRepository repository, ActivitySettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.settings = settings;
    }

    public ActivitySettings getSettings() {
        return settings;
    }

    public ActivityRepository getRepository() {
        return repository;
    }

    public CompletableFuture<List<MemberActivityScore>> getGuildScoresAsync(int guildId) {
        CompletableFuture<List<GuildMember>> membersF = plugin.getGuildService().getGuildMembersAsync(guildId);
        CompletableFuture<Map<UUID, Double>> netsF = plugin.getGuildService().getGuildContributionNetByPlayerAsync(guildId);
        CompletableFuture<List<MemberActivityRecord>> actsF = repository.findByGuildAsync(guildId);

        return CompletableFuture.allOf(membersF, netsF, actsF).thenApply(v -> {
            List<GuildMember> members = membersF.join();
            Map<UUID, Double> nets = netsF.join();
            Map<UUID, MemberActivityRecord> acts = new HashMap<>();
            for (MemberActivityRecord r : actsF.join()) {
                acts.put(r.getPlayerUuid(), r);
            }

            List<MemberActivityScore> raw = new ArrayList<>();
            for (GuildMember m : members) {
                double economy = Math.max(0.0, nets.getOrDefault(m.getPlayerUuid(), 0.0));
                MemberActivityRecord rec = acts.get(m.getPlayerUuid());
                boolean online = Bukkit.getPlayer(m.getPlayerUuid()) != null;
                double activity = calculateActivityPts(rec, online);
                double total = economy + activity * settings.getScoreWeightActivity();
                String name = m.getPlayerName() != null ? m.getPlayerName() : "";
                if (rec != null && rec.getPlayerName() != null && !rec.getPlayerName().isEmpty()) {
                    name = rec.getPlayerName();
                }
                raw.add(new MemberActivityScore(m.getPlayerUuid(), name, economy, activity, total, 0, online));
            }

            raw.sort(Comparator
                    .comparingDouble(MemberActivityScore::getTotalScore).reversed()
                    .thenComparing(MemberActivityScore::getPlayerName, String.CASE_INSENSITIVE_ORDER));

            List<MemberActivityScore> ranked = new ArrayList<>(raw.size());
            for (int i = 0; i < raw.size(); i++) {
                MemberActivityScore s = raw.get(i);
                ranked.add(new MemberActivityScore(
                        s.getPlayerUuid(), s.getPlayerName(),
                        s.getEconomyPts(), s.getActivityPts(), s.getTotalScore(),
                        i + 1, s.isOnline()));
            }
            return ranked;
        });
    }

    public CompletableFuture<MemberActivityScore> getMemberScoreAsync(int guildId, UUID playerUuid) {
        return getGuildScoresAsync(guildId).thenApply(list -> {
            for (MemberActivityScore s : list) {
                if (s.getPlayerUuid().equals(playerUuid)) {
                    return s;
                }
            }
            return new MemberActivityScore(playerUuid, "", 0, 0, 0, 0, Bukkit.getPlayer(playerUuid) != null);
        });
    }

    /**
     * Activity points 0–100 per plan formula.
     */
    public static double calculateActivityPts(MemberActivityRecord record, boolean online) {
        int onlineMinutesToday = 0;
        int activeDaysWeek = 0;
        long lastSeen = 0;
        boolean loggedInToday = false;

        if (record != null) {
            // Apply calendar normalization for display without mutating DB here
            String today = ActivityTracker.today();
            String week = ActivityTracker.weekStart();
            onlineMinutesToday = record.getOnlineMinutesToday();
            if (record.getTodayDate() == null || !today.equals(record.getTodayDate())) {
                onlineMinutesToday = 0;
            }
            activeDaysWeek = record.getActiveDaysWeek();
            if (record.getWeekStartDate() == null || !week.equals(record.getWeekStartDate())) {
                activeDaysWeek = 0;
            }
            lastSeen = record.getLastSeen();
            loggedInToday = ActivityTracker.loggedInToday(record);
        }

        double score = 0.0;
        score += Math.min(40.0, onlineMinutesToday * 0.5);
        score += Math.min(30.0, activeDaysWeek * 5.0);

        if (online) {
            score += 15.0;
        } else if (lastSeen > 0) {
            long offlineHours = Math.max(0L, (System.currentTimeMillis() - lastSeen) / 3_600_000L);
            if (offlineHours < 1) score += 12.0;
            else if (offlineHours < 6) score += 9.0;
            else if (offlineHours < 24) score += 6.0;
            else if (offlineHours < 72) score += 3.0;
        }

        if (loggedInToday) {
            score += 15.0;
        }

        return Math.min(100.0, Math.max(0.0, score));
    }
}
