package com.guild.core.language;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerLanguageStoreTest {

    private PlayerLanguageStore store;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        store = new PlayerLanguageStore(() -> "en", lang -> "en".equals(lang) || "zh".equals(lang));
        playerId = UUID.randomUUID();
    }

    @Test
    void get_returnsDefaultWhenPlayerNull() {
        assertEquals("en", store.get(null));
    }

    @Test
    void get_returnsStoredLanguage() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);

        store.set(player, "zh");
        assertEquals("zh", store.get(player));
    }

    @Test
    void set_ignoresUnsupportedLanguage() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);

        store.set(player, "fr");
        assertEquals("en", store.get(player));
    }

    @Test
    void setByUuid_worksLikePlayerSet() {
        store.set(playerId, "zh");
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        assertEquals("zh", store.get(player));
    }
}
