package com.guild.core.gui.imago;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiImageLayoutApplierTest {

    @Test
    void isFillerItem_recognizesGlassPanes() {
        assertTrue(GuiImageLayoutApplier.isFillerItem(Material.BLACK_STAINED_GLASS_PANE));
        assertTrue(GuiImageLayoutApplier.isFillerItem(Material.GRAY_STAINED_GLASS_PANE));
        assertTrue(GuiImageLayoutApplier.isFillerItem(Material.GLASS_PANE));
        assertTrue(GuiImageLayoutApplier.isFillerItem(Material.LIME_STAINED_GLASS_PANE));
    }

    @Test
    void isFillerItem_rejectsFunctionalItems() {
        assertFalse(GuiImageLayoutApplier.isFillerItem(Material.EMERALD_BLOCK));
        assertFalse(GuiImageLayoutApplier.isFillerItem(Material.PLAYER_HEAD));
        assertFalse(GuiImageLayoutApplier.isFillerItem(Material.AIR));
    }
}
