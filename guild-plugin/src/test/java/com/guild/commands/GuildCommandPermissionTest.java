package com.guild.commands;

import com.guild.GuildPlugin;
import com.guild.core.ServiceContainer;
import com.guild.core.language.LanguageManager;
import com.guild.core.module.ModuleManager;
import com.guild.core.permissions.GuildMembershipRules;
import com.guild.core.permissions.PermissionManager;
import com.guild.core.utils.ColorUtils;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.command.ModuleCommandHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 主公会命令权限门控单测（Mockito，不依赖 MockBukkit）。
 */
class GuildCommandPermissionTest {

    private GuildPlugin plugin;
    private LanguageManager languageManager;
    private PermissionManager permissionManager;
    private GuildMembershipRules membershipRules;
    private GuildPluginAPI api;
    private GuildCommand command;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        languageManager = mock(LanguageManager.class);
        permissionManager = mock(PermissionManager.class);
        membershipRules = mock(GuildMembershipRules.class);
        ServiceContainer serviceContainer = mock(ServiceContainer.class);
        ModuleManager moduleManager = mock(ModuleManager.class);
        api = mock(GuildPluginAPI.class);

        when(plugin.getLanguageManager()).thenReturn(languageManager);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("GuildCommandPermissionTest"));
        when(plugin.getFileLogger()).thenReturn(null);
        when(plugin.getPermissionManager()).thenReturn(permissionManager);
        when(plugin.getMembershipRules()).thenReturn(membershipRules);
        when(plugin.getServiceContainer()).thenReturn(serviceContainer);
        when(serviceContainer.get(ModuleManager.class)).thenReturn(moduleManager);
        when(moduleManager.getSharedApi()).thenReturn(api);

        when(languageManager.getCoreMessage("general.player-only",
                "&cThis command can only be executed by a player!"))
                .thenReturn("&cThis command can only be executed by a player!");
        stubNoPermissionMessage();
        when(languageManager.getCoreMessage(eq("general.unknown-command"), any()))
                .thenReturn("&cUnknown command! Use /guild help for help.");

        command = new GuildCommand(plugin);
    }

    private void stubNoPermissionMessage() {
        when(languageManager.getCoreMessage("general.no-permission",
                "&cYou do not have permission to perform this action!"))
                .thenReturn("&cYou do not have permission to perform this action!");
        when(languageManager.getCoreMessage(any(Player.class), eq("general.no-permission"),
                eq("&cYou do not have permission to perform this action!")))
                .thenReturn("&cYou do not have permission to perform this action!");
    }

    @Test
    void onCommand_rejectsNonPlayerSender() {
        CommandSender console = mock(CommandSender.class);
        Command bukkitCommand = mock(Command.class);

        boolean handled = command.onCommand(console, bukkitCommand, "guild", new String[]{"help"});

        assertTrue(handled);
        verify(console).sendMessage(ColorUtils.colorize(
                "&cThis command can only be executed by a player!"));
    }

    @Test
    void onCommand_deniesCreateWithoutGuildCreatePermission() {
        Player player = mock(Player.class);
        when(permissionManager.hasPermission(player, "guild.create")).thenReturn(false);
        Command bukkitCommand = mock(Command.class);

        boolean handled = command.onCommand(player, bukkitCommand, "guild", new String[]{"create", "MyGuild"});

        assertTrue(handled);
        verify(player).sendMessage(ColorUtils.colorize(
                "&cYou do not have permission to perform this action!"));
    }

    @Test
    void onCommand_deniesKickWithoutMembershipKickPermission() {
        Player player = mock(Player.class);
        when(membershipRules.canKick(player)).thenReturn(false);
        Command bukkitCommand = mock(Command.class);

        boolean handled = command.onCommand(player, bukkitCommand, "guild", new String[]{"kick", "Steve"});

        assertTrue(handled);
        verify(player).sendMessage(ColorUtils.colorize(
                "&cYou do not have permission to perform this action!"));
    }

    @Test
    void onCommand_deniesModuleSubcommandWithoutPermission() {
        Player player = mock(Player.class);
        when(api.hasSubCommand("guild", "quest")).thenReturn(true);
        when(api.getSubCommandPermission("guild", "quest")).thenReturn("guild.quest");
        when(permissionManager.hasPermission(player, "guild.quest")).thenReturn(false);
        Command bukkitCommand = mock(Command.class);

        boolean handled = command.onCommand(player, bukkitCommand, "guild", new String[]{"quest", "list"});

        assertTrue(handled);
        verify(player).sendMessage(ColorUtils.colorize(
                "&cYou do not have permission to perform this action!"));
        verify(api, never()).getSubCommandHandler(eq("guild"), eq("quest"));
    }

    @Test
    void onCommand_routesModuleSubcommandWhenPermitted() {
        Player player = mock(Player.class);
        ModuleCommandHandler moduleHandler = mock(ModuleCommandHandler.class);
        when(api.hasSubCommand("guild", "quest")).thenReturn(true);
        when(api.getSubCommandPermission("guild", "quest")).thenReturn("guild.quest");
        when(permissionManager.hasPermission(player, "guild.quest")).thenReturn(true);
        when(api.getSubCommandHandler("guild", "quest")).thenReturn(moduleHandler);
        Command bukkitCommand = mock(Command.class);

        boolean handled = command.onCommand(player, bukkitCommand, "guild", new String[]{"quest", "list"});

        assertTrue(handled);
        verify(moduleHandler).handle(player, new String[]{"list"});
        verify(player, never()).sendMessage(ColorUtils.colorize(
                "&cYou do not have permission to perform this action!"));
    }
}
