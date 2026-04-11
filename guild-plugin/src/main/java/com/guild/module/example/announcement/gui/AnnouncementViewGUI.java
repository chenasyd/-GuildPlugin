package com.guild.module.example.announcement.gui;

import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.module.example.announcement.Announcement;
import com.guild.module.example.announcement.AnnouncementModule;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 公告浏览GUI - 只读查看工会公告
 * <p>
 * 所有工会成员均可查看，无需管理权限。
 * 点击某条公告可查看完整内容。
 * <p>
 * 布局 (54槽 / 6行x9列)：
 * <ul>
 *   <li>Row 1: 边框</li>
 *   <li>Row 2-5: 公告列表</li>
 *   <li>Row 6: 边框 + 返回按钮(49)</li>
 * </ul>
 */
public class AnnouncementViewGUI implements GUI {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final AnnouncementModule module;
    private final Guild guild;
    private Inventory inventory;

    private static final int PER_PAGE = 14;

    /** 当前选中的公告（非null时显示详情页） */
    private Announcement selectedAnnouncement = null;
    private int currentPage = 1;

    public AnnouncementViewGUI(AnnouncementModule module, Guild guild) {
        this.module = module;
        this.guild = guild;
    }

    @Override
    public String getTitle() {
        if (selectedAnnouncement != null) {
            return ColorUtils.colorize(
                    module.getContext().getMessage("module.announcement.view.detail-title",
                            "&6&l公告详情"));
        }
        String baseTitle = ColorUtils.colorize(
                module.getContext().getMessage("module.announcement.view.title",
                        "&6&l工会公告"));
        int totalPages = getTotalPages();
        if (totalPages > 1) {
            baseTitle += ColorUtils.colorize(" &7(" +
                    module.getContext().getMessage("gui.page-info",
                            String.valueOf(currentPage), String.valueOf(totalPages)) +
                    ")");
        }
        return baseTitle;
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;
        fillBorder(inventory);
        clearInterior(inventory);

        if (selectedAnnouncement != null) {
            renderDetailPage(inventory);
        } else {
            renderListPage(inventory);
        }

        // 返回按钮
        if (selectedAnnouncement != null) {
            inventory.setItem(49, createBackButton(
                    module.getContext().getMessage("module.announcement.view.back-to-list", "&c返回列表"),
                    module.getContext().getMessage("module.announcement.view.back-to-list-hint", "&7点击返回公告列表")));
        } else {
            inventory.setItem(49, createBackButton(
                    module.getContext().getMessage("module.announcement.view.back", "&c返回"),
                    module.getContext().getMessage("module.announcement.view.back-hint", "&7点击返回工会信息")));
        }

        // 翻页按钮（仅列表页且多页时）
        if (selectedAnnouncement == null && getTotalPages() > 1) {
            setupPagination(inventory);
        }

        fillInteriorSlots(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 翻页
        if (selectedAnnouncement == null) {
            if (slot == 45 && currentPage > 1) {
                currentPage--;
                refresh(player);
                return;
            }
            if (slot == 53 && currentPage < getTotalPages()) {
                currentPage++;
                refresh(player);
                return;
            }
        }

        // 返回
        if (slot == 49) {
            if (selectedAnnouncement != null) {
                // 详情页 -> 返回列表
                selectedAnnouncement = null;
                refresh(player);
            } else {
                // 列表页 -> 返回工会信息
                module.getContext().navigateBack(player);
            }
            return;
        }

        // 列表页：点击公告查看详情
        if (selectedAnnouncement == null) {
            List<Announcement> announcements = module.getAnnouncementManager()
                    .getAnnouncements(guild.getId());
            int fromIndex = (currentPage - 1) * PER_PAGE;
            int displayCount = Math.min(PER_PAGE, announcements.size() - fromIndex);

            for (int i = 0; i < displayCount; i++) {
                int targetSlot = mapToSlot(i);
                if (targetSlot == slot) {
                    selectedAnnouncement = announcements.get(fromIndex + i);
                    refresh(player);
                    return;
                }
            }
        }
    }

    @Override
    public void onClose(Player player) {}

    @Override
    public void refresh(Player player) {
        setupInventory(inventory);
    }

    // ==================== 页面渲染 ====================

    private void renderListPage(Inventory inv) {
        List<Announcement> allAnnouncements = module.getAnnouncementManager()
                .getAnnouncements(guild.getId());

        if (allAnnouncements.isEmpty()) {
            // 空状态提示
            ItemStack emptyItem = createItem(Material.BARRIER,
                    ColorUtils.colorize("&7" +
                            module.getContext().getMessage("module.announcement.view.empty",
                                    "&7暂无公告")),
                    ColorUtils.colorize("&7" +
                            module.getContext().getMessage("module.announcement.view.empty-hint",
                                    "&7工会尚未发布任何公告")));
            inv.setItem(22, emptyItem);
            return;
        }

        int total = allAnnouncements.size();
        int fromIndex = (currentPage - 1) * PER_PAGE;
        int toIndex = Math.min(fromIndex + PER_PAGE, total);

        for (int i = fromIndex; i < toIndex; i++) {
            Announcement ann = allAnnouncements.get(i);
            int slotIndex = i - fromIndex;
            int targetSlot = mapToSlot(slotIndex);
            if (targetSlot >= 0) {
                inv.setItem(targetSlot, createAnnouncementItem(ann));
            }
        }
    }

    private void renderDetailPage(Inventory inv) {
        Announcement ann = selectedAnnouncement;
        if (ann == null) return;

        // 标题区域
        ItemStack titleItem = createItem(Material.NAME_TAG,
                ColorUtils.colorize("&6&l" + ann.getTitle()),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.view.detail-author",
                                ann.getAuthorName())),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.view.detail-time",
                                ann.getCreatedAt().format(DATE_FORMAT))));
        inv.setItem(13, titleItem);

        // 内容区域
        List<String> contentLore = new ArrayList<>();
        contentLore.add(ColorUtils.colorize("&f" +
                module.getContext().getMessage("module.announcement.view.detail-content-label",
                        "&f公告内容:")));
        contentLore.add("");
        String[] lines = ann.getContent().split("\n");
        for (String line : lines) {
            if (line.length() > 35) {
                int start = 0;
                while (start < line.length()) {
                    int end = Math.min(start + 35, line.length());
                    contentLore.add(ColorUtils.colorize("&f" + line.substring(start, end)));
                    start = end;
                }
            } else {
                contentLore.add(ColorUtils.colorize("&f" + line));
            }
        }
        ItemStack contentItem = createItem(Material.WRITABLE_BOOK,
                ColorUtils.colorize("&6" +
                        module.getContext().getMessage("module.announcement.view.detail-content",
                                "&6公告内容")),
                contentLore.toArray(new String[0]));
        inv.setItem(22, contentItem);

        // 摘要信息
        ItemStack summaryItem = createItem(Material.PAPER,
                ColorUtils.colorize("&e" +
                        module.getContext().getMessage("module.announcement.view.detail-summary")),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.view.detail-author",
                                ann.getAuthorName())),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.view.detail-time",
                                ann.getCreatedAt().format(DATE_FORMAT))),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.view.detail-updated",
                                ann.getUpdatedAt().format(DATE_FORMAT))));
        inv.setItem(31, summaryItem);
    }

    // ==================== 内部方法 ====================

    private int getTotalPages() {
        int total = module.getAnnouncementManager().getCount(guild.getId());
        if (total == 0) return 1;
        return (int) Math.ceil((double) total / PER_PAGE);
    }

    private int mapToSlot(int linearIndex) {
        if (linearIndex < 0 || linearIndex >= PER_PAGE) return -1;
        int row = linearIndex / 7;
        int col = linearIndex % 7;
        int baseRow = 9 + row * 9;
        return baseRow + col + 1;
    }

    private ItemStack createAnnouncementItem(Announcement ann) {
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" +
                module.getContext().getMessage("module.announcement.view.item-author",
                        ann.getAuthorName())));
        lore.add(ColorUtils.colorize("&7" +
                module.getContext().getMessage("module.announcement.view.item-time",
                        ann.getCreatedAt().format(DATE_FORMAT))));
        lore.add("");
        lore.add(ColorUtils.colorize("&7" + ann.getPreview(30)));
        lore.add("");
        lore.add(ColorUtils.colorize("&e" +
                module.getContext().getMessage("module.announcement.view.item-click",
                        "&e点击查看详情")));

        Material mat = Material.PAPER;
        try {
            mat = Material.valueOf("OAK_SIGN");
        } catch (IllegalArgumentException ignored) {}

        return createItem(mat,
                ColorUtils.colorize("&6" + ann.getTitle()),
                lore.toArray(new String[0]));
    }

    private void setupPagination(Inventory inv) {
        int totalPages = getTotalPages();

        if (currentPage > 1) {
            inv.setItem(45, createItem(Material.ARROW,
                    ColorUtils.colorize("&e&l" +
                            module.getContext().getMessage("gui.previous-page", "&e&l上一页")),
                    ColorUtils.colorize("&7" +
                            module.getContext().getMessage("gui.previous-page-hint",
                                    "&7点击返回上一页"))));
        }

        if (currentPage < totalPages) {
            inv.setItem(53, createItem(Material.ARROW,
                    ColorUtils.colorize("&e&l" +
                            module.getContext().getMessage("gui.next-page", "&e&l下一页")),
                    ColorUtils.colorize("&7" +
                            module.getContext().getMessage("gui.next-page-hint",
                                    "&7点击查看更多"))));
        }
    }

    // ==================== UI工具 ====================

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

    private void clearInterior(Inventory inventory) {
        for (int slot = 9; slot <= 44; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue;
            inventory.setItem(slot, null);
        }
    }

    private void fillInteriorSlots(Inventory inventory) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 9; slot <= 44; slot++) {
            int col = slot % 9;
            if (col == 0 || col == 8) continue;
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ColorUtils.colorize(line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackButton(String name, String hint) {
        return createItem(Material.ARROW, ColorUtils.colorize(name), ColorUtils.colorize(hint));
    }
}
