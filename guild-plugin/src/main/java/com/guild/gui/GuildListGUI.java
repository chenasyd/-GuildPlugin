package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;

import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
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

    // ── 图像模式功能常量 ──
    public static final String FUNC_PREV_PAGE = "PREV_PAGE";
    public static final String FUNC_NEXT_PAGE = "NEXT_PAGE";
    public static final String FUNC_SEARCH = "SEARCH";
    public static final String FUNC_FILTER = "FILTER";
    public static final String FUNC_BACK = "BACK";

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
        return ColorUtils.colorize(plugin.getLanguageManager().getGuiMessage(player, "gui.guild-list.guild-list-title", "&6Guild List"));
    }

    @Override
    public int getSize() {
        return 54;
    }

    // ==================== 基岩版表单 ====================

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) return false;
        sendBedrockGuildList(player, 0);
        return true;
    }

    /**
     * 构建并发送基岩版工会列表表单（异步加载数据）
     */
    private void sendBedrockGuildList(Player player, int page) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (!player.isOnline()) return;

                List<Guild> filtered = searchGuilds(guilds != null ? guilds : new ArrayList<>());

                if (filtered.isEmpty()) {
                    SimpleForm emptyForm = SimpleForm.builder()
                            .title(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-title", "&6Guild List"))
                            .content(searchQuery.isEmpty()
                                    ? languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-no-guilds", "&cNo guilds on the server yet")
                                    : languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-no-results", "&cNo matching guilds found"))
                            .button(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-search", "&eSearch Guilds"))
                            .button(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-back-main", "&cBack to Main Menu"))
                            .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                                if (response.clickedButtonId() == 0) {
                                    sendBedrockSearchForm(player);
                                } else {
                                    plugin.getGuiManager().openGUI(player,
                                            new MainGuildGUI(plugin, player));
                                }
                            }))
                            .build();
                    BedrockFormSender.sendForm(player.getUniqueId(), emptyForm);
                    return;
                }

                int totalPagesLocal = (filtered.size() - 1) / GUILDS_PER_PAGE;
                final int safePage = Math.max(0, Math.min(page, totalPagesLocal));

                int startIndex = safePage * GUILDS_PER_PAGE;
                int endIndex = Math.min(startIndex + GUILDS_PER_PAGE, filtered.size());

                StringBuilder content = new StringBuilder();
                content.append(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-page-info", "&fPage {page}/{total}",
                        "{page}", String.valueOf(safePage + 1), "{total}", String.valueOf(totalPagesLocal + 1)));
                if (!searchQuery.isEmpty()) {
                    content.append("\n").append(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-search-info", "&fSearch: &e{query}",
                            "{query}", searchQuery));
                }

                SimpleForm.Builder builder = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-title", "&6Guild List"))
                        .content(content.toString());

                // 工会按钮
                List<Guild> pageGuilds = new ArrayList<>();
                for (int i = startIndex; i < endIndex; i++) {
                    Guild g = filtered.get(i);
                    pageGuilds.add(g);
                    String tagStr = g.getTag() != null ? " §f[" + g.getTag() + "]" : "";
                    builder.button("§e" + g.getName() + tagStr + " §f- Lv." + g.getLevel());
                }

                // 导航按钮（固定顺序：上一页/下一页/搜索/返回）
                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-prev-page", "&aPrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-next-page", "&aNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-search", "&eSearch Guilds"));
                builder.button(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-back-main", "&cBack to Main Menu"));

                final int guildCount = pageGuilds.size();
                final int curPage = safePage;
                final int totPages = totalPagesLocal;

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int id = response.clickedButtonId();
                    if (id < guildCount) {
                        sendBedrockGuildDetail(player, pageGuilds.get(id));
                    } else if (id == guildCount) {
                        // 上一页
                        sendBedrockGuildList(player, curPage > 0 ? curPage - 1 : curPage);
                    } else if (id == guildCount + 1) {
                        // 下一页
                        sendBedrockGuildList(player, curPage < totPages ? curPage + 1 : curPage);
                    } else if (id == guildCount + 2) {
                        sendBedrockSearchForm(player);
                    } else {
                        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
                    }
                }));

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

    /**
     * 发送基岩版搜索表单（CustomForm 文本输入）
     */
    private void sendBedrockSearchForm(Player player) {
        CustomForm form = CustomForm.builder()
                .title(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-search-title", "&6Search Guilds"))
                .input(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-search-input", "&fEnter search keyword"),
                        languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-search-placeholder", "Leave empty to show all"), searchQuery)
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    String query = response.getInput(0);
                    this.searchQuery = query != null ? query.trim() : "";
                    this.currentPage = 0;
                    sendBedrockGuildList(player, 0);
                }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(plugin, player, () ->
                        sendBedrockGuildList(player, currentPage)))
                .build();
        BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    /**
     * 发送基岩版工会详情子菜单（查看详情 / 申请加入 / 返回列表）
     */
    private void sendBedrockGuildDetail(Player player, Guild targetGuild) {
        String tagStr = targetGuild.getTag() != null ? " [" + targetGuild.getTag() + "]" : "";
        SimpleForm form = SimpleForm.builder()
                .title("§6" + targetGuild.getName() + tagStr)
                .content(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-select-action", "&fSelect action:"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-view-detail", "&eView Details"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-apply-join", "&aApply to Join"))
                .button(languageManager.getGuiColoredMessage(player, "gui.guild-list.bedrock-back-list", "&cBack to List"))
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> plugin.getGuiManager().openGUI(player,
                                new GuildInfoGUI(plugin, player, targetGuild));
                        case 1 -> handleApplyToGuild(player, targetGuild);
                        case 2 -> sendBedrockGuildList(player, currentPage);
                    }
                }))
                .build();
        BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    // ==================== Java Inventory 布局 ====================

    @Override
    public void setupInventory(Inventory inventory) {
        fillBorder(inventory);
        setupFunctionButtons(inventory);
        loadGuilds(inventory);

        plugin.getGuiManager().applyImageModeIfNeeded(player, inventory, getGuiType());
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
            ? languageManager.getGuiMessage(player, "gui.guild-list.no-search", "None")
            : searchQuery;
        ItemStack search = createItem(
            Material.COMPASS,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.guild-list-search-name", "&eSearch Guilds")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.guild-list-search-lore-1", "&7Left click: Enter search keyword")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.guild-list-search-lore-2", "&7Right click: Clear search")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-list.current-search", "Current search: {query}", "{query}", searchText))
        );
        inventory.setItem(45, search);

        // 筛选按钮 (slot 47) - 仅跳转到 GuildFilterGUI
        ItemStack filter = createItem(
            Material.HOPPER,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.guild-list-filter-name", "&eFilter")),
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.guild-list-filter-lore-1", "&7Left click: Open filter options"))
        );
        inventory.setItem(47, filter);

        // 返回按钮 (slot 49)
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.common.back", "Back")),
            ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.common.back-to-main-menu", "Back to main menu"))
        );
        inventory.setItem(49, back);
    }

    /**
     * 加载工会列表（仅搜索筛选）
     */
    private void loadGuilds(Inventory inventory) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    ItemStack noGuilds = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.no-guilds", "&cNo guilds")),
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.no-guilds-lore", "&7There are no guilds on the server yet"))
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
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.no-results", "&cNo search results")),
                        ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.no-results-lore", "&7No matching guilds found"))
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
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.no-results", "&cNo search results")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.no-results-lore", "&7No matching guilds found"))
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
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.items.previous-page.name", "&cPrevious Page")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.items.previous-page.lore.1", "&7View previous page"))
            );
            inventory.setItem(18, previousPage);
        }

        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.items.next-page.name", "&aNext Page")),
                ColorUtils.colorize(languageManager.getGuiMessage(player, "gui.guild-list.items.next-page.lore.1", "&7View next page"))
            );
            inventory.setItem(26, nextPage);
        }
    }

    private ItemStack createGuildItem(Guild guild) {
        List<String> lore = new ArrayList<>();
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getGuiMessage(player, "gui.common.guild-tag", "Guild Tag") + ": {guild_tag}", guild, null));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getGuiMessage(player, "gui.common.leader", "Leader") + ": {leader_name}", guild, null));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player, "gui.guild-list.level", "Level") + ": " + guild.getLevel()));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7" + languageManager.getGuiMessage(player, "gui.guild-list.created-time", "Created time") + ": {guild_created_time}", guild, null));
        lore.add("");
        lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(player, "gui.guild-list.left-click-detail", "Left click: View details")));
        lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(player, "gui.guild-list.right-click-join", "Right click: Apply to join")));

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
            String message = languageManager.getGuiMessage(player, "gui.guild-list.search-cleared", "&eSearch cleared");
            player.sendMessage(ColorUtils.colorize(message));
            refreshInventory(player);
            return;
        }

        player.closeInventory();
        String cancelKey = languageManager.getGuiMessage(player, "gui.common.search-cancel-key", "C");
        String promptMsg = languageManager.getGuiMessage(player, "gui.guild-list.search-prompt", "&aType your search keyword in chat (type C to cancel):");
        player.sendMessage(ColorUtils.colorize(promptMsg));

        final GuildListGUI self = this;
        plugin.getGuiManager().setInputMode(player, input -> {
            if (input.equalsIgnoreCase(cancelKey) || input.trim().isEmpty()) {
                String cancelMsg = languageManager.getGuiMessage(player, "gui.guild-list.search-cancelled", "&eSearch cancelled");
                player.sendMessage(ColorUtils.colorize(cancelMsg));
                CompatibleScheduler.runTask(plugin, player, () -> plugin.getGuiManager().openGUI(player, self));
                return true;
            }

            self.searchQuery = input.trim();
            self.currentPage = 0;
            CompatibleScheduler.runTask(plugin, player, () -> plugin.getGuiManager().openGUI(player, self));
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
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (playerGuild != null) {
                    String message = languageManager.getGuiMessage(player, "gui.create-guild.create.already-in-guild", "&cYou are already in a guild!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }

                plugin.getGuildService().hasPendingApplicationAsync(player.getUniqueId(), guild.getId()).thenAccept(hasPending -> {
                    CompatibleScheduler.runTask(plugin, player, () -> {
                        if (hasPending) {
                            String message = languageManager.getGuiMessage(player, "gui.application-mgmt.apply.already-applied", "&cYou have already applied to this guild!");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }

                        plugin.getGuildService().submitApplicationAsync(guild.getId(), player.getUniqueId(), player.getName(), "").thenAccept(success -> {
                            CompatibleScheduler.runTask(plugin, player, () -> {
                                if (success) {
                                    String message = languageManager.getGuiMessage(player, "gui.application-mgmt.apply.success", "&aApplication submitted!");
                                    player.sendMessage(ColorUtils.colorize(message));
                                } else {
                                    String message = languageManager.getGuiMessage(player, "gui.application-mgmt.apply.failed", "&cApplication submission failed!");
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
