package com.guild.core.utils;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Klasa narzędziowa do testowania zmiennych - służy do weryfikacji zamiany zmiennych GUI
 */
public class VariableTestUtils {

    /**
     * Test zamiany zmiennych GUI
     * @param plugin Instancja pluginu
     * @param guild Obiekt gildii
     * @param player Obiekt gracza
     */
    public static void testGUIVariables(GuildPlugin plugin, Guild guild, Player player) {
        player.sendMessage("§6=== Test Zmiennych GUI ===");

        // Testuj podstawowe zmienne
        String[] testTexts = {
            "Nazwa gildii: {guild_name}",
            "Tag gildii: {guild_tag}",
            "Opis gildii: {guild_description}",
            "ID gildii: {guild_id}",
            "Lider: {leader_name}",
            "Poziom gildii: {guild_level}",
            "Fundusze gildii: {guild_balance_formatted}",
            "Maks. członków: {guild_max_members}",
            "Status gildii: {guild_frozen}",
            "Data utworzenia: {guild_created_date}",
            "Liczba członków: {member_count}/{guild_max_members}",
            "Wymagania ulepszenia: {guild_next_level_requirement}",
            "Postęp ulepszania: {guild_level_progress}"
        };

        for (String testText : testTexts) {
            String processed = GUIUtils.processGUIVariables(testText, guild, player);
            player.sendMessage("§eOryginał: §f" + testText);
            player.sendMessage("§aPrzetworzono: §f" + processed);

            // Sprawdź czy są nierozwiązane zmienne
            if (GUIUtils.hasUnresolvedVariables(processed)) {
                List<String> unresolved = GUIUtils.getUnresolvedVariables(processed);
                player.sendMessage("§cNierozwiązane zmienne: §f" + unresolved);
            }
            player.sendMessage("");
        }

        // Testuj zmienne asynchroniczne
        plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenAccept(memberCount -> {
            String asyncTest = "Liczba członków: {member_count}/{guild_max_members}";
            GUIUtils.processGUIVariablesAsync(asyncTest, guild, player, plugin).thenAccept(processed -> {
                player.sendMessage("§6Test asynchroniczny: §f" + asyncTest);
                player.sendMessage("§aWynik asynchroniczny: §f" + processed);
            });
        });
    }

    /**
     * Test kodów kolorów
     * @param player Obiekt gracza
     */
    public static void testColorCodes(Player player) {
        player.sendMessage("§6=== Test Kodów Kolorów ===");

        String[] colorTests = {
            "&aZielony tekst",
            "&cCzerwony tekst",
            "&eŻółty tekst",
            "&bTurkusowy tekst",
            "&dRóżowy tekst",
            "&fBiały tekst",
            "&7Szary tekst",
            "&8Ciemnoszary tekst",
            "&9Niebieski tekst",
            "&0Czarny tekst",
            "&lPogrubiony tekst",
            "&nPodkreślony tekst",
            "&oPochylony tekst",
            "&kLosowy znak",
            "&rReset formatowania"
        };

        for (String test : colorTests) {
            String processed = ColorUtils.colorize(test);
            player.sendMessage("§eOryginał: §f" + test);
            player.sendMessage("§aPrzetworzono: §f" + processed);
            player.sendMessage("");
        }
    }

    /**
     * Test PlaceholderUtils
     * @param guild Obiekt gildii
     * @param player Obiekt gracza
     */
    public static void testPlaceholderUtils(Guild guild, Player player) {
        player.sendMessage("§6=== Test PlaceholderUtils ===");

        String testText = "Gildia: {guild_name}, Lider: {leader_name}, Poziom: {guild_level}, Fundusze: {guild_balance_formatted}";
        String processed = PlaceholderUtils.replaceGuildPlaceholders(testText, guild, player);

        player.sendMessage("§eOryginał: §f" + testText);
        player.sendMessage("§aPrzetworzono: §f" + processed);

        // Sprawdź czy są nierozwiązane zmienne
        if (GUIUtils.hasUnresolvedVariables(processed)) {
            List<String> unresolved = GUIUtils.getUnresolvedVariables(processed);
            player.sendMessage("§cNierozwiązane zmienne: §f" + unresolved);
        }
    }
}
