package com.guild.module.example.territory;

import com.guild.core.language.LanguageManager;
import com.guild.core.module.ModuleContext;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerritoryTextsTest {

    private ModuleContext context;
    private LanguageManager languageManager;
    private TerritoryTexts texts;
    private Player player;

    @BeforeEach
    void setUp() {
        context = mock(ModuleContext.class);
        languageManager = mock(LanguageManager.class);
        player = mock(Player.class);
        when(context.getLanguageManager()).thenReturn(languageManager);
        texts = new TerritoryTexts(context);
    }

    @Test
    void indexedMessageDoesNotTreatFallbackAsFirstPlaceholder() {
        when(languageManager.getModuleIndexedMessage(
                eq(player),
                eq("module.territory.pos1"),
                eq("&a[领地] Pos1: &f{0},{1},{2}"),
                eq("10"), eq("64"), eq("-20")))
                .thenReturn("&a[领地] Pos1: &f10,64,-20");

        String result = texts.format(player, "module.territory.pos1",
                "&a[领地] Pos1: &f{0},{1},{2}", 10, 64, -20);

        assertEquals("§a[领地] Pos1: §f10,64,-20", result);
        verify(languageManager).getModuleIndexedMessage(
                eq(player),
                eq("module.territory.pos1"),
                eq("&a[领地] Pos1: &f{0},{1},{2}"),
                eq("10"), eq("64"), eq("-20"));
    }

    @Test
    void plainMessageUsesFallbackWhenNoPlaceholders() {
        when(languageManager.getModuleMessage(player, "module.territory.claiming", "&e正在声明领地…"))
                .thenReturn("&e正在声明领地…");

        String result = texts.format(player, "module.territory.claiming", "&e正在声明领地…");

        assertEquals("§e正在声明领地…", result);
    }
}
