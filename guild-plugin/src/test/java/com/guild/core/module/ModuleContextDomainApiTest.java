package com.guild.core.module;

import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.api.GuildQueryAPI;
import com.guild.sdk.api.ModuleExtensionAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class ModuleContextDomainApiTest {

    @Test
    void domainGetters_returnSharedApiInstance() {
        GuildPluginAPI api = mock(GuildPluginAPI.class);
        ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        ModuleContext context = new ModuleContext(null, descriptor, api);

        assertSame(api, context.getGuildQueryApi());
        assertSame(api, context.getModuleExtensionApi());
        assertSame(api, context.getApi());

        GuildQueryAPI query = context.getGuildQueryApi();
        ModuleExtensionAPI extensions = context.getModuleExtensionApi();
        assertSame(api, query);
        assertSame(api, extensions);
    }
}
