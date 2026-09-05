package com.guild.core.utils;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ColorUtilsTest {

    @Test
    void colorize_translatesAmpersandCodes() {
        String colored = ColorUtils.colorize("&aHello");
        assertEquals(ChatColor.GREEN + "Hello", colored);
    }

    @Test
    void colorize_nullReturnsEmptyString() {
        assertEquals("", ColorUtils.colorize(null));
    }

    @Test
    void colorize_replacesPlaceholdersBeforeColorizing() {
        String result = ColorUtils.colorize("&eGuild: {name}", "{name}", "Warriors");
        assertEquals(ChatColor.YELLOW + "Guild: Warriors", result);
    }

    @Test
    void stripColor_removesFormatting() {
        String plain = ColorUtils.stripColor("&c&lAlert");
        assertEquals("Alert", plain);
        assertFalse(plain.contains("§"));
    }
}
