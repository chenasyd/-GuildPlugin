package com.guild.core.utils;

import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.GuildPlugin;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import com.guild.core.time.TimeProvider;
import java.util.concurrent.CompletableFuture;

/**
 * Klasa narzędziowa do przetwarzania placeholderów
 */
public class PlaceholderUtils {

    private static final DateTimeFormatter DATE_FORMATTER = TimeProvider.FULL_FORMATTER;
    private static String cachedLeaderColor;
    private static String cachedOfficerColor;
    private static String cachedMemberColor;
    private static String cachedSeparatorText;
    private static boolean cachedSeparatorEnabled;
    private static boolean cachedSeparatorFollowRoleColor;
    private static String cachedSeparatorDefaultColor;

    /**
     * Zastąp placeholdery związane z gildią
     * @param text Oryginalny tekst
     * @param guild Obiekt gildii
     * @param player Obiekt gracza
     * @return Tekst po zamianie
     */
    public static String replaceGuildPlaceholders(String text, Guild guild, Player player) {
        if (text == null || guild == null) {
            return text;
        }

        String result = text
            // Podstawowe informacje o gildii
            .replace("{guild_name}", guild.getName())
            .replace("{guild_tag}", guild.getTag() != null ? guild.getTag() : "")
            .replace("{guild_description}", guild.getDescription() != null ? guild.getDescription() : "")
            .replace("{guild_id}", String.valueOf(guild.getId()))
            .replace("{guild_created_time}", guild.getCreatedAt().format(DATE_FORMATTER))
            .replace("{guild_created_date}", guild.getCreatedAt().toLocalDate().toString())

            // Informacje o liderze gildii
            .replace("{leader_name}", guild.getLeaderName())
            .replace("{leader_uuid}", guild.getLeaderUuid().toString())

            // Informacje o lokalizacji gildii
            .replace("{guild_home_world}", guild.getHomeWorld() != null ? guild.getHomeWorld() : "")
            .replace("{guild_home_x}", String.valueOf(guild.getHomeX()))
            .replace("{guild_home_y}", String.valueOf(guild.getHomeY()))
            .replace("{guild_home_z}", String.valueOf(guild.getHomeZ()))
            .replace("{guild_home_location}", formatHomeLocation(guild))

            // Informacje o graczu
            .replace("{player_name}", player != null ? player.getName() : "")
            .replace("{player_uuid}", player != null ? player.getUniqueId().toString() : "")
            .replace("{player_display_name}", player != null ? player.getDisplayName() : "")

            // Informacje statyczne
            .replace("{guild_level}", String.valueOf(guild.getLevel()))
            .replace("{guild_balance}", String.valueOf(guild.getBalance()))
            .replace("{guild_max_members}", String.valueOf(guild.getMaxMembers()))
            .replace("{guild_frozen}", guild.isFrozen() ? "Zamrożona" : "Normalna")

            // Zmienne związane z ekonomią - obsługują nazwy zmiennych w konfiguracji GUI
            .replace("{guild_balance_formatted}", formatBalance(guild.getBalance()))
            .replace("{guild_next_level_requirement}", getNextLevelRequirement(guild.getLevel()))
            .replace("{guild_level_progress}", getLevelProgress(guild.getLevel(), guild.getBalance()))
            .replace("{guild_upgrade_cost}", getUpgradeCost(guild.getLevel()))
            .replace("{guild_currency_name}", "Monety")
            .replace("{guild_currency_name_singular}", "Moneta")

            // Zmienne kompatybilności - obsługują stary format
            .replace("{guild_max_exp}", getNextLevelRequirement(guild.getLevel()))
            .replace("{guild_exp_percentage}", getLevelProgress(guild.getLevel(), guild.getBalance()));

        // Przetwórz kody kolorów
        return ColorUtils.colorize(result);
    }

    /**
     * Asynchronicznie zastąp placeholdery związane z gildią (w tym dane dynamiczne)
     * @param text Oryginalny tekst
     * @param guild Obiekt gildii
     * @param player Obiekt gracza
     * @param guildService Usługa gildii
     * @return CompletableFuture z tekstem po zamianie
     */
    public static CompletableFuture<String> replaceGuildPlaceholdersAsync(String text, Guild guild, Player player, com.guild.services.GuildService guildService) {
        if (text == null || guild == null) {
            return CompletableFuture.completedFuture(text);
        }

        // Najpierw zastąp statyczne placeholdery
        String result = replaceGuildPlaceholders(text, guild, player);

        // Asynchronicznie pobierz dane dynamiczne
        return guildService.getGuildMemberCountAsync(guild.getId()).thenApply(memberCount -> {
            try {
                return result
                    .replace("{member_count}", String.valueOf(memberCount))
                    .replace("{online_member_count}", String.valueOf(memberCount)) // Tymczasowo użyj całkowitej liczby członków, można później dodać statystyki online
                    .replace("{guild_max_exp}", getNextLevelRequirement(guild.getLevel()))
                    .replace("{guild_exp_percentage}", getLevelProgress(guild.getLevel(), guild.getBalance()));
            } catch (Exception e) {
                // W przypadku błędu pobierania, użyj wartości domyślnych
                return result
                    .replace("{member_count}", "0")
                    .replace("{online_member_count}", "0")
                    .replace("{guild_max_exp}", getNextLevelRequirement(guild.getLevel()))
                    .replace("{guild_exp_percentage}", getLevelProgress(guild.getLevel(), guild.getBalance()));
            }
        });
    }

    /**
     * Zastąp placeholdery związane z członkami
     * @param text Oryginalny tekst
     * @param member Obiekt członka
     * @param guild Obiekt gildii
     * @return Tekst po zamianie
     */
    public static String replaceMemberPlaceholders(String text, GuildMember member, Guild guild) {
        if (text == null || member == null) {
            return text;
        }

        String result = text
            // Podstawowe informacje o członku
            .replace("{member_name}", member.getPlayerName())
            .replace("{member_uuid}", member.getPlayerUuid().toString())
            .replace("{member_role}", getRoleDisplayName(member.getRole()))
            .replace("{member_role_color}", getRoleColorFromConfig(member.getRole()))
            .replace("{member_join_time}", member.getJoinedAt().format(DATE_FORMATTER))
            .replace("{member_join_date}", member.getJoinedAt().toLocalDate().toString())

            // Informacje o gildii
            .replace("{guild_name}", guild != null ? guild.getName() : "")
            .replace("{guild_tag}", guild != null && guild.getTag() != null ? guild.getTag() : "");

        // Przetwórz kody kolorów
        return ColorUtils.colorize(result);
    }

    /**
     * Zastąp placeholdery związane z aplikacjami
     * @param text Oryginalny tekst
     * @param applicantName Nazwa wnioskodawcy
     * @param guildName Nazwa gildii
     * @param applyTime Czas aplikacji
     * @return Tekst po zamianie
     */
    public static String replaceApplicationPlaceholders(String text, String applicantName, String guildName, java.time.LocalDateTime applyTime) {
        if (text == null) {
            return text;
        }

        String result = text
            .replace("{applicant_name}", applicantName != null ? applicantName : "")
            .replace("{guild_name}", guildName != null ? guildName : "")
            .replace("{apply_time}", applyTime != null ? applyTime.format(DATE_FORMATTER) : "")
            .replace("{apply_date}", applyTime != null ? applyTime.toLocalDate().toString() : "");

        // Przetwórz kody kolorów
        return ColorUtils.colorize(result);
    }

    /**
     * Zastąp ogólne placeholdery
     * @param text Oryginalny tekst
     * @param placeholders Mapa placeholderów
     * @return Tekst po zamianie
     */
    public static String replacePlaceholders(String text, String... placeholders) {
        if (text == null) {
            return text;
        }

        String result = text;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                result = result.replace(placeholder, value != null ? value : "");
            }
        }

        // Przetwórz kody kolorów
        return ColorUtils.colorize(result);
    }

    /**
     * Formatuj lokalizację domu gildii
     */
    private static String formatHomeLocation(Guild guild) {
        if (guild.getHomeWorld() == null) {
            return "Nie ustawiono";
        }
        return String.format("%s %.1f, %.1f, %.1f",
            guild.getHomeWorld(), guild.getHomeX(), guild.getHomeY(), guild.getHomeZ());
    }

    /**
     * Pobierz wyświetlaną nazwę roli
     */
    private static String getRoleDisplayName(GuildMember.Role role) {
        switch (role) {
            case LEADER: return "Lider";
            case OFFICER: return "Oficer";
            case MEMBER: return "Członek";
            default: return "Nieznana";
        }
    }

    /**
     * Pobierz kolor roli z konfiguracji
     */
    private static String getRoleColorFromConfig(GuildMember.Role role) {
        ensureRoleConfigCached();
        switch (role) {
            case LEADER: return cachedLeaderColor;
            case OFFICER: return cachedOfficerColor;
            case MEMBER: return cachedMemberColor;
            default: return "&f";
        }
    }

    /**
     * Publicznie dostępne: Pobierz kolorowy tekst wyświetlania roli
     */
    public static String getColoredRoleDisplay(GuildMember.Role role) {
        String color = getRoleColorFromConfig(role);
        return ColorUtils.colorize(color + getRoleDisplayName(role));
    }

    /**
     * Pobierz kod koloru dla roli
     */
    public static String getRoleColorCode(GuildMember.Role role) {
        return getRoleColorFromConfig(role);
    }

    /**
     * Pobierz separator roli (zwraca w zależności od konfiguracji i posiadania roli)
     */
    public static String getRoleSeparator(GuildMember.Role roleOrNull) {
        ensureRoleConfigCached();
        if (!cachedSeparatorEnabled) {
            return "";
        }
        // Nie pokazuj separatora, jeśli nie dołączył lub nie ma roli
        if (roleOrNull == null) {
            return "";
        }
        String color = cachedSeparatorFollowRoleColor ? getRoleColorFromConfig(roleOrNull) : cachedSeparatorDefaultColor;
        return ColorUtils.colorize(color + cachedSeparatorText);
    }

    private static void ensureRoleConfigCached() {
        if (cachedLeaderColor != null) {
            return;
        }
        GuildPlugin plugin = GuildPlugin.getInstance();
        if (plugin == null || plugin.getConfigManager() == null) {
            // Rozsądne wartości domyślne
            cachedLeaderColor = "&6";
            cachedOfficerColor = "&b";
            cachedMemberColor = "&7";
            cachedSeparatorText = " | ";
            cachedSeparatorEnabled = true;
            cachedSeparatorFollowRoleColor = true;
            cachedSeparatorDefaultColor = "&7";
            return;
        }
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        cachedLeaderColor = cfg.getString("display.role-colors.leader", "&6");
        cachedOfficerColor = cfg.getString("display.role-colors.officer", "&b");
        cachedMemberColor = cfg.getString("display.role-colors.member", "&7");
        cachedSeparatorText = cfg.getString("display.role-separator.text", " | ");
        cachedSeparatorEnabled = cfg.getBoolean("display.role-separator.enabled", true);
        cachedSeparatorFollowRoleColor = cfg.getBoolean("display.role-separator.color-per-role", true);
        cachedSeparatorDefaultColor = cfg.getString("display.role-separator.default-color", "&7");
    }

    /**
     * Formatuj saldo
     */
    private static String formatBalance(double balance) {
        // Spróbuj sformatować za pomocą menedżera ekonomii, jeśli niedostępny, użyj domyślnego formatu
        try {
            com.guild.GuildPlugin plugin = com.guild.GuildPlugin.getInstance();
            if (plugin != null && plugin.getEconomyManager() != null && plugin.getEconomyManager().isVaultAvailable()) {
                return plugin.getEconomyManager().format(balance);
            }
        } catch (Exception e) {
            // Ignoruj błędy, użyj domyślnego formatu
        }
        return String.format("%.2f", balance);
    }

    /**
     * Pobierz wymagania dla następnego poziomu
     */
    private static String getNextLevelRequirement(int currentLevel) {
        if (currentLevel >= 10) {
            return "Osiągnięto maksymalny poziom";
        }

        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }

        return String.format("%.2f", required);
    }

    /**
     * Pobierz postęp poziomu
     */
    private static String getLevelProgress(int currentLevel, double currentBalance) {
        if (currentLevel >= 10) {
            return "100%";
        }

        double required = 0;
        switch (currentLevel) {
            case 1: required = 5000; break;
            case 2: required = 10000; break;
            case 3: required = 20000; break;
            case 4: required = 35000; break;
            case 5: required = 50000; break;
            case 6: required = 75000; break;
            case 7: required = 100000; break;
            case 8: required = 150000; break;
            case 9: required = 200000; break;
        }

        double progress = (currentBalance / required) * 100;
        return String.format("%.1f%%", Math.min(progress, 100));
    }

    /**
     * Pobierz koszt ulepszenia
     */
    private static String getUpgradeCost(int currentLevel) {
        if (currentLevel >= 10) {
            return "0";
        }

        double cost = 0;
        switch (currentLevel) {
            case 1: cost = 5000; break;
            case 2: cost = 10000; break;
            case 3: cost = 20000; break;
            case 4: cost = 35000; break;
            case 5: cost = 50000; break;
            case 6: cost = 75000; break;
            case 7: cost = 100000; break;
            case 8: cost = 150000; break;
            case 9: cost = 200000; break;
        }

        return String.format("%.2f", cost);
    }
}
