package com.guild.war;

import com.guild.war.model.WarMatch;
import com.guild.war.model.WarPhase;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 活跃对局索引：matchId / guildId / playerUuid 三向映射。
 */
public final class WarMatchRegistry {

    private final Map<Integer, WarMatch> matches = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> guildToMatch = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerToMatch = new ConcurrentHashMap<>();

    public void register(WarMatch match) {
        matches.put(match.id(), match);
        guildToMatch.put(match.guildAId(), match.id());
        guildToMatch.put(match.guildBId(), match.id());
    }

    public void unregister(WarMatch match) {
        matches.remove(match.id());
        guildToMatch.remove(match.guildAId(), match.id());
        guildToMatch.remove(match.guildBId(), match.id());
    }

    public void linkPlayer(UUID playerId, int matchId) {
        playerToMatch.put(playerId, matchId);
    }

    public void unlinkPlayer(UUID playerId) {
        playerToMatch.remove(playerId);
    }

    public boolean isPlayerLinked(UUID playerId) {
        return playerToMatch.containsKey(playerId);
    }

    public Collection<WarMatch> allMatches() {
        return matches.values();
    }

    public WarMatch getMatch(int matchId) {
        return matches.get(matchId);
    }

    public WarMatch getByPlayer(UUID playerId) {
        Integer matchId = playerToMatch.get(playerId);
        return matchId == null ? null : matches.get(matchId);
    }

    public WarMatch getByGuild(int guildId) {
        Integer matchId = guildToMatch.get(guildId);
        return matchId == null ? null : matches.get(matchId);
    }

    public int countNonEnded() {
        int count = 0;
        for (WarMatch match : matches.values()) {
            if (match.phase() != WarPhase.ENDED) {
                count++;
            }
        }
        return count;
    }

    public boolean isArenaWorld(String worldName) {
        if (worldName == null) {
            return false;
        }
        for (WarMatch match : matches.values()) {
            if (match.worldName() != null && match.worldName().equals(worldName)
                    && match.phase() != WarPhase.ENDED
                    && match.phase() != WarPhase.PENDING) {
                return true;
            }
        }
        return false;
    }
}
