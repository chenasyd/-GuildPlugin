package com.guild.warehouse;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

/**
 * Saves warehouse on close, blocks invalid slot use, clears session on quit.
 */
public class WarehouseListener implements Listener {

    private final GuildWarehouseService warehouseService;

    public WarehouseListener(GuildWarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    private WarehouseChestHolder asHolder(Inventory inventory) {
        if (inventory == null || !(inventory.getHolder() instanceof WarehouseChestHolder holder)) {
            return null;
        }
        return holder;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        WarehouseChestHolder holder = asHolder(top);
        if (holder == null) {
            return;
        }

        int capacity = holder.getSlotCapacity();
        Inventory clicked = event.getClickedInventory();

        // Shift-click from player inv into warehouse: Bukkit fills top first — OK within size
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && clicked != null && clicked.equals(event.getView().getBottomInventory())) {
            // top size == capacity, nothing extra to block
            return;
        }

        if (clicked != null && clicked.equals(top)) {
            if (event.getSlot() < 0 || event.getSlot() >= capacity) {
                event.setCancelled(true);
            }
        }

        // Number-key / hotbar swap into out-of-range top slots
        if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize()
                && event.getRawSlot() >= capacity) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        WarehouseChestHolder holder = asHolder(top);
        if (holder == null) {
            return;
        }
        int capacity = holder.getSlotCapacity();
        for (int raw : event.getRawSlots()) {
            if (raw < top.getSize() && raw >= capacity) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        WarehouseChestHolder holder = asHolder(event.getInventory());
        if (holder == null) {
            return;
        }
        warehouseService.handleClose(player, holder, event.getInventory());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // InventoryCloseEvent normally fires on quit; this is a safety net for session locks
        warehouseService.releaseSessionByPlayer(event.getPlayer().getUniqueId());
    }
}
