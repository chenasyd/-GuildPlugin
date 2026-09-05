package com.guild.gui.base;

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
}
