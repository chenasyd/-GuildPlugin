package com.guild.commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.guild.GuildPlugin;
import com.guild.core.ServiceContainer;
import com.guild.core.language.LanguageManager;
import com.guild.core.module.ModuleManager;
import com.guild.core.permissions.PermissionManager;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.economy.CurrencyManager;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MockBukkit 薄集成：启动模拟 Bukkit 服务器 + 真实 {@link GuildPluginAPI} 子命令注册与路由。
 * <p>
 * 注：当前 classpath 为 Spigot API，{@code PlayerMock} 依赖 Paper {@code Bukkit.createProfile}，故 Player 仍用 Mockito。
 */
class GuildCommandModuleRouteIntegrationTest {

    private ServerMock server;
    private GuildPlugin plugin;
    private GuildPluginAPI api;
    private GuildCommand guildCommand;
    private RecordingModuleHandler questHandler;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        assertNotNull(server);

        plugin = mock(GuildPlugin.class);
        LanguageManager languageManager = mock(LanguageManager.class);
        PermissionManager permissionManager = mock(PermissionManager.class);
        ServiceContainer serviceContainer = mock(ServiceContainer.class);
        ModuleManager moduleManager = mock(ModuleManager.class);
        CurrencyManager currencyManager = mock(CurrencyManager.class);

        when(plugin.getLanguageManager()).thenReturn(languageManager);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("GuildCommandModuleRouteIntegrationTest"));
        when(plugin.getFileLogger()).thenReturn(null);
        when(plugin.getPermissionManager()).thenReturn(permissionManager);
        when(plugin.getServiceContainer()).thenReturn(serviceContainer);
        when(serviceContainer.get(ModuleManager.class)).thenReturn(moduleManager);
        when(serviceContainer.get(CurrencyManager.class)).thenReturn(currencyManager);

        api = new GuildPluginAPI(plugin);
        when(moduleManager.getSharedApi()).thenReturn(api);

        questHandler = new RecordingModuleHandler();
        api.registerSubCommand("guild", "quest", questHandler, "guild.quest");
        when(permissionManager.hasPermission(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("guild.quest")))
                .thenReturn(true);

        guildCommand = new GuildCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onCommand_routesRegisteredModuleSubcommandThroughRealApi() {
        Player player = mock(Player.class);
        Command bukkitCommand = mock(Command.class);

        boolean handled = guildCommand.onCommand(player, bukkitCommand, "guild", new String[]{"quest", "list"});

        assertTrue(handled);
        assertEquals(1, questHandler.invocations.size());
        assertEquals(player, questHandler.invocations.get(0).sender());
        assertEquals(List.of("list"), List.of(questHandler.invocations.get(0).args()));
    }

    private static final class RecordingModuleHandler implements ModuleCommandHandler {
        private final List<Invocation> invocations = new ArrayList<>();

        @Override
        public void handle(org.bukkit.command.CommandSender sender, String[] args) {
            invocations.add(new Invocation(sender, args == null ? new String[0] : args.clone()));
        }

        private record Invocation(org.bukkit.command.CommandSender sender, String[] args) {
        }
    }
}
