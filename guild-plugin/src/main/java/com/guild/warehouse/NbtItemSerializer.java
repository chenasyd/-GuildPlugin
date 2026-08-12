package com.guild.warehouse;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.inventory.ItemStack;

/**
 * Isolates NBTAPI calls so warehouse code can compile against the provided dependency
 * and only invoke serialization when the plugin is present at runtime.
 */
public final class NbtItemSerializer {

    private NbtItemSerializer() {}

    public static String itemToSnbt(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ReadWriteNBT nbt = NBT.itemStackToNBT(item);
        return nbt == null ? null : nbt.toString();
    }

    public static ItemStack itemFromSnbt(String snbt) {
        if (snbt == null || snbt.isBlank()) {
            return null;
        }
        ReadWriteNBT nbt = NBT.parseNBT(snbt);
        return NBT.itemStackFromNBT(nbt);
    }
}
