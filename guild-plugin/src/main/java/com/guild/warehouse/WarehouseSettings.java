package com.guild.warehouse;

import com.guild.GuildPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Config for guild warehouse capacity and enablement.
 * Total slots are multiples of 9 (no upper cap). Each GUI page holds at most {@link #PAGE_SIZE} slots.
 */
public class WarehouseSettings {

    /** Max slots per chest inventory page. */
    public static final int PAGE_SIZE = 54;

    private final GuildPlugin plugin;
    private boolean enabled = true;
    private final NavigableMap<Integer, Integer> slotsByLevel = new TreeMap<>();

    public WarehouseSettings(GuildPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        enabled = cfg.getBoolean("guild-warehouse.enabled", true);
        slotsByLevel.clear();
        ConfigurationSection section = cfg.getConfigurationSection("guild-warehouse.slots-by-level");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int slots = section.getInt(key, 9);
                    slotsByLevel.put(level, normalizeSlots(slots));
                } catch (NumberFormatException ignored) {
                    // skip invalid keys
                }
            }
        }
        if (slotsByLevel.isEmpty()) {
            slotsByLevel.put(1, 9);
            slotsByLevel.put(2, 18);
            slotsByLevel.put(3, 27);
            slotsByLevel.put(4, 36);
            slotsByLevel.put(5, 45);
            slotsByLevel.put(6, 54);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSlotsForPeakLevel(int peakLevel) {
        if (peakLevel < 1) {
            peakLevel = 1;
        }
        Integer exact = slotsByLevel.get(peakLevel);
        if (exact != null) {
            return exact;
        }
        var floor = slotsByLevel.floorEntry(peakLevel);
        if (floor != null) {
            return floor.getValue();
        }
        return 9;
    }

    public static int getPageCount(int totalSlots) {
        if (totalSlots <= 0) {
            return 1;
        }
        return (totalSlots + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    /** 0-based absolute start slot for a 1-based page. */
    public static int getPageOffset(int page) {
        return Math.max(0, page - 1) * PAGE_SIZE;
    }

    /** Inventory size for a 1-based page given total capacity. */
    public static int getPageSlotCount(int totalSlots, int page) {
        int pages = getPageCount(totalSlots);
        if (page < 1 || page > pages) {
            return 0;
        }
        int offset = getPageOffset(page);
        return Math.min(PAGE_SIZE, totalSlots - offset);
    }

    private static int normalizeSlots(int slots) {
        if (slots < 9) {
            slots = 9;
        }
        // Chest inventory sizes must be multiples of 9 (no max cap; extra pages via /guild warehouse <page>)
        return Math.max(9, (slots / 9) * 9);
    }
}
