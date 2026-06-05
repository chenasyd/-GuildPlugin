package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.language.LanguageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * 工会筛选GUI - 设置等级范围和人数排序条件
 */
public class GuildFilterGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private final String searchQuery;
    private int minLevel;
    private int maxLevel;
    private String sortMode; // DESC, ASC, FULL_ONLY
    private int sortIndex; // 0=DESC, 1=ASC, 2=FULL_ONLY

    private static final String[] SORT_MODES = {"DESC", "ASC", "FULL_ONLY"};
    private static final int SLOT_MIN_LEVEL = 46;
    private static final int SLOT_MAX_LEVEL = 47;
    private static final int SLOT_SORT = 48;
    private static final int SLOT_BACK = 52;

    public GuildFilterGUI(GuildPlugin plugin, Player player, String searchQuery, int minLevel, int maxLevel, String sortMode) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.searchQuery = searchQuery != null ? searchQuery : "";
        this.minLevel = Math.max(1, minLevel);
        this.maxLevel = Math.min(plugin.getMaxGuildLevel(), maxLevel);
        setSortMode(sortMode);
    }

    private void setSortMode(String mode) {
        this.sortMode = (mode != null && (mode.equals("DESC") || mode.equals("ASC") || mode.equals("FULL_ONLY")))
            ? mode : "DESC";
        // 计算索引
        for (int i = 0; i < SORT_MODES.length; i++) {
            if (SORT_MODES[i].equals(this.sortMode)) {
                this.sortIndex = i;
                break;
            }
        }
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.title", "&6工会筛选"));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        // 填充边框
        fillBorder(inventory);

        // 设置筛选条件按钮
        setupFilterButtons(inventory);
    }

    /**
     * 填充边框
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }

    /**
     * 设置筛选条件按钮
     */
    private void setupFilterButtons(Inventory inventory) {
        // 最低等级 (slot 46)
        ItemStack minLevelItem = createItem(
            Material.IRON_INGOT,
            ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.min-level.name", "&e最低等级")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.min-level.lore-1", "&7左键: +1 | 右键: -1")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.min-level.lore-2", "&7当前: {level}", "{level}", String.valueOf(minLevel)))
        );
        inventory.setItem(SLOT_MIN_LEVEL, minLevelItem);

        // 最高等级 (slot 47)
        ItemStack maxLevelItem = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.max-level.name", "&e最高等级")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.max-level.lore-1", "&7左键: +1 | 右键: -1")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.max-level.lore-2", "&7当前: {level}", "{level}", String.valueOf(maxLevel)))
        );
        inventory.setItem(SLOT_MAX_LEVEL, maxLevelItem);

        // 人数排序 (slot 48)
        ItemStack sortItem = createSortItem();
        inventory.setItem(SLOT_SORT, sortItem);

        // 返回按钮 (slot 52)
        ItemStack backItem = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.back.name", "&c返回")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.back.lore-1", "&7返回工会列表"))
        );
        inventory.setItem(SLOT_BACK, backItem);
    }

    /**
     * 创建人数排序按钮
     */
    private ItemStack createSortItem() {
        String markerOn = languageManager.getMessage(player, "guild-filter.marker-on", "&a✔");
        String markerOff = languageManager.getMessage(player, "guild-filter.marker-off", "&7");

        String descLore = languageManager.getMessage(player, "guild-filter.sort.lore-desc", "人数降序 #人数从多到少");
        String ascLore = languageManager.getMessage(player, "guild-filter.sort.lore-asc", "人数升序 #人数从少到多");
        String fullLore = languageManager.getMessage(player, "guild-filter.sort.lore-full", "仅满员");

        String line1, line2, line3;

        switch (sortMode) {
            case "ASC":
                line1 = markerOff + descLore;
                line2 = markerOn + ascLore;
                line3 = markerOff + fullLore;
                break;
            case "FULL_ONLY":
                line1 = markerOff + descLore;
                line2 = markerOff + ascLore;
                line3 = markerOn + fullLore;
                break;
            default: // DESC
                line1 = markerOn + descLore;
                line2 = markerOff + ascLore;
                line3 = markerOff + fullLore;
                break;
        }

        return createItem(
            Material.COMPARATOR,
            ColorUtils.colorize(languageManager.getMessage(player, "guild-filter.sort.name", "&e人数排序")),
            ColorUtils.colorize(line1),
            ColorUtils.colorize(line2),
            ColorUtils.colorize(line3)
        );
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case SLOT_MIN_LEVEL:
                handleMinLevelClick(player, clickType);
                break;
            case SLOT_MAX_LEVEL:
                handleMaxLevelClick(player, clickType);
                break;
            case SLOT_SORT:
                handleSortClick(player, clickType);
                break;
            case SLOT_BACK:
                handleBack(player);
                break;
        }
    }

    /**
     * 处理最低等级点击
     */
    private void handleMinLevelClick(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // 左键：增加
            if (minLevel < maxLevel) {
                minLevel++;
                refreshInventory(player);
            }
        } else if (clickType == ClickType.RIGHT) {
            // 右键：减少
            if (minLevel > 1) {
                minLevel--;
                refreshInventory(player);
            }
        }
    }

    /**
     * 处理最高等级点击
     */
    private void handleMaxLevelClick(Player player, ClickType clickType) {
        int maxGuildLevel = plugin.getMaxGuildLevel();
        if (clickType == ClickType.LEFT) {
            // 左键：增加
            if (maxLevel < maxGuildLevel) {
                maxLevel++;
                refreshInventory(player);
            }
        } else if (clickType == ClickType.RIGHT) {
            // 右键：减少
            if (maxLevel > minLevel) {
                maxLevel--;
                refreshInventory(player);
            }
        }
    }

    /**
     * 处理排序点击
     * 左键：向下循环（DESC -> ASC -> FULL_ONLY）
     * 右键：向上循环（FULL_ONLY -> ASC -> DESC）
     */
    private void handleSortClick(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // 向下
            sortIndex = (sortIndex + 1) % SORT_MODES.length;
        } else if (clickType == ClickType.RIGHT) {
            // 向上
            sortIndex = (sortIndex - 1 + SORT_MODES.length) % SORT_MODES.length;
        } else {
            return;
        }
        sortMode = SORT_MODES[sortIndex];
        refreshInventory(player);
    }

    /**
     * 返回GuildListGUI
     */
    private void handleBack(Player player) {
        GuildListGUI listGUI = new GuildListGUI(plugin, player, searchQuery, minLevel, maxLevel, sortMode);
        plugin.getGuiManager().openGUI(player, listGUI);
    }

    /**
     * 刷新GUI
     */
    private void refreshInventory(Player player) {
        plugin.getGuiManager().refreshGUI(player);
    }

    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }

        return item;
    }
}
