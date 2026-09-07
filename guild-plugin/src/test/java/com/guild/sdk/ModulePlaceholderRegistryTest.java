package com.guild.sdk;

import com.guild.sdk.placeholder.PlaceholderProvider;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModulePlaceholderRegistryTest {

    private ModulePlaceholderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ModulePlaceholderRegistry();
    }

    @Test
    void registerPlaceholderProvider_andClearByModule() {
        PlaceholderProvider provider = new PlaceholderProvider() {
            @Override
            public String getIdentifier() {
                return "test_ph";
            }

            @Override
            public String onRequest(Player player, String params) {
                return "ok";
            }
        };
        registry.registerPlaceholderProvider("mod-y", provider);

        assertTrue(registry.getPlaceholderProviders().containsKey("test_ph"));

        registry.clearModuleRegistrations("mod-y");

        assertFalse(registry.getPlaceholderProviders().containsKey("test_ph"));
    }

    @Test
    void unregisterPlaceholderProvider_removesEntry() {
        PlaceholderProvider provider = new PlaceholderProvider() {
            @Override
            public String getIdentifier() {
                return "id2";
            }

            @Override
            public String onRequest(Player player, String params) {
                return "";
            }
        };
        registry.registerPlaceholderProvider(provider);
        registry.unregisterPlaceholderProvider("id2");

        assertNull(registry.getPlaceholderProviders().get("id2"));
    }
}
