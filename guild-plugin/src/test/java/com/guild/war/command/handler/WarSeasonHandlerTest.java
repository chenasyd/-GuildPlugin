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

class WarSeasonHandlerTest {

    private GuildPlugin plugin;
    private GuildWarCommandContext ctx;
    private CommandSender sender;
    private WarSeasonHandler handler;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        when(plugin.getLanguageManager()).thenReturn(null);
        when(plugin.getWarSeasonService()).thenReturn(null);
        ctx = new GuildWarCommandContext(plugin, mock(GuildWarService.class));
        sender = mock(CommandSender.class);
        handler = new WarSeasonHandler();
    }

    @Test
    void handle_reportsUnavailableWhenServiceMissing() {
        handler.handle(ctx, sender, new String[]{"season"});

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertTrue(captor.getValue().contains("赛季系统未就绪"));
    }
}
