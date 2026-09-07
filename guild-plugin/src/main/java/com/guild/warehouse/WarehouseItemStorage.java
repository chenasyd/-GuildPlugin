package com.guild.warehouse;

import com.guild.core.database.DatabaseManager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

/**
 * Loads and saves guild warehouse item rows (NBT in SQLite/MySQL).
 */
final class WarehouseItemStorage {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    WarehouseItemStorage(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    CompletableFuture<Map<Integer, ItemStack>> loadItemsInRange(int guildId, int fromInclusive, int toExclusive) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, ItemStack> items = new HashMap<>();
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT slot, nbt FROM guild_warehouse_items WHERE guild_id = ? AND slot >= ? AND slot < ?")) {
                stmt.setInt(1, guildId);
                stmt.setInt(2, fromInclusive);
                stmt.setInt(3, toExclusive);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int slot = rs.getInt("slot");
                        try {
                            ItemStack stack = NbtItemSerializer.itemFromSnbt(rs.getString("nbt"));
                            if (stack != null && !stack.getType().isAir()) {
                                items.put(slot, stack);
                            }
                        } catch (Throwable t) {
                            logger.warning("[Warehouse] Skipping corrupt NBT at guild=" + guildId
                                    + " slot=" + slot + ": " + t.getMessage());
                        }
                    }
                }
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
            return items;
        });
    }

    /**
     * Saves one warehouse page. Only replaces absolute slots in [slotOffset, slotOffset + pageCapacity).
     * Callers must invoke from the region/main thread so inventory contents are snapshotted safely.
     */
    CompletableFuture<Boolean> savePage(int guildId, Inventory inventory, int slotOffset, int pageCapacity) {
        ItemStack[] contents = cloneContents(inventory.getContents());
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement del = conn.prepareStatement(
                            "DELETE FROM guild_warehouse_items WHERE guild_id = ? AND slot >= ? AND slot < ?")) {
                        del.setInt(1, guildId);
                        del.setInt(2, slotOffset);
                        del.setInt(3, slotOffset + pageCapacity);
                        del.executeUpdate();
                    }
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO guild_warehouse_items (guild_id, slot, nbt) VALUES (?, ?, ?)")) {
                        int limit = Math.min(pageCapacity, contents.length);
                        for (int local = 0; local < limit; local++) {
                            ItemStack stack = contents[local];
                            if (stack == null || stack.getType().isAir()) {
                                continue;
                            }
                            String snbt = NbtItemSerializer.itemToSnbt(stack);
                            if (snbt == null) {
                                continue;
                            }
                            ins.setInt(1, guildId);
                            ins.setInt(2, slotOffset + local);
                            ins.setString(3, snbt);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                    conn.commit();
                    return true;
                } catch (Exception e) {
                    conn.rollback();
                    logger.severe("[Warehouse] Failed to save items: " + e.getMessage());
                    return false;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                logger.severe("[Warehouse] Save connection error: " + e.getMessage());
                return false;
            }
        });
    }

    private static ItemStack[] cloneContents(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            ItemStack stack = source[i];
            copy[i] = stack == null ? null : stack.clone();
        }
        return copy;
    }
}
