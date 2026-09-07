package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.ServiceContainer;
import com.guild.sdk.api.GuildMemberAPI;
import com.guild.sdk.api.GuildQueryAPI;
import com.guild.sdk.api.ModuleEventAPI;
import com.guild.sdk.api.ModuleExtensionAPI;
import com.guild.sdk.api.ModuleHomeProtectAPI;
import com.guild.sdk.api.ModuleRuntimeAPI;
import com.guild.sdk.api.SdkCurrencyAPI;
import com.guild.sdk.economy.CurrencyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildPluginApiDomainTest {

    private GuildPluginAPI api;

    @BeforeEach
    void setUp() {
        GuildPlugin plugin = mock(GuildPlugin.class);
        ServiceContainer container = mock(ServiceContainer.class);
        when(plugin.getServiceContainer()).thenReturn(container);
        when(container.get(CurrencyManager.class)).thenReturn(mock(CurrencyManager.class));
        api = new GuildPluginAPI(plugin);
    }

    @Test
    void implementsAllDomainInterfaces() {
        assertTrue(GuildQueryAPI.class.isAssignableFrom(GuildPluginAPI.class));
        assertTrue(GuildMemberAPI.class.isAssignableFrom(GuildPluginAPI.class));
        assertTrue(ModuleExtensionAPI.class.isAssignableFrom(GuildPluginAPI.class));
        assertTrue(ModuleEventAPI.class.isAssignableFrom(GuildPluginAPI.class));
        assertTrue(SdkCurrencyAPI.class.isAssignableFrom(GuildPluginAPI.class));
        assertTrue(ModuleRuntimeAPI.class.isAssignableFrom(GuildPluginAPI.class));
        assertTrue(ModuleHomeProtectAPI.class.isAssignableFrom(GuildPluginAPI.class));
    }

    @Test
    void domainAccessors_returnSameInstance() {
        assertSame(api, api.guildQuery());
        assertSame(api, api.guildMember());
        assertSame(api, api.moduleExtensions());
        assertSame(api, api.moduleEvents());
        assertSame(api, api.currency());
        assertSame(api, api.moduleRuntime());
        assertSame(api, api.homeProtect());
    }
}
