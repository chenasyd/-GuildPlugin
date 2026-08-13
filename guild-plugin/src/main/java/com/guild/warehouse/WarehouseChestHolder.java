package com.guild.warehouse;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Marks a chest inventory as one page of a guild warehouse session.
 * DB slot indices are absolute: page local slot + {@link #getSlotOffset()}.
 */
public class WarehouseChestHolder implements InventoryHolder {

    private final int guildId;
    private final int page;
    private final int pageSlotCount;
    private final int totalSlots;
    private Inventory inventory;
    /** Snapshot of page contents when opened (for optional access-log diffs). */
    private ItemStack[] openSnapshot = new ItemStack[0];

    public WarehouseChestHolder(int guildId, int page, int pageSlotCount, int totalSlots) {
        this.guildId = guildId;
        this.page = page;
        this.pageSlotCount = pageSlotCount;
        this.totalSlots = totalSlots;
    }

    public int getGuildId() {
        return guildId;
    }

    /** 1-based page number. */
    public int getPage() {
        return page;
    }

    public int getPageSlotCount() {
        return pageSlotCount;
    }

    /** Absolute start index in the guild warehouse (0-based). */
    public int getSlotOffset() {
        return WarehouseSettings.getPageOffset(page);
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    /** Local inventory capacity for this page (alias for listeners). */
    public int getSlotCapacity() {
        return pageSlotCount;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void captureOpenSnapshot(ItemStack[] contents) {
        if (contents == null) {
            this.openSnapshot = new ItemStack[0];
            return;
        }
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            copy[i] = stack == null ? null : stack.clone();
        }
        this.openSnapshot = copy;
    }

    ItemStack[] getOpenSnapshot() {
        return openSnapshot;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
