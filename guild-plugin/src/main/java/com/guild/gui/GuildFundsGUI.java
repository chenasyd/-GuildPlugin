package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.language.LanguageManager;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;

import org.geysermc.cumulus.form.SimpleForm;
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

    // ── 图像模式功能常量 ──
    public static final String FUNC_PAGE_INFO = "PAGE_INFO";
    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_REFRESH = "REFRESH";
    public static final String FUNC_BACK = "BACK";

    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private final LanguageManager languageManager;
    private final int page;
    private final int itemsPerPage = 28;
    private final String sourceGuiType;
    private List<GuildContribution> totals;
    private int totalPlayers;

    public GuildFundsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this(plugin, guild, player, 0, "GuildSettingsGUI");
    }

    public GuildFundsGUI(GuildPlugin plugin, Guild guild, Player player, int page) {
        this(plugin, guild, player, page, "GuildSettingsGUI");
    }

    public GuildFundsGUI(GuildPlugin plugin, Guild guild, Player player, int page, String sourceGuiType) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.page = page;
        this.sourceGuiType = sourceGuiType != null ? sourceGuiType : "GuildSettingsGUI";
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(
                languageManager.getGuiMessage(player, "gui.guild-funds.title",
                        "&6工会资金 - {guild}", "{guild}", guild.getName()));
    }

    @Override
    public int getSize() {
        return 54;
    }

    // ==================== 基岩版表单 ====================

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;
        sendBedrockFundsForm(player, page);
        return true;
    }

    private void sendBedrockFundsForm(Player player, int pageNum) {
        loadDataAsync().thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (!player.isOnline()) return;

                String guildName = ColorUtils.stripColor(guild.getName());

                if (!success || totals == null || totals.isEmpty()) {
                    SimpleForm emptyForm = SimpleForm.builder()
                            .title(languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-title", "&6工会资金 - {guild}", "{guild}", guildName))
                            .content(languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-no-data", "&c暂无存款记录"))
                            .button(languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-back", "&c返回"))
                            .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () ->
                                    navigateBack(player)))
                            .build();
                    BedrockFormSender.sendForm(player.getUniqueId(), emptyForm);
                    return;
                }

                int totalPages = (totalPlayers - 1) / itemsPerPage + 1;
                final int safePage = Math.max(0, Math.min(pageNum, totalPages - 1));
                int startIndex = safePage * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, totals.size());

                String content = languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-page-info",
                        "&f第 {page}/{total} 页\n&f总人数: {count}",
                        "{page}", String.valueOf(safePage + 1),
                        "{total}", String.valueOf(totalPages),
                        "{count}", String.valueOf(totalPlayers));

                SimpleForm.Builder builder = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-title", "&6工会资金 - {guild}", "{guild}", guildName))
                        .content(content);

                List<GuildContribution> pageEntries = new ArrayList<>();
                for (int i = startIndex; i < endIndex; i++) {
                    GuildContribution entry = totals.get(i);
                    pageEntries.add(entry);
                    boolean isOnline = Bukkit.getPlayer(entry.getPlayerUuid()) != null;
                    String status = isOnline
                            ? languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-online", "&a在线")
                            : languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-offline", "&f离线");
                    builder.button("§e" + entry.getPlayerName()
                            + " §f- " + formatAmount(entry.getAmount()) + " " + status);
                }

                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-prev-page", "&a上一页"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-next-page", "&a下一页"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-refresh", "&a刷新"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-funds.bedrock-back", "&c返回"));

                final int entryCount = pageEntries.size();
                final int curPage = safePage;
                final int totPages = totalPages;

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int id = response.clickedButtonId();
                    if (id < entryCount) {
                        showPlayerDetails(player, pageEntries.get(id));
                        sendBedrockFundsForm(player, curPage);
                    } else if (id == entryCount) {
                        sendBedrockFundsForm(player, curPage > 0 ? curPage - 1 : curPage);
                    } else if (id == entryCount + 1) {
                        sendBedrockFundsForm(player, curPage < totPages - 1 ? curPage + 1 : curPage);
                    } else if (id == entryCount + 2) {
                        sendBedrockFundsForm(player, curPage);
                    } else {
                        navigateBack(player);
                    }
                }));

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

    // ==================== Java Inventory 布局 ====================

    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);

        loadDataAsync().thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (success) {
                    setupContentItems(inventory);
                    setupNavigation(inventory);
                    setupPageInfo(inventory);
                } else {
                    ItemStack error = createItem(Material.BARRIER,
                            ColorUtils.colorize("&c" + languageManager.getGuiMessage(player,
                                    "gui.guild-funds.load-failed", "加载失败")),
                            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                                    "gui.guild-funds.load-error", "无法加载资金数据，请重试")));
                    inventory.setItem(22, error);
                    setupBasicNav(inventory);
                }
                plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
            });
        });
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        // 返回 (slot 49)
        if (slot == 49) {
            navigateBack(player);
            return;
        }

        // 上一页 (slot 48)
        if (slot == 48 && page > 0) {
            plugin.getGuiManager().openGUI(player,
                    new GuildFundsGUI(plugin, guild, player, page - 1, sourceGuiType));
            return;
        }

        // 下一页 (slot 50)
        if (slot == 50 && (page + 1) * itemsPerPage < totalPlayers) {
            plugin.getGuiManager().openGUI(player,
                    new GuildFundsGUI(plugin, guild, player, page + 1, sourceGuiType));
            return;
        }

        // 刷新 (slot 51)
        if (slot == 51) {
            plugin.getGuiManager().openGUI(player,
                    new GuildFundsGUI(plugin, guild, player, page, sourceGuiType));
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
                    ColorUtils.colorize("&c" + languageManager.getGuiMessage(player,
                            "gui.guild-funds.no-data", "暂无存款记录")),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                            "gui.guild-funds.no-data-desc", "工会成员还没有存入资金")));
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
                "&7" + languageManager.getGuiMessage(player, "gui.guild-funds.deposit-total", "存款总额")
                        + ": &a" + formattedAmount));
        lore.add(ColorUtils.colorize(
                "&7" + languageManager.getGuiMessage(player, "gui.guild-funds.rank", "排名")
                        + ": &e#" + rank));
        lore.add(ColorUtils.colorize(
                "&7" + languageManager.getGuiMessage(player, "gui.guild-funds.status", "状态")
                        + ": " + (isOnline ? "&a" + languageManager.getGuiMessage(player,
                                "gui.guild-funds.online", "在线")
                                : "&7" + languageManager.getGuiMessage(player,
                                        "gui.guild-funds.offline", "离线"))));
        lore.add("");
        lore.add(ColorUtils.colorize(
                "&a" + languageManager.getGuiMessage(player, "gui.guild-funds.click-details",
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
                    ColorUtils.colorize("&e" + languageManager.getGuiMessage(player,
                            "gui.common.previous-page", "上一页")),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                            "gui.common.view-previous", "查看上一页"))));
        }

        inventory.setItem(49, createItem(Material.ARROW,
                ColorUtils.colorize("&c" + languageManager.getGuiMessage(player,
                        "gui.common.back", "返回")),
                ColorUtils.colorize("&7" + getBackLore())));

        if ((page + 1) * itemsPerPage < totalPlayers) {
            inventory.setItem(50, createItem(Material.ARROW,
                    ColorUtils.colorize("&a" + languageManager.getGuiMessage(player,
                            "gui.common.next-page", "下一页")),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                            "gui.common.view-next", "查看下一页"))));
        }

        inventory.setItem(51, createItem(Material.EMERALD,
                ColorUtils.colorize("&a" + languageManager.getGuiMessage(player,
                        "gui.guild-funds.refresh", "刷新")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                        "gui.guild-funds.refresh-desc", "刷新资金数据"))));
    }

    private void setupBasicNav(Inventory inventory) {
        inventory.setItem(49, createItem(Material.ARROW,
                ColorUtils.colorize("&c" + languageManager.getGuiMessage(player,
                        "gui.common.back", "返回")),
                ColorUtils.colorize("&7" + getBackLore())));
    }

    private void navigateBack(Player player) {
        if ("GuildInfoGUI".equals(sourceGuiType)) {
            plugin.getGuiManager().openGUI(player, new GuildInfoGUI(plugin, player, guild));
        } else {
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
        }
    }

    private String getBackLore() {
        if ("GuildInfoGUI".equals(sourceGuiType)) {
            return languageManager.getGuiMessage(player,
                    "gui.guild-funds.back-to-info", "返回工会信息");
        }
        return languageManager.getGuiMessage(player,
                "gui.guild-funds.back-to-settings", "返回工会设置");
    }

    private void setupPageInfo(Inventory inventory) {
        int totalPages = (totalPlayers - 1) / itemsPerPage + 1;
        if (totalPages < 1) totalPages = 1;
        inventory.setItem(46, createItem(Material.PAPER,
                ColorUtils.colorize("&e" + languageManager.getGuiMessage(player,
                        "gui.guild-funds.page-info", "页码信息")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                        "gui.guild-funds.current-page", "当前页: {page}",
                        "{page}", String.valueOf(page + 1))),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                        "gui.guild-funds.total-pages", "总页数: {total}",
                        "{total}", String.valueOf(totalPages))),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                        "gui.guild-funds.total-players", "人数: {count}",
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
                "&6=== " + languageManager.getGuiMessage(player,
                        "gui.guild-funds.details-header", "存款详情") + " ==="));
        player.sendMessage(ColorUtils.colorize(
                "&7" + languageManager.getGuiMessage(player, "gui.guild-funds.player", "玩家")
                        + ": &f" + entry.getPlayerName()));
        player.sendMessage(ColorUtils.colorize(
                "&7" + languageManager.getGuiMessage(player, "gui.guild-funds.total-deposit", "总存款")
                        + ": &a" + formatAmount(entry.getAmount())));

        // 异步查询该玩家的详细记录
        plugin.getGuildService().getPlayerContributionsAsync(entry.getPlayerUuid())
                .thenAccept(records -> {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        player.sendMessage(ColorUtils.colorize(
                                "&7" + languageManager.getGuiMessage(player,
                                        "gui.guild-funds.deposit-count", "Deposit times")
                                        + ": &f" + records.size()));
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
