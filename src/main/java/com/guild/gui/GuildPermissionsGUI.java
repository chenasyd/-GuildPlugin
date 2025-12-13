package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * GUI Uprawnień Gildii
 */
public class GuildPermissionsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;

    public GuildPermissionsGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Uprawnienia gildii");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // Wypełnij obramowanie
        fillBorder(inventory);

        // Wyświetl informacje o uprawnieniach
        displayPermissions(inventory);

        // Dodaj przycisk powrotu
        setupButtons(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 49) {
            // Powrót
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
        }
    }

    /**
     * Wypełnij obramowanie
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    /**
     * Wyświetl informacje o uprawnieniach
     */
    private void displayPermissions(Inventory inventory) {
        // Uprawnienia Lidera
        ItemStack leaderPerms = createItem(
            Material.GOLDEN_HELMET,
            ColorUtils.colorize("&6Uprawnienia Lidera"),
            ColorUtils.colorize("&7• Wszystkie uprawnienia"),
            ColorUtils.colorize("&7• Zarządzanie członkami"),
            ColorUtils.colorize("&7• Modyfikacja ustawień"),
            ColorUtils.colorize("&7• Usunięcie gildii")
        );
        inventory.setItem(10, leaderPerms);

        // Uprawnienia Oficera
        ItemStack officerPerms = createItem(
            Material.IRON_HELMET,
            ColorUtils.colorize("&eUprawnienia Oficera"),
            ColorUtils.colorize("&7• Zapraszanie członków"),
            ColorUtils.colorize("&7• Wyrzucanie członków"),
            ColorUtils.colorize("&7• Przetwarzanie aplikacji"),
            ColorUtils.colorize("&7• Ustawianie domu gildii")
        );
        inventory.setItem(12, officerPerms);

        // Uprawnienia Członka
        ItemStack memberPerms = createItem(
            Material.LEATHER_HELMET,
            ColorUtils.colorize("&7Uprawnienia Członka"),
            ColorUtils.colorize("&7• Podgląd informacji o gildii"),
            ColorUtils.colorize("&7• Teleportacja do domu gildii"),
            ColorUtils.colorize("&7• Aplikowanie do innych gildii")
        );
        inventory.setItem(14, memberPerms);

        // Opis uprawnień
        ItemStack info = createItem(
            Material.BOOK,
            ColorUtils.colorize("&eInformacje o systemie"),
            ColorUtils.colorize("&7Uprawnienia są przypisane do ról"),
            ColorUtils.colorize("&7Lider może awansować/degradować"),
            ColorUtils.colorize("&7Oficerowie zarządzają członkami"),
            ColorUtils.colorize("&7Członkowie mają podstawowe prawa")
        );
        inventory.setItem(16, info);

        // Status systemu
        ItemStack currentStatus = createItem(
            Material.SHIELD,
            ColorUtils.colorize("&aStatus systemu uprawnień"),
            ColorUtils.colorize("&7Gildia: &e" + guild.getName()),
            ColorUtils.colorize("&7System: &aAktywny"),
            ColorUtils.colorize("&7Weryfikacja: &aWłączona")
        );
        inventory.setItem(22, currentStatus);
    }

    /**
     * Skonfiguruj przyciski
     */
    private void setupButtons(Inventory inventory) {
        // Przycisk powrotu
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize("&7Powrót"),
            ColorUtils.colorize("&7Powrót do ustawień gildii")
        );
        inventory.setItem(49, back);
    }

    /**
     * Utwórz przedmiot
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }

        return item;
    }
}
