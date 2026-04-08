package com.guild.module.example.announcement.gui;

import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.gui.GuildSettingsGUI;
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
 * 工会公告列表GUI
 * <p>
 * 布局 (54槽 / 6行x9列)：
 * <ul>
 *   <li>第1行：边框</li>
 *   <li>Row 2-5: 公告列表（每条公告一行）+ 创建新公告按钮(最后)</li>
 *   <li>第6行: 边框 + 返回按钮(49)</li>
 * </ul>
 */
public class AnnouncementListGUI implements GUI {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final AnnouncementModule module;
    private final Guild guild;
    private final Player player;
    private Inventory inventory;

    /** 当前页码（从1开始）*/
    private int currentPage = 1;
    /** 每页显示的公告数（4个功能行 - 每行1条）*/
    private static final int PER_PAGE = 14; // 中间区域可用槽位

    public AnnouncementListGUI(AnnouncementModule module, Guild guild) {
        this.module = module;
        this.guild = guild;
        this.player = null; // 必须使用带 Player 参数的构造函数
    }

    public AnnouncementListGUI(AnnouncementModule module, Guild guild, Player player) {
        this.module = module;
        this.guild = guild;
        this.player = player;
    }

    @Override
    public String getTitle() {
        String baseTitle = ColorUtils.colorize(
                module.getContext().getMessage("module.announcement.list.title",
                        "&e&l工会公告管理"));
        int totalPages = getTotalPages();
        if (totalPages > 1) {
            baseTitle += ColorUtils.colorize(" &7(" +
                    module.getContext().getPlugin().getLanguageManager().getIndexedMessage(player,
                            "gui.page-info", "第{0}页/共{1}页",
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

        List<Announcement> allAnnouncements = module.getAnnouncementManager()
                .getAnnouncements(guild.getId());

        // 计算分页
        int total = allAnnouncements.size();
        int fromIndex = (currentPage - 1) * PER_PAGE;
        int toIndex = Math.min(fromIndex + PER_PAGE, total);

        // 渲染公告项
        for (int i = fromIndex; i < toIndex; i++) {
            Announcement ann = allAnnouncements.get(i);
            int slotIndex = i - fromIndex;
            // 将索引映射到可用槽位（中间4行 x 7列）
            int targetSlot = mapToSlot(slotIndex);
            if (targetSlot >= 0) {
                inventory.setItem(targetSlot, createAnnouncementItem(ann));
            }
        }

        // 如果当前页有空间，显示"创建新公告"按钮
        int usedCount = toIndex - fromIndex;
        if (usedCount < PER_PAGE && hasManagePermission()) {
            int createSlot = mapToSlot(usedCount);
            if (createSlot >= 0) {
                inventory.setItem(createSlot, createCreateButton());
            }
        } else if (hasManagePermission()) {
            // 当前页已满，在固定位置放一个创建按钮
            inventory.setItem(40, createCreateButton()); // Row 3 center
        }

        // 返回按钮
        ItemStack backItem = createItem(Material.ARROW,
                ColorUtils.colorize("&c" +
                        module.getContext().getMessage("module.announcement.list.back",
                                "&c返回")),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.list.back-hint",
                                "&7点击返回工会设置")));
        inventory.setItem(49, backItem);

        // 翻页按钮（如果有多页）
        if (getTotalPages() > 1) {
            setupPagination(inventory);
        }

        fillInteriorSlots(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 翻页
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

        // 返回
        if (slot == 49) {
            returnToSettings();
            return;
        }

        // 检查是否点击了公告项或创建按钮
        List<Announcement> announcements = module.getAnnouncementManager()
                .getAnnouncements(guild.getId());
        int fromIndex = (currentPage - 1) * PER_PAGE;
        int displayCount = Math.min(PER_PAGE, announcements.size() - fromIndex);

        for (int i = 0; i < displayCount; i++) {
            int targetSlot = mapToSlot(i);
            if (targetSlot == slot) {
                // 点击了某条公告 -> 打开编辑界面
                Announcement ann = announcements.get(fromIndex + i);
                if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_LEFT) {
                    // 右键或Shift左键 = 删除确认
                    handleDeleteConfirm(player, ann);
                } else {
                    // 左键 = 编辑
                    module.openEditGUI(player, guild, ann.getId());
                }
                return;
            }
        }

        // 检查是否点击了"创建新公告"按钮（动态槽位）
        if (displayCount < PER_PAGE && hasManagePermission()) {
            int createSlot = mapToSlot(displayCount);
            if (createSlot == slot) {
                module.openCreateGUI(player, guild);
                return;
            }
        }
    }

    @Override
    public void onClose(Player player) {}

    @Override
    public void refresh(Player player) {
        setupInventory(inventory);
    }

    // ==================== 内部方法 ====================

    private int getTotalPages() {
        int total = module.getAnnouncementManager().getCount(guild.getId());
        if (total == 0) return 1;
        return (int) Math.ceil((double) total / PER_PAGE);
    }

    /**
     * 将线性索引映射到GUI槽位号
     * 布局：Row 2-5 的中间7列 (10-16, 19-25, 28-34, 37-43)
     */
    private int mapToSlot(int linearIndex) {
        if (linearIndex < 0 || linearIndex >= PER_PAGE) return -1;
        int row = linearIndex / 7;       // 0-3 对应 row2-row5
        int col = linearIndex % 7;       // 0-6 对应 col1-col7
        int baseRow = 9 + row * 9;      // row2=18, row3=27, row4=36, row5=45
        return baseRow + col + 1;        // 从col1开始(+1跳过边框列0)
    }

    private void handleDeleteConfirm(Player player, Announcement ann) {
        // 简单确认：再次右键删除，或者直接在这里弹确认框
        // 为简化示例，直接执行删除并提示
        boolean success = module.getAnnouncementManager().delete(ann.getId());
        if (success) {
            player.sendMessage(ColorUtils.colorize(
                    module.getContext().getMessage("module.announcement.delete.success",
                            ann.getTitle())));
            refresh(player);
        } else {
            player.sendMessage(ColorUtils.colorize(
                    module.getContext().getMessage("module.announcement.delete.failed")));
        }
    }

    private void returnToSettings() {
        if (player != null) {
            module.getContext().getGuiManager().openGUI(player,
                    new GuildSettingsGUI(module.getContext().getPlugin(), guild, player));
        }
    }

    private boolean hasManagePermission() {
        return module.hasManagePermission(player);
    }

    private ItemStack createAnnouncementItem(Announcement ann) {
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" +
                module.getContext().getMessage("module.announcement.list.author",
                        ann.getAuthorName())));
        lore.add(ColorUtils.colorize("&7" +
                module.getContext().getMessage("module.announcement.list.time",
                        ann.getCreatedAt().format(DATE_FORMAT))));
        lore.add("");
        lore.add(ColorUtils.colorize("&7" + ann.getPreview(30)));
        lore.add("");
        lore.add(ColorUtils.colorize("&e\u27a0 " +
                module.getContext().getMessage("module.announcement.list.click-edit",
                        "&e左键编辑")));
        lore.add(ColorUtils.colorize("&c\u27a0 " +
                module.getContext().getMessage("module.announcement.list.click-delete",
                        "&c右键删除")));

        Material mat = Material.PAPER;
        try {
            // 尝试使用更明显的物品
            mat = Material.valueOf("OAK_SIGN");
        } catch (IllegalArgumentException ignored) {}

        return createItem(mat,
                ColorUtils.colorize("&6" + ann.getTitle()),
                lore.toArray(new String[0]));
    }

    private ItemStack createCreateButton() {
        return createItem(Material.GREEN_WOOL,
                ColorUtils.colorize("&a&l" +
                        module.getContext().getMessage("module.announcement.list.create",
                                "&a&l+ 发布新公告")),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.list.create-hint",
                                "&7点击创建新的工会公告")));
    }

    private void setupPagination(Inventory inv) {
        int totalPages = getTotalPages();

        // 上一页 (slot 45)
        if (currentPage > 1) {
            inv.setItem(45, createItem(Material.ARROW,
                    ColorUtils.colorize("&e&l" +
                            module.getContext().getMessage("gui.previous-page", "&e&l上一页")),
                    ColorUtils.colorize("&7" +
                            module.getContext().getMessage("gui.previous-page-hint",
                                    "&7点击返回上一页"))));
        }

        // 下一页 (slot 53)
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
}
