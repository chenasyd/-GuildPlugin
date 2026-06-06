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

    /**
     * 原地刷新 GUI 内容（不关闭/重开，保持鼠标位置）。
     * 默认直接调用 setupInventory(inventory) 刷新所有物品。
     */
    default void refreshInventory(Inventory inventory) {
        setupInventory(inventory);
    }

    default boolean isValid() {
        return true;
    }

    default String getGuiType() {
        return this.getClass().getSimpleName();
    }
}
