package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.services.GuildService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildDataMapperTest {

    private GuildPlugin plugin;
    private GuildService guildService;
    private GuildDataMapper mapper;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        guildService = mock(GuildService.class);
        when(plugin.getGuildService()).thenReturn(guildService);
        mapper = new GuildDataMapper(plugin);
    }

    @Test
    void convertGuild_nullReturnsNull() {
        assertNull(mapper.convertGuild(null));
    }

    @Test
    void convertGuild_mapsCoreFields() {
        Guild guild = new Guild();
        guild.setId(7);
        guild.setName("Alpha");
        guild.setLeaderUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        guild.setLeaderName("Leader");
        guild.setLevel(3);
        guild.setBalance(100.5);
        guild.setMaxMembers(20);
        guild.setDescription("motto");
        guild.setCreatedAt(LocalDateTime.of(2024, 1, 2, 3, 4, 5));
        when(guildService.getGuildMemberCount(7)).thenReturn(5);

        var dto = mapper.convertGuild(guild);

        assertEquals(7, dto.getId());
        assertEquals("Alpha", dto.getName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), dto.getMasterUuid());
        assertEquals("Leader", dto.getMasterName());
        assertEquals(3, dto.getLevel());
        assertEquals(100.5, dto.getBalance());
        assertEquals(5, dto.getMemberCount());
        assertEquals(20, dto.getMaxMembers());
        assertEquals("motto", dto.getMotto());
    }

    @Test
    void convertMember_nullReturnsNull() {
        assertNull(mapper.convertMember(null));
    }
}
