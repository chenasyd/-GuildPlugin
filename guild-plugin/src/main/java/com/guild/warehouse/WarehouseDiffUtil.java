package com.guild.warehouse;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds compact put/take summaries for warehouse access logs (material:amount only, no NBT).
 */
final class WarehouseDiffUtil {

    private WarehouseDiffUtil() {
    }

    static String summarizeDiff(ItemStack[] before, ItemStack[] after, int capacity) {
        Map<Material, Integer> delta = new HashMap<>();
        int limit = Math.min(capacity, Math.max(
                before != null ? before.length : 0,
                after != null ? after.length : 0));
        for (int i = 0; i < limit; i++) {
            ItemStack b = before != null && i < before.length ? before[i] : null;
            ItemStack a = after != null && i < after.length ? after[i] : null;
            applyStackDelta(delta, b, -1);
            applyStackDelta(delta, a, +1);
        }
        if (delta.isEmpty()) {
            return "unchanged";
        }
        StringBuilder put = new StringBuilder();
        StringBuilder take = new StringBuilder();
        for (Map.Entry<Material, Integer> e : delta.entrySet()) {
            int d = e.getValue();
            if (d == 0) {
                continue;
            }
            StringBuilder target = d > 0 ? put : take;
            if (target.length() > 0) {
                target.append(',');
            }
            target.append(e.getKey().name()).append(':').append(Math.abs(d));
        }
        StringBuilder out = new StringBuilder();
        if (put.length() > 0) {
            out.append("put=").append(put);
        }
        if (take.length() > 0) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append("take=").append(take);
        }
        return out.length() == 0 ? "unchanged" : out.toString();
    }

    private static void applyStackDelta(Map<Material, Integer> delta, ItemStack stack, int sign) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        delta.merge(stack.getType(), sign * stack.getAmount(), Integer::sum);
    }
}
