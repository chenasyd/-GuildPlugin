package com.guild.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface GUI {
    String getTitle();

    int getSize();

    void setupInventory(Inventory inventory);

    void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType);

    default void onClose(Player player) {
    }

    default void refresh(Player player) {
    }

    default boolean isValid() {
        return true;
    }

    default String getGuiType() {
        return this.getClass().getSimpleName();
    }
}
