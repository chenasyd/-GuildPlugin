package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.gui.base.AbstractPagedListGUI;
import com.guild.gui.base.GuiLayoutUtils;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.List;

/**
 * 公会列表 GUI：搜索 + 分页浏览。
 */
public class GuildListGUI extends AbstractPagedListGUI<Guild> {

    public static final String FUNC_SEARCH = "SEARCH";
    public static final String FUNC_FILTER = "FILTER";

    private static final int TOOLBAR_SEARCH = 45;
    private static final int TOOLBAR_FILTER = 47;
    private static final int TOOLBAR_BACK = 49;

    private String searchQuery = "";
    private boolean serverHasNoGuilds = false;

    public GuildListGUI(GuildPlugin plugin, Player player) {
        this(plugin, player, "");
    }

    public GuildListGUI(GuildPlugin plugin, Player player, String searchQuery) {
        super(plugin, player, PaginationLayout.SIDE, GuiLayoutUtils.ITEMS_PER_PAGE);
        this.searchQuery = searchQuery != null ? searchQuery : "";
        loadGuilds();
    }

    private void loadGuilds() {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            if (guilds == null || guilds.isEmpty()) {
                serverHasNoGuilds = true;
                setEntries(List.of());
            } else {
                List<Guild> filtered = searchGuilds(guilds);
                serverHasNoGuilds = false;
                setEntries(filtered);
            }
            CompatibleScheduler.runTask(plugin, viewer, () -> {
                if (viewer.isOnline()) {
                    plugin.getGuiManager().refreshGUI(viewer);
                }
            });
        });
    }

    private List<Guild> searchGuilds(List<Guild> guilds) {
        if (searchQuery.isEmpty()) {
            return new ArrayList<>(guilds);
        }
        List<Guild> filtered = new ArrayList<>();
        String lowerQuery = searchQuery.toLowerCase();
        for (Guild guild : guilds) {
            boolean nameMatch = guild.getName().toLowerCase().contains(lowerQuery);
            boolean tagMatch = guild.getTag() != null && guild.getTag().toLowerCase().contains(lowerQuery);
            boolean descMatch = guild.getDescription() != null
                    && guild.getDescription().toLowerCase().contains(lowerQuery);
            if (nameMatch || tagMatch || descMatch) {
                filtered.add(guild);
            }
        }
        return filtered;
    }

    @Override
    public String getTitle() {
        return ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                "gui.guild-list.guild-list-title", "&6Guild List"));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin, player));
    }

    @Override
    protected String prevPageTitleKey() {
        return "gui.guild-list.items.previous-page.name";
    }

    @Override
    protected String prevPageTitleDefault() {
        return "&cPrevious Page";
    }

    @Override
    protected String prevPageLoreKey() {
        return "gui.guild-list.items.previous-page.lore.1";
    }

    @Override
    protected String prevPageLoreDefault() {
        return "&7View previous page";
    }

    @Override
    protected String nextPageTitleKey() {
        return "gui.guild-list.items.next-page.name";
    }

    @Override
    protected String nextPageTitleDefault() {
        return "&aNext Page";
    }

    @Override
    protected String nextPageLoreKey() {
        return "gui.guild-list.items.next-page.lore.1";
    }

    @Override
    protected String nextPageLoreDefault() {
        return "&7View next page";
    }

    @Override
    protected void displayEmptyState(Inventory inventory) {
        if (serverHasNoGuilds) {
            inventory.setItem(22, createItem(
                    Material.BARRIER,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.guild-list.no-guilds", "&cNo guilds")),
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.guild-list.no-guilds-lore",
                            "&7There are no guilds on the server yet"))));
        } else {
            inventory.setItem(22, createItem(
                    Material.BARRIER,
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.guild-list.no-results", "&cNo search results")),
                    ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                            "gui.guild-list.no-results-lore", "&7No matching guilds found"))));
        }
    }

    @Override
    protected void setupToolbar(Inventory inventory) {
        String searchText = searchQuery.isEmpty()
                ? languageManager.getGuiMessage(viewer, "gui.guild-list.no-search", "None")
                : searchQuery;
        inventory.setItem(TOOLBAR_SEARCH, createItem(
                Material.COMPASS,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.guild-list.guild-list-search-name", "&eSearch Guilds")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.guild-list.guild-list-search-lore-1", "&7Left click: Enter search keyword")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.guild-list.guild-list-search-lore-2", "&7Right click: Clear search")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-list.current-search", "Current search: {query}", "{query}", searchText))));

        inventory.setItem(TOOLBAR_FILTER, createItem(
                Material.HOPPER,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.guild-list.guild-list-filter-name", "&eFilter")),
                ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.guild-list.guild-list-filter-lore-1", "&7Left click: Open filter options"))));

        inventory.setItem(TOOLBAR_BACK, createItem(
                Material.ARROW,
                ColorUtils.colorize(languageManager.getGuiMessage(viewer, "gui.common.back", "Back")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.common.back-to-main-menu", "Back to main menu"))));
    }

    @Override
    protected boolean handleToolbarClick(Player player, int slot, ClickType clickType) {
        switch (slot) {
            case TOOLBAR_SEARCH -> {
                handleSearch(player, clickType);
                return true;
            }
            case TOOLBAR_FILTER -> {
                plugin.getGuiManager().openGUI(player, new GuildFilterGUI(plugin, player, searchQuery));
                return true;
            }
            case TOOLBAR_BACK -> {
                openBackGui(player);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected ItemStack createEntryItem(Guild guild) {
        List<String> lore = new ArrayList<>();
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7"
                + languageManager.getGuiMessage(viewer, "gui.common.guild-tag", "Guild Tag")
                + ": {guild_tag}", guild, null));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7"
                + languageManager.getGuiMessage(viewer, "gui.common.leader", "Leader")
                + ": {leader_name}", guild, null));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-list.level", "Level") + ": " + guild.getLevel()));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7"
                + languageManager.getGuiMessage(viewer, "gui.guild-list.created-time", "Created time")
                + ": {guild_created_time}", guild, null));
        lore.add("");
        lore.add(ColorUtils.colorize("&a" + languageManager.getGuiMessage(viewer,
                "gui.guild-list.left-click-detail", "Left click: View details")));
        lore.add(ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                "gui.guild-list.right-click-join", "Right click: Apply to join")));

        return createItem(
                Material.SHIELD,
                PlaceholderUtils.replaceGuildPlaceholders("&e{guild_name}", guild, null),
                lore.toArray(new String[0]));
    }

    @Override
    protected void onEntryClick(Player player, Guild guild, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            plugin.getGuiManager().openGUI(player, new GuildInfoGUI(plugin, player, guild));
        } else if (clickType == ClickType.RIGHT) {
            handleApplyToGuild(player, guild);
        }
    }

    private void handleSearch(Player player, ClickType clickType) {
        if (clickType == ClickType.RIGHT) {
            searchQuery = "";
            currentPage = 0;
            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                    "gui.guild-list.search-cleared", "&eSearch cleared")));
            loadGuilds();
            return;
        }

        player.closeInventory();
        String cancelKey = languageManager.getGuiMessage(player, "gui.common.search-cancel-key", "C");
        player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                "gui.guild-list.search-prompt",
                "&aType your search keyword in chat (type C to cancel):")));

        final GuildListGUI self = this;
        plugin.getGuiManager().setInputMode(player, input -> {
            if (input.equalsIgnoreCase(cancelKey) || input.trim().isEmpty()) {
                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                        "gui.guild-list.search-cancelled", "&eSearch cancelled")));
                CompatibleScheduler.runTask(plugin, player, () -> plugin.getGuiManager().openGUI(player, self));
                return true;
            }
            self.searchQuery = input.trim();
            self.currentPage = 0;
            self.loadGuilds();
            CompatibleScheduler.runTask(plugin, player, () -> plugin.getGuiManager().openGUI(player, self));
            return true;
        });
    }

    private void handleApplyToGuild(Player player, Guild guild) {
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(playerGuild -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (playerGuild != null) {
                    player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                            "gui.create-guild.create.already-in-guild", "&cYou are already in a guild!")));
                    return;
                }
                plugin.getGuildService().hasPendingApplicationAsync(player.getUniqueId(), guild.getId())
                        .thenAccept(hasPending -> CompatibleScheduler.runTask(plugin, player, () -> {
                            if (hasPending) {
                                player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(player,
                                        "gui.application-mgmt.apply.already-applied",
                                        "&cYou have already applied to this guild!")));
                                return;
                            }
                            plugin.getGuildService().submitApplicationAsync(guild.getId(),
                                            player.getUniqueId(), player.getName(), "")
                                    .thenAccept(success -> CompatibleScheduler.runTask(plugin, player, () -> {
                                        if (success) {
                                            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(
                                                    player, "gui.application-mgmt.apply.success",
                                                    "&aApplication submitted!")));
                                        } else {
                                            player.sendMessage(ColorUtils.colorize(languageManager.getGuiMessage(
                                                    player, "gui.application-mgmt.apply.failed",
                                                    "&cApplication submission failed!")));
                                        }
                                    }));
                        }));
            });
        });
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        sendBedrockGuildList(player, 0);
        return true;
    }

    private void sendBedrockGuildList(Player player, int page) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            CompatibleScheduler.runTask(plugin, player, () -> {
                if (!player.isOnline()) {
                    return;
                }

                List<Guild> filtered = searchGuilds(guilds != null ? guilds : new ArrayList<>());

                if (filtered.isEmpty()) {
                    SimpleForm emptyForm = SimpleForm.builder()
                            .title(languageManager.getGuiColoredMessage(player,
                                    "gui.guild-list.bedrock-title", "&6Guild List"))
                            .content(searchQuery.isEmpty()
                                    ? languageManager.getGuiColoredMessage(player,
                                    "gui.guild-list.bedrock-no-guilds", "&cNo guilds on the server yet")
                                    : languageManager.getGuiColoredMessage(player,
                                    "gui.guild-list.bedrock-no-results", "&cNo matching guilds found"))
                            .button(languageManager.getGuiColoredMessage(player,
                                    "gui.guild-list.bedrock-search", "&eSearch Guilds"))
                            .button(languageManager.getGuiColoredMessage(player,
                                    "gui.guild-list.bedrock-back-main", "&cBack to Main Menu"))
                            .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                                if (response.clickedButtonId() == 0) {
                                    sendBedrockSearchForm(player);
                                } else {
                                    openBackGui(player);
                                }
                            }))
                            .build();
                    BedrockFormSender.sendForm(player.getUniqueId(), emptyForm);
                    return;
                }

                int totalPagesLocal = GuiLayoutUtils.maxPageIndex(filtered.size(), GuiLayoutUtils.ITEMS_PER_PAGE);
                final int safePage = Math.max(0, Math.min(page, totalPagesLocal));

                int startIndex = safePage * GuiLayoutUtils.ITEMS_PER_PAGE;
                int endIndex = Math.min(startIndex + GuiLayoutUtils.ITEMS_PER_PAGE, filtered.size());

                StringBuilder content = new StringBuilder();
                content.append(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-page-info", "&fPage {page}/{total}",
                        "{page}", String.valueOf(safePage + 1),
                        "{total}", String.valueOf(totalPagesLocal + 1)));
                if (!searchQuery.isEmpty()) {
                    content.append("\n").append(languageManager.getGuiColoredMessage(player,
                            "gui.guild-list.bedrock-search-info", "&fSearch: &e{query}", "{query}", searchQuery));
                }

                SimpleForm.Builder builder = SimpleForm.builder()
                        .title(languageManager.getGuiColoredMessage(player,
                                "gui.guild-list.bedrock-title", "&6Guild List"))
                        .content(content.toString());

                List<Guild> pageGuilds = new ArrayList<>();
                for (int i = startIndex; i < endIndex; i++) {
                    Guild g = filtered.get(i);
                    pageGuilds.add(g);
                    String tagStr = g.getTag() != null ? " §f[" + g.getTag() + "]" : "";
                    builder.button("§e" + g.getName() + tagStr + " §f- Lv." + g.getLevel());
                }

                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-prev-page", "&aPrevious Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-next-page", "&aNext Page"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-search", "&eSearch Guilds"));
                builder.button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-back-main", "&cBack to Main Menu"));

                final int guildCount = pageGuilds.size();
                final int curPage = safePage;
                final int totPages = totalPagesLocal;

                builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    int id = response.clickedButtonId();
                    if (id < guildCount) {
                        sendBedrockGuildDetail(player, pageGuilds.get(id));
                    } else if (id == guildCount) {
                        sendBedrockGuildList(player, curPage > 0 ? curPage - 1 : curPage);
                    } else if (id == guildCount + 1) {
                        sendBedrockGuildList(player, curPage < totPages ? curPage + 1 : curPage);
                    } else if (id == guildCount + 2) {
                        sendBedrockSearchForm(player);
                    } else {
                        openBackGui(player);
                    }
                }));

                BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
            });
        });
    }

    private void sendBedrockSearchForm(Player player) {
        CustomForm form = CustomForm.builder()
                .title(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-search-title", "&6Search Guilds"))
                .input(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-search-input", "&fEnter search keyword"),
                        languageManager.getGuiColoredMessage(player,
                                "gui.guild-list.bedrock-search-placeholder", "Leave empty to show all"),
                        searchQuery)
                .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                    String query = response.getInput(0);
                    searchQuery = query != null ? query.trim() : "";
                    currentPage = 0;
                    sendBedrockGuildList(player, 0);
                }))
                .closedResultHandler(() -> CompatibleScheduler.runTask(plugin, player,
                        () -> sendBedrockGuildList(player, currentPage)))
                .build();
        BedrockFormSender.sendForm(player.getUniqueId(), form);
    }

    private void sendBedrockGuildDetail(Player player, Guild targetGuild) {
        String tagStr = targetGuild.getTag() != null ? " [" + targetGuild.getTag() + "]" : "";
        SimpleForm form = SimpleForm.builder()
                .title("§6" + targetGuild.getName() + tagStr)
                .content(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-select-action", "&fSelect action:"))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-view-detail", "&eView Details"))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-apply-join", "&aApply to Join"))
                .button(languageManager.getGuiColoredMessage(player,
                        "gui.guild-list.bedrock-back-list", "&cBack to List"))
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
}
