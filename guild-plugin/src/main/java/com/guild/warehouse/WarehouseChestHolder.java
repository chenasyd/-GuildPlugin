package com.guild.warehouse;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

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

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
