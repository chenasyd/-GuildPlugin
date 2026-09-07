package com.guild.warehouse;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseDiffUtilTest {

    @Test
    void summarizeDiff_unchangedWhenIdentical() {
        ItemStack[] snapshot = {new ItemStack(Material.DIRT, 4)};

        assertEquals("unchanged", WarehouseDiffUtil.summarizeDiff(snapshot, snapshot, 1));
    }

    @Test
    void summarizeDiff_reportsPutAndTake() {
        ItemStack[] before = {new ItemStack(Material.DIRT, 4), new ItemStack(Material.STONE, 2)};
        ItemStack[] after = {new ItemStack(Material.DIRT, 1), new ItemStack(Material.OAK_LOG, 3)};

        String summary = WarehouseDiffUtil.summarizeDiff(before, after, 2);

        assertEquals("put=OAK_LOG:3", summary.substring(0, summary.indexOf(';')));
        assertTrue(summary.contains("DIRT:3"));
        assertTrue(summary.contains("STONE:2"));
        assertTrue(summary.startsWith("put=OAK_LOG:3;take="));
    }

    @Test
    void summarizeDiff_respectsCapacityLimit() {
        ItemStack[] before = {new ItemStack(Material.DIRT, 1), new ItemStack(Material.STONE, 1)};
        ItemStack[] after = {new ItemStack(Material.DIRT, 2), new ItemStack(Material.STONE, 2)};

        assertEquals("put=DIRT:1", WarehouseDiffUtil.summarizeDiff(before, after, 1));
    }
}
