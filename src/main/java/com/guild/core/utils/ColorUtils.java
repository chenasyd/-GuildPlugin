package com.guild.core.utils;

import org.bukkit.ChatColor;

/**
 * Klasa narzędziowa do obsługi kolorów
 */
public class ColorUtils {

    /**
     * Konwertuj symbole kolorów
     * @param message Oryginalna wiadomość
     * @return Skonwertowana wiadomość
     */
    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Konwertuj symbole kolorów i zastąp placeholdery
     * @param message Oryginalna wiadomość
     * @param placeholders Mapa placeholderów
     * @return Skonwertowana wiadomość
     */
    public static String colorize(String message, String... placeholders) {
        if (message == null) {
            return "";
        }

        String result = message;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                result = result.replace(placeholder, value != null ? value : "");
            }
        }

        return colorize(result);
    }

    /**
     * Usuń symbole kolorów
     * @param message Oryginalna wiadomość
     * @return Wiadomość po usunięciu symboli kolorów
     */
    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(colorize(message));
    }

    /**
     * Izoluj kolor zastępowanej treści podczas zastępowania placeholderów, aby uniknąć wpływu na kolejny tekst.
     * Sposób: Wstaw content + &r + activeColor w miejscu placeholdera (activeColor to ostatni kod koloru przed placeholderem).
     * Przekazany szablon używa kodów kolorów &; zwrócony wynik ma już skonwertowane kolory.
     */
    public static String replaceWithColorIsolation(String template, String placeholder, String content) {
        if (template == null) {
            return "";
        }
        if (placeholder == null || placeholder.isEmpty()) {
            return colorize(template);
        }
        int idx = template.indexOf(placeholder);
        if (idx < 0) {
            return colorize(template);
        }
        String prefix = template.substring(0, idx);
        String suffix = template.substring(idx + placeholder.length());

        // Oblicz ważny kod koloru przed placeholderem (ostatnie wystąpienie kodu koloru &0-&f lub &a-&f, ignorując kody stylu)
        String activeColor = extractLastColorCode(prefix);
        String injected = (content != null ? content : "") + "&r" + (activeColor != null ? activeColor : "");
        String result = prefix + injected + suffix;
        return colorize(result);
    }

    private static String extractLastColorCode(String text) {
        if (text == null || text.isEmpty()) return null;
        String last = null;
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            char n = text.charAt(i + 1);
            if (c == '&') {
                char lower = Character.toLowerCase(n);
                if (lower == 'r') {
                    last = null; // Reset
                } else if ((lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f')) {
                    last = "&" + lower; // Zapisz ostatni kod koloru
                }
            }
        }
        return last;
    }
}
