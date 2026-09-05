package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.services.GuildService;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员命令权限门控单测（Mockito Player，不依赖 MockBukkit PlayerMock / Paper API）。
 */
class GuildAdminCommandPermissionTest {

    private GuildPlugin plugin;
    private LanguageManager languageManager;
    private GuildAdminCommand command;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        languageManager = mock(LanguageManager.class);
        when(plugin.getLanguageManager()).thenReturn(languageManager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("GuildAdminCommandPermissionTest"));
        when(plugin.getFileLogger()).thenReturn(null);
        when(languageManager.getCoreMessage("general.no-permission",
                "&cYou do not have permission to perform this action!"))
                .thenReturn("&cYou do not have permission to perform this action!");
        command = new GuildAdminCommand(plugin);
    }

    @Test
    void onCommand_deniesPlayerWithoutGuildAdminPermission() {
        Player player = mock(Player.class);
        when(player.hasPermission("guild.admin")).thenReturn(false);
        Command bukkitCommand = mock(Command.class);
        when(bukkitCommand.getName()).thenReturn("guildadmin");

        boolean handled = command.onCommand(player, bukkitCommand, "guildadmin", new String[]{"list"});

        assertTrue(handled);
        verify(player).sendMessage(ColorUtils.colorize(
                "&cYou do not have permission to perform this action!"));
    }

    @Test
    void onCommand_routesListSubcommandWhenPlayerHasGuildAdminPermission() {
        GuildService stubService = mock(GuildService.class);
        when(stubService.getAllGuildsAsync()).thenReturn(CompletableFuture.completedFuture(List.of()));
        when(plugin.getGuildService()).thenReturn(stubService);
        when(languageManager.getCoreMessage("admin.list.title", "&6=== Guild List ==="))
                .thenReturn("&6=== Guild List ===");
        when(languageManager.getCoreMessage("admin.list.empty", "&cNo guilds available"))
                .thenReturn("&cNo guilds available");

        command = new GuildAdminCommand(plugin);

        Player player = mock(Player.class);
        when(player.hasPermission("guild.admin")).thenReturn(true);
        Command bukkitCommand = mock(Command.class);
        when(bukkitCommand.getName()).thenReturn("guildadmin");

        boolean handled = command.onCommand(player, bukkitCommand, "guildadmin", new String[]{"list"});

        assertTrue(handled);
        verify(player, never()).sendMessage(ColorUtils.colorize(
                "&cYou do not have permission to perform this action!"));
        verify(stubService).getAllGuildsAsync();
    }
}
