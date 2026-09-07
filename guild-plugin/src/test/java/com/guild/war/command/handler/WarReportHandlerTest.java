package com.guild.war.command.handler;

import com.guild.GuildPlugin;
import com.guild.war.GuildWarService;
import com.guild.war.model.VictoryMode;
import com.guild.war.model.WarParticipantSnapshot;
import com.guild.war.model.WarReportSnapshot;
import com.guild.war.model.WarTeamSide;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarReportHandlerTest {

    private GuildPlugin plugin;
    private GuildWarCommandContext ctx;
    private CommandSender sender;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        when(plugin.getLanguageManager()).thenReturn(null);
        ctx = new GuildWarCommandContext(plugin, mock(GuildWarService.class));
        sender = mock(CommandSender.class);
    }

    @Test
    void showReport_none_sendsNotFoundMessage() {
        WarReportHandler.showReport(ctx, sender, null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertTrue(captor.getValue().contains("没有找到战报"));
    }

    @Test
    void showReport_includesTeamsAndParticipants() {
        WarReportSnapshot snap = new WarReportSnapshot(
                1, 7,
                1, "Alpha", 2, "Beta",
                1,
                VictoryMode.FIRST_TO_SCORE,
                10, 6, 20,
                "arena", "war.reason.test",
                0L, 1_000L, 2_500L,
                "season-1",
                List.of(new WarParticipantSnapshot(
                        UUID.randomUUID(), "Hero", WarTeamSide.A, 1, 2, false)));

        WarReportHandler.showReport(ctx, sender, snap);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(captor.capture());
        String combined = String.join("\n", captor.getAllValues());
        assertTrue(combined.contains("Alpha"));
        assertTrue(combined.contains("Beta"));
        assertTrue(combined.contains("Hero"));
    }
}
