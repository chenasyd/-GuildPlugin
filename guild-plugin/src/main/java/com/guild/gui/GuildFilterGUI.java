package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.core.language.LanguageManager;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 工会筛选GUI - 筛选结果直接展示在本GUI中
 * 筛选独立于 GuildListGUI 的搜索，两者不同时生效
 */
public class GuildFilterGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private final String returnSearchQuery; // 仅用于返回 GuildListGUI 时保留搜索状态

    private int minLevel;
    private int maxLevel;
    private String sortMode; // DESC, ASC, FULL_ONLY
    private int sortIndex;   // 0=DESC, 1=ASC, 2=FULL_ONLY

    private int currentPage = 0;
    private int totalPages = 0;
    private static final int GUILDS_PER_PAGE = 28;
    private List<Guild> displayedGuilds = new ArrayList<>();

    private static final String[] SORT_MODES = {"DESC", "ASC", "FULL_ONLY"};
    private static final int SLOT_MIN_LEVEL = 46;
    private static final int SLOT_MAX_LEVEL = 47;
    private static final int SLOT_SORT = 48;
    private static final int SLOT_BACK = 52;

    /**
     * @param plugin   插件实例
     * @param player   玩家
     * @param returnSearchQuery 从GuildListGUI传来的搜索词，仅用于返回时保留
     */
    public GuildFilterGUI(GuildPlugin plugin, Player player, String returnSearchQuery) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.returnSearchQuery = returnSearchQuery != null ? returnSearchQuery : "";
        this.minLevel = 1;
        this.maxLevel = plugin.getMaxGuildLevel();
        setSortMode("DESC");
    }

    private void setSortMode(String mode) {
        this.sortMode = (mode != null && (mode.equals("DESC") || mode.equals("ASC") || mode.equals("FULL_ONLY")))
            ? mode : "DESC";
        for (int i = 0; i < SORT_MODES.length; i++) {
            if (SORT_MODES[i].equals(this.sortMode)) {
                this.sortIndex = i;
                break;
            }
        }
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.title", "&6工会筛选"));
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);
        setupFilterButtons(inventory);
        loadFilteredGuilds(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (isFilterControlSlot(slot)) {
            handleFilterControl(player, slot, clickType);
            return;
        }

        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }

        if (isGuildSlot(slot)) {
            handleGuildClick(player, slot, clickedItem, clickType);
        }
    }

    // ======================== 布局 ========================

    private boolean isFilterControlSlot(int slot) {
        return slot == SLOT_MIN_LEVEL || slot == SLOT_MAX_LEVEL || slot == SLOT_SORT || slot == SLOT_BACK;
    }

    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }

    private boolean isGuildSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
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

    // ======================== 筛选控件 ========================

    private void setupFilterButtons(Inventory inventory) {
        // 最低等级 (slot 46)
        ItemStack minLevelItem = createItem(
            Material.IRON_INGOT,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.min-level.name", "&e最低等级")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.min-level.lore-1", "&7左键: +1 | 右键: -1")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.min-level.lore-2", "&7当前: {level}", "{level}", String.valueOf(minLevel)))
        );
        inventory.setItem(SLOT_MIN_LEVEL, minLevelItem);

        // 最高等级 (slot 47)
        ItemStack maxLevelItem = createItem(
            Material.GOLD_INGOT,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.max-level.name", "&e最高等级")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.max-level.lore-1", "&7左键: +1 | 右键: -1")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.max-level.lore-2", "&7当前: {level}", "{level}", String.valueOf(maxLevel)))
        );
        inventory.setItem(SLOT_MAX_LEVEL, maxLevelItem);

        // 人数排序 (slot 48)
        ItemStack sortItem = createSortItem();
        inventory.setItem(SLOT_SORT, sortItem);

        // 返回按钮 (slot 52)
        ItemStack backItem = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.back.name", "&c返回")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.back.lore-1", "&7返回工会列表"))
        );
        inventory.setItem(SLOT_BACK, backItem);
    }

    private ItemStack createSortItem() {
        String markerOn = languageManager.getGuiMessage(player, "guild-filter.marker-on", "&a✔");
        String markerOff = languageManager.getGuiMessage(player, "guild-filter.marker-off", "&7");

        String descLore = languageManager.getGuiMessage(player, "guild-filter.sort.lore-desc", "人数降序 #人数从多到少");
        String ascLore = languageManager.getGuiMessage(player, "guild-filter.sort.lore-asc", "人数升序 #人数从少到多");
        String fullLore = languageManager.getGuiMessage(player, "guild-filter.sort.lore-full", "仅满员");

        String line1, line2, line3;
        switch (sortMode) {
            case "ASC":
                line1 = markerOff + descLore;
                line2 = markerOn + ascLore;
                line3 = markerOff + fullLore;
                break;
            case "FULL_ONLY":
                line1 = markerOff + descLore;
                line2 = markerOff + ascLore;
                line3 = markerOn + fullLore;
                break;
            default: // DESC
                line1 = markerOn + descLore;
                line2 = markerOff + ascLore;
                line3 = markerOff + fullLore;
                break;
        }

        return createItem(
            Material.COMPARATOR,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-filter.sort.name", "&e人数排序")),
            ColorUtils.colorize(line1),
            ColorUtils.colorize(line2),
            ColorUtils.colorize(line3)
        );
    }

    // ======================== 筛选控件点击处理 ========================

    private void handleFilterControl(Player player, int slot, ClickType clickType) {
        switch (slot) {
            case SLOT_MIN_LEVEL:
                handleMinLevelClick(player, clickType);
                break;
            case SLOT_MAX_LEVEL:
                handleMaxLevelClick(player, clickType);
                break;
            case SLOT_SORT:
                handleSortClick(player, clickType);
                break;
            case SLOT_BACK:
                handleBack(player);
                break;
        }
    }

    private void handleMinLevelClick(Player player, ClickType clickType) {
        boolean changed = false;
        if (clickType == ClickType.LEFT && minLevel < maxLevel) {
            minLevel++;
            changed = true;
        } else if (clickType == ClickType.RIGHT && minLevel > 1) {
            minLevel--;
            changed = true;
        }
        if (changed) {
            this.currentPage = 0;
            reFilterAndRefresh(player);
        }
    }

    private void handleMaxLevelClick(Player player, ClickType clickType) {
        int maxGuildLevel = plugin.getMaxGuildLevel();
        boolean changed = false;
        if (clickType == ClickType.LEFT && maxLevel < maxGuildLevel) {
            maxLevel++;
            changed = true;
        } else if (clickType == ClickType.RIGHT && maxLevel > minLevel) {
            maxLevel--;
            changed = true;
        }
        if (changed) {
            this.currentPage = 0;
            reFilterAndRefresh(player);
        }
    }

    private void handleSortClick(Player player, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            sortIndex = (sortIndex + 1) % SORT_MODES.length;
        } else if (clickType == ClickType.RIGHT) {
            sortIndex = (sortIndex - 1 + SORT_MODES.length) % SORT_MODES.length;
        } else {
            return;
        }
        sortMode = SORT_MODES[sortIndex];
        this.currentPage = 0;
        reFilterAndRefresh(player);
    }

    /**
     * 返回 GuildListGUI，保留搜索状态
     */
    private void handleBack(Player player) {
        GuildListGUI listGUI = new GuildListGUI(plugin, player, returnSearchQuery);
        plugin.getGuiManager().openGUI(player, listGUI);
    }

    /**
     * 条件变更时重新筛选并刷新
     */
    private void reFilterAndRefresh(Player player) {
        plugin.getGuiManager().refreshGUI(player);
    }

    // ======================== 公会列表加载（筛选逻辑） ========================

    /**
     * 加载筛选后的工会列表到本GUI
     */
    private void loadFilteredGuilds(Inventory inventory) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    showEmpty(inventory);
                    return;
                }

                // 第一轮：等级范围筛选
                List<Guild> levelFiltered = filterByLevel(guilds);

                if (levelFiltered.isEmpty()) {
                    showNoResults(inventory);
                    return;
                }

                // 第二轮：异步获取成员数，然后排序/满员筛选
                processWithMemberCounts(inventory, levelFiltered);
            });
        });
    }

    /**
     * 按等级范围筛选
     */
    private List<Guild> filterByLevel(List<Guild> guilds) {
        List<Guild> filtered = new ArrayList<>();
        for (Guild guild : guilds) {
            int guildLevel = guild.getLevel();
            if (guildLevel >= minLevel && guildLevel <= maxLevel) {
                filtered.add(guild);
            }
        }
        return filtered;
    }

    /**
     * 异步获取成员数，应用排序/仅满员，然后显示
     */
    private void processWithMemberCounts(Inventory inventory, List<Guild> levelFiltered) {
        List<GuildItemData> itemDataList = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < levelFiltered.size(); i++) {
            Guild guild = levelFiltered.get(i);
            final int originalIdx = i;

            CompletableFuture<Void> future = plugin.getGuildService().getGuildMemberCountAsync(guild.getId())
                .thenAccept(memberCount -> {
                    synchronized (itemDataList) {
                        itemDataList.add(new GuildItemData(guild, memberCount, originalIdx));
                    }
                });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            CompatibleScheduler.runTask(plugin, () -> {
                // 仅满员筛选
                if ("FULL_ONLY".equals(sortMode)) {
                    itemDataList.removeIf(data -> data.getMemberCount() < data.getGuild().getMaxMembers());
                }

                // 排序
                if ("ASC".equals(sortMode)) {
                    itemDataList.sort(Comparator.comparingInt(GuildItemData::getMemberCount));
                } else if ("FULL_ONLY".equals(sortMode)) {
                    itemDataList.sort(Comparator.comparingInt(GuildItemData::getOriginalIndex));
                } else {
                    // DESC: 人数降序
                    itemDataList.sort(Comparator.comparingInt(GuildItemData::getMemberCount).reversed());
                }

                // 存储显示列表
                List<Guild> displayList = new ArrayList<>();
                for (GuildItemData data : itemDataList) {
                    displayList.add(data.getGuild());
                }
                this.displayedGuilds = displayList;

                // 显示
                displayGuildsInInventory(inventory, itemDataList);
            });
        });
    }

    // ======================== 公会列表显示 ========================

    private void showEmpty(Inventory inventory) {
        ItemStack noGuilds = createItem(
            Material.BARRIER,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-guilds", "&c暂无工会")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-guilds-lore", "&7服务器中还没有工会"))
        );
        inventory.setItem(22, noGuilds);
        this.displayedGuilds = new ArrayList<>();
    }

    private void showNoResults(Inventory inventory) {
        ItemStack noResults = createItem(
            Material.BARRIER,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-results", "&c无匹配结果")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-results-lore", "&7没有找到匹配的工会"))
        );
        inventory.setItem(22, noResults);
        this.displayedGuilds = new ArrayList<>();
    }

    private void displayGuildsInInventory(Inventory inventory, List<GuildItemData> itemDataList) {
        int totalItems = itemDataList.size();
        if (totalItems == 0) {
            showNoResults(inventory);
            setupPaginationButtons(inventory, 0);
            return;
        }

        int totalPages = (totalItems - 1) / GUILDS_PER_PAGE;
        this.totalPages = totalPages;
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        setupPaginationButtons(inventory, totalPages);

        int startIndex = currentPage * GUILDS_PER_PAGE;
        int endIndex = Math.min(startIndex + GUILDS_PER_PAGE, totalItems);

        int slotIndex = 10;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= 44) break;

            GuildItemData data = itemDataList.get(i);
            ItemStack guildItem = createGuildItemWithMemberCount(data.getGuild(), data.getMemberCount());
            inventory.setItem(slotIndex, guildItem);

            slotIndex++;
            if (slotIndex % 9 == 8) {
                slotIndex += 2;
            }
        }
    }

    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.items.previous-page.name", "&c上一页")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.items.previous-page.lore.1", "&7查看上一页"))
            );
            inventory.setItem(18, previousPage);
        }

        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.items.next-page.name", "&a下一页")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.items.next-page.lore.1", "&7查看下一页"))
            );
            inventory.setItem(26, nextPage);
        }
    }

    private ItemStack createGuildItemWithMemberCount(Guild guild, int memberCount) {
        List<String> lore = new ArrayList<>();
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getGuiMessage(player, "gui.guild-tag", "标签") + ": {guild_tag}", guild, null));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getGuiMessage(player, "gui.leader", "会长") + ": {leader_name}", guild, null));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "guild-list.members", "成员") + ": " + memberCount));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "guild-list.level", "等级") + ": " + guild.getLevel()));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getGuiMessage(player, "guild-list.created-time", "创建时间") + ": {guild_created_time}", guild, null));
        lore.add("");
        lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(player, "guild-list.left-click-detail", "左键: 查看详情")));
        lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "guild-list.right-click-join", "右键: 申请加入")));

        return createItem(
            Material.SHIELD,
            PlaceholderUtils.replaceGuildPlaceholders("&e{guild_name}", guild, null),
            lore.toArray(new String[0])
        );
    }

    // ======================== 分页处理 ========================

    private void handlePaginationButton(Player player, int slot) {
        if (slot == 18) {
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
        } else if (slot == 26) {
            if (currentPage < totalPages) {
                currentPage++;
                refreshInventory(player);
            }
        }
    }

    // ======================== 公会点击处理 ========================

    private void handleGuildClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        int guildIndex = currentPage * GUILDS_PER_PAGE + slotToDisplayIndex(slot);
        if (guildIndex < 0 || guildIndex >= displayedGuilds.size()) return;

        Guild guild = displayedGuilds.get(guildIndex);
        if (guild == null) return;

        if (clickType == ClickType.LEFT) {
            GuildInfoGUI guildInfoGUI = new GuildInfoGUI(plugin, player, guild);
            plugin.getGuiManager().openGUI(player, guildInfoGUI);
        } else if (clickType == ClickType.RIGHT) {
            handleApplyToGuild(player, guild);
        }
    }

    private int slotToDisplayIndex(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 4 || col < 1 || col > 7) return -1;
        return (row - 1) * 7 + (col - 1);
    }

    private void handleApplyToGuild(Player player, Guild guild) {
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(playerGuild -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (playerGuild != null) {
                    String message = languageManager.getGuiMessage(player, "create.already-in-guild", "&c您已经在一个工会中了！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                plugin.getGuildService().hasPendingApplicationAsync(player.getUniqueId(), guild.getId()).thenAccept(hasPending -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (hasPending) {
                            String message = languageManager.getGuiMessage(player, "apply.already-applied", "&c您已经申请过这个工会了！");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        plugin.getGuildService().submitApplicationAsync(guild.getId(), player.getUniqueId(), player.getName(), "").thenAccept(success -> {
                            CompatibleScheduler.runTask(plugin, () -> {
                                if (success) {
                                    String message = languageManager.getGuiMessage(player, "apply.success", "&a申请已提交！");
                                    player.sendMessage(ColorUtils.colorize(message));
                                } else {
                                    String message = languageManager.getGuiMessage(player, "apply.failed", "&c申请提交失败！");
                                    player.sendMessage(ColorUtils.colorize(message));
                                }
                            });
                        });
                    });
                });
            });
        });
    }

    // ======================== 内部类 & 工具 ========================

    private static class GuildItemData {
        private final Guild guild;
        private final int memberCount;
        private final int originalIndex;

        GuildItemData(Guild guild, int memberCount, int originalIndex) {
            this.guild = guild;
            this.memberCount = memberCount;
            this.originalIndex = originalIndex;
        }

        Guild getGuild() { return guild; }
        int getMemberCount() { return memberCount; }
        int getOriginalIndex() { return originalIndex; }
    }

    private void refreshInventory(Player player) {
        plugin.getGuiManager().refreshGUI(player);
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
