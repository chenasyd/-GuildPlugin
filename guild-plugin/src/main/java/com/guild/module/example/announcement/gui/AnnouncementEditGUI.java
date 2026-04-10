package com.guild.module.example.announcement.gui;

import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.module.example.announcement.Announcement;
import com.guild.module.example.announcement.AnnouncementModule;
import com.guild.core.language.LanguageManager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 公告创建/编辑GUI
 * <p>
 * 布局 (54槽)：
 * <ul>
 *   <li>Row 1: 边框</li>
 *   <li>Row 2: 标题输入区域 (10-16)</li>
 *   <li>Row 3: 内容输入区域 (19-25)</li>
 *   <li>Row 4: 预览区域 (28-34)</li>
 *   <li>Row 5: 操作按钮 - 保存/取消</li>
 *   <li>Row 6: 边框 + 返回(49)</li>
 * </ul>
 */
public class AnnouncementEditGUI implements GUI {

    private final AnnouncementModule module;
    private final Guild guild;
    private final Player player;
    private Inventory inventory;

    /** 编辑模式：null 表示新建，非 null 表示编辑现有公告 */
    private final String editingId;

    /** 当前正在编辑的标题（临时存储）*/
    private String currentTitle = "";
    /** 当前正在编辑的内容（临时存储）*/
    private String currentContent = "";

    public AnnouncementEditGUI(AnnouncementModule module, Guild guild, String announcementId) {
        this.module = module;
        this.guild = guild;
        this.player = null; // 必须使用带 Player 参数的构造函数
        this.editingId = announcementId;

        if (announcementId != null) {
            Announcement ann = module.getAnnouncementManager().getById(announcementId);
            if (ann != null) {
                this.currentTitle = ann.getTitle();
                this.currentContent = ann.getContent();
            }
        }
    }

    public AnnouncementEditGUI(AnnouncementModule module, Guild guild,
                               String announcementId, Player player,
                               String currentTitle, String currentContent) {
        this.module = module;
        this.guild = guild;
        this.player = player;
        this.editingId = announcementId;
        this.currentTitle = currentTitle != null ? currentTitle : "";
        this.currentContent = currentContent != null ? currentContent : "";
    }

    public AnnouncementEditGUI(AnnouncementModule module, Guild guild,
                               String announcementId, Player player) {
        this.module = module;
        this.guild = guild;
        this.player = player;
        this.editingId = announcementId;

        if (announcementId != null) {
            Announcement ann = module.getAnnouncementManager().getById(announcementId);
            if (ann != null) {
                this.currentTitle = ann.getTitle();
                this.currentContent = ann.getContent();
            }
        }
    }

    @Override
    public String getTitle() {
        if (editingId == null) {
            return ColorUtils.colorize(
                    module.getContext().getMessage("module.announcement.edit.title-create",
                            "&e&l发布新公告"));
        } else {
            return ColorUtils.colorize(
                    module.getContext().getMessage("module.announcement.edit.title-edit",
                            "&e&l编辑公告"));
        }
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;
        fillBorder(inventory);

        // 标题区域
        ItemStack titleItem = createItem(Material.NAME_TAG,
                ColorUtils.colorize("&6" +
                        module.getContext().getMessage("module.announcement.edit.field-title",
                                "&6标题")),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.edit.field-title-hint",
                                "&7点击修改公告标题")),
                ColorUtils.colorize("&f" +
                        (currentTitle.isEmpty()
                                ? module.getContext().getMessage(
                                        "module.announcement.edit.empty",
                                        "&7未设置")
                                : currentTitle)));
        inventory.setItem(13, titleItem);

        // 内容区域
        String contentPreview = currentContent.isEmpty()
                ? module.getContext().getMessage("module.announcement.edit.empty", "&7未设置")
                : (currentContent.length() > 40 ? currentContent.substring(0, 40) + "..." : currentContent);
        ItemStack contentItem = createItem(Material.WRITABLE_BOOK,
                ColorUtils.colorize("&6" +
                        module.getContext().getMessage("module.announcement.edit.field-content",
                                "&6内容")),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.edit.field-content-hint",
                                "&7点击修改公告内容")),
                ColorUtils.colorize("&f" + contentPreview));
        inventory.setItem(22, contentItem);

        // 预览区域
        ItemStack previewItem = createPreviewItem();
        inventory.setItem(31, previewItem);

        // 操作按钮
        // 保存按钮 (slot 29)
        ItemStack saveButton = createItem(Material.LIME_WOOL,
                ColorUtils.colorize("&a&l" +
                        module.getContext().getMessage("module.announcement.edit.save",
                                "&a&l保存公告")),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.edit.save-hint",
                                "&7左键保存当前内容")));
        inventory.setItem(29, saveButton);

        // 取消按钮 (slot 33)
        ItemStack cancelButton = createItem(Material.RED_WOOL,
                ColorUtils.colorize("&c&l" +
                        module.getContext().getMessage("module.announcement.edit.cancel",
                                "&c&l取消")),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.edit.cancel-hint",
                                "&7放弃修改并返回")));
        inventory.setItem(33, cancelButton);

        // 返回按钮 (slot 49)
        ItemStack backButton = createItem(Material.ARROW,
                ColorUtils.colorize("&c" +
                        module.getContext().getMessage("module.announcement.edit.back",
                                "&c返回列表")),
                ColorUtils.colorize("&7" +
                        module.getContext().getMessage("module.announcement.edit.back-hint",
                                "&7返回公告列表")));
        inventory.setItem(49, backButton);

        fillInteriorSlots(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 13:
                // 点击标题 -> 进入聊天输入模式
                startChatInput("title");
                break;
            case 22:
                // 点击内容 -> 进入聊天输入模式
                startChatInput("content");
                break;
            case 29:
                // 保存
                handleSave(player);
                break;
            case 33:
                // 取消
                handleCancel(player);
                break;
            case 49:
                // 返回列表
                module.getContext().navigateBack(player);
                break;
        }
    }

    @Override
    public void onClose(Player player) {}

    @Override
    public void refresh(Player player) {
        setupInventory(inventory);
    }

    // ==================== 业务逻辑 ====================

    /**
     * 启动聊天输入模式
     *
     * @param field "title" 或 "content"
     */
    private void startChatInput(String field) {
        LanguageManager lang = module.getContext().getLanguageManager();
        String prompt;
        if ("title".equals(field)) {
            prompt = lang.getMessage(player,
                    "module.announcement.edit.input-title-prompt",
                    "&e请在聊天中输入公告标题（输入 &ccancel &e取消）：");
        } else {
            prompt = lang.getMessage(player,
                    "module.announcement.edit.input-content-prompt",
                    "&e请在聊天中输入公告内容（支持多行，输入 &csave &e保存，输入 &ccancel &e取消）：");
        }

        // 先关闭GUI，再注册输入模式
        module.getContext().getGuiManager().closeGUI(player);
        module.getContext().getGuiManager().setInputMode(player, input -> {
            String trimmed = input.trim();

            if ("cancel".equalsIgnoreCase(trimmed)) {
                player.sendMessage(ColorUtils.colorize("&e" +
                        lang.getMessage(player,
                                "module.announcement.edit.input-cancelled",
                                "&e已取消输入")));
                module.getContext().navigateBack(player);
                return true;
            }

            if ("title".equals(field)) {
                currentTitle = trimmed;
                player.sendMessage(ColorUtils.colorize("&a" +
                        lang.getIndexedMessage(player,
                                "module.announcement.edit.title-set",
                                "&a标题已设置为: {0}", trimmed)));
                module.getContext().getGuiManager().openGUI(player,
                        new AnnouncementEditGUI(module, guild, editingId, player,
                                currentTitle, currentContent));
                return true;
            } else {
                if ("save".equalsIgnoreCase(trimmed)) {
                    player.sendMessage(ColorUtils.colorize("&a" +
                            lang.getMessage(player,
                                    "module.announcement.edit.content-set",
                                    "&a内容已保存")));
                    module.getContext().getGuiManager().openGUI(player,
                            new AnnouncementEditGUI(module, guild, editingId, player,
                                    currentTitle, currentContent));
                    return true;
                }
                if (!currentContent.isEmpty()) {
                    currentContent += "\n" + trimmed;
                } else {
                    currentContent = trimmed;
                }
                player.sendMessage(ColorUtils.colorize("&a" +
                        lang.getIndexedMessage(player,
                                "module.announcement.edit.content-updated",
                                "&a内容已更新（已追加 {0} 行）",
                                String.valueOf(currentContent.split("\n").length))));
                return false;
            }
        });

        // 注册完毕后发送提示
        player.sendMessage(ColorUtils.colorize(prompt));
    }

    private void handleSave(Player player) {
        LanguageManager lang = module.getContext().getLanguageManager();

        if (currentTitle.isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&c" +
                    lang.getMessage(player,
                            "module.announcement.edit.error-empty-title",
                            "&c公告标题不能为空！请先填写标题。")));
            return;
        }
        if (currentContent.isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&c" +
                    lang.getMessage(player,
                            "module.announcement.edit.error-empty-content",
                            "&c公告内容不能为空！请先填写内容。")));
            return;
        }

        boolean success;
        if (editingId == null) {
            // 创建新公告
            Announcement created = module.getAnnouncementManager().create(
                    guild.getId(),
                    player.getUniqueId(),
                    player.getName(),
                    currentTitle,
                    currentContent
            );
            success = (created != null);
            if (success) {
                player.sendMessage(ColorUtils.colorize("&a" +
                        lang.getIndexedMessage(player,
                                "module.announce.create.success",
                                "&a公告发布成功: {0}", currentTitle)));
            } else {
                player.sendMessage(ColorUtils.colorize("&c" +
                        lang.getIndexedMessage(player,
                                "module.announcement.create.max-reached",
                                "&c已达到最大公告数量限制({0}条)", "10")));
            }
        } else {
            // 更新现有公告
            success = module.getAnnouncementManager().update(
                    editingId, currentTitle, currentContent);
            if (success) {
                player.sendMessage(ColorUtils.colorize("&a" +
                        lang.getIndexedMessage(player,
                                "module.announcement.update.success",
                                "&a公告更新成功: {0}", currentTitle)));
            } else {
                player.sendMessage(ColorUtils.colorize("&c" +
                        lang.getMessage(player,
                                "module.announcement.update.failed",
                                "&c公告更新失败")));
            }
        }

        if (success) {
            // 返回列表
            module.getContext().navigateBack(player);
        }
    }

    private void handleCancel(Player player) {
        LanguageManager lang = module.getContext().getLanguageManager();
        player.sendMessage(ColorUtils.colorize("&e" +
                lang.getMessage(player,
                        "module.announcement.edit.cancel-msg",
                        "&e已取消编辑")));
        module.getContext().navigateBack(player);
    }

    // ==================== UI组件 ====================

    private ItemStack createPreviewItem() {
        List<String> lore = new ArrayList<>();
        if (!currentTitle.isEmpty()) {
            lore.add(ColorUtils.colorize("&f&l" + currentTitle));
        }
        if (!currentContent.isEmpty()) {
            lore.add("");
            String[] lines = currentContent.split("\n");
            for (String line : lines) {
                if (line.length() > 35) {
                    lore.add(ColorUtils.colorize("&7" + line.substring(0, 35) + "..."));
                } else {
                    lore.add(ColorUtils.colorize("&7" + line));
                }
            }
        } else {
            lore.add(ColorUtils.colorize("&7&o" +
                    module.getContext().getMessage("module.announcement.edit.no-preview",
                            "&7&o预览将在此处显示...")));
        }

        return createItem(Material.MAP,
                ColorUtils.colorize("&6" +
                        module.getContext().getMessage("module.announcement.edit.preview",
                                "&6预览")),
                lore.toArray(new String[0]));
    }

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
