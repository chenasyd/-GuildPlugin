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

    /**
     * Called when a Bedrock Edition player opens this GUI.
     * Return true if a native Cumulus form was sent (skips Java Inventory path).
     * Default returns false (falls back to Geyser Inventory translation).
     *
     * @param player the Bedrock player
     * @return true if a form was sent
     */
    default boolean openBedrockForm(Player player) {
        return false;
    }
}
