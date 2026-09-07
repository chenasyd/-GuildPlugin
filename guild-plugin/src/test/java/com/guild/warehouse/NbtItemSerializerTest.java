package com.guild.warehouse;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NbtItemSerializerTest {

    @Test
    void itemToSnbt_nullOrAir_returnsNull() {
        assertNull(NbtItemSerializer.itemToSnbt(null));
        assertNull(NbtItemSerializer.itemToSnbt(new ItemStack(Material.AIR)));
    }

    @Test
    void itemFromSnbt_blank_returnsNull() {
        assertNull(NbtItemSerializer.itemFromSnbt(null));
        assertNull(NbtItemSerializer.itemFromSnbt(""));
        assertNull(NbtItemSerializer.itemFromSnbt("   "));
    }
}
