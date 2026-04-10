package com.guild.sdk.gui;

import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 模块 GUI 抽象基类 - 提供通用的 GUI 布局工具方法
 * <p>
 * 所有模块 GUI 推荐继承此类，避免重复编写边框填充、物品创建等通用代码。
 * <p>
 * 布局约定（6行 54槽）：
 * <ul>
 *   <li>第1行 (0-8): 顶部边框</li>
 *   <li>第2-5行 (9-44): 内容区域（中间7列可用）</li>
 *   <li>第6行 (45-53): 底部边框（常用于返回/翻页按钮）</li>
 * </ul>
 */
public abstract class AbstractModuleGUI implements GUI {

    /** 默认 GUI 大小（6行） */
    protected static final int DEFAULT_SIZE = 54;

    /** 内容区域起始行 */
    protected static final int CONTENT_START = 9;
    /** 内容区域结束行 */
    protected static final int CONTENT_END = 44;
    /** 每行内容列数（去掉两侧边框列） */
    protected static final int COLUMNS = 7;
    /** 每页内容行数 */
    protected static final int CONTENT_ROWS = 4;
    /** 每页最大内容项数 */
    protected static final int PER_PAGE = COLUMNS * CONTENT_ROWS;

    protected Inventory inventory;

    // ==================== 通用布局工具 ====================

    /**
     * 用黑色玻璃板填充四周边框（第1行、第6行、左列、右列）
     */
    protected void fillBorder(Inventory inv) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
    }

    /**
     * 用灰色玻璃板填充内容区域的空槽位
     */
    protected void fillInteriorSlots(Inventory inv) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = CONTENT_START; slot <= CONTENT_END; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue;
            if (inv.getItem(slot) == null) inv.setItem(slot, filler);
        }
    }

    /**
     * 将线性索引映射到 GUI 槽位号（基于中间7列布局）
     *
     * @param linearIndex 0-based 线性索引
     * @return GUI 槽位号，-1 表示越界
     */
    protected int mapToSlot(int linearIndex) {
        if (linearIndex < 0 || linearIndex >= PER_PAGE) return -1;
        int row = linearIndex / COLUMNS;
        int col = linearIndex % COLUMNS;
        int baseRow = CONTENT_START + row * 9;
        return baseRow + col + 1;
    }

    // ==================== 物品创建工具 ====================

    /**
     * 创建带名称和描述的物品
     */
    protected ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            if (lore != null && lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(ColorUtils.colorize(line));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 创建带颜色代码的物品（参数已经过 ColorUtils 处理）
     */
    protected ItemStack createItemRaw(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            if (lore != null && lore.length > 0) {
                List<String> loreList = new ArrayList<>(List.of(lore));
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 创建返回按钮（默认放在 slot 49）
     */
    protected ItemStack createBackButton(String name, String hint) {
        return createItem(Material.ARROW,
                ColorUtils.colorize(name),
                ColorUtils.colorize(hint));
    }

    /**
     * 创建翻页按钮
     */
    protected ItemStack createPageButton(Material material, String name, String hint) {
        return createItem(material,
                ColorUtils.colorize(name),
                ColorUtils.colorize(hint));
    }

    /**
     * 设置翻页按钮（slot 45 = 上一页, slot 53 = 下一页）
     *
     * @param inv          目标 Inventory
     * @param currentPage  当前页码（从1开始）
     * @param totalPages   总页数
     * @param prevPageKey  上一页按钮名称消息键
     * @param nextPageKey  下一页按钮名称消息键
     */
    protected void setupPagination(Inventory inv, int currentPage, int totalPages,
                                    String prevPageKey, String nextPageKey) {
        if (totalPages <= 1) return;

        if (currentPage > 1) {
            inv.setItem(45, createPageButton(Material.ARROW,
                    prevPageKey != null ? prevPageKey : "&e&l\u2190",
                    "&7"));
        }
        if (currentPage < totalPages) {
            inv.setItem(53, createPageButton(Material.ARROW,
                    nextPageKey != null ? nextPageKey : "&e&l\u2192",
                    "&7"));
        }
    }

    /**
     * 计算总页数
     */
    protected int getTotalPages(int totalItems) {
        if (totalItems == 0) return 1;
        return (int) Math.ceil((double) totalItems / PER_PAGE);
    }

    // ==================== GUI 接口默认实现 ====================

    @Override
    public int getSize() {
        return DEFAULT_SIZE;
    }

    @Override
    public void onClose(Player player) {}

    @Override
    public void refresh(Player player) {
        if (inventory != null) {
            setupInventory(inventory);
        }
    }
}
