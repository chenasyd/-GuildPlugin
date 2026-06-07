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
import java.util.List;

/**
 * 工会列表GUI - 仅负责搜索功能
 * 搜索：按名称/标签/描述搜索，结果展示在本GUI中
 */
public class GuildListGUI implements GUI {

    private final GuildPlugin plugin;
    private final Player player;
    private final LanguageManager languageManager;
    private int currentPage = 0;
    private int totalPages = 0;
    private static final int GUILDS_PER_PAGE = 28;
    private String searchQuery = "";
    private List<Guild> displayedGuilds = new ArrayList<>();

    public GuildListGUI(GuildPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
    }

    public GuildListGUI(GuildPlugin plugin, Player player, String searchQuery) {
        this.plugin = plugin;
        this.player = player;
        this.languageManager = plugin.getLanguageManager();
        this.searchQuery = searchQuery != null ? searchQuery : "";
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "guild-list-title", "&6工会列表"));
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
        // 搜索按钮 (slot 45)
        String searchText = searchQuery.isEmpty()
            ? languageManager.getGuiMessage(player, "guild-list.no-search", "无")
            : searchQuery;
        ItemStack search = createItem(
            Material.COMPASS,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list-search-name", "&e搜索工会")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list-search-lore-1", "&7左键: 输入关键词搜索")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list-search-lore-2", "&7右键: 清除搜索")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "guild-list.current-search", "当前搜索: {query}", "{query}", searchText))
        );
        inventory.setItem(45, search);

        // 筛选按钮 (slot 47) - 仅跳转到 GuildFilterGUI
        ItemStack filter = createItem(
            Material.HOPPER,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list-filter-name", "&e筛选")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list-filter-lore-1", "&7左键: 打开筛选条件"))
        );
        inventory.setItem(47, filter);

        // 返回按钮 (slot 49)
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.back", "&7返回")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.back-to-main-menu", "返回主菜单"))
        );
        inventory.setItem(49, back);
    }

    /**
     * 加载工会列表（仅搜索筛选）
     */
    private void loadGuilds(Inventory inventory) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    ItemStack noGuilds = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-guilds", "&c暂无工会")),
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-guilds-lore", "&7服务器中还没有工会"))
                    );
                    inventory.setItem(22, noGuilds);
                    this.displayedGuilds = new ArrayList<>();
                    return;
                }

                // 搜索筛选
                List<Guild> filtered = searchGuilds(guilds);

                if (filtered.isEmpty()) {
                    ItemStack noResults = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-results", "&c无搜索结果")),
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-results-lore", "&7没有找到匹配的工会"))
                    );
                    inventory.setItem(22, noResults);
                    this.displayedGuilds = new ArrayList<>();
                    return;
                }

                this.displayedGuilds = filtered;
                displayGuildsInInventory(inventory, filtered);
            });
        });
    }

    /**
     * 按搜索关键词筛选工会（名称、标签、描述）
     */
    private List<Guild> searchGuilds(List<Guild> guilds) {
        if (searchQuery.isEmpty()) {
            return new ArrayList<>(guilds);
        }

        List<Guild> filtered = new ArrayList<>();
        String lowerQuery = searchQuery.toLowerCase();
        for (Guild guild : guilds) {
            boolean nameMatch = guild.getName().toLowerCase().contains(lowerQuery);
            boolean tagMatch = guild.getTag() != null && guild.getTag().toLowerCase().contains(lowerQuery);
            boolean descMatch = guild.getDescription() != null && guild.getDescription().toLowerCase().contains(lowerQuery);
            if (nameMatch || tagMatch || descMatch) {
                filtered.add(guild);
            }
        }
        return filtered;
    }

    /**
     * 在GUI中显示工会列表
     */
    private void displayGuildsInInventory(Inventory inventory, List<Guild> guilds) {
        int totalItems = guilds.size();
        if (totalItems == 0) {
            ItemStack noResults = createItem(
                Material.BARRIER,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-results", "&c无搜索结果")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "guild-list.no-results-lore", "&7没有找到匹配的工会"))
            );
            inventory.setItem(22, noResults);
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

            Guild guild = guilds.get(i);
            ItemStack guildItem = createGuildItem(guild);
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

    private ItemStack createGuildItem(Guild guild) {
        List<String> lore = new ArrayList<>();
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getGuiMessage(player, "gui.guild-tag", "标签") + ": {guild_tag}", guild, null));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getGuiMessage(player, "gui.leader", "会长") + ": {leader_name}", guild, null));
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
            case 47: handleFilter(player); break;
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
            if (currentPage < totalPages) {
                currentPage++;
                refreshInventory(player);
            }
        }
    }

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

    /**
     * 处理搜索 - 仅 GuildListGUI 内部生效
     */
    private void handleSearch(Player player, ClickType clickType) {
        if (clickType == ClickType.RIGHT) {
            this.searchQuery = "";
            this.currentPage = 0;
            String message = languageManager.getGuiMessage(player, "guild-list.search-cleared", "&e已清除搜索");
            player.sendMessage(ColorUtils.colorize(message));
            refreshInventory(player);
            return;
        }

        player.closeInventory();
        String cancelKey = languageManager.getGuiMessage(player, "gui.search-cancel-key", "C");
        String promptMsg = languageManager.getGuiMessage(player, "guild-list.search-prompt", "&a请在聊天中输入搜索关键词（输入C取消）：");
        player.sendMessage(ColorUtils.colorize(promptMsg));

        final GuildListGUI self = this;
        plugin.getGuiManager().setInputMode(player, input -> {
            if (input.equalsIgnoreCase(cancelKey) || input.trim().isEmpty()) {
                String cancelMsg = languageManager.getGuiMessage(player, "guild-list.search-cancelled", "&e已取消搜索");
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
     * 打开筛选GUI - 筛选结果仅在GuildFilterGUI中显示
     */
    private void handleFilter(Player player) {
        plugin.getGuiManager().openGUI(player, new GuildFilterGUI(plugin, player, searchQuery));
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
