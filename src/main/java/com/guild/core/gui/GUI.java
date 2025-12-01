package com.guild.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Interfejs GUI - definiuje podstawowe metody GUI
 */
public interface GUI {

    /**
     * Pobierz tytuł GUI
     */
    String getTitle();

    /**
     * Pobierz rozmiar GUI (musi być wielokrotnością 9)
     */
    int getSize();

    /**
     * Ustaw zawartość GUI
     */
    void setupInventory(Inventory inventory);

    /**
     * Obsłuż zdarzenie kliknięcia GUI
     */
    void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType);

    /**
     * Obsłuż zdarzenie zamknięcia GUI
     */
    default void onClose(Player player) {
        // Domyślna implementacja jest pusta
    }

    /**
     * Odśwież GUI
     */
    default void refresh(Player player) {
        // Domyślna implementacja jest pusta
    }

    /**
     * Sprawdź, czy GUI jest ważne
     */
    default boolean isValid() {
        return true;
    }

    /**
     * Pobierz identyfikator typu GUI
     */
    default String getGuiType() {
        return this.getClass().getSimpleName();
    }
}
