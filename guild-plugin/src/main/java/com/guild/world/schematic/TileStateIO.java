package com.guild.world.schematic;

import org.bukkit.DyeColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** TileState 序列化（方块实体）；失败时返回 null，由调用方降级为仅方块。 */
public final class TileStateIO {

    private TileStateIO() {
    }

    public static Map<String, Object> serialize(TileState ts, Logger logger) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            if (ts instanceof Container c) {
                List<Map<String, Object>> inv = new ArrayList<>();
                var snap = c.getSnapshotInventory();
                for (int i = 0; i < snap.getSize(); i++) {
                    ItemStack it = snap.getItem(i);
                    if (it == null || it.getType().isAir()) {
                        continue;
                    }
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("slot", i);
                    e.put("item", encodeItem(it));
                    inv.add(e);
                }
                m.put("inv", inv);
            }
            if (ts instanceof Sign sign) {
                m.put("lines", Arrays.asList(sign.getLines()));
                if (sign.getColor() != null) {
                    m.put("color", sign.getColor().name());
                }
                m.put("glow", sign.isGlowingText());
            }
            if (ts instanceof CreatureSpawner spawner) {
                if (spawner.getSpawnedType() != null) {
                    m.put("type", spawner.getSpawnedType().name());
                }
                m.put("delay", spawner.getDelay());
            }
            return m.isEmpty() ? Map.of() : m;
        } catch (Throwable t) {
            if (logger != null) {
                logger.log(Level.FINE, "[World] TileState serialize skipped: " + t.getMessage());
            }
            return null;
        }
    }

    public static void apply(BlockState state, Map<String, Object> data, Logger logger) {
        if (data == null || data.isEmpty() || !(state instanceof TileState)) {
            return;
        }
        try {
            if (state instanceof Container c) {
                Object invRaw = data.get("inv");
                if (invRaw instanceof List<?> list) {
                    c.getInventory().clear();
                    for (Object o : list) {
                        if (!(o instanceof Map<?, ?> m)) {
                            continue;
                        }
                        int slot = ((Number) m.get("slot")).intValue();
                        Object itemRaw = m.get("item");
                        if (!(itemRaw instanceof String s)) {
                            continue;
                        }
                        ItemStack item = decodeItem(s);
                        if (item != null && !item.getType().isAir()) {
                            c.getSnapshotInventory().setItem(slot, item);
                        }
                    }
                    c.update(true, false);
                }
            }
            if (state instanceof Sign sign) {
                Object lines = data.get("lines");
                if (lines instanceof List<?> list) {
                    for (int i = 0; i < Math.min(4, list.size()); i++) {
                        sign.setLine(i, String.valueOf(list.get(i)));
                    }
                }
                Object color = data.get("color");
                if (color instanceof String cs) {
                    try {
                        sign.setColor(DyeColor.valueOf(cs));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                Object glow = data.get("glow");
                if (glow instanceof Boolean b) {
                    sign.setGlowingText(b);
                }
                sign.update(true, false);
            }
            if (state instanceof CreatureSpawner spawner) {
                Object type = data.get("type");
                if (type instanceof String ts) {
                    try {
                        spawner.setSpawnedType(org.bukkit.entity.EntityType.valueOf(ts));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                Object delay = data.get("delay");
                if (delay instanceof Number n) {
                    spawner.setDelay(n.intValue());
                }
                spawner.update(true, false);
            }
        } catch (Throwable t) {
            if (logger != null) {
                logger.log(Level.FINE, "[World] TileState apply skipped: " + t.getMessage());
            }
        }
    }

    private static String encodeItem(ItemStack item) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
            oos.writeObject(item);
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    private static ItemStack decodeItem(String b64) throws Exception {
        byte[] raw = Base64.getDecoder().decode(b64);
        try (BukkitObjectInputStream ois = new BukkitObjectInputStream(new ByteArrayInputStream(raw))) {
            Object obj = ois.readObject();
            return obj instanceof ItemStack is ? is : null;
        }
    }
}
