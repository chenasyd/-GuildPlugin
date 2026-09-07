package com.guild.war.command.handler;

import com.guild.GuildPlugin;
import com.guild.war.GuildWarService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarExportHandlerTest {

    private GuildPlugin plugin;
    private GuildWarCommandContext ctx;
    private CommandSender sender;
    private WarExportHandler handler;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        when(plugin.getLanguageManager()).thenReturn(null);
        ctx = new GuildWarCommandContext(plugin, mock(GuildWarService.class));
        sender = mock(CommandSender.class);
        handler = new WarExportHandler();
    }

    @Test
    void handle_rejectsWithoutAdminPermission() {
        when(sender.hasPermission(GuildWarCommandContext.PERM_ADMIN)).thenReturn(false);

        handler.handle(ctx, sender, new String[]{"export", "1"});

        assertSentContains("guild.war.admin");
    }

    @Test
    void handle_rejectsMissingReportId() {
        when(sender.hasPermission(GuildWarCommandContext.PERM_ADMIN)).thenReturn(true);

        handler.handle(ctx, sender, new String[]{"export"});

        assertSentContains("export");
    }

    @Test
    void handle_rejectsInvalidReportId() {
        when(sender.hasPermission(GuildWarCommandContext.PERM_ADMIN)).thenReturn(true);

        handler.handle(ctx, sender, new String[]{"export", "abc"});

        assertSentContains("无效");
    }

    @Test
    void handle_rejectsUnsupportedFormat() {
        when(sender.hasPermission(GuildWarCommandContext.PERM_ADMIN)).thenReturn(true);

        handler.handle(ctx, sender, new String[]{"export", "1", "xml"});

        assertSentContains("json");
    }

    @Test
    void handle_reportsUnavailableWhenApiMissing() {
        when(sender.hasPermission(GuildWarCommandContext.PERM_ADMIN)).thenReturn(true);
        when(plugin.getGuildWarAPI()).thenReturn(null);

        handler.handle(ctx, sender, new String[]{"export", "1", "json"});

        assertSentContains("不可用");
    }

    private void assertSentContains(String fragment) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertTrue(captor.getValue().contains(fragment), captor.getValue());
    }
}
