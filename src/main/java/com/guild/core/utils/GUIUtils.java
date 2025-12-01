package com.guild.core.utils;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Narzędzia GUI - ujednolicone przetwarzanie zmiennych i kodów kolorów w GUI
 */
public class GUIUtils {

    /**
     * Przetwórz zmienne w konfiguracji GUI
     * @param text Oryginalny tekst
     * @param guild Obiekt gildii
     * @param player Obiekt gracza
     * @return Tekst po zamianie
     */
    public static String processGUIVariables(String text, Guild guild, Player player) {
        if (text == null) {
            return "";
        }

        // Użyj PlaceholderUtils do obsługi podstawowych zmiennych
        String result = PlaceholderUtils.replaceGuildPlaceholders(text, guild, player);

        // Upewnij się, że kody kolorów są poprawnie zastosowane
        return ColorUtils.colorize(result);
    }

    /**
     * Asynchronicznie przetwórz zmienne w konfiguracji GUI (w tym dane dynamiczne)
     * @param text Oryginalny tekst
     * @param guild Obiekt gildii
     * @param player Obiekt gracza
     * @param plugin Instancja pluginu
     * @return CompletableFuture z tekstem po zamianie
     */
    public static CompletableFuture<String> processGUIVariablesAsync(String text, Guild guild, Player player, GuildPlugin plugin) {
        if (text == null) {
            return CompletableFuture.completedFuture("");
        }

        // Najpierw przetwórz zmienne statyczne
        String result = processGUIVariables(text, guild, player);

        // Asynchronicznie pobierz dane dynamiczne
        return plugin.getGuildService().getGuildMemberCountAsync(guild.getId()).thenApply(memberCount -> {
            return result
                .replace("{member_count}", String.valueOf(memberCount))
                .replace("{online_member_count}", String.valueOf(memberCount)); // Tymczasowo użyj całkowitej liczby członków
        });
    }

    /**
     * Przetwórz listę opisów przedmiotów w konfiguracji GUI
     * @param loreList Oryginalna lista opisów
     * @param guild Obiekt gildii
     * @param player Obiekt gracza
     * @return Przetworzona lista opisów
     */
    public static List<String> processGUILore(List<String> loreList, Guild guild, Player player) {
        List<String> processedLore = new ArrayList<>();

        if (loreList != null) {
            for (String line : loreList) {
                processedLore.add(processGUIVariables(line, guild, player));
            }
        }

        return processedLore;
    }

    /**
     * Asynchronicznie przetwórz listę opisów przedmiotów w konfiguracji GUI (w tym dane dynamiczne)
     * @param loreList Oryginalna lista opisów
     * @param guild Obiekt gildii
     * @param player Obiekt gracza
     * @param plugin Instancja pluginu
     * @return CompletableFuture z przetworzoną listą opisów
     */
    public static CompletableFuture<List<String>> processGUILoreAsync(List<String> loreList, Guild guild, Player player, GuildPlugin plugin) {
        if (loreList == null || loreList.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (String line : loreList) {
            futures.add(processGUIVariablesAsync(line, guild, player, plugin));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<String> processedLore = new ArrayList<>();
                for (CompletableFuture<String> future : futures) {
                    try {
                        processedLore.add(future.get());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Błąd podczas przetwarzania opisu GUI: " + e.getMessage());
                        processedLore.add("&cBłąd");
                    }
                }
                return processedLore;
            });
    }

    /**
     * Przetwórz zmienne GUI związane z członkami
     * @param text Oryginalny tekst
     * @param member Obiekt członka
     * @param guild Obiekt gildii
     * @return Tekst po zamianie
     */
    public static String processMemberGUIVariables(String text, GuildMember member, Guild guild) {
        if (text == null) {
            return "";
        }

        return PlaceholderUtils.replaceMemberPlaceholders(text, member, guild);
    }

    /**
     * Sprawdź, czy zmienne zostały poprawnie zastąpione
     * @param text Tekst do sprawdzenia
     * @return Czy zawiera niezastąpione zmienne
     */
    public static boolean hasUnresolvedVariables(String text) {
        if (text == null) {
            return false;
        }

        // Sprawdź czy zawiera niezastąpione placeholdery zmiennych
        return text.contains("{") && text.contains("}");
    }

    /**
     * Pobierz listę niezastąpionych zmiennych
     * @param text Tekst do sprawdzenia
     * @return Lista niezastąpionych zmiennych
     */
    public static List<String> getUnresolvedVariables(String text) {
        List<String> unresolved = new ArrayList<>();

        if (text == null) {
            return unresolved;
        }

        // Proste wykrywanie zmiennych (można rozszerzyć o bardziej złożone wyrażenia regularne)
        String[] parts = text.split("\\{");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int endIndex = part.indexOf("}");
            if (endIndex > 0) {
                String variable = part.substring(0, endIndex);
                unresolved.add("{" + variable + "}");
            }
        }

        return unresolved;
    }
}
