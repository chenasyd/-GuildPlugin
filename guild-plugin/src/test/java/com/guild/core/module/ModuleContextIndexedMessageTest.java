package com.guild.core.module;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 ModuleContext 占位符约定：args[0] 为 fallback，args[1..] 替换 {0}、{1}…
 */
class ModuleContextIndexedMessageTest {

    @Test
    void passesFallbackSeparatelyFromReplaceArgs() {
        GuildPlugin plugin = mock(GuildPlugin.class);
        LanguageManager lm = mock(LanguageManager.class);
        when(plugin.getLanguageManager()).thenReturn(lm);

        ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        when(descriptor.getId()).thenReturn("guild-territory");
        when(descriptor.getName()).thenReturn("territory");

        ModuleContext context = new ModuleContext(plugin, descriptor, null);
        when(lm.getModuleIndexedMessage(
                eq("module.territory.pos1"),
                eq("&a[领地] Pos1: &f{0},{1},{2}"),
                eq("361"), eq("63"), eq("361")))
                .thenReturn("&a[领地] Pos1: &f361,63,361");

        String result = context.getMessage(
                "module.territory.pos1",
                "&a[领地] Pos1: &f{0},{1},{2}",
                361, 63, 361);

        assertEquals("§a[领地] Pos1: §f361,63,361", result);
        verify(lm).getModuleIndexedMessage(
                eq("module.territory.pos1"),
                eq("&a[领地] Pos1: &f{0},{1},{2}"),
                eq("361"), eq("63"), eq("361"));
    }

    @Test
    void sendMessageUsesPlayerLocalizedFormatting() {
        GuildPlugin plugin = mock(GuildPlugin.class);
        LanguageManager lm = mock(LanguageManager.class);
        when(plugin.getLanguageManager()).thenReturn(lm);

        ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        when(descriptor.getId()).thenReturn("guild-territory");
        when(descriptor.getName()).thenReturn("territory");

        ModuleContext context = new ModuleContext(plugin, descriptor, null);
        Player player = mock(Player.class);

        when(lm.getModuleIndexedMessage(
                eq(player),
                eq("module.territory.pos1"),
                eq("&a[领地] Pos1: &f{0},{1},{2}"),
                eq("361"), eq("63"), eq("361")))
                .thenReturn("&a[领地] Pos1: &f361,63,361");

        context.sendMessage(player, "module.territory.pos1",
                "&a[领地] Pos1: &f{0},{1},{2}",
                361, 63, 361);

        verify(lm).getModuleIndexedMessage(
                eq(player),
                eq("module.territory.pos1"),
                eq("&a[领地] Pos1: &f{0},{1},{2}"),
                eq("361"), eq("63"), eq("361"));
        verify(player).sendMessage("§a[领地] Pos1: §f361,63,361");
    }
}
