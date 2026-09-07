package com.guild.module.example.announcement.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.gui.base.AbstractPagedListGUI;
import com.guild.gui.base.GuiLayoutUtils;
import com.guild.models.Guild;
import com.guild.module.example.announcement.Announcement;
import com.guild.module.example.announcement.AnnouncementModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 公会公告列表（分页），基于 {@link AbstractPagedListGUI}。
 */
public class AnnouncementListGUI extends AbstractPagedListGUI<Announcement> {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final int SLOT_CREATE = 40;

    private final AnnouncementModule module;
    private final Guild guild;

    public AnnouncementListGUI(AnnouncementModule module, Guild guild, Player player) {
        super(module.getContext().getPlugin(), player, PaginationLayout.BOTTOM, GuiLayoutUtils.ITEMS_PER_PAGE);
        this.module = module;
        this.guild = guild;
        reloadEntries();
    }

    @Override
    public String getTitle() {
        String baseTitle = ColorUtils.colorize(
                module.getContext().getMessage("module.announcement.list.title",
                        "&e&l公会公告管理"));
        int totalPages = maxPageIndex() + 1;
        if (totalPages > 1) {
            baseTitle += ColorUtils.colorize(" &7(" +
                    module.getContext().getLanguageManager().getGuiMessage(viewer,
                            "gui.page-info", "第{0}页/共{1}页",
                            String.valueOf(currentPage + 1), String.valueOf(totalPages)) +
                    ")");
        }
        return baseTitle;
    }

    @Override
    protected String backTitleKey() {
        return "module.announcement.list.back";
    }

    @Override
    protected String backTitleDefault() {
        return "&c返回";
    }

    @Override
    protected String backLoreKey() {
        return "module.announcement.list.back-hint";
    }

    @Override
    protected String backLoreDefault() {
        return "&7点击返回公会设置";
    }

    @Override
    protected ItemStack createEntryItem(Announcement ann) {
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
            mat = Material.valueOf("OAK_SIGN");
        } catch (IllegalArgumentException ignored) {
            // keep PAPER
        }

        return createItem(mat,
                ColorUtils.colorize("&6" + ann.getTitle()),
                lore.toArray(new String[0]));
    }

    @Override
    protected void onEntryClick(Player player, Announcement ann, ClickType clickType) {
        if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_LEFT) {
            handleDelete(player, ann);
        } else {
            module.openEditGUI(player, guild, ann.getId());
        }
    }

    @Override
    protected void openBackGui(Player player) {
        module.getContext().navigateBack(player);
    }

    @Override
    protected void setupToolbar(Inventory inventory) {
        if (hasManagePermission()) {
            inventory.setItem(SLOT_CREATE, createCreateButton());
        }
    }

    @Override
    protected boolean handleToolbarClick(Player player, int slot, ClickType clickType) {
        if (slot == SLOT_CREATE && hasManagePermission()) {
            module.openCreateGUI(player, guild);
            return true;
        }
        return false;
    }

    @Override
    public void refresh(Player player) {
        reloadEntries();
        plugin.getGuiManager().refreshGUI(player);
    }

    private void reloadEntries() {
        setEntries(module.getAnnouncementManager().getAnnouncements(guild.getId()));
    }

    private void handleDelete(Player player, Announcement ann) {
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

    private boolean hasManagePermission() {
        return module.hasManagePermission(viewer);
    }

    private ItemStack createCreateButton() {
        return createItem(Material.GREEN_WOOL,
                ColorUtils.colorize("&a&l" +
                        module.getContext().getMessage("module.announcement.list.create",
                                "&a&l+ 发布新公告")),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.list.create-hint",
                                "&7点击创建新的公会公告")));
    }
}
