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
 * 工会列表GUI
 */
public class GuildListGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private int currentPage = 0;
    private static final int GUILDS_PER_PAGE = 28;
    private String searchQuery = "";
    private int minLevel = 1;
    private int maxLevel = 10;
    private String sortMode = "DESC"; // DESC, ASC, FULL_ONLY
    private List<Guild> displayedGuilds = new ArrayList<>();

    public GuildListGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.maxLevel = plugin.getMaxGuildLevel();
    }

    public GuildListGUI(GuildPlugin plugin, Player player, String searchQuery) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.searchQuery = searchQuery != null ? searchQuery : "";
        this.maxLevel = plugin.getMaxGuildLevel();
    }

    public GuildListGUI(GuildPlugin plugin, Player player, String searchQuery, int minLevel, int maxLevel, String sortMode) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.searchQuery = searchQuery != null ? searchQuery : "";
        this.minLevel = Math.max(1, minLevel);
        this.maxLevel = Math.min(plugin.getMaxGuildLevel(), Math.max(this.minLevel, maxLevel));
        this.sortMode = (sortMode != null) ? sortMode : "DESC";
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getLanguageManager().getMessage(player, "guild-list-title", "&6工会列表"));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);
        setupFunctionButtons(inventory);
        loadGuilds(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot, clickType);
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
    
    private void setupFunctionButtons(Inventory inventory) {
        // 搜索按钮
        String searchText = searchQuery.isEmpty() ?
            languageManager.getMessage(player, "guild-list.no-search", "无") : searchQuery;
        ItemStack search = createItem(
            Material.COMPASS,
            ColorUtils.colorize(languageManager.getMessage(player, "guild-list-search-name", "&e搜索工会")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-list-search-lore-1", "&7左键: 输入关键词搜索")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-list-search-lore-2", "&7右键: 清除搜索")),
            ColorUtils.colorize("&7" + languageManager.getMessage(player, "guild-list.current-search", "当前搜索: {query}", "{query}", searchText))
        );
        inventory.setItem(45, search);

        // 筛选按钮
        String sortDisplay = getSortDisplayName();
        ItemStack filter = createItem(
            Material.HOPPER,
            ColorUtils.colorize(languageManager.getMessage(player, "guild-list-filter-name", "&e筛选")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-list-filter-lore-1", "&7左键: 打开筛选条件")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-list-filter-lore-2", "&7右键: 重置筛选")),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-list.filter-level-range", "&7等级: {min}-{max}", "{min}", String.valueOf(minLevel), "{max}", String.valueOf(maxLevel))),
            ColorUtils.colorize(languageManager.getMessage(player, "guild-list.filter-sort", "&7排序: {sort}", "{sort}", sortDisplay))
        );
        inventory.setItem(47, filter);

        // 返回按钮
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getMessage(player, "gui.back", "&7返回")),
            ColorUtils.colorize("&7" + languageManager.getMessage(player, "gui.back-to-main-menu", "返回主菜单"))
        );
        inventory.setItem(49, back);
    }
    
    private String getSortDisplayName() {
        switch (sortMode) {
            case "ASC":
                return languageManager.getMessage(player, "guild-filter.sort.asc", "人数升序");
            case "FULL_ONLY":
                return languageManager.getMessage(player, "guild-filter.sort.full-only", "仅满员");
            default:
                return languageManager.getMessage(player, "guild-filter.sort.desc", "人数降序");
        }
    }
    
    /**
     * 加载工会列表
     */
    private void loadGuilds(Inventory inventory) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    ItemStack noGuilds = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize(languageManager.getMessage(player, "guild-list.no-guilds", "&c暂无工会")),
                        ColorUtils.colorize(languageManager.getMessage(player, "guild-list.no-guilds-lore", "&7服务器中还没有工会"))
                    );
                    inventory.setItem(22, noGuilds);
                    this.displayedGuilds = new ArrayList<>();
                    return;
                }

                // 第一轮：搜索和等级范围筛选
                List<Guild> preFiltered = preFilterGuilds(guilds);

                if (preFiltered.isEmpty()) {
                    ItemStack noResults = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize(languageManager.getMessage(player, "guild-list.no-results", "&c无搜索结果")),
                        ColorUtils.colorize(languageManager.getMessage(player, "guild-list.no-results-lore", "&7没有找到匹配的工会"))
                    );
                    inventory.setItem(22, noResults);
                    this.displayedGuilds = new ArrayList<>();
                    return;
                }

                // 第二轮：异步获取成员数，然后排序/筛选
                processWithMemberCounts(inventory, preFiltered);
            });
        });
    }
    
    /**
     * 预筛选：搜索 + 等级范围
     */
    private List<Guild> preFilterGuilds(List<Guild> guilds) {
        List<Guild> filtered = new ArrayList<>();
        for (Guild guild : guilds) {
            // 搜索过滤：名称、标签、描述
            if (!searchQuery.isEmpty()) {
                String lowerQuery = searchQuery.toLowerCase();
                boolean nameMatch = guild.getName().toLowerCase().contains(lowerQuery);
                boolean tagMatch = guild.getTag() != null && guild.getTag().toLowerCase().contains(lowerQuery);
                boolean descMatch = guild.getDescription() != null && guild.getDescription().toLowerCase().contains(lowerQuery);
                if (!nameMatch && !tagMatch && !descMatch) {
                    continue;
                }
            }
            
            // 等级范围
            int guildLevel = guild.getLevel();
            if (guildLevel < minLevel || guildLevel > maxLevel) {
                continue;
            }
            
            filtered.add(guild);
        }
        return filtered;
    }
    
    /**
     * 异步获取成员数，应用排序/仅满员筛选，然后显示
     */
    private void processWithMemberCounts(Inventory inventory, List<Guild> preFiltered) {
        List<GuildItemData> itemDataList = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < preFiltered.size(); i++) {
            Guild guild = preFiltered.get(i);
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
                
                // 存储显示列表供点击处理使用
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
    
    /**
     * 在GUI中显示工会列表
     */
    private void displayGuildsInInventory(Inventory inventory, List<GuildItemData> itemDataList) {
        int totalItems = itemDataList.size();
        if (totalItems == 0) {
            ItemStack noResults = createItem(
                Material.BARRIER,
                ColorUtils.colorize(languageManager.getMessage(player, "guild-list.no-results", "&c无搜索结果")),
                ColorUtils.colorize(languageManager.getMessage(player, "guild-list.no-results-lore", "&7没有找到匹配的工会"))
            );
            inventory.setItem(22, noResults);
            setupPaginationButtons(inventory, 0);
            return;
        }
        
        int totalPages = (totalItems - 1) / GUILDS_PER_PAGE;
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
    
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getMessage(player, "guild-list.items.previous-page.name", "&c上一页")),
                ColorUtils.colorize(languageManager.getMessage(player, "guild-list.items.previous-page.lore.1", "&7查看上一页"))
            );
            inventory.setItem(18, previousPage);
        }

        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getMessage(player, "guild-list.items.next-page.name", "&a下一页")),
                ColorUtils.colorize(languageManager.getMessage(player, "guild-list.items.next-page.lore.1", "&7查看下一页"))
            );
            inventory.setItem(26, nextPage);
        }
    }
    
    private ItemStack createGuildItemWithMemberCount(Guild guild, int memberCount) {
        List<String> lore = new ArrayList<>();
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getMessage(player, "gui.guild-tag", "标签") + ": {guild_tag}", guild, null));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getMessage(player, "gui.leader", "会长") + ": {leader_name}", guild, null));
        lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "guild-list.members", "成员") + ": " + memberCount));
        lore.add(ColorUtils.colorize("&7" + languageManager.getMessage(player, "guild-list.level", "等级") + ": " + guild.getLevel()));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getMessage(player, "guild-list.created-time", "创建时间") + ": {guild_created_time}", guild, null));
        lore.add("");
        lore.add(ColorUtils.colorize("&a" + languageManager.getMessage(player, "guild-list.left-click-detail", "左键: 查看详情")));
        lore.add(ColorUtils.colorize("&e" + languageManager.getMessage(player, "guild-list.right-click-join", "右键: 申请加入")));

        return createItem(
            Material.SHIELD,
            PlaceholderUtils.replaceGuildPlaceholders("&e{guild_name}", guild, null),
            lore.toArray(new String[0])
        );
    }
    
    private boolean isFunctionButton(int slot) {
        return slot == 45 || slot == 47 || slot == 49;
    }
    
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }
    
    private boolean isGuildSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }
    
    private void handleFunctionButton(Player player, int slot, ClickType clickType) {
        switch (slot) {
            case 45: handleSearch(player, clickType); break;
            case 47: handleFilter(player, clickType); break;
            case 49: plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player)); break;
        }
    }
    
    private void handlePaginationButton(Player player, int slot) {
        if (slot == 18) {
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
        } else if (slot == 26) {
            currentPage++;
            refreshInventory(player);
        }
    }
    
    private void handleGuildClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // 计算在 displayedGuilds 中的索引
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
    
    /**
     * 将GUI槽位转换为显示索引（0-based within page）
     */
    private int slotToDisplayIndex(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        // 在边框内: row 1-4, col 1-7
        if (row < 1 || row > 4 || col < 1 || col > 7) return -1;
        return (row - 1) * 7 + (col - 1);
    }
    
    /**
     * 处理搜索
     */
    private void handleSearch(Player player, ClickType clickType) {
        if (clickType == ClickType.RIGHT) {
            this.searchQuery = "";
            this.currentPage = 0;
            String message = languageManager.getMessage(player, "guild-list.search-cleared", "&e已清除搜索");
            player.sendMessage(ColorUtils.colorize(message));
            refreshInventory(player);
            return;
        }
        
        player.closeInventory();
        String cancelKey = languageManager.getMessage(player, "gui.search-cancel-key", "C");
        String promptMsg = languageManager.getMessage(player, "guild-list.search-prompt", "&a请在聊天中输入搜索关键词（输入C取消）：");
        player.sendMessage(ColorUtils.colorize(promptMsg));
        
        final GuildListGUI self = this;
        plugin.getGuiManager().setInputMode(player, input -> {
            if (input.equalsIgnoreCase(cancelKey) || input.trim().isEmpty()) {
                String cancelMsg = languageManager.getMessage(player, "guild-list.search-cancelled", "&e已取消搜索");
                player.sendMessage(ColorUtils.colorize(cancelMsg));
                CompatibleScheduler.runTask(plugin, () -> plugin.getGuiManager().openGUI(player, self));
                return true;
            }
            
            self.searchQuery = input.trim();
            self.currentPage = 0;
            CompatibleScheduler.runTask(plugin, () -> plugin.getGuiManager().openGUI(player, self));
            return true;
        });
    }

    /**
     * 处理筛选
     */
    private void handleFilter(Player player, ClickType clickType) {
        if (clickType == ClickType.RIGHT) {
            this.minLevel = 1;
            this.maxLevel = plugin.getMaxGuildLevel();
            this.sortMode = "DESC";
            this.currentPage = 0;
            String message = languageManager.getMessage(player, "guild-list.filter-reset", "&e筛选已重置");
            player.sendMessage(ColorUtils.colorize(message));
            refreshInventory(player);
            return;
        }
        
        GuildFilterGUI filterGUI = new GuildFilterGUI(plugin, player, this.searchQuery, minLevel, maxLevel, sortMode);
        plugin.getGuiManager().openGUI(player, filterGUI);
    }
    
    private void handleApplyToGuild(Player player, Guild guild) {
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(playerGuild -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (playerGuild != null) {
                    String message = languageManager.getMessage(player, "create.already-in-guild", "&c您已经在一个工会中了！");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                plugin.getGuildService().hasPendingApplicationAsync(player.getUniqueId(), guild.getId()).thenAccept(hasPending -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (hasPending) {
                            String message = languageManager.getMessage(player, "apply.already-applied", "&c您已经申请过这个工会了！");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        plugin.getGuildService().submitApplicationAsync(guild.getId(), player.getUniqueId(), player.getName(), "").thenAccept(success -> {
                            CompatibleScheduler.runTask(plugin, () -> {
                                if (success) {
                                    String message = languageManager.getMessage(player, "apply.success", "&a申请已提交！");
                                    player.sendMessage(ColorUtils.colorize(message));
                                } else {
                                    String message = languageManager.getMessage(player, "apply.failed", "&c申请提交失败！");
                                    player.sendMessage(ColorUtils.colorize(message));
                                }
                            });
                        });
                    });
                });
            });
        });
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
