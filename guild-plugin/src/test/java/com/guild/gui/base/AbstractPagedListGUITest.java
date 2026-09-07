package com.guild.gui.base;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUIManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractPagedListGUITest {

    private GuildPlugin plugin;
    private Player viewer;
    private TestPagedListGUI gui;
    private GUIManager guiManager;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        viewer = mock(Player.class);
        guiManager = mock(GUIManager.class);
        when(plugin.getLanguageManager()).thenReturn(mock(com.guild.core.language.LanguageManager.class));
        when(plugin.getGuiManager()).thenReturn(guiManager);
        when(guiManager.isImageLayoutActive(viewer, "TestPagedListGUI")).thenReturn(false);
        gui = new TestPagedListGUI(plugin, viewer);
    }

    @Test
    void goNextPage_advancesWhenMoreEntriesExist() {
        gui.setEntries(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z", "aa", "ab", "ac"));

        gui.goNextPage(viewer);

        assertEquals(1, gui.currentPage);
        verify(guiManager).refreshGUI(viewer);
    }

    @Test
    void onClick_selectsEntryBySlotMapping() {
        gui.setEntries(List.of("alpha", "beta"));
        ItemStack marker = new ItemStack(Material.STONE);

        gui.onClick(viewer, GuiLayoutUtils.slotForPageIndex(1), marker, ClickType.LEFT);

        assertEquals(List.of("beta"), gui.selected);
    }

    @Test
    void onClick_sideLayoutPaginationUsesConfiguredSlots() {
        gui = new TestPagedListGUI(plugin, viewer, AbstractPagedListGUI.PaginationLayout.SIDE);
        gui.setEntries(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z", "aa", "ab", "ac"));
        gui.currentPage = 1;

        gui.onClick(viewer, 18, new ItemStack(Material.ARROW), ClickType.LEFT);

        assertEquals(0, gui.currentPage);
        verify(guiManager).refreshGUI(viewer);
    }

    @Test
    void handleToolbarClick_consumedBeforeEntrySelection() {
        gui.setEntries(List.of("only"));
        gui.toolbarSlot = 45;

        boolean handled = gui.handleToolbarClick(viewer, 45, ClickType.LEFT);

        assertTrue(handled);
        assertTrue(gui.toolbarClicked);
    }

    @Test
    void displayEmptyState_rendersPlaceholderWhenEntriesEmpty() {
        Inventory inventory = mock(Inventory.class);
        gui.setEntries(List.of());

        gui.displayEmptyState(inventory);

        assertTrue(gui.emptyRendered);
    }

    /** 最小具体子类，用于验证分页/点击行为 */
    static final class TestPagedListGUI extends AbstractPagedListGUI<String> {

        final List<String> selected = new java.util.ArrayList<>();
        boolean emptyRendered;
        boolean toolbarClicked;
        int toolbarSlot = -1;

        TestPagedListGUI(GuildPlugin plugin, Player viewer) {
            super(plugin, viewer);
        }

        TestPagedListGUI(GuildPlugin plugin, Player viewer, PaginationLayout layout) {
            super(plugin, viewer, layout, GuiLayoutUtils.ITEMS_PER_PAGE);
        }

        @Override
        protected boolean shouldFillBorder() {
            return false;
        }

        @Override
        protected void setupToolbar(org.bukkit.inventory.Inventory inventory) {
            // 测试环境跳过工具栏
        }

        @Override
        public String getGuiType() {
            return "TestPagedListGUI";
        }

        @Override
        public String getTitle() {
            return "Test";
        }

        @Override
        protected ItemStack createEntryItem(String entry) {
            return GuiLayoutUtils.createItem(Material.PAPER, entry);
        }

        @Override
        protected void onEntrySelected(Player player, String entry) {
            selected.add(entry);
        }

        @Override
        protected void openBackGui(Player player) {
        }

        @Override
        protected void displayEmptyState(org.bukkit.inventory.Inventory inventory) {
            emptyRendered = true;
        }

        @Override
        protected boolean handleToolbarClick(Player player, int slot, ClickType clickType) {
            if (slot == toolbarSlot) {
                toolbarClicked = true;
                return true;
            }
            return false;
        }
    }
}
