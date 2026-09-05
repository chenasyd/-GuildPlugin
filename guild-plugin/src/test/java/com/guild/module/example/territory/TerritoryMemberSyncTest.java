package com.guild.module.example.territory;

import com.guild.sdk.data.MemberData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TerritoryMemberSyncTest {

    private static final UUID LEADER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MEMBER = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void findLeaderUuid_picksLeaderRole() {
        UUID leader = TerritoryMemberSync.findLeaderUuid(List.of(
                member(MEMBER, "MEMBER"),
                member(LEADER, "LEADER")
        ));
        assertEquals(LEADER, leader);
    }

    @Test
    void findLeaderUuid_fallbackFirstMember() {
        UUID leader = TerritoryMemberSync.findLeaderUuid(List.of(
                member(MEMBER, "MEMBER")
        ));
        assertEquals(MEMBER, leader);
    }

    @Test
    void findLeaderUuid_empty() {
        assertNull(TerritoryMemberSync.findLeaderUuid(List.of()));
    }

    @Test
    void repositoryFindByGuildId() {
        TerritoryRepository repo = new TerritoryRepository(
                new java.io.File(System.getProperty("java.io.tmpdir")), java.util.logging.Logger.getAnonymousLogger());
        repo.put(new TerritoryRecord(1, "A", "guild_1", "world", 0, 0, 0, 1, 1, 1, 0L));
        repo.put(new TerritoryRecord(2, "B", "guild_2", "world", 0, 0, 0, 1, 1, 1, 0L));
        repo.put(new TerritoryRecord(1, "A", "guild_1", "world_nether", 0, 0, 0, 1, 1, 1, 0L));

        assertEquals(2, repo.findByGuildId(1).size());
        assertEquals(1, repo.findByGuildId(2).size());
        assertEquals(0, repo.findByGuildId(99).size());

        repo.removeAllForGuild(1);
        assertEquals(0, repo.findByGuildId(1).size());
        assertEquals(1, repo.findByGuildId(2).size());
    }

    private static MemberData member(UUID uuid, String role) {
        return new MemberData(uuid, "player", role, 0L, 0.0, false);
    }
}
