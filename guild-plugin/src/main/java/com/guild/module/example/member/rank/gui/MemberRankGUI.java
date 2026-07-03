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
 * Member Contribution Ranking GUI.
 * <p>
 * Displays the guild's member contribution leaderboard.
 * Admins can left-click to add contributions, right-click to subtract.
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
                "module.member-rank.gui.title", "&6&lMember A-Coin Ranking"));
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
            // Empty list hint
            inventory.setItem(4, createItem(Material.BOOK,
                    "&6&lMember A-Coin Ranking",
                    "",
                    "&7A-Coins in this module are managed independently",
                    "&8  (separate from Data Statistics module's B-Coins)",
                    "",
                    "&7Data source: &eMember Ranking module (read/write)",
                    "&7Update mode: &aOnline auto-award / admin manual adjustment",
                    "",
                    "&cNo member A-Coin records yet"));
            inventory.setItem(22, createItem(Material.BARRIER,
                    module.getContext().getMessage("module.member-rank.gui.empty", "&7No ranking data"),
                    module.getContext().getMessage("module.member-rank.gui.empty-hint", "&7No member A-Coin records yet")));
        } else {
            inventory.setItem(4, createItem(Material.BOOK,
                    "&6&lMember A-Coin Ranking",
                    "",
                    "&7A-Coins in this module are managed independently",
                    "&8  (separate from Data Statistics module's B-Coins)",
                    "",
                    "&7Data source: &eMember Ranking module (read/write)",
                    "&7Update mode: &aOnline auto-award / admin manual adjustment"));

            for (int i = fromIndex; i < toIndex; i++) {
                MemberRank rank = allRanks.get(i);
                int displayIndex = i + 1;
                int slot = mapToSlot(i - fromIndex);
                if (slot < 0) continue;

                ItemStack item = buildRankItem(rank, displayIndex);
                inventory.setItem(slot, item);
            }
        }

        // Pagination buttons
        setupPagination(inventory, currentPage, totalPages,
                module.getContext().getMessage("gui.previous-page", "&e&lPrevious"),
                module.getContext().getLanguageManager().getGuiMessage(player,
                        "gui.next-page", "&e&lNext"));

        // Back button
        inventory.setItem(49, createBackButton(
                module.getContext().getMessage("module.member-rank.gui.back", "&cBack"),
                module.getContext().getMessage("module.member-rank.gui.back-hint", "&7Click to return")));

        // Page info
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

        // Back button
        if (slot == 49) {
            module.getContext().navigateBack(player);
            return;
        }

        // Pagination
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

        // Admin actions: left-click +10 A-Coins, right-click -10 A-Coins
        if (managementEnabled && module.hasManagePermission(player) && slot >= CONTENT_START && slot <= CONTENT_END) {
            int col = slot % 9;
            if (col == 0 || col == 8) return; // Border

            int linearIndex = slotToLinearIndex(slot);
            if (linearIndex < 0) return;

            int fromIndex = (currentPage - 1) * PER_PAGE;
            int realIndex = fromIndex + linearIndex;
            if (realIndex >= allRanks.size()) return;

            MemberRank rank = allRanks.get(realIndex);

            if (clickType == ClickType.LEFT) {
                // Left-click: +10 A-Coins
                module.getRankManager().addACoin(guild.getId(),
                        rank.getPlayerUuid(), rank.getPlayerName(), 10);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                String msg = module.getContext().getMessage("module.member-rank.gui.add-success",
                        rank.getPlayerName());
                player.sendMessage(ColorUtils.colorize(msg));
            } else if (clickType == ClickType.RIGHT) {
                // Right-click: -10 A-Coins
                module.getRankManager().reduceACoin(guild.getId(),
                        rank.getPlayerUuid(), rank.getPlayerName(), 10);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                String msg = module.getContext().getMessage("module.member-rank.gui.reduce-success",
                        rank.getPlayerName());
                player.sendMessage(ColorUtils.colorize(msg));
            } else if (clickType == ClickType.SHIFT_LEFT) {
                // Shift+Left-click: +100 A-Coins
                module.getRankManager().addACoin(guild.getId(),
                        rank.getPlayerUuid(), rank.getPlayerName(), 100);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
                String msg = module.getContext().getMessage("module.member-rank.gui.add-bulk-success",
                        rank.getPlayerName());
                player.sendMessage(ColorUtils.colorize(msg));
            } else if (clickType == ClickType.SHIFT_RIGHT) {
                // Shift+Right-click: -100 A-Coins
                module.getRankManager().reduceACoin(guild.getId(),
                        rank.getPlayerUuid(), rank.getPlayerName(), 100);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                String msg = module.getContext().getMessage("module.member-rank.gui.reduce-bulk-success",
                        rank.getPlayerName());
                player.sendMessage(ColorUtils.colorize(msg));
            }

            // Refresh GUI
            setupInventory(inventory);
        }
    }

    // ==================== Private Methods ====================

    /**
     * Convert GUI slot back to linear index
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
     * Build ranking display item
     */
    private ItemStack buildRankItem(MemberRank rank, int displayIndex) {
        Material material;
        String prefix;

        // Top 3 use special materials
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

        // Admin sees operation hints
        String manageHint = "";
        if (managementEnabled && module.hasManagePermission(viewer)) {
            manageHint = module.getContext().getMessage("module.member-rank.gui.manage-hint",
                    "&8L+10 | R-10 | Shift+L+100 | Shift+R-100");
        }

        String dataSourceHint = module.getContext().getMessage(
                "module.member-rank.gui.data-source",
                "&8| Data source: this module manages independently (non-core)");

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
