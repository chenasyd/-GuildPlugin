package com.guild.sdk;

import com.guild.GuildPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildQueryFacadeTest {

    private GuildPlugin plugin;
    private GuildQueryFacade facade;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        facade = new GuildQueryFacade(plugin, new GuildDataMapper(plugin));
    }

    @Test
    void getMemberActivityScores_whenServiceUnavailable_returnsEmptyList() {
        when(plugin.getActivityScoreService()).thenReturn(null);

        List<com.guild.sdk.data.ActivityScoreData> scores = facade.getMemberActivityScores(1).join();

        assertEquals(List.of(), scores);
    }
}
