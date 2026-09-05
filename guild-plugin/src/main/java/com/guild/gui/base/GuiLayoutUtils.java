package com.guild.gui.base;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** GUI 布局与物品创建工具（54/27 槽通用）。 */
public final class GuiLayoutUtils {

    public static final int PAGE_CONTENT_ROWS = 4;
    public static final int PAGE_CONTENT_COLS = 7;
    public static final int ITEMS_PER_PAGE = PAGE_CONTENT_ROWS * PAGE_CONTENT_COLS;
    public static final int BEDROCK_ITEMS_PER_PAGE = 10;

    private GuiLayoutUtils() {
    }

    public static ItemStack createItem(Material material, String name, String... lore) {
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

    /** 54 槽 GUI 边框（内容区 10-43，底栏 45-53 保留）。 */
    public static void fillBorder54(Inventory inventory) {
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

    /** 27 槽 GUI 边框（中间行 9-17 保留）。 */
    public static void fillBorder27(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 18, border);
        }
        for (int i = 9; i < 18; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    /** 页内索引 (0..ITEMS_PER_PAGE-1) → inventory 槽位 (10-16, 19-25, 28-34, 37-43)。 */
    public static int slotForPageIndex(int pageIndex) {
        int row = pageIndex / PAGE_CONTENT_COLS;
        int col = pageIndex % PAGE_CONTENT_COLS;
        return (row + 1) * 9 + col + 1;
    }

    /** inventory 槽位 → 全局列表索引；无效槽位返回 -1。 */
    public static int listIndexFromSlot(int slot, int currentPage, int itemsPerPage) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > PAGE_CONTENT_ROWS || col < 1 || col > PAGE_CONTENT_COLS) {
            return -1;
        }
        return currentPage * itemsPerPage + (row - 1) * PAGE_CONTENT_COLS + (col - 1);
    }

    public static int maxPageIndex(int listSize, int itemsPerPage) {
        if (listSize <= 0) {
            return 0;
        }
        return (listSize - 1) / itemsPerPage;
    }

    public static boolean isPageContentSlot(int slot) {
        return listIndexFromSlot(slot, 0, ITEMS_PER_PAGE) >= 0
                && listIndexFromSlot(slot, 0, ITEMS_PER_PAGE) < ITEMS_PER_PAGE;
    }
}
