package com.guild.gui.base;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.gui.layout.GuiImageLayoutConfig;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 泛型分页列表 GUI 基类：28 槽内容区 + 可配置翻页/底栏布局。
 * <p>
 * 子类保留独立类名，以便 imago-gui.yml / gui-image-layout.yml 按屏配置。
 */
public abstract class AbstractPagedListGUI<T> implements GUI {

    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_BACK = "BACK";

    /** 翻页按钮默认槽位策略 */
    public enum PaginationLayout {
        /** 底栏：上一页 45、返回 49、下一页 53 */
        BOTTOM(45, 53, 49),
        /** 侧栏：上一页 18、下一页 26（返回由 {@link #setupToolbar} 处理） */
        SIDE(18, 26, -1);

        private final int prevSlot;
        private final int nextSlot;
        private final int backSlot;

        PaginationLayout(int prevSlot, int nextSlot, int backSlot) {
            this.prevSlot = prevSlot;
            this.nextSlot = nextSlot;
            this.backSlot = backSlot;
        }

        int prevSlot() {
            return prevSlot;
        }

        int nextSlot() {
            return nextSlot;
        }

        int backSlot() {
            return backSlot;
        }
    }

    protected final GuildPlugin plugin;
    protected final LanguageManager languageManager;
    protected final Player viewer;
    protected int currentPage = 0;
    protected List<T> entries = List.of();

    private final PaginationLayout paginationLayout;
    private final int itemsPerPage;

    protected AbstractPagedListGUI(GuildPlugin plugin, Player viewer) {
        this(plugin, viewer, PaginationLayout.BOTTOM, GuiLayoutUtils.ITEMS_PER_PAGE);
    }

    protected AbstractPagedListGUI(GuildPlugin plugin, Player viewer,
                                   PaginationLayout paginationLayout, int itemsPerPage) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.viewer = viewer;
        this.paginationLayout = paginationLayout;
        this.itemsPerPage = itemsPerPage;
    }

    protected int itemsPerPage() {
        return itemsPerPage;
    }

    protected PaginationLayout paginationLayout() {
        return paginationLayout;
    }

    protected void setEntries(List<T> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        int maxPage = maxPageIndex();
        if (currentPage > maxPage) {
            currentPage = maxPage;
        }
    }

    protected List<T> getEntries() {
        return entries;
    }

    protected abstract ItemStack createEntryItem(T entry);

    /** 简单选择场景：单击条目 */
    protected void onEntrySelected(Player player, T entry) {
    }

    /** 复杂点击；默认左键委托 {@link #onEntrySelected} */
    protected void onEntryClick(Player player, T entry, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            onEntrySelected(player, entry);
        }
    }

    protected abstract void openBackGui(Player player);

    /** 底栏功能按钮（邀请/踢人等）；默认无 */
    protected void setupToolbar(Inventory inventory) {
    }

    /** 列表为空时的占位渲染 */
    protected void displayEmptyState(Inventory inventory) {
    }

    protected boolean shouldFillBorder() {
        return !plugin.getGuiManager().isImageLayoutActive(viewer, getGuiType());
    }

    protected boolean validateAccess(Player player) {
        return true;
    }

    protected void onUnauthorizedAccess(Player player) {
    }

    protected String prevPageTitleKey() {
        return "gui.common.previous-page";
    }

    protected String prevPageTitleDefault() {
        return "&e&lPrevious Page";
    }

    protected String prevPageLoreKey() {
        return "gui.common.view-previous";
    }

    protected String prevPageLoreDefault() {
        return "View previous page";
    }

    protected String nextPageTitleKey() {
        return "gui.common.next-page";
    }

    protected String nextPageTitleDefault() {
        return "&e&lNext Page";
    }

    protected String nextPageLoreKey() {
        return "gui.common.view-next";
    }

    protected String nextPageLoreDefault() {
        return "View next page";
    }

    protected String backTitleKey() {
        return "gui.common.back";
    }

    protected String backTitleDefault() {
        return "Back";
    }

    protected String backLoreKey() {
        return "gui.common.member-operation.back-to-settings";
    }

    protected String backLoreDefault() {
        return "Return to guild settings";
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        if (shouldFillBorder()) {
            GuiLayoutUtils.fillBorder54(inventory);
        }
        setupToolbar(inventory);
        if (entries.isEmpty()) {
            displayEmptyState(inventory);
        } else {
            displayEntries(inventory);
        }
        setupNavigationButtons(inventory);
        plugin.getGuiManager().applyImageModeIfNeeded(viewer, inventory, getGuiType());
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (!validateAccess(player)) {
            onUnauthorizedAccess(player);
            return;
        }

        if (handleImageLayoutClick(player, slot)) {
            return;
        }

        if (handleToolbarClick(player, slot, clickType)) {
            return;
        }

        int entryIndex = GuiLayoutUtils.listIndexFromSlot(slot, currentPage, itemsPerPage);
        if (entryIndex >= 0 && entryIndex < entries.size()) {
            onEntryClick(player, entries.get(entryIndex), clickType);
            return;
        }

        if (slot == slotForFunction(FUNC_PREV_PAGE, paginationLayout.prevSlot())) {
            goPrevPage(player);
        } else if (slot == slotForFunction(FUNC_NEXT_PAGE, paginationLayout.nextSlot())) {
            goNextPage(player);
        } else if (paginationLayout.backSlot() >= 0
                && slot == slotForFunction(FUNC_BACK, paginationLayout.backSlot())) {
            openBackGui(player);
        }
    }

    /** 子类可覆盖以处理底栏功能按钮；返回 true 表示已消费点击 */
    protected boolean handleToolbarClick(Player player, int slot, ClickType clickType) {
        return false;
    }

    protected boolean handleImageLayoutClick(Player player, int slot) {
        if (!plugin.getGuiManager().isImageLayoutActive(player, getGuiType())) {
            return false;
        }
        GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
        String func = layoutConfig.getFunctionAtSlot(getGuiType(), slot);
        if (func == null) {
            return false;
        }
        dispatchNavFunction(player, func);
        return true;
    }

    protected void dispatchNavFunction(Player player, String func) {
        if (FUNC_PREV_PAGE.equals(func)) {
            goPrevPage(player);
        } else if (FUNC_NEXT_PAGE.equals(func)) {
            goNextPage(player);
        } else if (FUNC_BACK.equals(func)) {
            openBackGui(player);
        }
    }

    protected void goPrevPage(Player player) {
        if (currentPage > 0) {
            currentPage--;
            plugin.getGuiManager().refreshGUI(player);
        }
    }

    protected void goNextPage(Player player) {
        if (currentPage < maxPageIndex()) {
            currentPage++;
            plugin.getGuiManager().refreshGUI(player);
        }
    }

    protected int maxPageIndex() {
        return GuiLayoutUtils.maxPageIndex(entries.size(), itemsPerPage);
    }

    protected void displayEntries(Inventory inventory) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, entries.size());
        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(GuiLayoutUtils.slotForPageIndex(i - startIndex), createEntryItem(entries.get(i)));
        }
    }

    protected void setupNavigationButtons(Inventory inventory) {
        if (currentPage > 0) {
            inventory.setItem(slotForFunction(FUNC_PREV_PAGE, paginationLayout.prevSlot()),
                    GuiLayoutUtils.createItem(
                            Material.ARROW,
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    prevPageTitleKey(), prevPageTitleDefault())),
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    prevPageLoreKey(), prevPageLoreDefault()))));
        }

        if (currentPage < maxPageIndex()) {
            inventory.setItem(slotForFunction(FUNC_NEXT_PAGE, paginationLayout.nextSlot()),
                    GuiLayoutUtils.createItem(
                            Material.ARROW,
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    nextPageTitleKey(), nextPageTitleDefault())),
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    nextPageLoreKey(), nextPageLoreDefault()))));
        }

        if (paginationLayout.backSlot() >= 0) {
            inventory.setItem(slotForFunction(FUNC_BACK, paginationLayout.backSlot()),
                    GuiLayoutUtils.createItem(
                            Material.ARROW,
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    backTitleKey(), backTitleDefault())),
                            ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                                    backLoreKey(), backLoreDefault()))));
        }
    }

    protected int slotForFunction(String function, int defaultSlot) {
        GuiImageLayoutConfig layoutConfig = plugin.getGuiManager().getImageLayoutConfig();
        if (layoutConfig == null) {
            return defaultSlot;
        }
        List<Integer> slots = layoutConfig.getSlots(getGuiType(), function);
        return slots.isEmpty() ? defaultSlot : slots.get(0);
    }

    protected ItemStack createItem(Material material, String name, String... lore) {
        return GuiLayoutUtils.createItem(material, name, lore);
    }
}
