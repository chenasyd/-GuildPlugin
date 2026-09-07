package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.geyser.BedrockFormSender;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.base.AbstractPagedListGUI;
import com.guild.gui.base.GuiLayoutUtils;
import com.guild.models.Guild;
import com.guild.models.GuildLog;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.List;

/**
 * 公会日志 GUI：服务端分页（每页单独查询）+ 标准 7×4 布局。
 */
public class GuildLogsGUI extends AbstractPagedListGUI<GuildLog> {

    public static final String FUNC_PAGE_INFO = "PAGE_INFO";
    public static final String FUNC_REFRESH = "REFRESH";

    private static final int SLOT_PAGE_INFO = 46;
    private static final int TOOLBAR_REFRESH = 51;

    private final Guild guild;
    private int totalLogs = 0;
    private boolean isLoading = true;
    private boolean loadFailed = false;

    public GuildLogsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this(plugin, guild, player, 0);
    }

    public GuildLogsGUI(GuildPlugin plugin, Guild guild, Player player, int initialPage) {
        super(plugin, player, PaginationLayout.CENTER_BAR, GuiLayoutUtils.ITEMS_PER_PAGE);
        this.guild = guild;
        this.currentPage = Math.max(0, initialPage);
        loadLogs();
    }

    @Override
    public String getTitle() {
        String title = languageManager.getGuiMessage(viewer, "gui.guild-logs.title",
                "&6Guild Logs - {guild_name}");
        return ColorUtils.colorize(title.replace("{guild_name}", ColorUtils.stripColor(guild.getName())));
    }

    @Override
    protected void openBackGui(Player player) {
        plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild, player));
    }

    @Override
    protected int maxPageIndex() {
        return GuiLayoutUtils.maxPageIndex(totalLogs, itemsPerPage());
    }

    /** entries 仅为当前页切片，槽位映射不含页偏移 */
    @Override
    protected int listIndexFromSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > GuiLayoutUtils.PAGE_CONTENT_ROWS
                || col < 1 || col > GuiLayoutUtils.PAGE_CONTENT_COLS) {
            return -1;
        }
        return (row - 1) * GuiLayoutUtils.PAGE_CONTENT_COLS + (col - 1);
    }

    @Override
    protected void goNextPage(Player player) {
        if (currentPage < maxPageIndex()) {
            currentPage++;
            loadLogs();
        }
    }

    @Override
    protected void goPrevPage(Player player) {
        if (currentPage > 0) {
            currentPage--;
            loadLogs();
        }
    }

    @Override
    public void setupInventory(Inventory inventory) {
        if (shouldFillBorder()) {
            GuiLayoutUtils.fillBorder54(inventory);
        }
        setupToolbar(inventory);
        if (isLoading) {
            inventory.setItem(22, createItem(Material.SAND,
                    ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                            "gui.common.loading", "Loading...")),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                            "gui.relation-management.loading-data", "Loading data..."))));
        } else if (loadFailed) {
            inventory.setItem(22, createItem(Material.BARRIER,
                    ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                            "gui.guild-logs.load-failed", "Loading failed")),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                            "gui.guild-logs.load-error",
                            "Failed to load log data, please try again"))));
        } else if (entries.isEmpty()) {
            displayEmptyState(inventory);
        } else {
            displayEntries(inventory);
        }
        setupNavigationButtons(inventory);
        plugin.getGuiManager().applyImageModeIfNeeded(viewer, inventory, getGuiType());
    }

    @Override
    protected void displayEmptyState(Inventory inventory) {
        inventory.setItem(22, createItem(Material.BARRIER,
                ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.no-logs", "No log records")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.no-logs-desc", "This guild has no activity logs yet")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.wait-for-logs", "Logs appear when guild activity happens"))));
    }

    @Override
    protected void setupToolbar(Inventory inventory) {
        int totalPages = totalLogs <= 0 ? 1 : maxPageIndex() + 1;
        inventory.setItem(SLOT_PAGE_INFO, createItem(Material.PAPER,
                ColorUtils.colorize("&e" + languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.page-info", "Page Info")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.current-page", "Current page: {page}",
                        "{page}", String.valueOf(currentPage + 1))),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.total-pages", "Total pages: {total}",
                        "{total}", String.valueOf(totalPages))),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.total-records", "Total records: {count}",
                        "{count}", String.valueOf(totalLogs)))));

        inventory.setItem(slotForFunction(FUNC_REFRESH, TOOLBAR_REFRESH), createItem(Material.EMERALD,
                ColorUtils.colorize("&a" + languageManager.getGuiMessage(viewer,
                        "gui.common.refresh", "&aRefresh")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.refresh-logs", "Refresh log list"))));
    }

    @Override
    protected void setupNavigationButtons(Inventory inventory) {
        inventory.setItem(slotForFunction(FUNC_BACK, 49), createItem(Material.ARROW,
                ColorUtils.colorize("&c" + languageManager.getGuiMessage(viewer,
                        "gui.common.back", "Back")),
                ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.back-to-settings", "Return to Guild Settings"))));

        if (currentPage < maxPageIndex()) {
            inventory.setItem(slotForFunction(FUNC_NEXT_PAGE, 50), createItem(Material.ARROW,
                    ColorUtils.colorize("&a" + languageManager.getGuiMessage(viewer,
                            "gui.common.next-page", "&e&lNext Page")),
                    ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                            "gui.common.view-next", "View next page"))));
        }
    }

    @Override
    protected boolean handleToolbarClick(Player player, int slot, ClickType clickType) {
        if (slot == slotForFunction(FUNC_REFRESH, TOOLBAR_REFRESH)) {
            loadLogs();
            return true;
        }
        return false;
    }

    @Override
    protected void displayEntries(Inventory inventory) {
        for (int i = 0; i < Math.min(entries.size(), itemsPerPage()); i++) {
            inventory.setItem(slotForEntryIndex(i), createEntryItem(entries.get(i)));
        }
    }

    @Override
    protected void dispatchNavFunction(Player player, String func) {
        if (FUNC_REFRESH.equals(func)) {
            loadLogs();
            return;
        }
        super.dispatchNavFunction(player, func);
    }

    @Override
    protected ItemStack createEntryItem(GuildLog log) {
        Material material = getLogMaterial(log.getLogType());
        String name = ColorUtils.colorize("&e" + log.getLogType().getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-logs.operator", "Operator") + ": &f"
                + ColorUtils.stripColor(log.getPlayerName())));
        if (log.getPlayerUuid() != null && !log.getPlayerUuid().equals("SYSTEM")) {
            lore.add(ColorUtils.colorize("&8UUID: " + log.getPlayerUuid().substring(0, 8) + "..."));
        }
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-logs.time", "Time") + ": &f"
                + log.getSimpleTime(languageManager.getPlayerLanguage(viewer))));
        lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                "gui.guild-logs.description", "Description") + ": &f"
                + ColorUtils.stripColor(log.getDescription())));

        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            lore.add(ColorUtils.colorize("&7" + languageManager.getGuiMessage(viewer,
                    "gui.guild-logs.details", "Details") + ": &f"
                    + ColorUtils.stripColor(log.getDetails())));
        }

        if (log.getLogType() == GuildLog.LogType.FUND_DEPOSITED
                || log.getLogType() == GuildLog.LogType.FUND_WITHDRAWN
                || log.getLogType() == GuildLog.LogType.FUND_TRANSFERRED) {
            if ("SYSTEM".equals(log.getPlayerUuid())) {
                lore.add(ColorUtils.colorize(""));
                lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.system-operator",
                        "&c\u26A0 Operator is SYSTEM (may be legacy record)")));
            } else {
                lore.add(ColorUtils.colorize(""));
                lore.add(ColorUtils.colorize(languageManager.getGuiMessage(viewer,
                        "gui.guild-logs.real-operator", "&a\u2713 Real operator recorded")));
            }
        }

        return createItem(material, name, lore.toArray(new String[0]));
    }

    @Override
    protected void onEntrySelected(Player player, GuildLog log) {
        handleLogClick(player, log);
    }

    private void loadLogs() {
        if (guild.getId() <= 0) {
            plugin.getLogger().warning("Invalid guild ID: " + guild.getId());
            totalLogs = 0;
            setEntries(List.of());
            isLoading = false;
            loadFailed = true;
            refreshViewerIfOnline();
            return;
        }

        isLoading = true;
        loadFailed = false;
        refreshViewerIfOnline();

        plugin.getGuildService().getGuildLogsCountAsync(guild.getId())
                .thenCompose(count -> {
                    totalLogs = count;
                    int maxPage = maxPageIndex();
                    if (currentPage > maxPage) {
                        currentPage = maxPage;
                    }
                    int offset = currentPage * itemsPerPage();
                    return plugin.getGuildService()
                            .getGuildLogsAsync(guild.getId(), itemsPerPage(), offset);
                })
                .thenAccept(pageLogs -> CompatibleScheduler.runTask(plugin, viewer, () -> {
                    setEntries(pageLogs != null ? pageLogs : List.of());
                    isLoading = false;
                    loadFailed = false;
                    refreshViewerIfOnline();
                }))
                .exceptionally(e -> {
                    plugin.getLogger().severe("Error loading guild logs: " + e.getMessage());
                    CompatibleScheduler.runTask(plugin, viewer, () -> {
                        totalLogs = 0;
                        setEntries(List.of());
                        isLoading = false;
                        loadFailed = true;
                        refreshViewerIfOnline();
                    });
                    return null;
                });
    }

    private void refreshViewerIfOnline() {
        if (viewer.isOnline()) {
            plugin.getGuiManager().refreshGUI(viewer);
        }
    }

    private Material getLogMaterial(GuildLog.LogType logType) {
        return switch (logType) {
            case GUILD_CREATED -> Material.GREEN_WOOL;
            case GUILD_DISSOLVED -> Material.RED_WOOL;
            case MEMBER_JOINED -> Material.EMERALD;
            case MEMBER_LEFT, MEMBER_KICKED -> Material.REDSTONE;
            case MEMBER_PROMOTED -> Material.GOLD_INGOT;
            case MEMBER_DEMOTED -> Material.IRON_INGOT;
            case LEADER_TRANSFERRED -> Material.DIAMOND;
            case FUND_DEPOSITED -> Material.GOLD_NUGGET;
            case FUND_WITHDRAWN -> Material.IRON_NUGGET;
            case FUND_TRANSFERRED -> Material.EMERALD_BLOCK;
            case RELATION_CREATED, RELATION_ACCEPTED -> Material.BLUE_WOOL;
            case RELATION_DELETED, RELATION_REJECTED -> Material.ORANGE_WOOL;
            case GUILD_FROZEN -> Material.ICE;
            case GUILD_UNFROZEN -> Material.WATER_BUCKET;
            case GUILD_LEVEL_UP -> Material.EXPERIENCE_BOTTLE;
            case APPLICATION_SUBMITTED, APPLICATION_ACCEPTED, APPLICATION_REJECTED -> Material.PAPER;
            case INVITATION_SENT, INVITATION_ACCEPTED, INVITATION_REJECTED -> Material.BOOK;
            case WAREHOUSE_PERM_CHANGED -> Material.CHEST;
            default -> Material.GRAY_WOOL;
        };
    }

    private void handleLogClick(Player player, GuildLog log) {
        player.sendMessage(ColorUtils.colorize("&6" + languageManager.getGuiMessage(player,
                "gui.guild-logs.details-header", "=== Log Details ===")));
        player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                "gui.guild-logs.type", "Type") + ": &f" + log.getLogType().getDisplayName()));
        player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                "gui.guild-logs.operator", "Operator") + ": &f"
                + ColorUtils.stripColor(log.getPlayerName())));
        player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                "gui.guild-logs.time", "Time") + ": &f"
                + log.getSimpleTime(languageManager.getPlayerLanguage(player))));
        player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                "gui.guild-logs.description", "Description") + ": &f"
                + ColorUtils.stripColor(log.getDescription())));
        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&7" + languageManager.getGuiMessage(player,
                    "gui.guild-logs.details", "Details") + ": &f"
                    + ColorUtils.stripColor(log.getDetails())));
        }
        player.sendMessage(ColorUtils.colorize("&6" + languageManager.getGuiMessage(player,
                "gui.guild-logs.separator", "==================")));
    }

    @Override
    public boolean openBedrockForm(Player player) {
        if (!BedrockFormSender.isAvailable()) {
            return false;
        }
        sendBedrockLogsForm(player, currentPage);
        return true;
    }

    private void sendBedrockLogsForm(Player player, int pageNum) {
        plugin.getGuildService().getGuildLogsCountAsync(guild.getId()).thenAccept(count -> {
            int offset = pageNum * itemsPerPage();
            plugin.getGuildService().getGuildLogsAsync(guild.getId(), itemsPerPage(), offset)
                    .thenAccept(pageLogs -> CompatibleScheduler.runTask(plugin, player, () -> {
                        if (!player.isOnline()) {
                            return;
                        }

                        String guildName = ColorUtils.stripColor(guild.getName());

                        if (pageLogs == null || pageLogs.isEmpty()) {
                            SimpleForm emptyForm = SimpleForm.builder()
                                    .title(languageManager.getGuiColoredMessage(player,
                                            "gui.guild-logs.bedrock-title",
                                            "&6Guild Logs - {guild}", "{guild}", guildName))
                                    .content(languageManager.getGuiColoredMessage(player,
                                            "gui.guild-logs.bedrock-no-data", "&cNo log records"))
                                    .button(languageManager.getGuiColoredMessage(player,
                                            "gui.guild-logs.bedrock-back", "&cBack"))
                                    .validResultHandler(response -> CompatibleScheduler.runTask(plugin, player,
                                            () -> openBackGui(player)))
                                    .build();
                            BedrockFormSender.sendForm(player.getUniqueId(), emptyForm);
                            return;
                        }

                        int totalLogsLocal = count;
                        int totalPages = (totalLogsLocal - 1) / itemsPerPage() + 1;
                        final int safePage = Math.max(0, Math.min(pageNum, totalPages - 1));

                        String content = languageManager.getGuiColoredMessage(player,
                                "gui.guild-logs.bedrock-page-info",
                                "&fPage {page}/{total}\n&fTotal records: {count}",
                                "{page}", String.valueOf(safePage + 1),
                                "{total}", String.valueOf(totalPages),
                                "{count}", String.valueOf(totalLogsLocal));

                        SimpleForm.Builder builder = SimpleForm.builder()
                                .title(languageManager.getGuiColoredMessage(player,
                                        "gui.guild-logs.bedrock-title",
                                        "&6Guild Logs - {guild}", "{guild}", guildName))
                                .content(content);

                        for (GuildLog log : pageLogs) {
                            String time = log.getSimpleTime(languageManager.getPlayerLanguage(player));
                            builder.button("§e" + log.getLogType().getDisplayName()
                                    + " §f- " + ColorUtils.stripColor(log.getPlayerName())
                                    + " §f" + time);
                        }

                        builder.button(languageManager.getGuiColoredMessage(player,
                                "gui.guild-logs.bedrock-prev-page", "&aPrevious Page"));
                        builder.button(languageManager.getGuiColoredMessage(player,
                                "gui.guild-logs.bedrock-next-page", "&aNext Page"));
                        builder.button(languageManager.getGuiColoredMessage(player,
                                "gui.guild-logs.bedrock-refresh", "&aRefresh"));
                        builder.button(languageManager.getGuiColoredMessage(player,
                                "gui.guild-logs.bedrock-back", "&cBack"));

                        final int logCount = pageLogs.size();
                        final int curPage = safePage;
                        final int totPages = totalPages;
                        final List<GuildLog> finalLogs = pageLogs;

                        builder.validResultHandler(response -> CompatibleScheduler.runTask(plugin, player, () -> {
                            int id = response.clickedButtonId();
                            if (id < logCount) {
                                handleLogClick(player, finalLogs.get(id));
                                sendBedrockLogsForm(player, curPage);
                            } else if (id == logCount) {
                                sendBedrockLogsForm(player, curPage > 0 ? curPage - 1 : curPage);
                            } else if (id == logCount + 1) {
                                sendBedrockLogsForm(player, curPage < totPages - 1 ? curPage + 1 : curPage);
                            } else if (id == logCount + 2) {
                                sendBedrockLogsForm(player, curPage);
                            } else {
                                openBackGui(player);
                            }
                        }));

                        BedrockFormSender.sendForm(player.getUniqueId(), builder.build());
                    }));
        });
    }

    @Override
    public void refresh(Player player) {
        loadLogs();
    }
}
