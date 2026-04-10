package com.guild.sdk.gui;

import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractModuleGUI implements GUI {
    protected static final int DEFAULT_SIZE = 54;
    protected static final int CONTENT_START = 9;
    protected static final int CONTENT_END = 44;
    protected static final int COLUMNS = 7;
    protected static final int CONTENT_ROWS = 4;
    protected static final int PER_PAGE = COLUMNS * CONTENT_ROWS;

    protected Inventory inventory;

    protected void fillBorder(Inventory inv) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
    }

    protected void fillInteriorSlots(Inventory inv) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = CONTENT_START; slot <= CONTENT_END; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue;
            if (inv.getItem(slot) == null) inv.setItem(slot, filler);
        }
    }

    protected int mapToSlot(int linearIndex) {
        if (linearIndex < 0 || linearIndex >= PER_PAGE) return -1;
        int row = linearIndex / COLUMNS;
        int col = linearIndex % COLUMNS;
        int baseRow = CONTENT_START + row * 9;
        return baseRow + col + 1;
    }

    protected ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            if (lore != null && lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(ColorUtils.colorize(line));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack createBackButton(String name, String hint) {
        return createItem(Material.ARROW, name, hint);
    }

    protected void setupPagination(Inventory inv, int currentPage, int totalPages, String prevPageKey, String nextPageKey) {
        if (totalPages <= 1) return;
        if (currentPage > 1) {
            inv.setItem(45, createItem(Material.ARROW, prevPageKey != null ? prevPageKey : "&e&l<"));
        }
        if (currentPage < totalPages) {
            inv.setItem(53, createItem(Material.ARROW, nextPageKey != null ? nextPageKey : "&e&l>"));
        }
    }

    protected int getTotalPages(int totalItems) {
        if (totalItems == 0) return 1;
        return (int) Math.ceil((double) totalItems / PER_PAGE);
    }

    @Override
    public int getSize() {
        return DEFAULT_SIZE;
    }

    @Override
    public void onClose(Player player) {
    }

    @Override
    public void refresh(Player player) {
        if (inventory != null) {
            setupInventory(inventory);
        }
    }
}
