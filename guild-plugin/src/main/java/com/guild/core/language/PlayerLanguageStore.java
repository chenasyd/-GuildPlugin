package com.guild.core.language;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** 玩家个人语言偏好存储。 */
public final class PlayerLanguageStore {

    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private final Supplier<String> defaultLanguageSupplier;
    private final Predicate<String> languageSupported;

    public PlayerLanguageStore(Supplier<String> defaultLanguageSupplier, Predicate<String> languageSupported) {
        this.defaultLanguageSupplier = defaultLanguageSupplier;
        this.languageSupported = languageSupported;
    }

    public String get(Player player) {
        if (player == null) {
            return defaultLanguageSupplier.get();
        }
        return playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguageSupplier.get());
    }

    public void set(Player player, String lang) {
        if (player == null || !languageSupported.test(lang)) {
            return;
        }
        playerLanguages.put(player.getUniqueId(), lang.toLowerCase());
    }

    public void set(UUID uuid, String lang) {
        if (uuid == null || !languageSupported.test(lang)) {
            return;
        }
        playerLanguages.put(uuid, lang.toLowerCase());
    }
}
