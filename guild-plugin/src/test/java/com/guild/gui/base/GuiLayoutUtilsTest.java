package com.guild.gui.base;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiLayoutUtilsTest {

    @Test
    void slotForPageIndex_mapsFirstRow() {
        assertEquals(10, GuiLayoutUtils.slotForPageIndex(0));
        assertEquals(16, GuiLayoutUtils.slotForPageIndex(6));
    }

    @Test
    void slotForPageIndex_mapsSecondRow() {
        assertEquals(19, GuiLayoutUtils.slotForPageIndex(7));
        assertEquals(25, GuiLayoutUtils.slotForPageIndex(13));
    }

    @Test
    void slotForPageIndex_mapsLastCell() {
        assertEquals(43, GuiLayoutUtils.slotForPageIndex(27));
    }

    @Test
    void listIndexFromSlot_roundTripsOnFirstPage() {
        for (int pageIndex = 0; pageIndex < GuiLayoutUtils.ITEMS_PER_PAGE; pageIndex++) {
            int slot = GuiLayoutUtils.slotForPageIndex(pageIndex);
            assertEquals(pageIndex, GuiLayoutUtils.listIndexFromSlot(slot, 0, GuiLayoutUtils.ITEMS_PER_PAGE));
        }
    }

    @Test
    void listIndexFromSlot_appliesPageOffset() {
        int slot = GuiLayoutUtils.slotForPageIndex(0);
        assertEquals(GuiLayoutUtils.ITEMS_PER_PAGE,
                GuiLayoutUtils.listIndexFromSlot(slot, 1, GuiLayoutUtils.ITEMS_PER_PAGE));
    }

    @Test
    void listIndexFromSlot_rejectsBorderSlots() {
        assertEquals(-1, GuiLayoutUtils.listIndexFromSlot(0, 0, GuiLayoutUtils.ITEMS_PER_PAGE));
        assertEquals(-1, GuiLayoutUtils.listIndexFromSlot(9, 0, GuiLayoutUtils.ITEMS_PER_PAGE));
        assertEquals(-1, GuiLayoutUtils.listIndexFromSlot(49, 0, GuiLayoutUtils.ITEMS_PER_PAGE));
    }

    @Test
    void maxPageIndex_handlesEmptyAndPartialPages() {
        assertEquals(0, GuiLayoutUtils.maxPageIndex(0, GuiLayoutUtils.ITEMS_PER_PAGE));
        assertEquals(0, GuiLayoutUtils.maxPageIndex(1, GuiLayoutUtils.ITEMS_PER_PAGE));
        assertEquals(0, GuiLayoutUtils.maxPageIndex(28, GuiLayoutUtils.ITEMS_PER_PAGE));
        assertEquals(1, GuiLayoutUtils.maxPageIndex(29, GuiLayoutUtils.ITEMS_PER_PAGE));
    }

    @Test
    void isPageContentSlot_matchesContentAreaOnly() {
        assertTrue(GuiLayoutUtils.isPageContentSlot(10));
        assertTrue(GuiLayoutUtils.isPageContentSlot(43));
        assertFalse(GuiLayoutUtils.isPageContentSlot(9));
        assertFalse(GuiLayoutUtils.isPageContentSlot(45));
    }

    @Test
    void adminGridLayout_roundTripsOnFirstPage() {
        for (int pageIndex = 0; pageIndex < GuiLayoutUtils.ADMIN_GRID_ITEMS_PER_PAGE; pageIndex++) {
            int slot = GuiLayoutUtils.slotForGridPageIndex(pageIndex,
                    GuiLayoutUtils.ADMIN_GRID_COLS, GuiLayoutUtils.ADMIN_GRID_START_COL);
            assertEquals(pageIndex, GuiLayoutUtils.listIndexFromGridSlot(slot, 0,
                    GuiLayoutUtils.ADMIN_GRID_ITEMS_PER_PAGE,
                    GuiLayoutUtils.ADMIN_GRID_ROWS, GuiLayoutUtils.ADMIN_GRID_COLS,
                    GuiLayoutUtils.ADMIN_GRID_START_COL));
        }
    }

    @Test
    void adminGridLayout_rejectsOutsideColumns() {
        assertEquals(-1, GuiLayoutUtils.listIndexFromGridSlot(13, 0,
                GuiLayoutUtils.ADMIN_GRID_ITEMS_PER_PAGE,
                GuiLayoutUtils.ADMIN_GRID_ROWS, GuiLayoutUtils.ADMIN_GRID_COLS,
                GuiLayoutUtils.ADMIN_GRID_START_COL));
    }

    @Test
    void adminGridLayout_appliesPageOffset() {
        int slot = GuiLayoutUtils.slotForGridPageIndex(0,
                GuiLayoutUtils.ADMIN_GRID_COLS, GuiLayoutUtils.ADMIN_GRID_START_COL);
        assertEquals(GuiLayoutUtils.ADMIN_GRID_ITEMS_PER_PAGE,
                GuiLayoutUtils.listIndexFromGridSlot(slot, 1,
                        GuiLayoutUtils.ADMIN_GRID_ITEMS_PER_PAGE,
                        GuiLayoutUtils.ADMIN_GRID_ROWS, GuiLayoutUtils.ADMIN_GRID_COLS,
                        GuiLayoutUtils.ADMIN_GRID_START_COL));
    }

    @Test
    void listIndexFromGridSlot_rejectsRowOutsideGrid() {
        assertEquals(-1, GuiLayoutUtils.listIndexFromGridSlot(46, 0,
                GuiLayoutUtils.ADMIN_GRID_ITEMS_PER_PAGE,
                GuiLayoutUtils.ADMIN_GRID_ROWS, GuiLayoutUtils.ADMIN_GRID_COLS,
                GuiLayoutUtils.ADMIN_GRID_START_COL));
    }
}
