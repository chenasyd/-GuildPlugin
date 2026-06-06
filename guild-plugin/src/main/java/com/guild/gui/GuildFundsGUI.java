package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildContribution;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 工会资金 GUI — 展示每名成员在工会中的存款总额
 * <p>
 * 布局：9×6，边框 BLACK_STAINED_GLASS_PANE
 * 内容区 4×7 (slot 10~43)，每页最多 28 条
 * 分页：48←上一页  49→返回  50→下一页  51→刷新
 */
public class GuildFundsGUI implements GUI {

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;
    private final int page;
    private final int itemsPerPage = 28;
    private List<GuildContribution> totals;
    private int totalPlayers;

    public GuildFundsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this(plugin, guild, player, 0);
    }

    public GuildFundsGUI(GuildPlugin plugin, Guild guild, Player player, int page) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.page = page;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(
                languageManager.getMessage(player, "guild-funds.title",
                        "&6工会资金 - {guild}", "{guild}", guild.getName()));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);

        loadDataAsync().thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    setupContentItems(inventory);
                    setupNavigation(inventory);
                    setupPageInfo(inventory);
                } else {
                    ItemStack error = createItem(Material.BARRIER,
                            ColorUtils.colorize("&c" + languageManager.getMessage(player,
                                    "guild-funds.load-failed", "加载失败")),
                            ColorUtils.colorize("&7" + languageManager.getMessage(player,
                                    "guild-funds.load-error", "无法加载资金数据，请重试")));
                    inventory.setItem(22, error);
                    setupBasicNav(inventory);
                }
            });
        });
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        // 返回 (slot 49)
        if (slot == 49) {
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
            return;
        }

        // 上一页 (slot 48)
        if (slot == 48 && page > 0) {
            plugin.getGuiManager().openGUI(player,
                    new GuildFundsGUI(plugin, guild, player, page - 1));
            return;
        }

        // 下一页 (slot 50)
        if (slot == 50 && (page + 1) * itemsPerPage < totalPlayers) {
            plugin.getGuiManager().openGUI(player,
                    new GuildFundsGUI(plugin, guild, player, page + 1));
            return;
        }

        // 刷新 (slot 51)
        if (slot == 51) {
            plugin.getGuiManager().openGUI(player,
                    new GuildFundsGUI(plugin, guild, player, page));
            return;
        }

        // 内容区点击 — 展示该玩家的详细记录
        if (slot >= 10 && slot <= 43) {
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIdx = (row - 1) * 7 + (col - 1);
                int totalIdx = (page * itemsPerPage) + relativeIdx;
                if (totals != null && totalIdx < totals.size()) {
                    GuildContribution c = totals.get(totalIdx);
                    showPlayerDetails(player, c);
                }
            }
        }
    }

    // ==================== 数据加载 ====================

    private CompletableFuture<Boolean> loadDataAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                totals = plugin.getGuildService()
                        .getGuildContributionTotalsAsync(guild.getId()).get();
                totalPlayers = totals != null ? totals.size() : 0;
                if (totals == null) totals = new ArrayList<>();
                return true;
            } catch (Exception e) {
                plugin.getLogger().warning("加载工会资金数据失败: " + e.getMessage());
                totals = new ArrayList<>();
                totalPlayers = 0;
                return false;
            }
        });
    }

    // ==================== UI 渲染 ====================

    private void setupContentItems(Inventory inventory) {
        if (totals.isEmpty()) {
            ItemStack empty = createItem(Material.BARRIER,
                    ColorUtils.colorize("&c" + languageManager.getMessage(player,
                            "guild-funds.no-data", "暂无存款记录")),
                    ColorUtils.colorize("&7" + languageManager.getMessage(player,
                            "guild-funds.no-data-desc", "工会成员还没有存入资金")));
            inventory.setItem(22, empty);
            return;
        }

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totals.size());

        for (int i = startIndex; i < endIndex; i++) {
            GuildContribution entry = totals.get(i);
            int relativeIdx = i - startIndex;
            int slot = getContentSlot(relativeIdx);
            inventory.setItem(slot, createContributionItem(entry));
        }
    }

    private ItemStack createContributionItem(GuildContribution entry) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        String formattedAmount = formatAmount(entry.getAmount());
        String playerName = entry.getPlayerName();
        int rank = getPlayerRank(entry.getPlayerUuid());
        boolean isOnline = Bukkit.getPlayer(entry.getPlayerUuid()) != null;

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(
                "&7" + languageManager.getMessage(player, "guild-funds.deposit-total", "存款总额")
                        + ": &a" + formattedAmount));
        lore.add(ColorUtils.colorize(
                "&7" + languageManager.getMessage(player, "guild-funds.rank", "排名")
                        + ": &e#" + rank));
        lore.add(ColorUtils.colorize(
                "&7" + languageManager.getMessage(player, "guild-funds.status", "状态")
                        + ": " + (isOnline ? "&a" + languageManager.getMessage(player,
                                "guild-funds.online", "在线")
                                : "&7" + languageManager.getMessage(player,
                                        "guild-funds.offline", "离线"))));
        lore.add("");
        lore.add(ColorUtils.colorize(
                "&a" + languageManager.getMessage(player, "guild-funds.click-details",
                        "点击查看详细记录")));

        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getPlayerUuid()));
            meta.setDisplayName(ColorUtils.colorize("&e" + playerName));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }

        return head;
    }

    // ==================== 导航按钮 ====================

    private void setupNavigation(Inventory inventory) {
        if (page > 0) {
            inventory.setItem(48, createItem(Material.ARROW,
                    ColorUtils.colorize("&e" + languageManager.getMessage(player,
                            "gui.previous-page", "上一页")),
                    ColorUtils.colorize("&7" + languageManager.getMessage(player,
                            "gui.view-previous", "查看上一页"))));
        }

        inventory.setItem(49, createItem(Material.ARROW,
                ColorUtils.colorize("&c" + languageManager.getMessage(player,
                        "gui.back", "返回")),
                ColorUtils.colorize("&7" + languageManager.getMessage(player,
                        "guild-funds.back-to-settings", "返回工会设置"))));

        if ((page + 1) * itemsPerPage < totalPlayers) {
            inventory.setItem(50, createItem(Material.ARROW,
                    ColorUtils.colorize("&a" + languageManager.getMessage(player,
                            "gui.next-page", "下一页")),
                    ColorUtils.colorize("&7" + languageManager.getMessage(player,
                            "gui.view-next", "查看下一页"))));
        }

        inventory.setItem(51, createItem(Material.EMERALD,
                ColorUtils.colorize("&a" + languageManager.getMessage(player,
                        "guild-funds.refresh", "刷新")),
                ColorUtils.colorize("&7" + languageManager.getMessage(player,
                        "guild-funds.refresh-desc", "刷新资金数据"))));
    }

    private void setupBasicNav(Inventory inventory) {
        inventory.setItem(49, createItem(Material.ARROW,
                ColorUtils.colorize("&c" + languageManager.getMessage(player,
                        "gui.back", "返回")),
                ColorUtils.colorize("&7" + languageManager.getMessage(player,
                        "guild-funds.back-to-settings", "返回工会设置"))));
    }

    private void setupPageInfo(Inventory inventory) {
        int totalPages = (totalPlayers - 1) / itemsPerPage + 1;
        if (totalPages < 1) totalPages = 1;
        inventory.setItem(46, createItem(Material.PAPER,
                ColorUtils.colorize("&e" + languageManager.getMessage(player,
                        "guild-funds.page-info", "页码信息")),
                ColorUtils.colorize("&7" + languageManager.getMessage(player,
                        "guild-funds.current-page", "当前页: {page}",
                        "{page}", String.valueOf(page + 1))),
                ColorUtils.colorize("&7" + languageManager.getMessage(player,
                        "guild-funds.total-pages", "总页数: {total}",
                        "{total}", String.valueOf(totalPages))),
                ColorUtils.colorize("&7" + languageManager.getMessage(player,
                        "guild-funds.total-players", "人数: {count}",
                        "{count}", String.valueOf(totalPlayers)))));
    }

    // ==================== 工具方法 ====================

    private int getContentSlot(int index) {
        int row = index / 7;
        int col = index % 7;
        return (row + 1) * 9 + (col + 1);
    }

    private int getPlayerRank(UUID playerUuid) {
        if (totals == null) return 0;
        for (int i = 0; i < totals.size(); i++) {
            if (totals.get(i).getPlayerUuid().equals(playerUuid)) {
                return i + 1;
            }
        }
        return totals.size();
    }

    private void showPlayerDetails(Player player, GuildContribution entry) {
        player.sendMessage(ColorUtils.colorize(
                "&6=== " + languageManager.getMessage(player,
                        "guild-funds.details-header", "存款详情") + " ==="));
        player.sendMessage(ColorUtils.colorize(
                "&7" + languageManager.getMessage(player, "guild-funds.player", "玩家")
                        + ": &f" + entry.getPlayerName()));
        player.sendMessage(ColorUtils.colorize(
                "&7" + languageManager.getMessage(player, "guild-funds.total-deposit", "总存款")
                        + ": &a" + formatAmount(entry.getAmount())));

        // 异步查询该玩家的详细记录
        plugin.getGuildService().getPlayerContributionsAsync(entry.getPlayerUuid())
                .thenAccept(records -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        player.sendMessage(ColorUtils.colorize(
                                "&7" + languageManager.getMessage(player,
                                        "guild-funds.deposit-count", "存款次数")
                                        + ": &f" + records.size() + " 次"));
                        player.sendMessage(ColorUtils.colorize("&6=================="));
                    });
                });
    }

    private String formatAmount(double amount) {
        return String.format("%.0f", amount);
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
