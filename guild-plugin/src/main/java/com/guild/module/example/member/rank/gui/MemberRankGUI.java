package com.guild.module.example.member.rank.gui;

import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;
import com.guild.module.example.member.rank.MemberRank;
import com.guild.module.example.member.rank.MemberRankModule;
import com.guild.sdk.gui.AbstractModuleGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 成员贡献排名 GUI
 * <p>
 * 展示工会所有成员的贡献值排行榜。
 * 管理员可以左键点击成员增加贡献，右键点击减少贡献。
 */
public class MemberRankGUI extends AbstractModuleGUI {

    private final MemberRankModule module;
    private final Guild guild;
    private final Player viewer;
    private final boolean managementEnabled;
    private int currentPage = 1;

    public MemberRankGUI(MemberRankModule module, Guild guild, Player viewer, boolean managementEnabled) {
        this.module = module;
        this.guild = guild;
        this.viewer = viewer;
        this.managementEnabled = managementEnabled;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(module.getContext().getMessage(
                "module.member-rank.gui.title", "&6&l成员A币排名"));
    }

    @Override
    public void setupInventory(Inventory inventory) {
        this.inventory = inventory;
        inventory.clear();
        fillBorder(inventory);

        List<MemberRank> allRanks = module.getRankManager().getRanks(guild.getId());
        int totalPages = getTotalPages(allRanks.size());
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * PER_PAGE;
        int toIndex = Math.min(fromIndex + PER_PAGE, allRanks.size());

        if (allRanks.isEmpty()) {
            // 空列表提示
            inventory.setItem(4, createItem(Material.BOOK,
                    "&6&l成员A币排名",
                    "",
                    "&7此模块的A币由本模块独立管理",
                    "&8  与「数据统计」模块的B币相互独立",
                    "",
                    "&7数据来源: &e成员排名模块 (可读写)",
                    "&7更新方式: &a在线自动发放 / 管理员手动调整",
                    "",
                    "&c暂无成员A币记录"));
            inventory.setItem(22, createItem(Material.BARRIER,
                    module.getContext().getMessage("module.member-rank.gui.empty", "&7暂无排名数据"),
                    module.getContext().getMessage("module.member-rank.gui.empty-hint", "&7暂无成员A币记录")));
        } else {
            inventory.setItem(4, createItem(Material.BOOK,
                    "&6&l成员A币排名",
                    "",
                    "&7此模块的A币由本模块独立管理",
                    "&8  与「数据统计」模块的B币相互独立",
                    "",
                    "&7数据来源: &e成员排名模块 (可读写)",
                    "&7更新方式: &a在线自动发放 / 管理员手动调整"));

            for (int i = fromIndex; i < toIndex; i++) {
                MemberRank rank = allRanks.get(i);
                int displayIndex = i + 1;
                int slot = mapToSlot(i - fromIndex);
                if (slot < 0) continue;

                ItemStack item = buildRankItem(rank, displayIndex);
                inventory.setItem(slot, item);
            }
        }

        // 翻页按钮
        setupPagination(inventory, currentPage, totalPages,
                module.getContext().getMessage("gui.previous-page", "&e&l上一页"),
                module.getContext().getMessage("gui.next-page", "&e&l下一页"));

        // 返回按钮
        inventory.setItem(49, createBackButton(
                module.getContext().getMessage("module.member-rank.gui.back", "&c返回"),
                module.getContext().getMessage("module.member-rank.gui.back-hint", "&7点击返回上一页")));

        // 页码信息
        if (!allRanks.isEmpty()) {
            String pageInfo = module.getContext().getMessage("gui.page-info",
                    String.valueOf(currentPage), String.valueOf(totalPages));
            inventory.setItem(48, createItem(Material.PAPER, pageInfo));
        }

        fillInteriorSlots(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // 返回按钮
        if (slot == 49) {
            module.getContext().navigateBack(player);
            return;
        }

        // 翻页
        List<MemberRank> allRanks = module.getRankManager().getRanks(guild.getId());
        int totalPages = getTotalPages(allRanks.size());

        if (slot == 45 && currentPage > 1) {
            currentPage--;
            setupInventory(inventory);
            return;
        }
        if (slot == 53 && currentPage < totalPages) {
            currentPage++;
            setupInventory(inventory);
            return;
        }

        // 管理员操作：左键 +10 贡献，右键 -10 贡献
        if (managementEnabled && module.hasManagePermission(player) && slot >= CONTENT_START && slot <= CONTENT_END) {
            int col = slot % 9;
            if (col == 0 || col == 8) return; // 边框

            int linearIndex = slotToLinearIndex(slot);
            if (linearIndex < 0) return;

            int fromIndex = (currentPage - 1) * PER_PAGE;
            int realIndex = fromIndex + linearIndex;
            if (realIndex >= allRanks.size()) return;

            MemberRank rank = allRanks.get(realIndex);

            if (clickType == ClickType.LEFT) {
                // 左键：增加A币
                module.getRankManager().addACoin(guild.getId(),
                        rank.getPlayerUuid(), rank.getPlayerName(), 10);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                String msg = module.getContext().getMessage("module.member-rank.gui.add-success",
                        rank.getPlayerName());
                player.sendMessage(ColorUtils.colorize(msg));
            } else if (clickType == ClickType.RIGHT) {
                // 右键：减少A币
                module.getRankManager().reduceACoin(guild.getId(),
                        rank.getPlayerUuid(), rank.getPlayerName(), 10);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                String msg = module.getContext().getMessage("module.member-rank.gui.reduce-success",
                        rank.getPlayerName());
                player.sendMessage(ColorUtils.colorize(msg));
            } else if (clickType == ClickType.SHIFT_LEFT) {
                // Shift+左键：增加 100
                module.getRankManager().addACoin(guild.getId(),
                        rank.getPlayerUuid(), rank.getPlayerName(), 100);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
                String msg = module.getContext().getMessage("module.member-rank.gui.add-bulk-success",
                        rank.getPlayerName());
                player.sendMessage(ColorUtils.colorize(msg));
            } else if (clickType == ClickType.SHIFT_RIGHT) {
                // Shift+右键：减少 100
                module.getRankManager().reduceACoin(guild.getId(),
                        rank.getPlayerUuid(), rank.getPlayerName(), 100);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                String msg = module.getContext().getMessage("module.member-rank.gui.reduce-bulk-success",
                        rank.getPlayerName());
                player.sendMessage(ColorUtils.colorize(msg));
            }

            // 刷新 GUI
            setupInventory(inventory);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 将 GUI 槽位号转换回线性索引
     */
    private int slotToLinearIndex(int slot) {
        if (slot < CONTENT_START || slot > CONTENT_END) return -1;
        int col = slot % 9;
        if (col == 0 || col == 8) return -1;
        int row = (slot - CONTENT_START) / 9;
        int innerCol = col - 1;
        return row * COLUMNS + innerCol;
    }

    /**
     * 构建排名物品
     */
    private ItemStack buildRankItem(MemberRank rank, int displayIndex) {
        Material material;
        String prefix;

        // 前三名使用特殊材质
        if (displayIndex == 1) {
            material = Material.GOLD_BLOCK;
            prefix = "&6&l[1] ";
        } else if (displayIndex == 2) {
            material = Material.IRON_BLOCK;
            prefix = "&f&l[2] ";
        } else if (displayIndex == 3) {
            material = Material.LAPIS_BLOCK;
            prefix = "&9&l[3] ";
        } else {
            material = Material.PLAYER_HEAD;
            prefix = "&7[" + displayIndex + "] ";
        }

        boolean isOnline = Bukkit.getPlayer(rank.getPlayerUuid()) != null;
        String onlineTag = isOnline
                ? "&a●"
                : "&7●";

        String name = ColorUtils.colorize(prefix + onlineTag + " &f" + rank.getPlayerName());

        String contribText = module.getContext().getMessage("module.member-rank.gui.contribution",
                String.valueOf(rank.getACoin()));

        String activeText = module.getContext().getMessage("module.member-rank.gui.last-active",
                rank.getLastActive() != null ? rank.getLastActive().toLocalDate().toString() : "-");

        // 管理员显示操作提示
        String manageHint = "";
        if (managementEnabled && module.hasManagePermission(viewer)) {
            manageHint = module.getContext().getMessage("module.member-rank.gui.manage-hint",
                    "&8左键+10 | 右键-10 | Shift+左键+100 | Shift+右键-100");
        }

        String dataSourceHint = module.getContext().getMessage(
                "module.member-rank.gui.data-source",
                "&8┃ 数据来源: 本模块独立管理 (非核心系统)");

        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(ColorUtils.colorize(contribText));
            lore.add(ColorUtils.colorize(activeText));
            lore.add(ColorUtils.colorize(dataSourceHint));
            if (!manageHint.isEmpty()) {
                lore.add(ColorUtils.colorize(manageHint));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
