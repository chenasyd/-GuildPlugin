package com.guild.warehouse;

import com.guild.core.database.DatabaseManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarehouseItemStorageTest {

    private HikariDataSource dataSource;
    private WarehouseItemStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:wh_items_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE guild_warehouse_items (
                        guild_id INT NOT NULL,
                        slot INT NOT NULL,
                        nbt MEDIUMTEXT NOT NULL,
                        PRIMARY KEY (guild_id, slot)
                    )
                    """);
        }

        DatabaseManager databaseManager = org.mockito.Mockito.mock(DatabaseManager.class);
        org.mockito.Mockito.when(databaseManager.getConnection()).thenAnswer(inv -> dataSource.getConnection());
        storage = new WarehouseItemStorage(databaseManager, Logger.getLogger("WarehouseItemStorageTest"));
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void savePage_emptyInventory_clearsStoredSlots() throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_warehouse_items (guild_id, slot, nbt) VALUES (?, ?, ?)")) {
            ps.setInt(1, 7);
            ps.setInt(2, 0);
            ps.setString(3, "placeholder");
            ps.executeUpdate();
        }

        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[54]);

        assertTrue(storage.savePage(7, inventory, 0, 54).join());

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM guild_warehouse_items WHERE guild_id = ?")) {
            ps.setInt(1, 7);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    @Test
    void savePage_airSlots_onlyClearsPageRange() throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_warehouse_items (guild_id, slot, nbt) VALUES (?, ?, ?)")) {
            ps.setInt(1, 3);
            ps.setInt(2, 54);
            ps.setString(3, "outside-page");
            ps.executeUpdate();
        }

        ItemStack[] contents = new ItemStack[54];
        contents[0] = new ItemStack(Material.AIR);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(contents);

        assertTrue(storage.savePage(3, inventory, 0, 54).join());

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT slot FROM guild_warehouse_items WHERE guild_id = ?")) {
            ps.setInt(1, 3);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(54, rs.getInt("slot"));
            }
        }
    }

    @Test
    void loadItemsInRange_skipsCorruptNbt() throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_warehouse_items (guild_id, slot, nbt) VALUES (?, ?, ?)")) {
            ps.setInt(1, 1);
            ps.setInt(2, 0);
            ps.setString(3, "{invalid-nbt");
            ps.executeUpdate();
        }

        Map<Integer, ItemStack> loaded = storage.loadItemsInRange(1, 0, 9).join();
        assertTrue(loaded.isEmpty());
    }

    @Test
    void loadItemsInRange_emptyWhenNoRows() {
        Map<Integer, ItemStack> loaded = storage.loadItemsInRange(99, 0, 54).join();
        assertTrue(loaded.isEmpty());
    }
}
